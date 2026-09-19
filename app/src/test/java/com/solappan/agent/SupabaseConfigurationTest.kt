package com.solappan.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class SupabaseConfigurationTest {
    @Test fun `accepts HTTPS project endpoint`() {
        assertEquals("https://demo.supabase.co", verifiedSupabaseUrl("https://demo.supabase.co/"))
    }
    @Test fun `rejects credential exfiltration endpoints`() {
        listOf("http://demo.supabase.co", "https://supabase.co.evil.test", "https://user@demo.supabase.co", "https://demo.supabase.co/path", "https://demo.supabase.co?x=1").forEach {
            try { verifiedSupabaseUrl(it); throw AssertionError("Accepted $it") } catch (_: IllegalArgumentException) { }
        }
    }
}
