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
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.solappan.agent.AgentController
import com.solappan.agent.AgentRuntimeCoordinator
import com.solappan.agent.AgentRuntimeUiState
import com.solappan.agent.AgentState
import com.solappan.agent.TimelineEntry
import com.solappan.agent.TimelineStatus
import com.solappan.agent.tools.ToolConfirmation
import com.solappan.agent.tools.ToolRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SolVoiceInteractionSession(private val sessionContext: Context) : VoiceInteractionSession(sessionContext) {
    private lateinit var statusText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var timelineContainer: LinearLayout
    private lateinit var confirmationContainer: LinearLayout
    private lateinit var confirmationSummary: TextView
    private lateinit var confirmationReview: CheckBox
    private lateinit var cancelButton: Button
    private lateinit var approveButton: Button
    private lateinit var retryButton: Button
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false
    private var uiVisible = false
    private var pendingConfirmation: CompletableDeferred<Boolean>? = null
    private var confirmationArmJob: Job? = null
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val runtime = AgentRuntimeCoordinator(
        AgentController(registry = ToolRegistry.sessionThree(sessionContext.applicationContext)),
    )

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
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.rgb(183, 174, 195))
        }
        panel.addView(transcriptText.withTopMargin(6))

        timelineContainer = LinearLayout(sessionContext).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        panel.addView(
            timelineContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(12) },
        )

        confirmationContainer = LinearLayout(sessionContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground(Color.rgb(48, 36, 55), 16)
            visibility = View.GONE
        }
        confirmationContainer.addView(TextView(sessionContext).apply {
            text = "APPROVAL REQUIRED"
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.rgb(255, 155, 84))
        })
        confirmationSummary = TextView(sessionContext).apply {
            textSize = 15f
            setTextColor(Color.rgb(243, 238, 247))
        }
        confirmationContainer.addView(confirmationSummary.withTopMargin(8))
        confirmationReview = CheckBox(sessionContext).apply {
            text = "I reviewed this action"
            textSize = 14f
            setTextColor(Color.rgb(243, 238, 247))
            isEnabled = false
            setOnCheckedChangeListener { _, checked ->
                approveButton.isEnabled = isEnabled && checked
            }
        }
        confirmationContainer.addView(confirmationReview.withTopMargin(8))
        val confirmationActions = LinearLayout(sessionContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        cancelButton = Button(sessionContext).apply {
            text = "Cancel"
            isAllCaps = false
            isEnabled = false
            setOnClickListener { resolveConfirmation(false) }
        }
        confirmationActions.addView(cancelButton)
        approveButton = Button(sessionContext).apply {
            text = "Approve action"
            isAllCaps = false
            isEnabled = false
            setOnClickListener { resolveConfirmation(true) }
        }
        confirmationActions.addView(approveButton)
        confirmationContainer.addView(
            confirmationActions,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        panel.addView(
            confirmationContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(12) },
        )

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
        uiVisible = true
        Log.i(TAG, "Assistant session shown")
        startListening()
    }

    override fun onHide() {
        uiVisible = false
        stopListening()
        resolveConfirmation(false)
        Log.i(TAG, "Assistant session hidden")
        super.onHide()
    }

    override fun onDestroy() {
        resolveConfirmation(false)
        confirmationArmJob?.cancel()
        sessionScope.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    private fun startListening() {
        if (!::statusText.isInitialized) return
        resolveConfirmation(false)
        retryButton.visibility = View.GONE
        timelineContainer.removeAllViews()
        timelineContainer.visibility = View.GONE

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
        timelineContainer.visibility = View.GONE
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
                retryButton.visibility = View.GONE
                submitTranscript(transcript)
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

    private fun submitTranscript(transcript: String) {
        statusText.text = "Understanding…"
        sessionScope.launch {
            withContext(Dispatchers.IO) {
                runtime.run(
                    goal = transcript,
                    onState = { state ->
                        withContext(Dispatchers.Main) { renderRuntimeState(state, transcript) }
                    },
                    requestConfirmation = { request -> requestAssistantConfirmation(request) },
                )
            }
        }
    }

    private suspend fun requestAssistantConfirmation(request: ToolConfirmation): Boolean =
        withContext(Dispatchers.Main) {
            pendingConfirmation?.complete(false)
            val decision = CompletableDeferred<Boolean>()
            pendingConfirmation = decision
            confirmationArmJob?.cancel()
            confirmationSummary.text = "${request.summary}\nRisk: ${request.riskLevel.name}"
            confirmationReview.isChecked = false
            confirmationReview.isEnabled = false
            cancelButton.isEnabled = false
            approveButton.isEnabled = false
            confirmationContainer.visibility = View.VISIBLE
            statusText.text = "Approval required"
            confirmationArmJob = sessionScope.launch {
                delay(CONFIRMATION_ARM_DELAY_MS)
                if (pendingConfirmation === decision && uiVisible) {
                    confirmationReview.isEnabled = true
                    cancelButton.isEnabled = true
                }
            }
            decision.await()
        }

    private fun resolveConfirmation(approved: Boolean) {
        val decision = pendingConfirmation ?: return
        if (approved && (!confirmationReview.isEnabled || !confirmationReview.isChecked)) return
        pendingConfirmation = null
        confirmationArmJob?.cancel()
        confirmationArmJob = null
        confirmationContainer.visibility = View.GONE
        confirmationReview.isChecked = false
        decision.complete(approved)
    }

    private fun renderRuntimeState(state: AgentRuntimeUiState, transcript: String) {
        if (!uiVisible) return
        statusText.text = when (state.agentState) {
            AgentState.IDLE -> "Ready"
            AgentState.THINKING -> "Understanding…"
            AgentState.PLANNING -> "Planning…"
            AgentState.EXECUTING -> "Working…"
            AgentState.WAITING_FOR_CONFIRMATION -> "Approval required"
            AgentState.VERIFYING -> "Checking result…"
            AgentState.COMPLETED -> "Done"
            AgentState.FAILED -> "Couldn't complete"
            AgentState.CANCELLED -> "Cancelled"
        }
        transcriptText.text = when {
            state.error != null -> state.error
            state.response.isNotBlank() -> state.response
            else -> "“$transcript”"
        }
        renderTimeline(state.timeline)
        if (!state.loading) {
            if (state.error != null) {
                retryButton.visibility = View.VISIBLE
            } else {
                sessionScope.launch {
                    delay(1_500)
                    if (uiVisible) finish()
                }
            }
        }
    }

    private fun renderTimeline(entries: List<TimelineEntry>) {
        timelineContainer.removeAllViews()
        timelineContainer.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        entries.takeLast(MAX_VISIBLE_TIMELINE_ITEMS).forEach { entry ->
            val marker = when (entry.status) {
                TimelineStatus.RUNNING -> "●"
                TimelineStatus.SUCCESS -> "✓"
                TimelineStatus.FAILED -> "!"
                TimelineStatus.CANCELLED -> "×"
            }
            val color = when (entry.status) {
                TimelineStatus.RUNNING -> Color.rgb(255, 155, 84)
                TimelineStatus.SUCCESS -> Color.rgb(117, 216, 183)
                TimelineStatus.FAILED, TimelineStatus.CANCELLED -> Color.rgb(255, 113, 106)
            }
            timelineContainer.addView(TextView(sessionContext).apply {
                text = "$marker  ${entry.toolName.toDisplayName()} — ${entry.message}"
                textSize = 13f
                setTextColor(color)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(12), dp(9), dp(12), dp(9))
                background = roundedBackground(Color.rgb(37, 30, 53), 12)
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(6) })
        }
    }

    private fun String.toDisplayName(): String =
        split('_').joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }

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
        private const val MAX_VISIBLE_TIMELINE_ITEMS = 4
        private const val CONFIRMATION_ARM_DELAY_MS = 750L
    }
}
