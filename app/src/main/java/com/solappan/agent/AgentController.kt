package com.solappan.agent

import com.solappan.agent.tools.ToolConfirmation
import com.solappan.agent.tools.ToolRegistry
import com.solappan.agent.tools.ToolResult
import org.json.JSONArray
import org.json.JSONObject

class AgentController(
    private val client: OpenAiClient = OpenAiClient(),
    private val registry: ToolRegistry = ToolRegistry.sessionTwo(),
) {
    suspend fun run(
        goal: String,
        onEvent: suspend (AgentEvent) -> Unit = {},
        requestConfirmation: suspend (ToolConfirmation) -> Boolean = { false },
    ): Result<AgentRun> {
        return try {
            require(goal.isNotBlank()) { "Enter a goal before sending." }
            onEvent(AgentEvent.StateChanged(AgentState.THINKING))
            var response = client.createResponse(goal, registry.apiDefinitions(), INSTRUCTIONS)
            val executedTools = mutableListOf<ToolExecution>()
            var actionCancelled = false
            onEvent(AgentEvent.StateChanged(AgentState.PLANNING))

            repeat(MAX_AGENT_TURNS) {
                val calls = parseToolCalls(response)
                if (calls.isEmpty()) {
                    val finalText = client.parseOutputText(response)
                    onEvent(AgentEvent.StateChanged(if (actionCancelled) AgentState.CANCELLED else AgentState.COMPLETED))
                    return Result.success(AgentRun(finalText, executedTools.toList(), actionCancelled))
                }

                val outputs = JSONArray()
                calls.forEach { call ->
                    onEvent(AgentEvent.ToolStarted(call.name))
                    val confirmation = registry.confirmationRequest(call.name, call.arguments)
                    val result = if (confirmation != null) {
                        onEvent(AgentEvent.StateChanged(AgentState.WAITING_FOR_CONFIRMATION))
                        if (requestConfirmation(confirmation)) {
                            onEvent(AgentEvent.StateChanged(AgentState.EXECUTING))
                            registry.execute(call.name, call.arguments, confirmationGranted = true)
                        } else {
                            actionCancelled = true
                            ToolResult.failure(
                                "The user cancelled '${call.name}'. No action was executed.",
                                "USER_CANCELLED",
                            )
                        }
                    } else {
                        onEvent(AgentEvent.StateChanged(AgentState.EXECUTING))
                        registry.execute(call.name, call.arguments)
                    }
                    executedTools += ToolExecution(call.name, result.success, result.message)
                    onEvent(AgentEvent.ToolFinished(call.name, result.success, result.message, result.errorCode))
                    outputs.put(
                        JSONObject()
                            .put("type", "function_call_output")
                            .put("call_id", call.callId)
                            .put("output", result.toJson()),
                    )
                }

                onEvent(AgentEvent.StateChanged(AgentState.VERIFYING))
                response = client.createResponse(
                    input = outputs,
                    tools = registry.apiDefinitions(),
                    instructions = INSTRUCTIONS,
                    previousResponseId = response.getString("id"),
                )
                onEvent(AgentEvent.StateChanged(AgentState.PLANNING))
            }
            error("Agent stopped after $MAX_AGENT_TURNS turns to prevent an infinite tool loop.")
        } catch (error: Exception) {
            onEvent(AgentEvent.StateChanged(AgentState.FAILED))
            Result.failure(error)
        }
    }

    private fun parseToolCalls(response: JSONObject): List<ToolCall> {
        val output = response.optJSONArray("output") ?: return emptyList()
        return buildList {
            for (index in 0 until output.length()) {
                val item = output.optJSONObject(index) ?: continue
                if (item.optString("type") == "function_call") {
                    add(ToolCall(item.getString("call_id"), item.getString("name"), item.optString("arguments", "{}")))
                }
            }
        }
    }

    private data class ToolCall(val callId: String, val name: String, val arguments: String)

    private companion object {
        const val MAX_AGENT_TURNS = 8
        const val INSTRUCTIONS = """
            You are the reasoning layer of a safe Android agent runtime.
            Use only the registered tools. When a user explicitly requests a tool, call it rather than claiming you did.
            Use multiple tool calls when the goal requires them. Never invent tool results.
            The Android runtime independently asks for approval before protected tools. Never claim a cancelled tool succeeded.
            After tool results arrive, briefly tell the user what actually completed, failed, or was cancelled.
        """
    }
}

enum class AgentState {
    IDLE, THINKING, PLANNING, EXECUTING, WAITING_FOR_CONFIRMATION, VERIFYING, COMPLETED, FAILED, CANCELLED,
}

data class AgentRun(
    val finalText: String,
    val toolExecutions: List<ToolExecution>,
    val actionCancelled: Boolean,
)

data class ToolExecution(val name: String, val success: Boolean, val message: String)

sealed interface AgentEvent {
    data class StateChanged(val state: AgentState) : AgentEvent
    data class ToolStarted(val toolName: String) : AgentEvent
    data class ToolFinished(
        val toolName: String,
        val success: Boolean,
        val message: String,
        val errorCode: String?,
    ) : AgentEvent
}
