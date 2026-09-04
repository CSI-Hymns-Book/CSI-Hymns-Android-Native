package com.reyzie.hymns.data

import android.content.Context

object AdminPrefs {
    private const val PREF_SUDO_ADMIN = "is_sudo_admin_mode_enabled"

    fun isSudoAdminEnabled(context: Context): Boolean {
        return context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .getBoolean(PREF_SUDO_ADMIN, false)
    }

    fun setSudoAdminEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_SUDO_ADMIN, enabled)
            .apply()
    }

    suspend fun verifyPasscode(context: Context, input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false

        // 1. Check DB remote app_config passcode
        try {
            val remoteConfig = AppConfigRepository(context = context).fetchRemoteConfig()
            val remotePasscode = remoteConfig.masterRootPasscode?.trim()
            if (!remotePasscode.isNullOrEmpty() && trimmed == remotePasscode) {
                return true
            }
        } catch (_: Exception) {}

        // 2. Check cached passcode in SharedPreferences
        val cachedPasscode = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .getString("cached_master_root_passcode", null)?.trim()
        if (!cachedPasscode.isNullOrEmpty() && trimmed == cachedPasscode) {
            return true
        }

        return false
    }

    enum class AdminRole(val roleKey: String, val label: String) {
        ADMIN("admin", "Super Admin"),
        LYRICS("lyrics", "Lyric Corrections"),
        PR_MANAGER("pr_manager", "Announcements Manager"),
        APP_CONFIG("app_config", "App Config Manager"),
        TUNE_METER_VIEW("tune_meter_view", "Tune Meters View");

        companion object {
            fun fromKey(key: String): AdminRole? {
                val normalized = key.lowercase().trim()
                return entries.firstOrNull { it.roleKey == normalized }
            }
        }
    }

    fun parseAdminRoles(adminEmailsConfig: String?): Map<String, Set<AdminRole>> {
        if (adminEmailsConfig.isNullOrBlank()) return emptyMap()
        val raw = normalizeAdminEmailsRaw(adminEmailsConfig)
        val resultMap = mutableMapOf<String, MutableSet<AdminRole>>()

        try {
            if (raw.startsWith("{")) {
                val jsonObject = org.json.JSONObject(raw)
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val originalEmail = keys.next()
                    val email = originalEmail.lowercase().trim()
                    val roleSet = mutableSetOf<AdminRole>()
                    when (val value = jsonObject.opt(originalEmail)) {
                        is org.json.JSONArray -> {
                            for (i in 0 until value.length()) {
                                val roleStr = value.optString(i)
                                AdminRole.fromKey(roleStr)?.let { roleSet.add(it) }
                            }
                        }
                        is String -> {
                            val roleStr = value.lowercase().trim()
                            if (roleStr == "admin" || roleStr == "all") {
                                roleSet.add(AdminRole.ADMIN)
                            } else {
                                AdminRole.fromKey(roleStr)?.let { roleSet.add(it) }
                            }
                        }
                    }
                    if (roleSet.isNotEmpty()) {
                        resultMap[email] = roleSet
                    }
                }
                return resultMap
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminPrefs", "Failed to parse admin_emails as JSON object", e)
        }

        // Fallback: Comma-separated list or JSON array of email strings -> grant default ADMIN role
        val cleanedList = raw.replace("[", "").replace("]", "").replace("\"", "").replace("'", "")
            .split(",").map { it.lowercase().trim() }.filter { it.isNotEmpty() }

        for (email in cleanedList) {
            resultMap[email] = mutableSetOf(AdminRole.ADMIN)
        }
        return resultMap
    }

    /**
     * Normalize legacy DB values where admin_emails was stored as a JSON **string**
     * (with escaped \\n) instead of a jsonb object.
     */
    fun normalizeAdminEmailsRaw(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s

        // Unwrap jsonb string wrapping: "\"{ ... }\""
        if (s.startsWith("\"") && s.endsWith("\"")) {
            runCatching {
                val unquoted = org.json.JSONTokener(s).nextValue()
                if (unquoted is String) s = unquoted.trim()
            }
        }

        // Literal backslash-n sequences from bad saves (not real newlines).
        if (s.contains("\\n") && !s.contains('\n')) {
            s = s.replace("\\n", "\n").replace("\\\"", "\"")
        }

        return s.trim()
    }

    /** Pretty-print admin email JSON for the App Config editor. */
    fun prettifyAdminEmailsConfig(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val normalized = normalizeAdminEmailsRaw(raw)
        return try {
            when {
                normalized.startsWith("{") -> org.json.JSONObject(normalized).toString(2)
                normalized.startsWith("[") -> org.json.JSONArray(normalized).toString(2)
                else -> normalized
            }
        } catch (_: Exception) {
            normalized
        }
    }

    fun hasRole(
        context: Context,
        currentUserEmail: String?,
        adminEmailsConfig: String?,
        requiredRole: AdminRole
    ): Boolean {
        // 1. Sudo Master Passcode Mode ALWAYS has ALL permissions!
        if (isSudoAdminEnabled(context)) {
            return true
        }
        if (currentUserEmail.isNullOrBlank()) return false
        val normalizedEmail = currentUserEmail.lowercase().trim()
        val rolesMap = parseAdminRoles(adminEmailsConfig)
        val userRoles = rolesMap[normalizedEmail] ?: return false

        return userRoles.contains(AdminRole.ADMIN) || userRoles.contains(requiredRole)
    }

    fun hasAnyAdminRole(
        context: Context,
        currentUserEmail: String?,
        adminEmailsConfig: String?
    ): Boolean {
        if (isSudoAdminEnabled(context)) return true
        if (currentUserEmail.isNullOrBlank()) return false
        val normalizedEmail = currentUserEmail.lowercase().trim()
        val rolesMap = parseAdminRoles(adminEmailsConfig)
        return (rolesMap[normalizedEmail]?.isNotEmpty() == true)
    }
}
