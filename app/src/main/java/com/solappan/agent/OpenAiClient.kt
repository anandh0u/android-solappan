package com.solappan.agent

import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL

class OpenAiClient(
    private val apiKey: String = BuildConfig.OPENAI_API_KEY,
    private val model: String = BuildConfig.OPENAI_MODEL,
) {
    fun createResponse(
        input: Any,
        tools: JSONArray,
        instructions: String,
        previousResponseId: String? = null,
    ): JSONObject {
        check(apiKey.isNotBlank()) {
            "OpenAI API key is missing. Add OPENAI_API_KEY to local.properties and rebuild."
        }

        Log.i(TAG, "OpenAI request started")
        val connection = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }

        return try {
            val body = JSONObject()
                .put("model", model)
                .put("input", input)
                .put("instructions", instructions)
                .put("tools", tools)
                .put("parallel_tool_calls", true)
                .put("max_output_tokens", 800)
                .apply { previousResponseId?.let { put("previous_response_id", it) } }
                .toString()
            connection.outputStream.bufferedWriter().use { it.write(body) }

            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(responseBody).getJSONObject("error").optString("message")
                }.getOrNull().orEmpty()
                throw IOException(message.ifBlank { "OpenAI request failed (HTTP $status)." })
            }

            JSONObject(responseBody).also {
                Log.i(TAG, "OpenAI request completed")
            }
        } catch (error: Exception) {
            Log.e(TAG, "OpenAI request failed: ${error.javaClass.simpleName}")
            throw when (error) {
                is UnknownHostException, is ConnectException -> IOException(
                    "Network unavailable. Check your connection and try again.",
                    error,
                )
                is SocketTimeoutException -> IOException(
                    "The model request timed out. Check your connection and try again.",
                    error,
                )
                else -> error
            }
        } finally {
            connection.disconnect()
        }
    }

    fun parseOutputText(response: JSONObject): String {
        val output = response.optJSONArray("output")
            ?: throw IOException("OpenAI returned no output.")
        val parts = buildList {
            for (outputIndex in 0 until output.length()) {
                val content = output.optJSONObject(outputIndex)?.optJSONArray("content") ?: continue
                for (contentIndex in 0 until content.length()) {
                    val item = content.optJSONObject(contentIndex) ?: continue
                    if (item.optString("type") == "output_text") {
                        item.optString("text").takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }
        }
        return parts.joinToString("\n").ifBlank {
            throw IOException("OpenAI returned an empty response.")
        }
    }

    private companion object {
        const val TAG = "OpenAiClient"
        const val RESPONSES_URL = "https://api.openai.com/v1/responses"
    }
}
