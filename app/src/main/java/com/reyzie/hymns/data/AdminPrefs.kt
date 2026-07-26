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
        APP_CONFIG("app_config", "App Config Manager");

        companion object {
            fun fromKey(key: String): AdminRole? {
                val normalized = key.lowercase().trim()
                return entries.firstOrNull { it.roleKey == normalized }
            }
        }
    }

    fun parseAdminRoles(adminEmailsConfig: String?): Map<String, Set<AdminRole>> {
        if (adminEmailsConfig.isNullOrBlank()) return emptyMap()
        val raw = adminEmailsConfig.trim()
        val resultMap = mutableMapOf<String, MutableSet<AdminRole>>()

        try {
            if (raw.startsWith("{")) {
                val jsonObject = org.json.JSONObject(raw)
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val email = keys.next().lowercase().trim()
                    val roleSet = mutableSetOf<AdminRole>()
                    val value = jsonObject.opt(email)
                    if (value is org.json.JSONArray) {
                        for (i in 0 until value.length()) {
                            val roleStr = value.optString(i)
                            AdminRole.fromKey(roleStr)?.let { roleSet.add(it) }
                        }
                    } else if (value is String) {
                        val roleStr = value.lowercase().trim()
                        if (roleStr == "admin" || roleStr == "all") {
                            roleSet.add(AdminRole.ADMIN)
                        } else {
                            AdminRole.fromKey(roleStr)?.let { roleSet.add(it) }
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
