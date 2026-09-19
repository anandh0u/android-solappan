package com.solappan.agent.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityPolicyTest {
    @Test fun `consequential UI targets are blocked`() {
        listOf("Send", "Approve action", "Call Afnan", "Pay now", "Delete", "Tap to send message",
            "com.example:id/send_button", "confirmPurchase", "Pay: ₹100", "Enter OTP").forEach {
            assertTrue(it, isSensitiveAccessibilityTarget(it))
        }
    }

    @Test fun `ordinary navigation targets remain available`() {
        listOf("Search", "Library", "Next", "Settings item").forEach {
            assertFalse(it, isSensitiveAccessibilityTarget(it))
        }
    }

    @Test fun `shared traversal reaches nodes beyond the old eighty node limit`() {
        val children = (1..100).toList()
        val observed = boundedScreenNodes(0) { if (it == 0) children else emptyList() }
        assertEquals((0..100).toList(), observed)
        assertTrue(99 in observed)
    }

    @Test fun `traversal bounds wide and cyclic trees`() {
        assertEquals(512, boundedScreenNodes(0) { (1..1000).toList() }.size)
        assertEquals(33, boundedScreenNodes(0) { listOf(it) }.size)
    }

    @Test fun `approval cannot resolve to a changed or expired observed target`() {
        val window = 4 to "com.android.chrome"
        val identities = setOf("url-bar-at-observed-bounds")
        assertTrue(isObservedTarget(window, window, 100, identities, identities.single()))
        assertFalse(isObservedTarget(window, 5 to window.second, 100, identities, identities.single()))
        assertFalse(isObservedTarget(window, 4 to "another.app", 100, identities, identities.single()))
        assertFalse(isObservedTarget(window, window, 120_000, identities, identities.single()))
        assertFalse(isObservedTarget(window, window, 100, identities, "changed-label-or-bounds"))
        assertFalse(isObservedTarget(window, window, 100, emptySet(), identities.single()))
    }
}
