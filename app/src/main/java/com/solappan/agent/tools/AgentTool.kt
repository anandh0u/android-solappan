package com.solappan.agent.tools

import org.json.JSONObject

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
}

interface AgentTool {
    val name: String
    val description: String
    val parameters: JSONObject
    val riskLevel: RiskLevel
    val requiresConfirmation: Boolean

    fun execute(arguments: JSONObject): ToolResult

    fun confirmationSummary(arguments: JSONObject): String = description

    fun apiDefinition(): JSONObject = JSONObject()
        .put("type", "function")
        .put("name", name)
        .put("description", description)
        .put("parameters", parameters)
        .put("strict", true)
}

data class ToolResult(
    val success: Boolean,
    val message: String,
    val data: JSONObject = JSONObject(),
    val errorCode: String? = null,
) {
    fun toJson(): String = JSONObject()
        .put("success", success)
        .put("message", message)
        .put("data", data)
        .apply { errorCode?.let { put("errorCode", it) } }
        .toString()

    companion object {
        fun failure(message: String, errorCode: String) = ToolResult(
            success = false,
            message = message,
            errorCode = errorCode,
        )
    }
}
