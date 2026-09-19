package com.solappan.agent.accessibility

import android.annotation.SuppressLint
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.json.JSONArray
import org.json.JSONObject

class SolAccessibilityService : AccessibilityService() {
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AccessibilityProtocol.COMMAND_ACTION) return
            val requestId = intent.getStringExtra(AccessibilityProtocol.EXTRA_REQUEST_ID) ?: return
            val result = runCatching { execute(intent) }.getOrElse {
                ServiceResult(false, "Accessibility action failed safely.", "ACCESSIBILITY_ACTION_FAILED")
            }
            sendBroadcast(
                Intent(AccessibilityProtocol.RESULT_ACTION)
                    .setPackage(packageName)
                    .putExtra(AccessibilityProtocol.EXTRA_REQUEST_ID, requestId)
                    .putExtra(AccessibilityProtocol.EXTRA_SUCCESS, result.success)
                    .putExtra(AccessibilityProtocol.EXTRA_MESSAGE, result.message)
                    .putExtra(AccessibilityProtocol.EXTRA_ERROR, result.error)
                    .putExtra(AccessibilityProtocol.EXTRA_DATA, result.data?.toString()),
                AccessibilityProtocol.PERMISSION,
            )
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val filter = IntentFilter(AccessibilityProtocol.COMMAND_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                commandReceiver,
                filter,
                AccessibilityProtocol.PERMISSION,
                null,
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(commandReceiver, filter, AccessibilityProtocol.PERMISSION, null)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(commandReceiver) }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private fun execute(intent: Intent): ServiceResult = when (intent.getStringExtra(AccessibilityProtocol.EXTRA_COMMAND)) {
        "observe" -> observe()
        "tap" -> tap(intent.getStringExtra(AccessibilityProtocol.EXTRA_TARGET).orEmpty())
        "type" -> type(
            intent.getStringExtra(AccessibilityProtocol.EXTRA_TARGET).orEmpty(),
            intent.getStringExtra(AccessibilityProtocol.EXTRA_TEXT).orEmpty(),
        )
        "scroll" -> scroll(intent.getStringExtra(AccessibilityProtocol.EXTRA_DIRECTION).orEmpty())
        "back" -> global(GLOBAL_ACTION_BACK, "Pressed Back.")
        "home" -> global(GLOBAL_ACTION_HOME, "Pressed Home.")
        else -> ServiceResult(false, "Unknown accessibility command was rejected.", "INVALID_PARAMETERS")
    }

    private fun observe(): ServiceResult {
        val root = controlledRoot()
            ?: return ServiceResult(false, "No active Android window is available.", "SCREEN_UNAVAILABLE")
        val nodes = JSONArray()
        var visited = 0
        fun collect(node: AccessibilityNodeInfo, depth: Int) {
            if (visited >= MAX_NODES || depth > MAX_DEPTH) return
            visited++
            val text = if (node.isPassword) "" else node.text?.toString()?.trim().orEmpty()
            val description = if (node.isPassword) "" else node.contentDescription?.toString()?.trim().orEmpty()
            if (text.isNotEmpty() || description.isNotEmpty() || node.isClickable || node.isScrollable || node.isEditable) {
                nodes.put(
                    JSONObject()
                        .put("text", text.take(MAX_TEXT_LENGTH))
                        .put("description", description.take(MAX_TEXT_LENGTH))
                        .put("viewId", node.viewIdResourceName.orEmpty())
                        .put("clickable", node.isClickable)
                        .put("scrollable", node.isScrollable)
                        .put("editable", node.isEditable),
                )
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let { collect(it, depth + 1) }
        }
        collect(root, 0)
        return ServiceResult(
            true,
            "Observed the active Android window.",
            data = JSONObject().put("package", root.packageName?.toString().orEmpty()).put("nodes", nodes),
        )
    }

    private fun tap(target: String): ServiceResult {
        if (target.isBlank()) return invalidTarget()
        if (isSensitiveAccessibilityTarget(target)) {
            return ServiceResult(false, "That consequential UI action is blocked. Use a dedicated confirmed tool.", "SENSITIVE_ACTION_BLOCKED")
        }
        val node = findNode(target) ?: return ServiceResult(false, "No visible element matched '$target'.", "ELEMENT_NOT_FOUND")
        val clickable = generateSequence(node) { it.parent }.firstOrNull(AccessibilityNodeInfo::isClickable)
            ?: return ServiceResult(false, "The matched element is not actionable.", "ELEMENT_NOT_ACTIONABLE")
        return if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            ServiceResult(true, "Tapped the element matching '$target'.")
        } else ServiceResult(false, "Android rejected the tap action.", "ACCESSIBILITY_ACTION_FAILED")
    }

    private fun type(target: String, text: String): ServiceResult {
        if (target.isBlank() || text.isBlank()) return invalidTarget()
        val node = findNode(target) ?: return ServiceResult(false, "No visible field matched '$target'.", "ELEMENT_NOT_FOUND")
        if (!node.isEditable || node.isPassword) {
            return ServiceResult(false, "The matched element is not an allowed editable field.", "ELEMENT_NOT_EDITABLE")
        }
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
            ServiceResult(true, "Entered text in the field matching '$target'.")
        } else ServiceResult(false, "Android rejected the text action.", "ACCESSIBILITY_ACTION_FAILED")
    }

    private fun scroll(direction: String): ServiceResult {
        val action = when (direction) {
            "forward", "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "backward", "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> return ServiceResult(false, "Unsupported scroll direction.", "INVALID_PARAMETERS")
        }
        val root = controlledRoot()
            ?: return ServiceResult(false, "No active Android window is available.", "SCREEN_UNAVAILABLE")
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited++ < MAX_NODES) {
            val node = queue.removeFirst()
            if (node.isScrollable && node.performAction(action)) {
                return ServiceResult(true, "Scrolled $direction in the active window.")
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        return ServiceResult(false, "No scrollable element accepted the action.", "SCROLL_UNAVAILABLE")
    }

    private fun global(action: Int, message: String): ServiceResult =
        if (performGlobalAction(action)) ServiceResult(true, message)
        else ServiceResult(false, "Android rejected the global action.", "ACCESSIBILITY_ACTION_FAILED")

    private fun findNode(target: String): AccessibilityNodeInfo? {
        val normalized = target.trim().lowercase()
        val root = controlledRoot() ?: return null
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited++ < MAX_NODES) {
            val node = queue.removeFirst()
            val values = listOf(node.text, node.contentDescription, node.viewIdResourceName)
                .mapNotNull { it?.toString()?.trim()?.lowercase()?.takeIf(String::isNotEmpty) }
            if (values.any { it == normalized || it.contains(normalized) }) return node
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        return null
    }

    private fun invalidTarget() = ServiceResult(false, "A visible target and non-empty text are required.", "INVALID_PARAMETERS")

    private fun controlledRoot(): AccessibilityNodeInfo? = windows
        .asSequence()
        .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        .mapNotNull { it.root }
        .firstOrNull { it.packageName?.toString() != packageName }
        ?: rootInActiveWindow?.takeIf { it.packageName?.toString() != packageName }

    private data class ServiceResult(
        val success: Boolean,
        val message: String,
        val error: String? = null,
        val data: JSONObject? = null,
    )

    private companion object {
        const val MAX_NODES = 80
        const val MAX_DEPTH = 32
        const val MAX_TEXT_LENGTH = 160
    }
}
