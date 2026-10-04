package com.brendigo.ghosium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class UrlResolverTest {
    @Test public void emptyInputOpensNewTab() { assertEquals(UrlResolver.NEW_TAB_URL, UrlResolver.resolve("   ")); }
    @Test public void httpsUrlIsPreserved() { assertEquals("https://example.com/a", UrlResolver.resolve("https://example.com/a")); }
    @Test public void httpUrlIsPreserved() { assertEquals("http://example.com", UrlResolver.resolve("http://example.com")); }
    @Test public void hostGetsHttps() { assertEquals("https://example.com/docs", UrlResolver.resolve("example.com/docs")); }
    @Test public void localhostGetsHttps() { assertEquals("https://localhost:8443/test", UrlResolver.resolve("localhost:8443/test")); }
    @Test public void validIpv4GetsHttps() { assertEquals("https://192.168.1.10:8443/path", UrlResolver.resolve("192.168.1.10:8443/path")); }
    @Test public void internationalDomainGetsHttps() { assertEquals("https://münchen.de", UrlResolver.resolve("münchen.de")); }
    @Test public void invalidIpv4FallsBackToSearch() { assertEquals("https://www.google.com/search?q=999.1.1.1", UrlResolver.resolve("999.1.1.1")); }
    @Test public void malformedDomainFallsBackToSearch() { assertEquals("https://www.google.com/search?q=example..com", UrlResolver.resolve("example..com")); }
    @Test public void invalidPortFallsBackToSearch() { assertEquals("https://www.google.com/search?q=example.com%3A70000", UrlResolver.resolve("example.com:70000")); }
    @Test public void queryUsesGoogle() { assertEquals("https://www.google.com/search?q=privacy+browser", UrlResolver.resolve("privacy browser")); }
    @Test public void externalSchemeIsPreservedForNativeDispatch() { assertEquals("mailto:hello@example.com", UrlResolver.resolve("mailto:hello@example.com")); }
    @Test public void schemeHelpersAreStrict() {
        assertTrue(UrlResolver.isHttpOrHttps("HTTPS://example.com"));
        assertFalse(UrlResolver.isHttpOrHttps("javascript:alert(1)"));
        assertTrue(UrlResolver.isNewTab(UrlResolver.NEW_TAB_URL));
    }
}
