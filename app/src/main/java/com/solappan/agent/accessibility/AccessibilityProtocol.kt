package com.solappan.agent.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import java.util.UUID

object AccessibilityProtocol {
    const val COMMAND_ACTION = "com.solappan.agent.ACCESSIBILITY_COMMAND"
    const val RESULT_ACTION = "com.solappan.agent.ACCESSIBILITY_RESULT"
    const val PERMISSION = "com.solappan.agent.permission.INVOKE_ASSISTANT"
    const val EXTRA_REQUEST_ID = "request_id"
    const val EXTRA_COMMAND = "command"
    const val EXTRA_TARGET = "target"
    const val EXTRA_TEXT = "text"
    const val EXTRA_DIRECTION = "direction"
    const val EXTRA_SUCCESS = "success"
    const val EXTRA_MESSAGE = "message"
    const val EXTRA_ERROR = "error"
    const val EXTRA_DATA = "data"

    fun requestId(): String = UUID.randomUUID().toString()

    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, SolAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').mapNotNull(ComponentName::unflattenFromString).any { it == expected }
    }
}
