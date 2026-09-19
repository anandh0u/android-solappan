package com.solappan.agent.accessibility

import java.util.Locale

internal fun isSensitiveAccessibilityTarget(target: String): Boolean {
    val normalized = target.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase(Locale.ROOT)
    val words = normalized.split(Regex("[^\\p{L}\\p{N}]+"))
    return words.any { it in SENSITIVE_TARGET_TERMS }
}

private val SENSITIVE_TARGET_TERMS = setOf(
    "allow", "approve", "buy", "call", "confirm", "delete", "install",
    "pay", "purchase", "send", "uninstall", "permission", "password", "checkout",
    "transfer", "subscribe", "erase", "reset", "submit", "otp", "pin", "cvv",
)

/** One traversal order/budget shared by observation and action resolution. */
internal fun <T> boundedScreenNodes(root: T, children: (T) -> List<T>): List<T> {
    val queue = ArrayDeque<Pair<T, Int>>().apply { add(root to 0) }
    val result = mutableListOf<T>()
    while (queue.isNotEmpty() && result.size < 512) {
        val (node, depth) = queue.removeFirst()
        result += node
        if (depth < 32) children(node).take(512 - result.size - queue.size).forEach {
            queue.addLast(it to depth + 1)
        }
    }
    return result
}

internal fun isObservedTarget(
    observedWindow: Pair<Int, String>?, currentWindow: Pair<Int, String>,
    ageMs: Long, observedIdentities: Set<String>, currentIdentity: String,
): Boolean = observedWindow == currentWindow && ageMs in 0 until 120_000 &&
    currentIdentity in observedIdentities
