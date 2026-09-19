package com.solappan.agent.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityPolicyTest {
    @Test fun `consequential UI targets are blocked`() {
        listOf("Send", "Approve action", "Call Afnan", "Pay now", "Delete").forEach {
            assertTrue(it, isSensitiveAccessibilityTarget(it))
        }
    }

    @Test fun `ordinary navigation targets remain available`() {
        listOf("Search", "Library", "Next", "Settings item").forEach {
            assertFalse(it, isSensitiveAccessibilityTarget(it))
        }
    }
}
