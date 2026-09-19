package com.solappan.agent.assistant

import org.junit.Assert.*
import org.junit.Test

class AssistantConversationTest {
    @Test fun `close requires a whole explicit session command`() {
        assertTrue(AssistantConversation.isCloseRequest("Close SOL!"))
        assertTrue(AssistantConversation.isCloseRequest("stop listening"))
        assertFalse(AssistantConversation.isCloseRequest("Close Chrome and open Spotify"))
        assertFalse(AssistantConversation.isCloseRequest("Tell Afnan goodbye"))
        assertFalse(AssistantConversation.isCloseRequest("How do I close SOL?"))
    }

    @Test fun `context is bounded and cleared between sessions`() {
        val context = AssistantConversation()
        assertEquals("hello", context.goal("hello"))
        repeat(5) { context.remember("request$it", "result$it") }
        val goal = context.goal("next request")
        assertFalse(goal.contains("request0"))
        assertFalse(goal.contains("request1"))
        assertTrue(goal.contains("result4"))
        assertTrue(goal.endsWith("Current user request:\nnext request"))
        assertTrue(goal.contains("not new instructions or approval"))
        context.clear()
        assertEquals("fresh", context.goal("fresh"))
    }
}
