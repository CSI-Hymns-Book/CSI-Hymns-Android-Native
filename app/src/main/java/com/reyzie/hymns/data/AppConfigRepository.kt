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
    val pageFlipVisible: Boolean? = null
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
}

class AppConfigRepository(
    private val appConfigService: AppConfigService = AppConfigService(),
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_CHRISTMAS_REMOTE = "is_christmas_time_remote"
        private const val PREF_MANUAL_CHRISTMAS_OVERRIDE = "manual_christmas_override"
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
                AppConfigKeys.PAGE_FLIP_VISIBLE
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
            pageFlipVisible = appConfigService.parseBoolean(raw[AppConfigKeys.PAGE_FLIP_VISIBLE])
        ).also { config ->
            config.isChristmasTime?.let { value ->
                prefs?.edit()?.putBoolean(PREF_CHRISTMAS_REMOTE, value)?.apply()
            }
            Log.d(
                "AppConfigRepository",
                "Loaded app_config: christmas=${config.isChristmasTime}, cast=${config.castEnabled}, pageFlipVisible=${config.pageFlipVisible}"
            )
        }
    }

    fun hasManualChristmasOverride(): Boolean {
        return prefs?.getBoolean(PREF_MANUAL_CHRISTMAS_OVERRIDE, false) == true
    }

    fun setManualChristmasOverride(enabled: Boolean) {
        prefs?.edit()?.putBoolean(PREF_MANUAL_CHRISTMAS_OVERRIDE, enabled)?.apply()
    }

    fun cachedChristmasRemote(): Boolean? {
        if (prefs == null || !prefs.contains(PREF_CHRISTMAS_REMOTE)) return null
        return prefs.getBoolean(PREF_CHRISTMAS_REMOTE, false)
    }
}
