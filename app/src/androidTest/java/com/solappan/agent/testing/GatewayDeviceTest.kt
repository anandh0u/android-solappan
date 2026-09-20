package com.solappan.agent.testing

import androidx.test.platform.app.InstrumentationRegistry
import com.solappan.agent.AgentController
import com.solappan.agent.BuildConfig
import com.solappan.agent.OpenAiClient
import com.solappan.agent.SupabaseSession
import com.solappan.agent.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Uses the user's already-saved session, never takes credentials as test arguments. */
class GatewayDeviceTest {
    @Test fun savedSessionCompletesRegisteredToolRoundTrip() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("This qualification requires gateway mode", BuildConfig.USE_GATEWAY)
        assertTrue("Provider credential must be absent", BuildConfig.OPENAI_API_KEY.isEmpty())
        assertNotNull("Sign in through SOL Setup first", SupabaseSession(context).accountEmail())
        val registry = ToolRegistry.sessionThree(context)
        assertNull("Development mock must not reach the gateway", registry.find("echo"))
        val result = AgentController(OpenAiClient(context = context), registry).run(
            "Use list_apps exactly once to check that installed app discovery works. Do not open apps or execute any other tools. Then reply briefly without listing app names.",
        )
        assertTrue(result.exceptionOrNull()?.message ?: "Gateway failed", result.isSuccess)
        val run = result.getOrThrow()
        assertTrue("Expected a real registered tool execution", run.toolExecutions.any { it.name == "list_apps" && it.success })
        assertTrue(run.finalText.isNotBlank())
    }
}
