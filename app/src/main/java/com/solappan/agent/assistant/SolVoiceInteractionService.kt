package com.solappan.agent.assistant

import android.service.voice.VoiceInteractionService
import android.util.Log

class SolVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.i(TAG, "Solappan is the active voice interaction service")
    }

    companion object {
        private const val TAG = "SolVoiceInteraction"
    }
}
