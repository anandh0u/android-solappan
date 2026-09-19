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
import android.os.SystemClock
import android.graphics.Rect
import android.text.InputType
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class SolAccessibilityService : AccessibilityService() {
    private var observedWindow: Pair<Int, String>? = null
    private var observedAt = 0L
    private var observedIdentities = emptySet<String>()
    private var activeCommand: Intent? = null
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AccessibilityProtocol.COMMAND_ACTION) return
            val requestId = intent.getStringExtra(AccessibilityProtocol.EXTRA_REQUEST_ID) ?: return
            activeCommand = intent
            val result = runCatching {
                val deadline = intent.getLongExtra(AccessibilityProtocol.EXTRA_DEADLINE, 0L)
                if (deadline <= SystemClock.elapsedRealtime() ||
                    !AccessibilityProtocol.commandLease(this@SolAccessibilityService, requestId).exists()) {
                    ServiceResult(false, "Expired accessibility command was rejected.", "COMMAND_EXPIRED")
                } else execute(intent)
            }.getOrElse {
                ServiceResult(false, "Accessibility action failed safely.", "ACCESSIBILITY_ACTION_FAILED")
            }
            activeCommand = null
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
        val visibleNodes = screenNodes(root).filter { it.isVisibleToUser }
        for (node in visibleNodes) {
            val text = if (isPassword(node)) "" else node.text?.toString()?.trim().orEmpty()
            val description = if (isPassword(node)) "" else node.contentDescription?.toString()?.trim().orEmpty()
            val hint = if (isPassword(node)) "" else node.hintText?.toString()?.trim().orEmpty()
            if (text.isNotEmpty() || description.isNotEmpty() || node.isClickable || node.isScrollable || node.isEditable) {
                nodes.put(
                    JSONObject()
                        .put("text", text.take(MAX_TEXT_LENGTH))
                        .put("description", description.take(MAX_TEXT_LENGTH))
                        .put("hint", hint.take(MAX_TEXT_LENGTH))
                        .put("viewId", node.viewIdResourceName.orEmpty())
                        .put("class", node.className?.toString().orEmpty())
                        .put("clickable", node.isClickable)
                        .put("scrollable", node.isScrollable)
                        .put("editable", node.isEditable),
                )
            }
        }
        observedWindow = root.windowId to root.packageName.toString()
        observedAt = SystemClock.elapsedRealtime()
        observedIdentities = visibleNodes.filterNot(::isPassword).map(::identity).toSet()
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
        val root = controlledRoot()
            ?: return ServiceResult(false, "No active Android window is available.", "SCREEN_UNAVAILABLE")
        val matches = findNodes(root, target)
        if (matches.isEmpty()) return ServiceResult(false, "No visible element matched '$target'.", "ELEMENT_NOT_FOUND")
        if (matches.size > 1) return ServiceResult(false, "More than one visible element matched '$target'. Use a more specific target.", "ELEMENT_AMBIGUOUS")
        val node = matches.single()
        if (!matchesObservation(root, node)) return staleTarget()
        val clickable = generateSequence(node) { it.parent }.take(MAX_ANCESTOR_CHECKS)
            .firstOrNull { it.isClickable && it.isEnabled && it.isVisibleToUser }
            ?: return ServiceResult(false, "The matched element is not actionable.", "ELEMENT_NOT_ACTIONABLE")
        if (isPassword(node) || hasSensitiveIdentityThrough(node, clickable) ||
            screenNodes(clickable).any { isPassword(it) || hasSensitiveIdentity(it) }) {
            return ServiceResult(false, "That consequential UI action is blocked. Use a dedicated confirmed tool.", "SENSITIVE_ACTION_BLOCKED")
        }
        observedIdentities = emptySet()
        if (!commandIsLive()) return expiredCommand()
        return if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            ServiceResult(true, "Tapped the element matching '$target'.")
        } else ServiceResult(false, "Android rejected the tap action.", "ACCESSIBILITY_ACTION_FAILED")
    }

    private fun type(target: String, text: String): ServiceResult {
        if (target.isBlank() || text.isBlank()) return invalidTarget()
        if (isSensitiveAccessibilityTarget(target)) {
            return ServiceResult(false, "That sensitive field is blocked.", "SENSITIVE_ACTION_BLOCKED")
        }
        val root = controlledRoot()
            ?: return ServiceResult(false, "No active Android window is available.", "SCREEN_UNAVAILABLE")
        val matches = findNodes(root, target)
        if (matches.isEmpty()) return ServiceResult(false, "No visible field matched '$target'.", "ELEMENT_NOT_FOUND")
        if (matches.size > 1) return ServiceResult(false, "More than one visible field matched '$target'. Use a more specific target.", "ELEMENT_AMBIGUOUS")
        val node = matches.single()
        if (!matchesObservation(root, node)) return staleTarget()
        if (!node.isEditable || isPassword(node)) {
            return ServiceResult(false, "The matched element is not an allowed editable field.", "ELEMENT_NOT_EDITABLE")
        }
        if (generateSequence(node) { it.parent }.take(MAX_ANCESTOR_CHECKS).any(::hasSensitiveIdentity)) {
            return ServiceResult(false, "That sensitive field is blocked.", "SENSITIVE_ACTION_BLOCKED")
        }
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        observedIdentities = emptySet()
        if (!commandIsLive()) return expiredCommand()
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
        observedIdentities = emptySet()
        for (node in screenNodes(root)) {
            if (!commandIsLive()) return expiredCommand()
            if (node.isVisibleToUser && node.isEnabled && node.isScrollable && node.performAction(action)) {
                return ServiceResult(true, "Scrolled $direction in the active window.")
            }
        }
        return ServiceResult(false, "No scrollable element accepted the action.", "SCROLL_UNAVAILABLE")
    }

    private fun global(action: Int, message: String): ServiceResult {
        observedIdentities = emptySet()
        if (!commandIsLive()) return expiredCommand()
        return if (performGlobalAction(action)) ServiceResult(true, message)
        else ServiceResult(false, "Android rejected the global action.", "ACCESSIBILITY_ACTION_FAILED")
    }

    private fun findNodes(root: AccessibilityNodeInfo, target: String): List<AccessibilityNodeInfo> {
        val normalized = target.trim().lowercase(Locale.ROOT)
        val exact = mutableListOf<AccessibilityNodeInfo>()
        val partial = mutableListOf<AccessibilityNodeInfo>()
        for (node in screenNodes(root)) {
            if (!node.isVisibleToUser || !node.isEnabled || isPassword(node)) continue
            val values = listOf(node.text, node.contentDescription, node.hintText, node.viewIdResourceName)
                .mapNotNull { it?.toString()?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) }
            when {
                values.any { it == normalized } -> exact += node
                values.any { it.contains(normalized) } -> partial += node
            }
        }
        return (exact.ifEmpty { partial }).distinctBy { it.windowId to it.hashCode() }
    }

    private fun hasSensitiveIdentity(node: AccessibilityNodeInfo): Boolean =
        listOf(node.text, node.contentDescription, node.hintText, node.viewIdResourceName)
            .mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
            .any(::isSensitiveAccessibilityTarget)

    private fun hasSensitiveIdentityThrough(
        start: AccessibilityNodeInfo,
        inclusiveEnd: AccessibilityNodeInfo,
    ): Boolean {
        var node: AccessibilityNodeInfo? = start
        repeat(MAX_ANCESTOR_CHECKS) {
            val current = node ?: return false
            if (hasSensitiveIdentity(current)) return true
            if (current == inclusiveEnd) return false
            node = current.parent
        }
        return true
    }

    private fun invalidTarget() = ServiceResult(false, "A visible target and non-empty text are required.", "INVALID_PARAMETERS")

    private fun screenNodes(root: AccessibilityNodeInfo) = boundedScreenNodes(root) { node ->
        (0 until node.childCount.coerceAtMost(512)).mapNotNull(node::getChild)
    }

    private fun isPassword(node: AccessibilityNodeInfo): Boolean {
        val variation = node.inputType and (InputType.TYPE_MASK_CLASS or InputType.TYPE_MASK_VARIATION)
        return node.isPassword || variation in setOf(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
        )
    }

    private fun identity(node: AccessibilityNodeInfo): String {
        val bounds = Rect().also(node::getBoundsInScreen)
        return listOf(node.viewIdResourceName, node.className, node.text, node.contentDescription,
            node.hintText, bounds.toShortString(), node.isClickable, node.isEditable).joinToString("\u001f")
    }

    private fun matchesObservation(root: AccessibilityNodeInfo, node: AccessibilityNodeInfo): Boolean =
        isObservedTarget(observedWindow, root.windowId to root.packageName.toString(),
            SystemClock.elapsedRealtime() - observedAt, observedIdentities, identity(node))

    private fun staleTarget() = ServiceResult(false,
        "The target was not in the recent observation or changed. Observe again before requesting a new action.",
        "STALE_TARGET")

    private fun commandIsLive(): Boolean {
        val intent = activeCommand ?: return false
        val id = intent.getStringExtra(AccessibilityProtocol.EXTRA_REQUEST_ID) ?: return false
        return intent.getLongExtra(AccessibilityProtocol.EXTRA_DEADLINE, 0) > SystemClock.elapsedRealtime() &&
            AccessibilityProtocol.commandLease(this, id).exists()
    }

    private fun expiredCommand() = ServiceResult(false,
        "The action expired or was cancelled before dispatch.", "COMMAND_EXPIRED")

    private fun controlledRoot(): AccessibilityNodeInfo? {
        val applicationWindows = windows
            .filter {
                it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isActive || it.isFocused)
            }
            .sortedByDescending { (if (it.isActive) 2 else 0) + (if (it.isFocused) 1 else 0) }
        val selected = applicationWindows
            .mapNotNull { it.root }
            .firstOrNull { it.packageName?.toString() != packageName }
        if (selected == null) {
            val summary = windows.joinToString(limit = 8) { window ->
                "type=${window.type},active=${window.isActive},focused=${window.isFocused}"
            }
            Log.w(TAG, "No controllable window. Windows: $summary")
        }
        return selected
    }

    private data class ServiceResult(
        val success: Boolean,
        val message: String,
        val error: String? = null,
        val data: JSONObject? = null,
    )

    private companion object {
        const val MAX_TEXT_LENGTH = 160
        const val MAX_ANCESTOR_CHECKS = 12
        const val TAG = "SolAccessibility"
    }
}
