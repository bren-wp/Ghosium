package com.brendigo.ghosium

import org.junit.Assert.*
import org.junit.Test

class UrlResolverTest {
    @Test fun opensHttpsSite() { assertEquals("https://example.com", UrlResolver.resolve("example.com")) }
    @Test fun preservesHttps() { assertEquals("https://example.com/path", UrlResolver.resolve("https://example.com/path")) }
    @Test fun blocksHttp() { assertNull(UrlResolver.resolve("http://example.com")) }
    @Test fun blocksDangerousScheme() { assertNull(UrlResolver.resolve("javascript:alert(1)")) }
    @Test fun searchesPhrase() { assertEquals("https://duckduckgo.com/?q=hrvatske+novosti", UrlResolver.resolve("hrvatske novosti")) }
    @Test fun ignoresEmpty() { assertNull(UrlResolver.resolve("    ")) }
}
