package com.reyzie.hymns.data

object AdminRls {
    const val SUDO_CLOUD_SAVE_MESSAGE =
        "Sign in with an admin account to save to the server. Sudo only unlocks this device’s admin UI."

    fun mapSaveError(throwable: Throwable): String {
        val raw = listOfNotNull(throwable.message, throwable.toString())
            .joinToString(" ")
            .lowercase()
        val looksLikeRls = raw.contains("row-level security") ||
            raw.contains("42501") ||
            raw.contains("permission denied") ||
            raw.contains("rls") ||
            raw.contains("not allowed") ||
            (raw.contains("403") && raw.contains("policy"))
        return if (looksLikeRls) SUDO_CLOUD_SAVE_MESSAGE
        else throwable.localizedMessage ?: throwable.message ?: throwable.toString()
    }
}
