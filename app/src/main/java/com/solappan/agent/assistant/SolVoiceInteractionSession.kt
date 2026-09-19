package com.solappan.agent.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class SolVoiceInteractionSession(private val sessionContext: Context) : VoiceInteractionSession(sessionContext) {
    private lateinit var statusText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var retryButton: Button
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    override fun onCreateContentView(): View {
        val root = FrameLayout(sessionContext).apply {
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }

        val panel = LinearLayout(sessionContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            elevation = dp(12).toFloat()
            background = roundedBackground(Color.rgb(26, 21, 39), 28)
        }
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

        panel.addView(TextView(sessionContext).apply {
            text = "✦"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(33, 17, 11))
            background = roundedBackground(Color.rgb(255, 155, 84), 22)
        }, LinearLayout.LayoutParams(dp(64), dp(64)))

        panel.addView(TextView(sessionContext).apply {
            text = "SOL  /  ANDROID AGENT"
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(255, 155, 84))
        }.withTopMargin(14))

        statusText = TextView(sessionContext).apply {
            text = "Ready"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(243, 238, 247))
        }
        panel.addView(statusText.withTopMargin(8))

        transcriptText = TextView(sessionContext).apply {
            text = "Invoke SOL and speak naturally"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(183, 174, 195))
        }
        panel.addView(transcriptText.withTopMargin(6))

        retryButton = Button(sessionContext).apply {
            text = "Retry listening"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { startListening() }
        }
        panel.addView(retryButton.withTopMargin(12))

        panel.addView(Button(sessionContext).apply {
            text = "Dismiss"
            isAllCaps = false
            setOnClickListener { finish() }
        }.withTopMargin(14))

        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i(TAG, "Assistant session shown")
        startListening()
    }

    override fun onHide() {
        stopListening()
        Log.i(TAG, "Assistant session hidden")
        super.onHide()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    private fun startListening() {
        if (!::statusText.isInitialized) return
        retryButton.visibility = View.GONE

        if (sessionContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showSpeechError("Microphone permission required", "Open Solappan and enable the assistant microphone.")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(sessionContext)) {
            showSpeechError("Speech unavailable", "No Android speech recognizer is available on this device.")
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(sessionContext).apply {
                setRecognitionListener(SessionRecognitionListener())
            }
        }
        statusText.text = "Listening…"
        transcriptText.text = "Speak your request"
        listening = true
        speechRecognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            },
        )
    }

    private fun stopListening() {
        if (listening) speechRecognizer?.cancel()
        listening = false
    }

    private fun showSpeechError(title: String, detail: String) {
        listening = false
        statusText.text = title
        transcriptText.text = detail
        retryButton.visibility = View.VISIBLE
    }

    private inner class SessionRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            statusText.text = "Listening…"
        }

        override fun onBeginningOfSpeech() {
            statusText.text = "Listening…"
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            statusText.text = "Understanding…"
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't understand that."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition needs a working network connection."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The speech recognizer is busy."
                else -> "Speech recognition failed safely."
            }
            showSpeechError("Try again", message)
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val transcript = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            if (transcript.isNullOrEmpty()) {
                showSpeechError("Try again", "No speech was recognized.")
            } else {
                statusText.text = "Heard you"
                transcriptText.text = "“$transcript”"
                retryButton.visibility = View.VISIBLE
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { transcriptText.text = "“$it”" }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun TextView.withTopMargin(margin: Int): TextView = apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(margin) }
    }

    private fun roundedBackground(color: Int, radiusDp: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
    }

    private fun dp(value: Int): Int =
        (value * sessionContext.resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "SolAssistantSession"
    }
}
