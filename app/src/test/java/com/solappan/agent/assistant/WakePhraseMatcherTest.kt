package com.solappan.agent.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakePhraseMatcherTest {
    @Test fun `supported wake phrases match natural speech`() {
        listOf("Hey SOL", "hello sol!", "Okay Soul", "hey saul").forEach {
            assertTrue(it, containsSolWakePhrase(it))
        }
    }

    @Test fun `unrelated and embedded words do not match`() {
        listOf("solar panels", "console", "hello assistant", "solve this", "please hey sol", "Hey SOL open Spotify", "").forEach {
            assertFalse(it, containsSolWakePhrase(it))
        }
    }
}
