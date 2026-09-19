package com.solappan.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRuntimeUiStateTest {
    @Test
    fun `failed tool cannot become a successful completed state`() {
        val state = AgentRuntimeUiState()
            .apply(AgentEvent.ToolStarted("open_app"))
            .apply(AgentEvent.ToolFinished("open_app", false, "Unavailable", "INTENT_FAILED"))
            .apply(AgentEvent.StateChanged(AgentState.COMPLETED))
        assertEquals(AgentState.FAILED, state.agentState)
        assertEquals(AgentState.THINKING, state.beginRun().agentState)
    }
    @Test
    fun `begin run clears previous output and timeline`() {
        val state = AgentRuntimeUiState(
            agentState = AgentState.COMPLETED,
            timeline = listOf(TimelineEntry("echo", "done", TimelineStatus.SUCCESS)),
            response = "done",
            error = "old error",
        ).beginRun()

        assertTrue(state.loading)
        assertTrue(state.timeline.isEmpty())
        assertEquals("", state.response)
        assertEquals(null, state.error)
    }

    @Test
    fun `tool events update the matching timeline entry`() {
        val started = AgentRuntimeUiState()
            .apply(AgentEvent.ToolStarted("open_app"))
        val finished = started.apply(
            AgentEvent.ToolFinished("open_app", true, "Opened Spotify.", null),
        )

        assertEquals(1, finished.timeline.size)
        assertEquals(TimelineStatus.SUCCESS, finished.timeline.single().status)
        assertEquals("Opened Spotify.", finished.timeline.single().message)
    }

    @Test
    fun `cancelled tool is visibly distinct from failure`() {
        val state = AgentRuntimeUiState()
            .apply(AgentEvent.ToolStarted("call_contact"))
            .apply(AgentEvent.ToolFinished("call_contact", false, "Cancelled", "USER_CANCELLED"))

        assertEquals(TimelineStatus.CANCELLED, state.timeline.single().status)
        assertFalse(state.timeline.single().status == TimelineStatus.FAILED)
    }
}
