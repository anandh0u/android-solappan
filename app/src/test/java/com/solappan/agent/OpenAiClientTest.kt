package com.solappan.agent

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class OpenAiClientTest {
    private val client = OpenAiClient(apiKey = "test", model = "test")

    @Test
    fun `parses text from a valid response`() {
        val response = JSONObject(
            """{"output":[{"type":"message","content":[{"type":"output_text","text":"Ready"}]}]}""",
        )

        assertEquals("Ready", client.parseOutputText(response))
    }

    @Test(expected = IOException::class)
    fun `rejects malformed response without output`() {
        client.parseOutputText(JSONObject("{}"))
    }

    @Test(expected = IOException::class)
    fun `rejects empty model output`() {
        client.parseOutputText(JSONObject("""{"output":[]}"""))
    }
}
