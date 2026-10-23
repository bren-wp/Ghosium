package com.brendigo.ghosium

import org.junit.Assert.*
import org.junit.Test

class SessionPolicyTest {
    @Test fun invalidRestoredUrlsBecomeHome() {
        val state = SessionPolicy.normalize(listOf(
            SessionPolicy.Entry("javascript:alert(1)", false),
            SessionPolicy.Entry("https://example.com", false)), 0)
        assertTrue(state.entries[0].home)
        assertEquals("", state.entries[0].url)
        assertEquals("https://example.com", state.entries[1].url)
    }
    @Test fun capsTabCount() {
        val s=SessionPolicy.normalize(List(20) {
            SessionPolicy.Entry("https://example.com", false)
        }, 19)
        assertEquals(8, s.entries.size)
        assertEquals(7, s.selected)
    }
    @Test fun emptyStateAlwaysHasHome() {
        val s=SessionPolicy.normalize(emptyList(), -1)
        assertEquals(1, s.entries.size)
        assertTrue(s.entries[0].home)
        assertEquals(0, s.selected)
    }
    @Test fun protectsOutOfBoundsIndex() {
        val s=SessionPolicy.normalize(listOf(SessionPolicy.Entry("",true)), -42)
        assertEquals(0,s.selected)
    }
    @Test fun homeNeverRetainsStaleUrl() {
        val s=SessionPolicy.normalize(listOf(SessionPolicy.Entry("https://example.com",true)),0)
        assertEquals("",s.entries.single().url)
    }
}
