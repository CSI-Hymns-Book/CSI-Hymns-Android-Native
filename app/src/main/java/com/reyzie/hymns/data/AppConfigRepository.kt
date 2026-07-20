package com.reyzie.hymns.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

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
    val isMangaloreHymnsEnabled: Boolean? = null,
    val midiHymnsRanges: String? = null,
    val midiKeerthanesRanges: String? = null
) {
    val parsedMidiHymns: Set<String> by lazy { parseMeters(midiHymnsRanges) }
    val parsedMidiKeerthanes: Set<Int> by lazy { parseRanges(midiKeerthanesRanges) }

    companion object {
        fun parseMeters(metersStr: String?): Set<String> {
            if (metersStr.isNullOrBlank()) return emptySet()
            return metersStr.split(",")
                .map { com.reyzie.hymns.utils.MeterUtils.getNormalizedMeter(it) }
                .filter { it.isNotEmpty() && it != "default" }
                .toSet()
        }

        fun parseRanges(rangeStr: String?): Set<Int> {
            if (rangeStr.isNullOrBlank()) return emptySet()
            val numbers = mutableSetOf<Int>()
            val parts = rangeStr.split(",")
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.contains("-")) {
                    val bounds = trimmed.split("-")
                    if (bounds.size == 2) {
                        val start = bounds[0].trim().toIntOrNull()
                        val end = bounds[1].trim().toIntOrNull()
                        if (start != null && end != null && start <= end) {
                            for (i in start..end) {
                                numbers.add(i)
                            }
                        }
                    }
                } else {
                    val num = trimmed.toIntOrNull()
                    if (num != null) {
                        numbers.add(num)
                    }
                }
            }
            return numbers
        }
    }
}

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
    const val MIDI_HYMNS_RANGES = "midi_hymns_ranges"
    const val MIDI_KEERTHANES_RANGES = "midi_keerthanes_ranges"
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
                AppConfigKeys.IS_MANGALORE_HYMNS_ENABLED,
                AppConfigKeys.MIDI_HYMNS_RANGES,
                AppConfigKeys.MIDI_KEERTHANES_RANGES
            )
        )

        val remoteConfig = RemoteAppConfig(
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
            isMangaloreHymnsEnabled = appConfigService.parseBoolean(raw[AppConfigKeys.IS_MANGALORE_HYMNS_ENABLED]),
            midiHymnsRanges = raw[AppConfigKeys.MIDI_HYMNS_RANGES]?.trim(),
            midiKeerthanesRanges = raw[AppConfigKeys.MIDI_KEERTHANES_RANGES]?.trim()
        )

        // Cache remote values locally
        prefs?.edit()?.apply {
            if (remoteConfig.isChristmasTime != null) putBoolean("is_christmas_time_cached", remoteConfig.isChristmasTime)
            if (remoteConfig.forceUpdateEnabled != null) putBoolean("force_update_enabled_cached", remoteConfig.forceUpdateEnabled)
            putString("force_update_min_version_cached", remoteConfig.forceUpdateMinVersion)
            putLong("force_update_min_build_number_cached", remoteConfig.forceUpdateMinBuildNumber ?: 0L)
            putString("force_update_message_cached", remoteConfig.forceUpdateMessage)
            putString("force_update_android_store_url_cached", remoteConfig.forceUpdateAndroidStoreUrl)
            if (remoteConfig.castEnabled != null) putBoolean("cast_enabled_cached", remoteConfig.castEnabled)
            putString("cast_app_id_cached", remoteConfig.castAppId)
            putString("cast_receiver_url_cached", remoteConfig.castReceiverUrl)
            if (remoteConfig.pageFlipVisible != null) putBoolean("page_flip_visible_cached", remoteConfig.pageFlipVisible)
            putString("admin_emails_cached", remoteConfig.adminEmails)
            putString("github_token_cached", remoteConfig.githubToken)
            if (remoteConfig.isMangaloreHymnsEnabled != null) putBoolean("is_mangalore_hymns_enabled_cached", remoteConfig.isMangaloreHymnsEnabled)
            putString("midi_hymns_ranges_cached", remoteConfig.midiHymnsRanges)
            putString("midi_keerthanes_ranges_cached", remoteConfig.midiKeerthanesRanges)
            
            // Legacy / flutter compatibility
            if (remoteConfig.isChristmasTime != null) putBoolean(PREF_CHRISTMAS_REMOTE, remoteConfig.isChristmasTime)
            if (remoteConfig.isMangaloreHymnsEnabled != null) putBoolean(PREF_MANGALORE_REMOTE, remoteConfig.isMangaloreHymnsEnabled)
            apply()
        }

        Log.d(
            "AppConfigRepository",
            "Loaded app_config: christmas=${remoteConfig.isChristmasTime}, mangalore=${remoteConfig.isMangaloreHymnsEnabled}, cast=${remoteConfig.castEnabled}, pageFlipVisible=${remoteConfig.pageFlipVisible}, adminEmails=${remoteConfig.adminEmails}, githubToken=${remoteConfig.githubToken != null}, midiHymns=${remoteConfig.midiHymnsRanges}, midiKeerthanes=${remoteConfig.midiKeerthanesRanges}"
        )

        return applyLocalOverrides(remoteConfig)
    }

    fun getCachedRemoteConfig(): RemoteAppConfig {
        val cached = RemoteAppConfig(
            isChristmasTime = if (prefs?.contains("is_christmas_time_cached") == true) prefs.getBoolean("is_christmas_time_cached", false) else cachedChristmasRemote(),
            forceUpdateEnabled = prefs?.getBoolean("force_update_enabled_cached", false),
            forceUpdateMinVersion = prefs?.getString("force_update_min_version_cached", null),
            forceUpdateMinBuildNumber = prefs?.getLong("force_update_min_build_number_cached", 0L)?.takeIf { it > 0 },
            forceUpdateMessage = prefs?.getString("force_update_message_cached", null),
            forceUpdateAndroidStoreUrl = prefs?.getString("force_update_android_store_url_cached", null),
            castEnabled = prefs?.getBoolean("cast_enabled_cached", false),
            castAppId = prefs?.getString("cast_app_id_cached", null),
            castReceiverUrl = prefs?.getString("cast_receiver_url_cached", null),
            pageFlipVisible = prefs?.getBoolean("page_flip_visible_cached", false),
            adminEmails = prefs?.getString("admin_emails_cached", null),
            githubToken = prefs?.getString("github_token_cached", null),
            isMangaloreHymnsEnabled = if (prefs?.contains("is_mangalore_hymns_enabled_cached") == true) prefs.getBoolean("is_mangalore_hymns_enabled_cached", false) else cachedMangaloreRemote(),
            midiHymnsRanges = prefs?.getString("midi_hymns_ranges_cached", null),
            midiKeerthanesRanges = prefs?.getString("midi_keerthanes_ranges_cached", null)
        )
        return applyLocalOverrides(cached)
    }

    fun isLocalOverridesEnabled(): Boolean {
        val enabled = prefs?.getBoolean("app_config_use_local_overrides", false) == true
        android.util.Log.d("AppConfigRepository", "isLocalOverridesEnabled: $enabled")
        return enabled
    }

    fun setLocalOverridesEnabled(enabled: Boolean) {
        android.util.Log.d("AppConfigRepository", "setLocalOverridesEnabled: $enabled")
        prefs?.edit()?.putBoolean("app_config_use_local_overrides", enabled)?.commit()
    }

    fun applyLocalOverrides(config: RemoteAppConfig): RemoteAppConfig {
        val enabled = isLocalOverridesEnabled()
        android.util.Log.d("AppConfigRepository", "applyLocalOverrides: enabled=$enabled, inputConfig=$config")
        if (!enabled) return config
        val overridden = RemoteAppConfig(
            isChristmasTime = if (prefs?.contains("app_config_override_is_christmas_time") == true) {
                prefs.getBoolean("app_config_override_is_christmas_time", false)
            } else config.isChristmasTime,
            forceUpdateEnabled = if (prefs?.contains("app_config_override_force_update_enabled") == true) {
                prefs.getBoolean("app_config_override_force_update_enabled", false)
            } else config.forceUpdateEnabled,
            forceUpdateMinVersion = prefs?.getString("app_config_override_force_update_min_version", null) ?: config.forceUpdateMinVersion,
            forceUpdateMinBuildNumber = if (prefs?.contains("app_config_override_force_update_min_build_number") == true) {
                prefs.getLong("app_config_override_force_update_min_build_number", 0L)
            } else config.forceUpdateMinBuildNumber,
            forceUpdateMessage = prefs?.getString("app_config_override_force_update_message", null) ?: config.forceUpdateMessage,
            forceUpdateAndroidStoreUrl = prefs?.getString("app_config_override_force_update_android_store_url", null) ?: config.forceUpdateAndroidStoreUrl,
            castEnabled = if (prefs?.contains("app_config_override_cast_enabled") == true) {
                prefs.getBoolean("app_config_override_cast_enabled", false)
            } else config.castEnabled,
            castAppId = prefs?.getString("app_config_override_cast_app_id", null) ?: config.castAppId,
            castReceiverUrl = prefs?.getString("app_config_override_cast_receiver_url", null) ?: config.castReceiverUrl,
            pageFlipVisible = if (prefs?.contains("app_config_override_page_flip_visible") == true) {
                prefs.getBoolean("app_config_override_page_flip_visible", false)
            } else config.pageFlipVisible,
            adminEmails = prefs?.getString("app_config_override_admin_emails", null) ?: config.adminEmails,
            githubToken = prefs?.getString("app_config_override_github_token", null) ?: config.githubToken,
            isMangaloreHymnsEnabled = if (prefs?.contains("app_config_override_is_mangalore_hymns_enabled") == true) {
                prefs.getBoolean("app_config_override_is_mangalore_hymns_enabled", false)
            } else config.isMangaloreHymnsEnabled,
            midiHymnsRanges = prefs?.getString("app_config_override_midi_hymns_ranges", null) ?: config.midiHymnsRanges,
            midiKeerthanesRanges = prefs?.getString("app_config_override_midi_keerthanes_ranges", null) ?: config.midiKeerthanesRanges
        )
        android.util.Log.d("AppConfigRepository", "applyLocalOverrides: outputConfig=$overridden")
        return overridden
    }

    suspend fun saveConfigValue(key: String, value: Any?) = withContext(Dispatchers.IO) {
        val useLocal = isLocalOverridesEnabled()
        android.util.Log.d("AppConfigRepository", "saveConfigValue key=$key, value=$value, useLocal=$useLocal")
        if (useLocal) {
            prefs?.edit()?.run {
                val prefKey = "app_config_override_$key"
                when (value) {
                    null -> remove(prefKey)
                    is Boolean -> putBoolean(prefKey, value)
                    is Long -> putLong(prefKey, value)
                    is Int -> putLong(prefKey, value.toLong())
                    else -> putString(prefKey, value.toString())
                }
                val success = commit()
                android.util.Log.d("AppConfigRepository", "saveConfigValue (local): key=$prefKey, success=$success")
            }
        } else {
            val jsonValue = when (value) {
                null -> JsonNull
                is Boolean -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString())
            }
            android.util.Log.d("AppConfigRepository", "saveConfigValue (remote): key=$key, jsonValue=$jsonValue")
            appConfigService.update(key, jsonValue)
        }
    }

    fun clearOverrides() {
        prefs?.edit()?.run {
            prefs.all.keys.filter { it.startsWith("app_config_override_") }.forEach {
                remove(it)
            }
            commit()
        }
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
