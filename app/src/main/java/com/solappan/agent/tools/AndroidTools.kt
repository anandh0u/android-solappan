package com.solappan.agent.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Telephony
import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal abstract class ContextTool(protected val context: Context) : AgentTool {
    protected fun launch(intent: Intent): ToolResult = try {
        if (Thread.currentThread().isInterrupted) throw InterruptedException()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) {
            ToolResult.failure("No compatible Android application is installed.", "INTENT_UNAVAILABLE")
        } else {
            context.startActivity(intent)
            ToolResult(success = true, message = "Android accepted the action.")
        }
    } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
        throw interrupted
    } catch (_: Exception) {
        ToolResult.failure("Android could not open the requested action.", "INTENT_FAILED")
    }
}

internal class OpenAppTool(context: Context) : ContextTool(context) {
    override val name = "open_app"
    override val description = "Open an installed Android application by its user-facing name."
    override val parameters = objectSchema("appName", "The visible application name, such as Spotify")
    override val riskLevel = RiskLevel.LOW
    override val requiresConfirmation = false

    override fun execute(arguments: JSONObject): ToolResult {
        val appName = requiredString(arguments, "appName") ?: return invalid("appName")
        val packageManager = context.packageManager
        val normalized = appName.lowercase(Locale.ROOT).trim()
        val knownPackage = KNOWN_APPS[normalized]
        val knownIntent = knownPackage?.let(packageManager::getLaunchIntentForPackage)
        val candidates = if (knownIntent == null) packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.MATCH_ALL,
            ).map { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT) to it.activityInfo.packageName }
            else emptyList()
        val exact = candidates.filter { it.first == normalized }
        val matchingPackages = exact.ifEmpty { candidates.filter { it.first.contains(normalized) } }
            .map { it.second }.distinct()
        if (knownIntent == null && matchingPackages.size > 1) {
            return ToolResult.failure("More than one app matched '$appName'. Use its full app name.", "APP_AMBIGUOUS")
        }
        val launchIntent = knownIntent
            ?: matchingPackages.singleOrNull()?.let(packageManager::getLaunchIntentForPackage)
            ?: return ToolResult.failure("Application '$appName' was not found.", "APP_NOT_FOUND")

        return launch(launchIntent).withSuccessDetails(
            message = "Opened $appName.",
            data = JSONObject().put("appName", appName),
        )
    }

    private companion object {
        val KNOWN_APPS = mapOf(
            "spotify" to "com.spotify.music",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "google maps" to "com.google.android.apps.maps",
            "maps" to "com.google.android.apps.maps",
        )
    }
}

internal class OpenMapsTool(context: Context) : ContextTool(context) {
    override val name = "open_maps"
    override val description = "Open a map application with a destination searched and ready for navigation."
    override val parameters = objectSchema("destination", "Place name or address")
    override val riskLevel = RiskLevel.LOW
    override val requiresConfirmation = false

    override fun execute(arguments: JSONObject): ToolResult {
        val destination = requiredString(arguments, "destination") ?: return invalid("destination")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(destination)}"))
        return launch(intent).withSuccessDetails(
            message = "Opened Maps for $destination.",
            data = JSONObject().put("destination", destination),
        )
    }
}

internal class SetAlarmTool(context: Context) : ContextTool(context) {
    override val name = "set_alarm"
    override val description = "Open the Android alarm flow with a requested hour, minute, and label."
    override val parameters = JSONObject(
        """{"type":"object","properties":{"hour":{"type":"integer","minimum":0,"maximum":23},"minute":{"type":"integer","minimum":0,"maximum":59},"label":{"type":"string"}},"required":["hour","minute","label"],"additionalProperties":false}""",
    )
    override val riskLevel = RiskLevel.LOW
    override val requiresConfirmation = false

    override fun execute(arguments: JSONObject): ToolResult {
        if (!arguments.has("hour") || !arguments.has("minute")) return invalid("hour and minute")
        val hour = arguments.optInt("hour", -1)
        val minute = arguments.optInt("minute", -1)
        val label = requiredString(arguments, "label") ?: return invalid("label")
        if (hour !in 0..23 || minute !in 0..59) return invalid("hour or minute")
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        return launch(intent).withSuccessDetails(
            message = "Opened the alarm flow for ${"%02d:%02d".format(hour, minute)}.",
            data = JSONObject().put("hour", hour).put("minute", minute).put("label", label),
        )
    }
}

private data class ContactMatch(val id: Long, val name: String, val number: String)

private fun findContacts(context: Context, query: String, limit: Int = 5): Result<List<ContactMatch>> = runCatching {
    check(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
        "CONTACT_PERMISSION_REQUIRED"
    }
    val matches = mutableListOf<ContactMatch>()
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
    )
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
        arrayOf("%$query%"),
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext() && matches.map { it.id }.distinct().size < limit) {
            val match = ContactMatch(cursor.getLong(idIndex), cursor.getString(nameIndex), cursor.getString(numberIndex))
            if (matches.none { it.id == match.id && it.number == match.number }) matches += match
        }
    }
    matches
}

internal class FindContactTool(context: Context) : ContextTool(context) {
    override val name = "find_contact"
    override val description = "Search Android contacts by name and return matching contact references."
    override val parameters = objectSchema("query", "Full or partial contact name")
    override val riskLevel = RiskLevel.LOW
    override val requiresConfirmation = false

    override fun execute(arguments: JSONObject): ToolResult {
        val query = requiredString(arguments, "query") ?: return invalid("query")
        val matches = findContacts(context, query).getOrElse {
            return if (it.message == "CONTACT_PERMISSION_REQUIRED") permissionFailure()
            else ToolResult.failure("Contact search failed safely.", "CONTACT_LOOKUP_FAILED")
        }
        if (matches.isEmpty()) return ToolResult.failure("No contact matched '$query'.", "CONTACT_NOT_FOUND")
        val data = JSONArray().apply {
            matches.distinctBy { it.id }.forEach { put(JSONObject().put("contactId", it.id).put("name", it.name)) }
        }
        return ToolResult(true, "Found ${data.length()} contact match(es).", JSONObject().put("contacts", data))
    }
}

internal class CallContactTool(context: Context) : ContextTool(context) {
    override val name = "call_contact"
    override val description = "Find a contact and open the system dialer with their number. The user must press Call."
    override val parameters = objectSchema("contact", "Contact name to dial")
    override val riskLevel = RiskLevel.MEDIUM
    override val requiresConfirmation = true

    override fun confirmationSummary(arguments: JSONObject): String =
        "Open the system dialer for ${arguments.optString("contact", "the selected contact")}. You will still press Call in the dialer."

    override fun execute(arguments: JSONObject): ToolResult {
        val query = requiredString(arguments, "contact") ?: return invalid("contact")
        val matches = findContacts(context, query).getOrElse {
            return if (it.message == "CONTACT_PERMISSION_REQUIRED") permissionFailure()
            else ToolResult.failure("Contact lookup failed safely.", "CONTACT_LOOKUP_FAILED")
        }
        if (matches.isEmpty()) return ToolResult.failure("No contact matched '$query'.", "CONTACT_NOT_FOUND")
        if (matches.size > 1) return ToolResult.failure("Contact '$query' is ambiguous; ${matches.size} matches found.", "CONTACT_AMBIGUOUS")
        val match = matches.single()
        return launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(match.number)}"))).withSuccessDetails(
            message = "Opened the dialer for ${match.name}; the user must press Call.",
            data = JSONObject().put("contactId", match.id).put("name", match.name),
        )
    }
}

internal class PrepareSmsTool(context: Context) : ContextTool(context) {
    override val name = "prepare_sms"
    override val description = "Find a contact and open the SMS composer with a draft. The user must review and press Send."
    override val parameters = JSONObject(
        """{"type":"object","properties":{"recipient":{"type":"string"},"message":{"type":"string"}},"required":["recipient","message"],"additionalProperties":false}""",
    )
    override val riskLevel = RiskLevel.MEDIUM
    override val requiresConfirmation = true

    override fun confirmationSummary(arguments: JSONObject): String =
        "Prepare an SMS to ${arguments.optString("recipient", "the selected contact")}: “${arguments.optString("message")}”. You will still press Send in the messaging app."

    override fun execute(arguments: JSONObject): ToolResult {
        val recipient = requiredString(arguments, "recipient") ?: return invalid("recipient")
        val message = requiredString(arguments, "message") ?: return invalid("message")
        val matches = findContacts(context, recipient).getOrElse {
            return if (it.message == "CONTACT_PERMISSION_REQUIRED") permissionFailure()
            else ToolResult.failure("Contact lookup failed safely.", "CONTACT_LOOKUP_FAILED")
        }
        if (matches.isEmpty()) return ToolResult.failure("No contact matched '$recipient'.", "CONTACT_NOT_FOUND")
        if (matches.size > 1) return ToolResult.failure("Contact '$recipient' is ambiguous; ${matches.size} matches found.", "CONTACT_AMBIGUOUS")
        val match = matches.single()
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(match.number)}"))
            .putExtra("sms_body", message)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        Telephony.Sms.getDefaultSmsPackage(context)?.let(intent::setPackage)
        return launch(intent).withSuccessDetails(
            message = "Prepared an SMS to ${match.name}; the user must review and send it.",
            data = JSONObject().put("contactId", match.id).put("name", match.name),
        )
    }
}

internal class ControlMediaTool(context: Context) : ContextTool(context) {
    override val name = "control_media"
    override val description =
        "Control the active Android media session. Use only when the user asks to play, pause, skip to the next item, or return to the previous item."
    override val parameters = JSONObject(
        """{"type":"object","properties":{"action":{"type":"string","enum":["play","pause","next","previous"]}},"required":["action"],"additionalProperties":false}""",
    )
    override val riskLevel = RiskLevel.LOW
    override val requiresConfirmation = false

    override fun execute(arguments: JSONObject): ToolResult {
        val action = requiredString(arguments, "action") ?: return invalid("action")
        val keyCode = when (action) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> return ToolResult.failure(
                "Unsupported media action '$action'.",
                "INVALID_PARAMETERS",
            )
        }
        val audioManager = context.getSystemService(AudioManager::class.java)
            ?: return ToolResult.failure("Android media controls are unavailable.", "MEDIA_UNAVAILABLE")
        return try {
            val eventTime = android.os.SystemClock.uptimeMillis()
            audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
            ToolResult(
                success = true,
                message = "Sent the $action command to the active media session.",
                data = JSONObject().put("action", action),
            )
        } catch (_: Exception) {
            ToolResult.failure("Android could not control the active media session.", "MEDIA_CONTROL_FAILED")
        }
    }
}

internal class SearchMusicTool(context: Context) : ContextTool(context) {
    override val name = "search_music"
    override val description =
        "Open Spotify directly to search results for a song, artist, album, or playlist. This does not prove playback started."
    override val parameters = JSONObject(
        """{"type":"object","properties":{"query":{"type":"string"},"provider":{"type":"string","enum":["spotify"]}},"required":["query","provider"],"additionalProperties":false}""",
    )
    override val riskLevel = RiskLevel.LOW
    override val requiresConfirmation = false

    override fun execute(arguments: JSONObject): ToolResult {
        val query = requiredString(arguments, "query") ?: return invalid("query")
        if (arguments.optString("provider") != "spotify") return invalid("provider")
        val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(query)}"))
            .setPackage("com.spotify.music")
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://open.spotify.com/search/${Uri.encode(query)}"),
        )
        val intent = if (spotifyIntent.resolveActivity(context.packageManager) != null) spotifyIntent else webIntent
        return launch(intent).withSuccessDetails(
            message = "Opened Spotify search results for $query. Playback has not been verified.",
            data = JSONObject().put("query", query).put("provider", "spotify"),
        )
    }
}

private fun objectSchema(property: String, description: String) = JSONObject()
    .put("type", "object")
    .put("properties", JSONObject().put(property, JSONObject().put("type", "string").put("description", description)))
    .put("required", JSONArray().put(property))
    .put("additionalProperties", false)

private fun requiredString(arguments: JSONObject, name: String): String? =
    arguments.optString(name).trim().takeIf { it.isNotEmpty() }

private fun invalid(parameter: String) = ToolResult.failure(
    "A valid $parameter parameter is required.",
    "INVALID_PARAMETERS",
)

private fun permissionFailure() = ToolResult.failure(
    "Contacts permission is required. Grant it in the agent screen and retry.",
    "PERMISSION_REQUIRED",
)
