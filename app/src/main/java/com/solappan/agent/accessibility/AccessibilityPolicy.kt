package com.solappan.agent.accessibility

import java.util.Locale

internal fun isSensitiveAccessibilityTarget(target: String): Boolean {
    val normalized = target.trim().lowercase(Locale.ROOT)
    return SENSITIVE_TARGET_TERMS.any { term ->
        normalized == term || normalized.startsWith("$term ") || normalized.endsWith(" $term")
    }
}

private val SENSITIVE_TARGET_TERMS = setOf(
    "allow", "approve", "buy", "call", "confirm", "delete", "install",
    "pay", "purchase", "send", "uninstall", "permission", "password",
)
