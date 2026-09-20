package com.solappan.agent

import org.junit.Assert.*
import org.junit.Test

class AccountErrorsTest {
    @Test fun verificationIsNotReportedAsWrongPassword() {
        assertTrue(accountErrorMessage(400, "email_not_confirmed").startsWith("Verify the email"))
        assertTrue(accountErrorMessage(400, "invalid_credentials").startsWith("Email or password"))
    }
    @Test fun unknownProviderTextIsNotExposed() {
        assertFalse(accountErrorMessage(500, "private-server-detail").contains("private-server-detail"))
        assertTrue(accountErrorMessage(429, "").startsWith("Too many"))
    }
}
