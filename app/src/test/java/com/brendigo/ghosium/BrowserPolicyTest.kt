package com.brendigo.ghosium

import org.junit.Assert.*
import org.junit.Test

class BrowserPolicyTest {
    @Test fun validHttpsCanBeRecorded() {
        assertTrue(BrowserPolicy.shouldRecordVisit("https://example.com/path", false, false))
    }
    @Test fun errorPageIsNotHistory() {
        assertFalse(BrowserPolicy.shouldRecordVisit("https://example.com", false, true))
    }
    @Test fun homePageIsNotHistory() {
        assertFalse(BrowserPolicy.shouldRecordVisit("https://example.com", true, false))
    }
    @Test fun untrustedSchemesAreRejected() {
        assertFalse(BrowserPolicy.isHttpsAddress("javascript:alert(1)"))
        assertFalse(BrowserPolicy.isHttpsAddress("http://example.com"))
        assertFalse(BrowserPolicy.shouldRecordVisit(null,false,false))
    }
    @Test fun filenameIsSanitized() {
        assertEquals("file_name_.pdf",BrowserPolicy.safeFileName("file:name?.pdf"))
        assertEquals("ghosium-download",BrowserPolicy.safeFileName("../.."))
        assertEquals("document.pdf",BrowserPolicy.safeFileName("a/b/document.pdf"))
    }
    @Test fun filenameLengthIsBounded() {
        assertTrue(BrowserPolicy.safeFileName("x".repeat(300)).length <= 120)
    }
}
