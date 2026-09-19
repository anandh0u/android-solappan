package com.solappan.agent.testing

import android.content.Intent
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import com.solappan.agent.tools.ToolRegistry
import org.json.JSONObject

class MessageSendDeviceTest {
    @Test fun testConfirmedSendAndDuplicatePrevention() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testPackage = instrumentation.context.packageName
        instrumentation.context.startActivity(Intent().setClassName(testPackage, FakeConversationActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        SystemClock.sleep(1500)
        val registry = ToolRegistry.sessionThree(instrumentation.targetContext)
        val observed = registry.execute("observe_screen", "{}")
        assertTrue(observed.message, observed.success)
        val args = JSONObject().put("package", testPackage).put("recipient", "Synthetic Recipient")
            .put("message", "Synthetic hello").put("target", "Send").toString()
        assertEquals("CONFIRMATION_REQUIRED", registry.execute("send_message", args).errorCode)
        assertEquals("SENSITIVE_ACTION_BLOCKED", registry.execute("tap_element", """{"target":"Send"}""", true).errorCode)
        val wrongRecipient = JSONObject(args).put("recipient", "Different Recipient").toString()
        assertFalse(registry.execute("send_message", wrongRecipient, true).success)
        val sent = registry.execute("send_message", args, true)
        assertTrue(sent.message, sent.success)
        SystemClock.sleep(500)
        val after = registry.execute("observe_screen", "{}")
        assertTrue(after.toJson().contains("Synthetic sent: Synthetic hello"))
        assertFalse("A second send must fail after the draft clears", registry.execute("send_message", args, true).success)
    }
}
