package com.solappan.agent.tools

import java.util.Locale

internal fun matchingAppPackages(candidates: List<Pair<String, String>>, query: String): List<String> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    if (normalized.isEmpty()) return emptyList()
    val exact = candidates.filter { it.first.lowercase(Locale.ROOT) == normalized }
    return exact.ifEmpty { candidates.filter { it.first.lowercase(Locale.ROOT).contains(normalized) } }
        .map { it.second }.distinct()
}
