package com.brendigo.ghosium

import org.junit.Assert.*
import org.junit.Test

class UrlResolverTest {
    @Test fun opensHttpsSite() { assertEquals("https://example.com", UrlResolver.resolve("example.com")) }
    @Test fun preservesHttps() { assertEquals("https://example.com/path", UrlResolver.resolve("https://example.com/path")) }
    @Test fun blocksHttp() { assertNull(UrlResolver.resolve("http://example.com")) }
    @Test fun blocksDangerousScheme() { assertNull(UrlResolver.resolve("javascript:alert(1)")) }
    @Test fun blocksExternalScheme() { assertNull(UrlResolver.resolve("intent://example.com")) }
    @Test fun blocksSchemeRelative() { assertNull(UrlResolver.resolve("//evil.example")) }
    @Test fun blocksCredentials() { assertNull(UrlResolver.resolve("https://user@evil.example")) }
    @Test fun blocksMissingHost() { assertNull(UrlResolver.resolve("https://")) }
    @Test fun blocksBadPort() { assertNull(UrlResolver.resolve("example.com:99999")) }
    @Test fun searchesPhrase() { assertEquals("https://duckduckgo.com/?q=hrvatske+novosti", UrlResolver.resolve("hrvatske novosti")) }
    @Test fun searchesEmail() { assertEquals("https://duckduckgo.com/?q=ime%40example.com", UrlResolver.resolve("ime@example.com")) }
    @Test fun ignoresEmpty() { assertNull(UrlResolver.resolve("    ")) }
}
