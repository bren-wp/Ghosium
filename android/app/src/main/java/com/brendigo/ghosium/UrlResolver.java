package com.brendigo.ghosium;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Pattern;

final class UrlResolver {
    static final String NEW_TAB_URL = "https://appassets.androidplatform.net/assets/newtab.html";
    private static final Pattern SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*$");
    private static final Pattern WHITESPACE = Pattern.compile(".*\\s+.*");
    private static final Pattern DOMAIN_LABEL =
            Pattern.compile("(?i)^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");

    private UrlResolver() {}

    static String resolve(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return NEW_TAB_URL;
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) return value;
        if (looksLikeHost(value)) return "https://" + value;
        if (SCHEME.matcher(value).matches()) return value;
        return "https://www.google.com/search?q=" + encodeQuery(value);
    }

    static boolean isHttpOrHttps(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    static boolean isNewTab(String value) {
        return NEW_TAB_URL.equals(value);
    }

    private static String encodeQuery(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException("UTF-8 is unavailable", impossible);
        }
    }

    private static boolean looksLikeHost(String value) {
        if (WHITESPACE.matcher(value).matches() || value.startsWith(".") || value.endsWith(".")) return false;
        String hostPart = value;
        int delimiter = firstDelimiter(hostPart);
        if (delimiter >= 0) hostPart = hostPart.substring(0, delimiter);
        if (hostPart.isEmpty()) return false;

        int colon = hostPart.lastIndexOf(':');
        if (colon > 0 && hostPart.indexOf(':') == colon) {
            String port = hostPart.substring(colon + 1);
            if (!port.matches("\\d{1,5}")) return false;
            try {
                int portValue = Integer.parseInt(port);
                if (portValue < 1 || portValue > 65535) return false;
            } catch (NumberFormatException error) {
                return false;
            }
            hostPart = hostPart.substring(0, colon);
        }

        if ("localhost".equalsIgnoreCase(hostPart)) return true;
        if (isValidIpv4(hostPart)) return true;
        return isValidDomain(hostPart);
    }

    private static int firstDelimiter(String value) {
        int first = -1;
        for (char delimiter : new char[]{'/', '?', '#'}) {
            int index = value.indexOf(delimiter);
            if (index >= 0 && (first < 0 || index < first)) first = index;
        }
        return first;
    }

    private static boolean isValidIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.matches("\\d+")) return false;
            try {
                int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255) return false;
            } catch (NumberFormatException error) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidDomain(String host) {
        final String ascii;
        try {
            ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES);
        } catch (IllegalArgumentException error) {
            return false;
        }
        if (ascii.length() > 253 || !ascii.contains(".")) return false;
        for (String label : ascii.split("\\.", -1)) {
            if (!DOMAIN_LABEL.matcher(label).matches()) return false;
        }
        return true;
    }
}
