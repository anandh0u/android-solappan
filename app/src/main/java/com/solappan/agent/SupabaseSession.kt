package com.solappan.agent

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** All calls are blocking: use Dispatchers.IO. Passwords are never persisted. */
class SupabaseSession(context: Context) {
    private val directory = context.applicationContext.noBackupFilesDir
    private val file = AtomicFile(File(directory, "sol-session.enc"))
    val baseUrl: String get() = verifiedSupabaseUrl(BuildConfig.SUPABASE_URL)
    val gatewayUrl: String get() = "$baseUrl/functions/v1/agent-gateway"

    fun signIn(email: String, password: String) = locked {
        require(email.contains('@') && password.isNotEmpty()) { "Enter your beta account email and password." }
        val session = auth("token?grant_type=password", JSONObject().put("email", email.trim()).put("password", password))
        save(session)
    }

    fun accountEmail(): String? = locked { read()?.optJSONObject("user")?.optString("email") }

    fun accessToken(): String = locked {
        var session = read() ?: throw IOException("Sign in to your SOL beta account in Setup.")
        if (session.optLong("expires_at") <= System.currentTimeMillis() / 1000 + 60) {
            try {
                session = auth("token?grant_type=refresh_token", JSONObject().put("refresh_token", session.getString("refresh_token")))
                save(session)
            } catch (error: AuthRejected) {
                file.delete()
                throw IOException("Your session expired. Sign in again in Setup.", error)
            }
        }
        session.getString("access_token")
    }

    fun signOut() {
        val token = locked { val value = read()?.optString("access_token"); file.delete(); value }
        if (!token.isNullOrBlank()) auth("logout", JSONObject(), token)
    }

    private fun auth(path: String, body: JSONObject, token: String? = null): JSONObject {
        check(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) { "Supabase is not configured in this build." }
        val connection = URL("$baseUrl/auth/v1/$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Content-Type", "application/json")
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            val status = connection.responseCode
            if (status !in 200..299) {
                if (status in listOf(400, 401, 403)) throw AuthRejected("Sign-in rejected. Check your email/password and email verification.")
                throw IOException(if (status == 429) "Too many attempts. Wait before trying again." else "Account service unavailable. Try again later.")
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally { connection.disconnect() }
    }

    private fun read(): JSONObject? {
        if (!file.baseFile.exists()) return null
        return try {
            val envelope = JSONObject(file.openRead().bufferedReader().use { it.readText() })
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)))
            JSONObject(String(cipher.doFinal(Base64.decode(envelope.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8))
        } catch (_: Exception) {
            file.delete()
            throw IOException("Saved session could not be unlocked. Sign in again.")
        }
    }

    private fun save(session: JSONObject) {
        require(session.optString("access_token").isNotBlank() && session.optString("refresh_token").isNotBlank())
        if (!session.has("expires_at")) session.put("expires_at", System.currentTimeMillis()/1000 + session.optLong("expires_in", 3600))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val envelope = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(cipher.doFinal(session.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        val stream = file.startWrite()
        try { stream.write(envelope.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw error }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    // Serialize token refresh both within this process and across the assistant process.
    private fun <T> locked(block: () -> T): T = synchronized(LOCK) {
        RandomAccessFile(File(directory, "sol-session.lock"), "rw").use { handle ->
            handle.channel.lock().use { block() }
        }
    }
    private class AuthRejected(message: String) : IOException(message)
    companion object {
        private val LOCK = Any()
        private const val KEY_ALIAS = "sol-account-v1"
    }
}

internal fun verifiedSupabaseUrl(value: String): String {
    val uri = java.net.URI(value)
    require(uri.scheme == "https" && uri.host?.endsWith(".supabase.co") == true &&
        uri.userInfo == null && uri.port == -1 && uri.query == null && uri.fragment == null &&
        uri.path in listOf("", "/")) { "Configure a valid HTTPS Supabase project URL." }
    return value.trimEnd('/')
}
