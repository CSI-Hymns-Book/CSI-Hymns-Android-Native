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

    fun verifyPasscode(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed == "2026" || trimmed.equals("csiroot", ignoreCase = true) || trimmed.equals("reyzie", ignoreCase = true)
    }
}
