package com.solappan.agent

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.io.File

/** User-only temporary navigation consent, shared across SOL processes. */
class DemoAccess(private val context: Context) {
    private val file get() = File(context.noBackupFilesDir, "demo-navigation-consent")
    fun enable() { file.writeText("${boot()}:${SystemClock.elapsedRealtime() + DURATION}") }
    fun disable() { file.delete() }
    fun active(): Boolean = runCatching {
        val parts = file.readText().split(':')
        parts.size == 2 && boot() >= 0 && parts[0].toInt() == boot() &&
            validExpiry(parts[1].toLong(), SystemClock.elapsedRealtime())
    }.getOrDefault(false)
    fun permits(tool: String) = eligible(tool) && active()
    private fun boot() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    companion object {
        const val DURATION = 600_000L
        fun eligible(tool: String) = tool == "tap_element" || tool == "type_text"
        fun validExpiry(expiry: Long, now: Long) = expiry > now && expiry - now <= DURATION
    }
}
