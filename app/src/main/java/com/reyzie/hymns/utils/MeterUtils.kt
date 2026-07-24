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
        
        // Trim, remove all spaces
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

    /**
     * Extracts and formats the tune name from a signature/config option string.
     * Example: "11.11.11.5_Christe_fons_jugis" -> "Christe Fons Jugis"
     * Example: "11.11.11.5_fleming" -> "Fleming"
     * Example: "11.11.11.5" -> "11.11.11.5"
     */
    fun getDisplayTuneName(option: String): String {
        var cleanOption = option.trim()
        if (cleanOption.lowercase().startsWith("hymn_")) {
            val suffix = cleanOption.substringAfter("_")
            if (!suffix.contains("_")) {
                return "Hymn $suffix"
            }
            cleanOption = suffix
        }
        if (cleanOption.firstOrNull()?.isDigit() == true && cleanOption.contains("_")) {
            cleanOption = cleanOption.substringAfter("_")
        }
        val targetText = if (cleanOption.contains("_") && option.contains("_")) {
            if (option.lowercase().startsWith("hymn_") || option.firstOrNull()?.isDigit() == true) {
                cleanOption
            } else {
                option.substringAfter("_")
            }
        } else {
            cleanOption
        }
        
        if (targetText.lowercase().matches(Regex("^v\\d+$"))) {
            val vNum = targetText.drop(1)
            return "Version $vNum"
        }

        if (!targetText.contains("_")) {
            return targetText.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        
        return targetText.split("_")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
