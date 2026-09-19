package com.solappan.agent.assistant

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.*
import android.service.voice.VoiceInteractionService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.solappan.agent.MainActivity
import com.solappan.agent.R
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.util.concurrent.Executors

/** Continuous offline wake recognition. No SpeechRecognizer restart loop or audio uploads. */
class SolWakeWordService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    // Native resources are owned exclusively by worker, including cleanup.
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private val pauseOwners = mutableSetOf<String>()
    private var modelReady = false
    private var generation = 0
    private var destroyed = false
    private var failed = false
    private var listening = false
    private var receiverRegistered = false
    private val launchTimeout = Runnable {
        if (!destroyed && LAUNCH_OWNER in pauseOwners) fail("SOL did not open. Check your default assistant in Setup and retry.")
    }
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (destroyed || failed) return
            val owner = intent?.getStringExtra(EXTRA_PAUSE_OWNER) ?: "assistant_session"
            when (intent?.action) {
                ACTION_PAUSE -> {
                    pauseOwners.add(owner)
                    if (owner == "assistant_session") {
                        pauseOwners.remove(LAUNCH_OWNER)
                        handler.removeCallbacks(launchTimeout)
                    }
                    val pending = if (isOrderedBroadcast) goAsync() else null
                    pauseAudio { pending?.finish() }
                }
                ACTION_RESUME -> { pauseOwners.remove(owner); startListening() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Hey SOL wake phrase", NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) },
        )
        try {
            val notification = notification("Preparing Hey SOL", "Loading the offline voice model")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTIFICATION_ID, notification)
        } catch (_: RuntimeException) {
            failed = true
            setEnabledPreference(false)
            stopSelf()
            return
        }
        setEnabledPreference(true)
        val filter = IntentFilter().apply { addAction(ACTION_PAUSE); addAction(ACTION_RESUME) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(controlReceiver, filter, PERMISSION, null, RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(controlReceiver, filter, PERMISSION, null)
        }
        receiverRegistered = true
        worker.execute {
            try {
                model = Model(OfflineWakeModel.install(applicationContext).absolutePath)
                handler.post { if (!destroyed && !failed) { modelReady = true; startListening() } }
            } catch (error: Exception) {
                Log.w(TAG, "Offline wake model unavailable", error)
                handler.post { fail("Offline voice model could not load. Enable Hey SOL again to retry.") }
            } catch (error: LinkageError) {
                Log.w(TAG, "Offline wake engine unavailable", error)
                handler.post { fail("Offline voice engine is unavailable on this device.") }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else if (!failed) {
            intent?.getStringExtra(EXTRA_PAUSE_OWNER)?.let { pauseOwners.add(it) }
            if (pauseOwners.isNotEmpty()) pauseAudio() else startListening()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        destroyed = true
        generation++
        listening = false
        handler.removeCallbacksAndMessages(null)
        if (receiverRegistered) runCatching { unregisterReceiver(controlReceiver) }
        setEnabledPreference(false)
        worker.execute { releaseAudio(); model?.close(); model = null }
        worker.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startListening() {
        if (destroyed || failed || listening || !modelReady) return
        if (pauseOwners.isNotEmpty()) { updateNotification("Hey SOL paused", "Microphone is in use by SOL"); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail("Microphone permission is required. Enable it in Setup."); return
        }
        if (!VoiceInteractionService.isActiveService(this, ComponentName(this, SolVoiceInteractionService::class.java))) {
            fail("Select SOL as your default digital assistant in Setup."); return
        }
        listening = true
        val token = ++generation
        worker.execute {
            try {
                releaseAudio()
                recognizer = Recognizer(checkNotNull(model), SAMPLE_RATE, WAKE_GRAMMAR)
                speechService = SpeechService(recognizer, SAMPLE_RATE)
                check(speechService!!.startListening(WakeListener(token)))
                handler.post { if (current(token)) updateNotification("Hey SOL is listening", "Offline · say Hey SOL to open your assistant") }
            } catch (error: Exception) {
                releaseAudio()
                Log.w(TAG, "Wake microphone unavailable", error)
                handler.post { if (current(token)) fail("Microphone unavailable. Close other recording apps and enable Hey SOL again.") }
            }
        }
    }

    private fun pauseAudio(onReleased: () -> Unit = {}) {
        generation++
        listening = false
        worker.execute { try { releaseAudio() } finally { onReleased() } }
        updateNotification("Hey SOL paused", "Microphone is in use by SOL")
    }
    private fun current(token: Int) = !destroyed && !failed && listening && generation == token

    private fun handleResult(token: Int, hypothesis: String) {
        if (!current(token)) return
        val candidate = runCatching { JSONObject(hypothesis).optString("text") }.getOrDefault("")
        // Exact phrase only: never wake from arbitrary conversation or partial hypotheses.
        if (!containsSolWakePhrase(candidate)) return
        pauseOwners.add(LAUNCH_OWNER)
        generation++
        listening = false
        val launchGeneration = generation
        updateNotification("Opening SOL", "Wake phrase recognized")
        handler.postDelayed(launchTimeout, 8_000)
        worker.execute {
            releaseAudio()
            handler.post {
                if (destroyed || failed || generation != launchGeneration || LAUNCH_OWNER !in pauseOwners) return@post
                sendBroadcast(Intent(SolVoiceInteractionService.ACTION_SHOW_ASSISTANT).setPackage(packageName), PERMISSION)
            }
        }
    }

    private inner class WakeListener(private val token: Int) : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) = Unit
        override fun onResult(hypothesis: String?) { handleResult(token, hypothesis.orEmpty()) }
        override fun onFinalResult(hypothesis: String?) { handleResult(token, hypothesis.orEmpty()) }
        override fun onError(exception: Exception?) { if (current(token)) fail("Offline listening stopped. Enable Hey SOL again to retry.") }
        override fun onTimeout() { if (current(token)) fail("Offline listening stopped. Enable Hey SOL again to retry.") }
    }
    private fun releaseAudio() {
        runCatching { speechService?.cancel() }
        runCatching { speechService?.shutdown() }
        speechService = null
        runCatching { recognizer?.close() }
        recognizer = null
    }
    private fun fail(message: String) {
        if (destroyed || failed) return
        failed = true
        generation++
        listening = false
        setEnabledPreference(false)
        updateNotification("Hey SOL stopped", message, false)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }
    private fun updateNotification(title: String, text: String, ongoing: Boolean = true) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification(title, text, ongoing))
    }
    private fun notification(title: String, text: String, ongoing: Boolean = true): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sol_tile).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setOngoing(ongoing).setSilent(true)
            .setContentIntent(PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), pendingFlags()))
            .addAction(0, "Stop", PendingIntent.getService(this, 2, Intent(this, SolWakeWordService::class.java).setAction(ACTION_STOP), pendingFlags()))
            .build()
    private fun pendingFlags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    private fun setEnabledPreference(enabled: Boolean) {
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
    companion object {
        const val ACTION_START = "com.solappan.agent.action.START_WAKE_WORD"
        const val ACTION_STOP = "com.solappan.agent.action.STOP_WAKE_WORD"
        const val ACTION_PAUSE = "com.solappan.agent.action.PAUSE_WAKE_WORD"
        const val ACTION_RESUME = "com.solappan.agent.action.RESUME_WAKE_WORD"
        const val EXTRA_PAUSE_OWNER = "pause_owner"
        const val PREFERENCES = "sol_wake_word"
        const val KEY_ENABLED = "enabled"
        private const val CHANNEL_ID = "sol_wake_word"
        private const val NOTIFICATION_ID = 904
        private const val PERMISSION = "com.solappan.agent.permission.INVOKE_ASSISTANT"
        private const val TAG = "SolWakeWord"
        private const val LAUNCH_OWNER = "wake_launch"
        private const val SAMPLE_RATE = 16_000f
        private const val WAKE_GRAMMAR = "[\"hey sol\",\"hello sol\",\"okay sol\",\"ok sol\",\"hey soul\",\"hello soul\",\"okay soul\",\"ok soul\",\"hey saul\",\"[unk]\"]"
    }
}
