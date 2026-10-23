package com.brendigo.ghosium

import org.junit.Assert.*
import org.junit.Test

class BrowserDestinationTest {
    @Test fun queryUsesGhosiumSearch() {
        assertEquals(BrowserDestination.Search("vrijeme rijeka"),
            BrowserDestination.fromInput("vrijeme rijeka"))
    }
    @Test fun domainIsPage() {
        assertEquals(BrowserDestination.Page("https://example.com"),
            BrowserDestination.fromInput("example.com"))
    }
    @Test fun httpsIsPage() {
        assertEquals(BrowserDestination.Page("https://example.com/x"),
            BrowserDestination.fromInput("https://example.com/x"))
    }
    @Test fun dangerousSchemeInvalid() {
        assertEquals(BrowserDestination.Invalid,
            BrowserDestination.fromInput("javascript:alert(1)"))
    }
    @Test fun httpSchemeIsNotSearch() {
        assertEquals(BrowserDestination.Invalid,
            BrowserDestination.fromInput("http://example.com"))
    }
    @Test fun emptyIsInvalid() {
        assertEquals(BrowserDestination.Invalid,BrowserDestination.fromInput(" "))
    }
    @Test fun credentialsRejected() {
        assertEquals(BrowserDestination.Invalid,
            BrowserDestination.fromInput("https://test:pw@example.com"))
    }
    @Test fun customPortOpensPage() { assertEquals(BrowserDestination.Page("https://example.com:8443"), BrowserDestination.fromInput("example.com:8443")) }
    @Test fun mailtoCannotLaunch() { assertEquals(BrowserDestination.Invalid, BrowserDestination.fromInput("mailto:someone@example.com")) }

}
