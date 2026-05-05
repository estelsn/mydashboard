package com.aifomo.dashboard.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class ContentHashUtil {

    private ContentHashUtil() {
    }

    public static String sha256Normalized(String rawContent) {
        String normalized = normalize(rawContent);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static String normalize(String rawContent) {
        if (rawContent == null) {
            throw new IllegalArgumentException("rawContent must not be null");
        }
        return rawContent.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
