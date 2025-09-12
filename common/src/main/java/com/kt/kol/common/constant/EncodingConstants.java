package com.kt.kol.common.constant;

/**
 * Character encoding constants for consistent encoding handling
 */
public final class EncodingConstants {
    
    // Character Encodings
    public static final String UTF_8 = "UTF-8";
    public static final String ISO_8859_1 = "ISO-8859-1";
    
    // Prevent instantiation
    private EncodingConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}