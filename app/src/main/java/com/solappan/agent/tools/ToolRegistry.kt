package com.solappan.agent.tools

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ToolRegistry(tools: List<AgentTool>) {
    private val toolsByName: Map<String, AgentTool> = tools.associateBy { it.name }.also {
        require(it.size == tools.size) { "Tool names must be unique." }
    }

    fun apiDefinitions(): JSONArray = JSONArray().apply {
        toolsByName.values.forEach { put(it.apiDefinition()) }
    }

    fun find(name: String): AgentTool? = toolsByName[name]

    fun confirmationRequest(name: String, rawArguments: String): ToolConfirmation? {
        val tool = find(name) ?: return null
        if (!tool.requiresConfirmation) return null
        val arguments = try {
            JSONObject(rawArguments)
        } catch (_: JSONException) {
            return null
        }
        return ToolConfirmation(tool.name, tool.confirmationSummary(arguments), tool.riskLevel)
    }

    fun execute(
        name: String,
        rawArguments: String,
        confirmationGranted: Boolean = false,
    ): ToolResult {
        val tool = find(name) ?: return ToolResult.failure(
            message = "Unknown tool '$name' was rejected.",
            errorCode = "UNKNOWN_TOOL",
        )
        val arguments = try {
            JSONObject(rawArguments)
        } catch (_: JSONException) {
            return ToolResult.failure(
                message = "Tool '$name' received malformed JSON arguments.",
                errorCode = "INVALID_PARAMETERS",
            )
        }
        if (tool.requiresConfirmation && !confirmationGranted) {
            return ToolResult.failure(
                message = "Tool '$name' requires explicit user confirmation.",
                errorCode = "CONFIRMATION_REQUIRED",
            )
        }
        return try {
            tool.execute(arguments)
        } catch (_: Exception) {
            ToolResult.failure(
                message = "Tool '$name' failed safely.",
                errorCode = "TOOL_EXECUTION_FAILED",
            )
        }
    }

    companion object {
        fun sessionTwo(): ToolRegistry = ToolRegistry(listOf(EchoTool()))

        fun sessionThree(context: Context): ToolRegistry = ToolRegistry(
            listOf(
                EchoTool(),
                OpenAppTool(context),
                OpenMapsTool(context),
                SetAlarmTool(context),
                FindContactTool(context),
                CallContactTool(context),
                PrepareSmsTool(context),
            ),
        )
    }
}

data class ToolConfirmation(
    val toolName: String,
    val summary: String,
    val riskLevel: RiskLevel,
)
