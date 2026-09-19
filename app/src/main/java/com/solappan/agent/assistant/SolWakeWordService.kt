package com.solappan.agent.assistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.solappan.agent.MainActivity
import com.solappan.agent.R

class SolWakeWordService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var pausedForAssistant = false
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE -> pauseForAssistant()
                ACTION_RESUME -> {
                    pausedForAssistant = false
                    handler.removeCallbacksAndMessages(null)
                    startListening()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val stopIntent = Intent(this, SolWakeWordService::class.java).setAction(ACTION_STOP)
        val openIntent = Intent(this, MainActivity::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sol_tile)
            .setContentTitle("Hey SOL is listening")
            .setContentText("Say “Hey SOL” to open your assistant")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(PendingIntent.getActivity(this, 1, openIntent, pendingFlags()))
            .addAction(0, "Stop", PendingIntent.getService(this, 2, stopIntent, pendingFlags()))
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        setEnabledPreference(true)
        val controlFilter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, controlFilter, PERMISSION, null, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(controlReceiver, controlFilter, PERMISSION, null)
        }
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!listening && !pausedForAssistant) startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        listening = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        runCatching { unregisterReceiver(controlReceiver) }
        setEnabledPreference(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startListening() {
        if (pausedForAssistant || listening) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            !SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Wake listening unavailable")
            stopSelf()
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(WakeRecognitionListener())
            }
        }
        listening = true
        recognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_200L)
            },
        )
    }

    private fun scheduleRestart(delayMs: Long = RESTART_DELAY_MS) {
        listening = false
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(::startListening, delayMs)
    }

    private fun handleCandidates(candidates: List<String>): Boolean {
        if (candidates.none(::containsSolWakePhrase)) return false
        pauseForAssistant()
        handler.postDelayed({
            sendBroadcast(Intent(SolVoiceInteractionService.ACTION_SHOW_ASSISTANT).setPackage(packageName))
        }, ASSISTANT_LAUNCH_DELAY_MS)
        handler.postDelayed({
            pausedForAssistant = false
            startListening()
        }, ASSISTANT_SAFETY_RESUME_MS)
        return true
    }

    private fun pauseForAssistant() {
        pausedForAssistant = true
        listening = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
    }

    private inner class WakeRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit

        override fun onError(error: Int) {
            if (!pausedForAssistant) scheduleRestart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1_000L else RESTART_DELAY_MS)
        }

        override fun onResults(results: android.os.Bundle?) {
            val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (!handleCandidates(candidates)) scheduleRestart()
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) {
            val candidates = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            handleCandidates(candidates)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Hey SOL wake phrase", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Required while optional Hey SOL listening is active"
                    setSound(null, null)
                },
            )
        }
    }

    private fun pendingFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun setEnabledPreference(enabled: Boolean) {
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        const val ACTION_START = "com.solappan.agent.action.START_WAKE_WORD"
        const val ACTION_STOP = "com.solappan.agent.action.STOP_WAKE_WORD"
        const val ACTION_PAUSE = "com.solappan.agent.action.PAUSE_WAKE_WORD"
        const val ACTION_RESUME = "com.solappan.agent.action.RESUME_WAKE_WORD"
        const val PREFERENCES = "sol_wake_word"
        const val KEY_ENABLED = "enabled"
        private const val CHANNEL_ID = "sol_wake_word"
        private const val NOTIFICATION_ID = 904
        private const val RESTART_DELAY_MS = 350L
        private const val ASSISTANT_LAUNCH_DELAY_MS = 250L
        private const val ASSISTANT_SAFETY_RESUME_MS = 90_000L
        private const val PERMISSION = "com.solappan.agent.permission.INVOKE_ASSISTANT"
        private const val TAG = "SolWakeWord"
    }
}
