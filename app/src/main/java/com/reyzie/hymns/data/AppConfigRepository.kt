package com.reyzie.hymns.data

import android.content.Context
import android.util.Log

/**
 * Single entry point for Supabase `app_config` remote flags.
 * Mirrors Flutter usage in ChristmasModeService, ForceUpdateService, and CastService.
 */
data class RemoteAppConfig(
    val isChristmasTime: Boolean? = null,
    val forceUpdateEnabled: Boolean? = null,
    val forceUpdateMinVersion: String? = null,
    val forceUpdateMinBuildNumber: Long? = null,
    val forceUpdateMessage: String? = null,
    val forceUpdateAndroidStoreUrl: String? = null,
    val castEnabled: Boolean? = null,
    val castAppId: String? = null,
    val castReceiverUrl: String? = null,
    /** When true (1), the Page Flip option is shown in Settings and available in hymn detail. */
    val pageFlipVisible: Boolean? = null,
    val adminEmails: String? = null,
    val githubToken: String? = null,
    val isMangaloreHymnsEnabled: Boolean? = null
)

object AppConfigKeys {
    const val IS_CHRISTMAS_TIME = "is_christmas_time"
    const val FORCE_UPDATE_ENABLED = "force_update_enabled"
    const val FORCE_UPDATE_MIN_VERSION = "force_update_min_version"
    const val FORCE_UPDATE_MIN_BUILD_NUMBER = "force_update_min_build_number"
    const val FORCE_UPDATE_MESSAGE = "force_update_message"
    const val FORCE_UPDATE_ANDROID_STORE_URL = "force_update_android_store_url"
    const val CAST_ENABLED = "cast_enabled"
    const val CAST_APP_ID = "cast_app_id"
    const val CAST_RECEIVER_URL = "cast_receiver_url"
    const val PAGE_FLIP_VISIBLE = "page_flip_visible"
    const val ADMIN_EMAILS = "admin_emails"
    const val GITHUB_TOKEN = "github_token"
    const val IS_MANGALORE_HYMNS_ENABLED = "is_mangalore_hymns_enabled"
}

class AppConfigRepository(
    private val appConfigService: AppConfigService = AppConfigService(),
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_CHRISTMAS_REMOTE = "is_christmas_time_remote"
        private const val PREF_MANGALORE_REMOTE = "is_mangalore_hymns_enabled_remote"
        private const val PREF_MANUAL_CHRISTMAS_OVERRIDE = "manual_christmas_override"
        /** Default accent — matches Theme.kt BaselineBlueSeed / Blue40 */
        const val DEFAULT_THEME_COLOR = 0xFF0061A4.toInt()
    }

    suspend fun fetchRemoteConfig(): RemoteAppConfig {
        val raw = appConfigService.fetch(
            listOf(
                AppConfigKeys.IS_CHRISTMAS_TIME,
                AppConfigKeys.FORCE_UPDATE_ENABLED,
                AppConfigKeys.FORCE_UPDATE_MIN_VERSION,
                AppConfigKeys.FORCE_UPDATE_MIN_BUILD_NUMBER,
                AppConfigKeys.FORCE_UPDATE_MESSAGE,
                AppConfigKeys.FORCE_UPDATE_ANDROID_STORE_URL,
                AppConfigKeys.CAST_ENABLED,
                AppConfigKeys.CAST_APP_ID,
                AppConfigKeys.CAST_RECEIVER_URL,
                AppConfigKeys.PAGE_FLIP_VISIBLE,
                AppConfigKeys.ADMIN_EMAILS,
                AppConfigKeys.GITHUB_TOKEN,
                AppConfigKeys.IS_MANGALORE_HYMNS_ENABLED
            )
        )

        return RemoteAppConfig(
            isChristmasTime = appConfigService.parseBoolean(raw[AppConfigKeys.IS_CHRISTMAS_TIME]),
            forceUpdateEnabled = appConfigService.parseBoolean(raw[AppConfigKeys.FORCE_UPDATE_ENABLED]),
            forceUpdateMinVersion = raw[AppConfigKeys.FORCE_UPDATE_MIN_VERSION]?.trim()?.takeIf { it.isNotEmpty() },
            forceUpdateMinBuildNumber = raw[AppConfigKeys.FORCE_UPDATE_MIN_BUILD_NUMBER]?.trim()?.toLongOrNull(),
            forceUpdateMessage = raw[AppConfigKeys.FORCE_UPDATE_MESSAGE]?.trim()?.takeIf { it.isNotEmpty() },
            forceUpdateAndroidStoreUrl = raw[AppConfigKeys.FORCE_UPDATE_ANDROID_STORE_URL]?.trim()?.takeIf { it.isNotEmpty() },
            castEnabled = appConfigService.parseBoolean(raw[AppConfigKeys.CAST_ENABLED]),
            castAppId = raw[AppConfigKeys.CAST_APP_ID]?.trim()?.takeIf { it.isNotEmpty() },
            castReceiverUrl = raw[AppConfigKeys.CAST_RECEIVER_URL]?.trim()?.takeIf { it.isNotEmpty() },
            pageFlipVisible = appConfigService.parseBoolean(raw[AppConfigKeys.PAGE_FLIP_VISIBLE]),
            adminEmails = raw[AppConfigKeys.ADMIN_EMAILS]?.trim()?.takeIf { it.isNotEmpty() },
            githubToken = raw[AppConfigKeys.GITHUB_TOKEN]?.trim()?.takeIf { it.isNotEmpty() },
            isMangaloreHymnsEnabled = appConfigService.parseBoolean(raw[AppConfigKeys.IS_MANGALORE_HYMNS_ENABLED])
        ).also { config ->
            // Cache remote values locally
            prefs?.edit()?.apply {
                if (config.isChristmasTime != null) putBoolean(PREF_CHRISTMAS_REMOTE, config.isChristmasTime)
                if (config.isMangaloreHymnsEnabled != null) putBoolean(PREF_MANGALORE_REMOTE, config.isMangaloreHymnsEnabled)
                if (config.githubToken != null) putString("github_token_cached", config.githubToken)
                if (config.adminEmails != null) putString("admin_emails_cached", config.adminEmails)
                if (config.castEnabled != null) putBoolean("cast_enabled_cached", config.castEnabled)
                if (config.pageFlipVisible != null) putBoolean("page_flip_visible_cached", config.pageFlipVisible)
                apply()
            }
            Log.d(
                "AppConfigRepository",
                "Loaded app_config: christmas=${config.isChristmasTime}, mangalore=${config.isMangaloreHymnsEnabled}, cast=${config.castEnabled}, pageFlipVisible=${config.pageFlipVisible}, adminEmails=${config.adminEmails}, githubToken=${config.githubToken != null}"
            )
        }
    }

    fun getCachedRemoteConfig(): RemoteAppConfig {
        return RemoteAppConfig(
            isChristmasTime = cachedChristmasRemote(),
            isMangaloreHymnsEnabled = cachedMangaloreRemote(),
            githubToken = prefs?.getString("github_token_cached", null),
            adminEmails = prefs?.getString("admin_emails_cached", null),
            castEnabled = prefs?.getBoolean("cast_enabled_cached", false),
            pageFlipVisible = prefs?.getBoolean("page_flip_visible_cached", false)
        )
    }

    fun hasManualChristmasOverride(): Boolean {
        return prefs?.getBoolean(PREF_MANUAL_CHRISTMAS_OVERRIDE, false) == true
    }

    fun setManualChristmasOverride(enabled: Boolean) {
        prefs?.edit()?.putBoolean(PREF_MANUAL_CHRISTMAS_OVERRIDE, enabled)?.apply()
    }

    fun clearManualChristmasOverride() {
        prefs?.edit()?.remove(PREF_MANUAL_CHRISTMAS_OVERRIDE)?.apply()
    }

    fun cachedChristmasRemote(): Boolean? {
        if (prefs == null || !prefs.contains(PREF_CHRISTMAS_REMOTE)) return null
        return prefs.getBoolean(PREF_CHRISTMAS_REMOTE, false)
    }

    fun cachedMangaloreRemote(): Boolean? {
        if (prefs == null || !prefs.contains(PREF_MANGALORE_REMOTE)) return null
        return prefs.getBoolean(PREF_MANGALORE_REMOTE, false)
    }
}
