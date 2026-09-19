package com.solappan.agent.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakePhraseMatcherTest {
    @Test fun `supported wake phrases match natural speech`() {
        listOf("Hey SOL", "hello sol!", "Okay Sol open Spotify", "please, hey sol").forEach {
            assertTrue(it, containsSolWakePhrase(it))
        }
    }

    @Test fun `unrelated and embedded words do not match`() {
        listOf("solar panels", "console", "hello assistant", "solve this").forEach {
            assertFalse(it, containsSolWakePhrase(it))
        }
    }
}
