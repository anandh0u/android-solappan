package com.solappan.agent.tools

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RuntimeAuditTest {
    @Test fun `failed launch retains failure message and data`() {
        val failure = ToolResult.failure("No compatible app", "INTENT_UNAVAILABLE")
        val result = failure.withSuccessDetails("Opened Maps", JSONObject().put("destination", "college"))
        assertSame(failure, result)
        assertFalse(result.success)
        assertEquals("No compatible app", result.message)
    }

    @Test fun `successful launch gets success details`() {
        val result = ToolResult(true, "Accepted").withSuccessDetails("Opened Maps", JSONObject().put("destination", "college"))
        assertTrue(result.success)
        assertEquals("Opened Maps", result.message)
    }

    @Test fun `invalid schema values never reach execution or approval`() {
        var count = 0
        val tool = object : AgentTool {
            override val name = "test"
            override val description = "test"
            override val parameters = JSONObject("""{"type":"object","properties":{"hour":{"type":"integer","minimum":0,"maximum":23},"action":{"type":"string","enum":["play","pause"]}},"required":["hour","action"],"additionalProperties":false}""")
            override val riskLevel = RiskLevel.MEDIUM
            override val requiresConfirmation = true
            override fun execute(arguments: JSONObject): ToolResult { count++; return ToolResult(true, "executed") }
        }
        val registry = ToolRegistry(listOf(tool))
        listOf(
            "{}", """{"hour":7.5,"action":"play"}""", """{"hour":"7","action":"play"}""",
            """{"hour":24,"action":"play"}""", """{"hour":-1,"action":"play"}""",
            """{"hour":7,"action":null}""", """{"hour":7,"action":"delete"}""",
            """{"hour":7,"action":"play","extra":true}""",
        ).forEach { arguments ->
            assertNull(registry.confirmationRequest("test", arguments))
            assertEquals("INVALID_PARAMETERS", registry.execute("test", arguments, true).errorCode)
        }
        assertEquals(0, count)
        assertTrue(registry.execute("test", """{"hour":7,"action":"play"}""", true).success)
        assertEquals(1, count)
    }
}
