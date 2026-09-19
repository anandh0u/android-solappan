package com.solappan.agent.tools

import org.json.JSONObject

class EchoTool : AgentTool {
    override val name = "echo"
    override val description = "Echo a message back unchanged. Use this when the user explicitly asks to use the echo tool."
    override val parameters = JSONObject(
        """
        {
          "type": "object",
          "properties": {
            "message": {
              "type": "string",
              "description": "The exact message to echo"
            }
          },
          "required": ["message"],
          "additionalProperties": false
        }
        """.trimIndent(),
    )
    override val riskLevel = RiskLevel.LOW
    override val requiresConfirmation = false

    override fun execute(arguments: JSONObject): ToolResult {
        val unexpected = arguments.keys().asSequence().filter { it != "message" }.toList()
        if (unexpected.isNotEmpty()) {
            return ToolResult.failure(
                message = "Unexpected echo parameter: ${unexpected.first()}",
                errorCode = "INVALID_PARAMETERS",
            )
        }
        val message = arguments.optString("message").trim()
        if (message.isBlank()) {
            return ToolResult.failure(
                message = "The echo tool requires a non-empty message.",
                errorCode = "INVALID_PARAMETERS",
            )
        }
        return ToolResult(
            success = true,
            message = "Echo completed.",
            data = JSONObject().put("echo", message),
        )
    }
}

