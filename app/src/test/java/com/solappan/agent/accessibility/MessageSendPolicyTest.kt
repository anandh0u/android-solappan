package com.solappan.agent.accessibility

import org.junit.Assert.*
import org.junit.Test

class MessageSendPolicyTest {
    @Test fun sendLabelsDoNotAuthorizeOtherActions() {
        assertTrue(isMessageSendLabel("Send"))
        assertTrue(isMessageSendLabel("Send message"))
        listOf("Send money", "Confirm", "Buy", "Forward", "Send payment", "Send to all").forEach { assertFalse(isMessageSendLabel(it)) }
    }
    @Test fun exactDraftBindingIsRequired() {
        assertTrue(matchesMessageDraft("app", "app", "Hello", "Hello", "Person", "Person"))
        assertFalse(matchesMessageDraft("app", "other", "Hello", "Hello", "Person", "Person"))
        assertFalse(matchesMessageDraft("app", "app", "Hello", "Changed", "Person", "Person"))
        assertFalse(matchesMessageDraft("app", "app", "Hello", "Hello", "Person", "Other"))
        assertFalse(matchesMessageDraft("app", "app", "", "", "Person", "Person"))
    }
}
