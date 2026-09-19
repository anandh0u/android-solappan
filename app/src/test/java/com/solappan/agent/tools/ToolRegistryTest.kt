package com.solappan.agent.tools

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {
    private val registry = ToolRegistry.sessionTwo()

    @Test
    fun `echo returns supplied message`() {
        val result = registry.execute("echo", """{"message":"Hello Android"}""")

        assertTrue(result.success)
        assertEquals("Hello Android", result.data.getString("echo"))
    }

    @Test
    fun `unknown tool is rejected`() {
        val result = registry.execute("not_registered", "{}")

        assertFalse(result.success)
        assertEquals("UNKNOWN_TOOL", result.errorCode)
    }

    @Test
    fun `malformed arguments are rejected`() {
        val result = registry.execute("echo", "not-json")

        assertFalse(result.success)
        assertEquals("INVALID_PARAMETERS", result.errorCode)
    }

    @Test
    fun `missing required argument is rejected`() {
        val result = registry.execute("echo", JSONObject().toString())

        assertFalse(result.success)
        assertEquals("INVALID_PARAMETERS", result.errorCode)
    }

    @Test
    fun `protected tool produces confirmation metadata`() {
        val protectedTool = object : AgentTool {
            override val name = "protected_test"
            override val description = "Protected test action"
            override val parameters = JSONObject("""{"type":"object","properties":{},"additionalProperties":false}""")
            override val riskLevel = RiskLevel.MEDIUM
            override val requiresConfirmation = true
            override fun execute(arguments: JSONObject) = ToolResult(true, "executed")
            override fun confirmationSummary(arguments: JSONObject) = "Approve protected test action"
        }
        val protectedRegistry = ToolRegistry(listOf(protectedTool))

        val confirmation = protectedRegistry.confirmationRequest("protected_test", "{}")

        assertEquals("protected_test", confirmation?.toolName)
        assertEquals(RiskLevel.MEDIUM, confirmation?.riskLevel)
        assertEquals("Approve protected test action", confirmation?.summary)
    }

    @Test
    fun `low risk echo does not request confirmation`() {
        val confirmation = registry.confirmationRequest("echo", """{"message":"safe"}""")

        assertEquals(null, confirmation)
    }
}
