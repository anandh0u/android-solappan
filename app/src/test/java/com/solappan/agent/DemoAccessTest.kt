package com.solappan.agent

import org.junit.Assert.*
import org.junit.Test

class DemoAccessTest {
    @Test fun onlyNavigationCanBePreapproved() {
        assertTrue(DemoAccess.eligible("tap_element"))
        assertTrue(DemoAccess.eligible("type_text"))
        listOf("send_message", "call_contact", "prepare_sms", "unknown").forEach {
            assertFalse(DemoAccess.eligible(it))
        }
    }
    @Test fun grantsExpireAndRejectFutureOrInvalidValues() {
        assertTrue(DemoAccess.validExpiry(600_001, 1))
        assertFalse(DemoAccess.validExpiry(1, 1))
        assertFalse(DemoAccess.validExpiry(600_002, 1))
        assertFalse(DemoAccess.validExpiry(0, 1))
    }
}
