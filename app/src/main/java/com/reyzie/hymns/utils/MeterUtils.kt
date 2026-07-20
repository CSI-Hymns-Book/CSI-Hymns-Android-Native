package com.reyzie.hymns.utils

import java.net.URLEncoder

object MeterUtils {
    /**
     * Normalizes a meter signature by removing spaces, converting to lowercase, and stripping trailing dots.
     * Example: "C.M." -> "c.m", "7. 6. 7. 6. D." -> "7.6.7.6.d"
     */
    fun getNormalizedMeter(signature: String?): String {
        if (signature.isNullOrBlank()) return "default"
        var clean = signature.trim().replace(" ", "").lowercase()
        while (clean.endsWith(".")) {
            clean = clean.dropLast(1)
        }
        return clean
    }

    /**
     * Converts a hymn signature (meter) string into a clean, normalized midi file name (URL safe).
     * Example: "C.M." -> "C.M.mid", "7. 6. 7. 6. D." -> "7.6.7.6.D.mid"
     */
    fun getMeterMidiFileName(signature: String?): String {
        if (signature.isNullOrBlank()) return "default"
        
        // Trim, remove all spaces (so "7. 6. 7. 6." becomes "7.6.7.6.")
        var clean = signature.trim().replace(" ", "")
        
        // Strip any trailing dot to avoid double dots when appending ".mid"
        while (clean.endsWith(".")) {
            clean = clean.dropLast(1)
        }
        
        // URL encode the clean name to make it safe for HTTP requests
        return try {
            URLEncoder.encode(clean, "UTF-8")
                .replace("+", "%20")
        } catch (e: Exception) {
            clean
        }
    }
}
