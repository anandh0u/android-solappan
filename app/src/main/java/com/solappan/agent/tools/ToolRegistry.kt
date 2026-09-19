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
        if (!validArguments(tool, arguments)) return null
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
        if (!validArguments(tool, arguments)) {
            return ToolResult.failure("Tool '$name' received invalid parameters.", "INVALID_PARAMETERS")
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

    private fun validArguments(tool: AgentTool, arguments: JSONObject): Boolean {
        val properties = tool.parameters.optJSONObject("properties") ?: JSONObject()
        val required = tool.parameters.optJSONArray("required") ?: JSONArray()
        for (index in 0 until required.length()) {
            if (!arguments.has(required.getString(index))) return false
        }
        for (key in arguments.keys()) {
            val spec = properties.optJSONObject(key) ?: return false
            val value = arguments.opt(key)
            when (spec.optString("type")) {
                "string" -> if (value !is String || value.isBlank()) return false
                "integer" -> {
                    if (value !is Number || !value.toDouble().isFinite() ||
                        value.toDouble() % 1.0 != 0.0) return false
                    if (spec.has("minimum") && value.toDouble() < spec.getDouble("minimum")) return false
                    if (spec.has("maximum") && value.toDouble() > spec.getDouble("maximum")) return false
                }
            }
            spec.optJSONArray("enum")?.let { choices ->
                if ((0 until choices.length()).none { choices.get(it) == value }) return false
            }
        }
        return true
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
                SearchMusicTool(context),
                ControlMediaTool(context),
                ObserveScreenTool(context),
                TapElementTool(context),
                TypeTextTool(context),
                ScrollScreenTool(context),
                PressNavigationTool(context, "press_back", "back", "Back"),
                PressNavigationTool(context, "press_home", "home", "Home"),
            ),
        )
    }
}

data class ToolConfirmation(
    val toolName: String,
    val summary: String,
    val riskLevel: RiskLevel,
)
