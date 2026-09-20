package com.solappan.agent.tools

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {
    @Test fun `temporary navigation consent is rechecked before execution`() {
        var granted = true
        var executions = 0
        val tool = object : AgentTool {
            override val name = "tap_element"
            override val description = "Navigation test"
            override val parameters = JSONObject("""{"type":"object","properties":{},"additionalProperties":false}""")
            override val riskLevel = RiskLevel.MEDIUM
            override val requiresConfirmation = true
            override fun confirmationSummary(arguments: JSONObject) = "Tap"
            override fun execute(arguments: JSONObject): ToolResult { executions++; return ToolResult(true, "executed") }
        }
        val tested = ToolRegistry(listOf(tool)) { granted && com.solappan.agent.DemoAccess.eligible(it) }
        assertEquals(null, tested.confirmationRequest(tool.name, "{}"))
        assertTrue(tested.execute(tool.name, "{}").success)
        granted = false
        assertEquals("CONFIRMATION_REQUIRED", tested.execute(tool.name, "{}").errorCode)
        assertEquals(1, executions)
        assertEquals("INVALID_PARAMETERS", tested.execute(tool.name, """{"extra":true}""").errorCode)
    }

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
        var executionCount = 0
        val protectedTool = object : AgentTool {
            override val name = "protected_test"
            override val description = "Protected test action"
            override val parameters = JSONObject("""{"type":"object","properties":{},"additionalProperties":false}""")
            override val riskLevel = RiskLevel.MEDIUM
            override val requiresConfirmation = true
            override fun execute(arguments: JSONObject): ToolResult {
                executionCount += 1
                return ToolResult(true, "executed")
            }
            override fun confirmationSummary(arguments: JSONObject) = "Approve protected test action"
        }
        val protectedRegistry = ToolRegistry(listOf(protectedTool))

        val confirmation = protectedRegistry.confirmationRequest("protected_test", "{}")

        assertEquals("protected_test", confirmation?.toolName)
        assertEquals(RiskLevel.MEDIUM, confirmation?.riskLevel)
        assertEquals("Approve protected test action", confirmation?.summary)
        assertEquals(0, executionCount)
    }

    @Test
    fun `protected tool cannot execute without explicit confirmation`() {
        var executed = false
        val protectedTool = object : AgentTool {
            override val name = "protected_test"
            override val description = "Protected test action"
            override val parameters = JSONObject("""{"type":"object","properties":{},"additionalProperties":false}""")
            override val riskLevel = RiskLevel.MEDIUM
            override val requiresConfirmation = true
            override fun execute(arguments: JSONObject): ToolResult {
                executed = true
                return ToolResult(true, "executed")
            }
            override fun confirmationSummary(arguments: JSONObject) = "Approve protected test action"
        }
        val protectedRegistry = ToolRegistry(listOf(protectedTool))

        val result = protectedRegistry.execute("protected_test", "{}")

        assertFalse(result.success)
        assertEquals("CONFIRMATION_REQUIRED", result.errorCode)
        assertFalse(executed)
    }

    @Test
    fun `protected tool executes after explicit confirmation`() {
        var executed = false
        val protectedTool = object : AgentTool {
            override val name = "protected_test"
            override val description = "Protected test action"
            override val parameters = JSONObject("""{"type":"object","properties":{},"additionalProperties":false}""")
            override val riskLevel = RiskLevel.MEDIUM
            override val requiresConfirmation = true
            override fun execute(arguments: JSONObject): ToolResult {
                executed = true
                return ToolResult(true, "executed")
            }
            override fun confirmationSummary(arguments: JSONObject) = "Approve protected test action"
        }
        val protectedRegistry = ToolRegistry(listOf(protectedTool))

        val result = protectedRegistry.execute("protected_test", "{}", confirmationGranted = true)

        assertTrue(result.success)
        assertTrue(executed)
    }

    @Test
    fun `low risk echo does not request confirmation`() {
        val confirmation = registry.confirmationRequest("echo", """{"message":"safe"}""")

        assertEquals(null, confirmation)
    }
}
