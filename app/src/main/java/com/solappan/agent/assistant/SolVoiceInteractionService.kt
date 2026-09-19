package com.solappan.agent.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log

class SolVoiceInteractionService : VoiceInteractionService() {
    private val showAssistantReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SHOW_ASSISTANT) {
                showSession(
                    Bundle.EMPTY,
                    VoiceInteractionSession.SHOW_SOURCE_APPLICATION or
                        VoiceInteractionSession.SHOW_WITH_ASSIST or
                        VoiceInteractionSession.SHOW_WITH_SCREENSHOT,
                )
            }
        }
    }

    override fun onReady() {
        super.onReady()
        val filter = IntentFilter(ACTION_SHOW_ASSISTANT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(showAssistantReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(showAssistantReceiver, filter)
        }
        Log.i(TAG, "Solappan is the active voice interaction service")
    }

    override fun onShutdown() {
        runCatching { unregisterReceiver(showAssistantReceiver) }
        super.onShutdown()
    }

    companion object {
        const val ACTION_SHOW_ASSISTANT = "com.solappan.agent.action.SHOW_ASSISTANT"
        private const val TAG = "SolVoiceInteraction"
    }
}
