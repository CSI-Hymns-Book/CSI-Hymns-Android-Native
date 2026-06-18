package com.reyzie.hymns.data

import android.content.Context

/** First-run onboarding + privacy + changelog sequencing (mirrors Flutter OnboardingPrefs). */
object OnboardingPrefs {
    const val WELCOME_COMPLETED = "onboarding_welcome_completed"
    const val PRIVACY_ACCEPTED_LOCAL = "privacy_policy_accepted_local"
    const val PENDING_CHANGELOG_AFTER_ONBOARDING = "pending_changelog_after_onboarding"
    const val PENDING_MENU_SHOWCASE = "pending_menu_showcase"
    const val MENU_SHOWCASE_DONE = "menu_showcase_done"
    private const val LEGACY_IS_FIRST_RUN = "isFirstRun"

    fun migrateFromLegacy(context: Context) {
        val prefs = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
        if (prefs.contains(WELCOME_COMPLETED)) return
        if (prefs.getBoolean(LEGACY_IS_FIRST_RUN, true) == false) {
            prefs.edit()
                .putBoolean(WELCOME_COMPLETED, true)
                .putBoolean(MENU_SHOWCASE_DONE, true)
                .putBoolean(PENDING_CHANGELOG_AFTER_ONBOARDING, false)
                .putBoolean(PENDING_MENU_SHOWCASE, false)
                .apply()
        }
    }

    fun isWelcomeCompleted(context: Context): Boolean =
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .getBoolean(WELCOME_COMPLETED, false)

    fun markWelcomeCompleted(context: Context, privacyAccepted: Int, pendingChangelog: Boolean) {
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean(WELCOME_COMPLETED, true)
            .putInt(PRIVACY_ACCEPTED_LOCAL, privacyAccepted)
            .putBoolean(PENDING_CHANGELOG_AFTER_ONBOARDING, pendingChangelog)
            .putBoolean(PENDING_MENU_SHOWCASE, false)
            .apply()
    }

    fun isPendingChangelogAfterOnboarding(context: Context): Boolean =
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .getBoolean(PENDING_CHANGELOG_AFTER_ONBOARDING, false)

    fun clearPendingChangelogAfterOnboarding(context: Context) {
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean(PENDING_CHANGELOG_AFTER_ONBOARDING, false)
            .apply()
    }
}
