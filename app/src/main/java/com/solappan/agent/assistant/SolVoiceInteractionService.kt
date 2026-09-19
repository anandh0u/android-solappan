package com.solappan.agent.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log

class SolVoiceInteractionService : VoiceInteractionService() {
    private var receiverRegistered = false
    private var lastInvocationAt = -2_000L
    private val showAssistantReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SHOW_ASSISTANT) {
                if (!isActiveService(this@SolVoiceInteractionService,
                        ComponentName(this@SolVoiceInteractionService, SolVoiceInteractionService::class.java))) return
                val now = SystemClock.elapsedRealtime()
                if (now - lastInvocationAt < 1_500) return
                lastInvocationAt = now
                runCatching { showSession(
                    Bundle.EMPTY,
                    VoiceInteractionSession.SHOW_SOURCE_APPLICATION or
                        VoiceInteractionSession.SHOW_WITH_ASSIST or
                        VoiceInteractionSession.SHOW_WITH_SCREENSHOT,
                ) }.onFailure { Log.w(TAG, "Android could not show the assistant session", it) }
            }
        }
    }

    override fun onReady() {
        super.onReady()
        if (receiverRegistered) return
        val filter = IntentFilter(ACTION_SHOW_ASSISTANT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(showAssistantReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(showAssistantReceiver, filter, "com.solappan.agent.permission.INVOKE_ASSISTANT", null)
        }
        receiverRegistered = true
        Log.i(TAG, "Solappan is the active voice interaction service")
    }

    override fun onShutdown() {
        unregisterInvocationReceiver()
        super.onShutdown()
    }

    override fun onDestroy() {
        unregisterInvocationReceiver()
        super.onDestroy()
    }

    private fun unregisterInvocationReceiver() {
        if (receiverRegistered) runCatching { unregisterReceiver(showAssistantReceiver) }
        receiverRegistered = false
    }

    companion object {
        const val ACTION_SHOW_ASSISTANT = "com.solappan.agent.action.SHOW_ASSISTANT"
        private const val TAG = "SolVoiceInteraction"
    }
}
