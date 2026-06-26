package com.reyzie.hymns.data

import android.content.Context

/** First-run onboarding + privacy + changelog sequencing (mirrors Flutter OnboardingPrefs). */
object OnboardingPrefs {
    const val WELCOME_COMPLETED = "onboarding_welcome_completed"
    const val PRIVACY_ACCEPTED_LOCAL = "privacy_policy_accepted_local"
    const val PENDING_CHANGELOG_AFTER_ONBOARDING = "pending_changelog_after_onboarding"
    const val PENDING_MENU_SHOWCASE = "pending_menu_showcase"
    const val MENU_SHOWCASE_DONE = "menu_showcase_done"
    const val APP_LAUNCH_COUNT = "app_launch_count"
    const val NOTIFICATION_PROMPT_DONE = "notification_prompt_done"
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

    fun incrementLaunchCount(context: Context): Int {
        val prefs = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
        val next = prefs.getInt(APP_LAUNCH_COUNT, 0) + 1
        prefs.edit().putInt(APP_LAUNCH_COUNT, next).apply()
        return next
    }

    fun getLaunchCount(context: Context): Int =
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .getInt(APP_LAUNCH_COUNT, 0)

    fun isNotificationPromptDone(context: Context): Boolean =
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .getBoolean(NOTIFICATION_PROMPT_DONE, false)

    fun markNotificationPromptDone(context: Context) {
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean(NOTIFICATION_PROMPT_DONE, true)
            .apply()
    }

    fun isPendingMenuShowcase(context: Context): Boolean =
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .getBoolean(PENDING_MENU_SHOWCASE, false)

    fun isMenuShowcaseDone(context: Context): Boolean =
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
            .getBoolean(MENU_SHOWCASE_DONE, false)

    fun markMenuShowcaseDone(context: Context) {
        context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean(MENU_SHOWCASE_DONE, true)
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
