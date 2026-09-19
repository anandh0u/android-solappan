package com.solappan.agent.assistant

import java.util.Locale

internal fun containsSolWakePhrase(candidate: String): Boolean {
    val normalized = candidate.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
    return SOL_WAKE_PHRASES.any { phrase ->
        normalized == phrase || normalized.startsWith("$phrase ") || normalized.endsWith(" $phrase")
    }
}

private val SOL_WAKE_PHRASES = setOf("hey sol", "hello sol", "okay sol", "ok sol")
