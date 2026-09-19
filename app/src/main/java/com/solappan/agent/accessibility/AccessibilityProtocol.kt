package com.solappan.agent.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import java.util.UUID
import java.io.File

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
    const val EXTRA_DEADLINE = "deadline_elapsed"

    fun requestId(): String = UUID.randomUUID().toString()

    // Shared app-private cancellation lease. A queued IPC command cannot outlive its caller.
    fun commandLease(context: Context, requestId: String): File {
        require(UUID.fromString(requestId).toString() == requestId)
        return File(context.cacheDir, "accessibility-command-$requestId")
    }

    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, SolAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').mapNotNull(ComponentName::unflattenFromString).any { it == expected }
    }
}
