package com.solappan.agent

import com.solappan.agent.tools.ToolConfirmation

enum class TimelineStatus { RUNNING, SUCCESS, FAILED, CANCELLED }

data class TimelineEntry(
    val toolName: String,
    val message: String,
    val status: TimelineStatus,
)

data class AgentRuntimeUiState(
    val agentState: AgentState = AgentState.IDLE,
    val loading: Boolean = false,
    val timeline: List<TimelineEntry> = emptyList(),
    val response: String = "",
    val error: String? = null,
) {
    internal fun beginRun(): AgentRuntimeUiState = copy(
        loading = true,
        timeline = emptyList(),
        response = "",
        error = null,
    )

    internal fun apply(event: AgentEvent): AgentRuntimeUiState = when (event) {
        is AgentEvent.StateChanged -> copy(agentState = event.state)
        is AgentEvent.ToolStarted -> copy(
            timeline = timeline + TimelineEntry(event.toolName, "Starting", TimelineStatus.RUNNING),
        )
        is AgentEvent.ToolFinished -> {
            val index = timeline.indexOfLast {
                it.toolName == event.toolName && it.status == TimelineStatus.RUNNING
            }
            if (index < 0) this else copy(
                timeline = timeline.toMutableList().also { entries ->
                    entries[index] = TimelineEntry(
                        toolName = event.toolName,
                        message = event.message,
                        status = when {
                            event.errorCode == "USER_CANCELLED" -> TimelineStatus.CANCELLED
                            event.success -> TimelineStatus.SUCCESS
                            else -> TimelineStatus.FAILED
                        },
                    )
                },
            )
        }
    }
}

class AgentRuntimeCoordinator(private val controller: AgentController) {
    suspend fun run(
        goal: String,
        initialState: AgentRuntimeUiState = AgentRuntimeUiState(),
        onState: suspend (AgentRuntimeUiState) -> Unit,
        requestConfirmation: suspend (ToolConfirmation) -> Boolean,
    ) {
        var state = initialState.beginRun()
        onState(state)

        val result = controller.run(
            goal = goal,
            onEvent = { event ->
                state = state.apply(event)
                onState(state)
            },
            requestConfirmation = requestConfirmation,
        )

        state = result.fold(
            onSuccess = { state.copy(loading = false, response = it.finalText) },
            onFailure = {
                state.copy(
                    loading = false,
                    error = it.message ?: "The request failed. Please try again.",
                )
            },
        )
        onState(state)
    }
}
