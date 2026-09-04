package com.reyzie.hymns.data

import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.posthog.PostHog
import com.reyzie.hymns.BuildConfig

object AnalyticsService {
    private const val TAG = "Analytics"
    private var isInitialized = false
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var lastIdentifiedUserId: String? = null

    private var appVersion: String = "Unknown"
    private var buildNumber: String = "Unknown"

    private var applicationRef: Application? = null

    fun init(application: Application) {
        applicationRef = application
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(application)
            firebaseAnalytics?.setAnalyticsCollectionEnabled(ConsentManager.analyticsConsent.value)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics unavailable (google-services.json may not be added yet)")
        }

        try {
            val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            appVersion = pInfo.versionName ?: "Unknown"
            buildNumber = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                pInfo.versionCode.toString()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        if (ConsentManager.analyticsConsent.value) {
            setupPostHog(application)
        } else {
            Log.i(TAG, "PostHog deferred until analytics consent")
        }
    }

    fun applyAnalyticsConsent(enabled: Boolean) {
        try {
            firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Could not toggle Firebase Analytics collection", e)
        }
        if (enabled) {
            applicationRef?.let { setupPostHog(it) }
        } else if (isInitialized) {
            try {
                PostHog.reset()
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting PostHog", e)
            }
            isInitialized = false
            lastIdentifiedUserId = null
        }
    }

    private fun setupPostHog(application: Application) {
        if (isInitialized) return
        val apiKey = BuildConfig.POSTHOG_API_KEY
        val host = BuildConfig.POSTHOG_HOST
        if (apiKey.isEmpty() || apiKey == "YOUR_POSTHOG_API_KEY") {
            Log.w(TAG, "PostHog API Key is missing or default. PostHog tracking disabled.")
            return
        }
        try {
            val config = PostHogAndroidConfig(
                apiKey = apiKey,
                host = host
            ).apply {
                captureApplicationLifecycleEvents = true
                captureScreenViews = true
                preloadFeatureFlags = false
                sendFeatureFlagEvent = false
                sessionReplay = true
                sessionReplayConfig.maskAllTextInputs = false
                sessionReplayConfig.maskAllImages = false
                sessionReplayConfig.screenshot = true
                sessionReplayConfig.sampleRate = 1.0
            }

            PostHogAndroid.setup(application, config)
            isInitialized = true
            Log.i(TAG, "PostHog ready ($host, v$appVersion+$buildNumber)")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up PostHog", e)
        }
    }

    fun syncAuthIdentity(userId: String?, authProvider: String? = null) {
        if (!ConsentManager.analyticsConsent.value) return
        try {
            firebaseAnalytics?.setUserId(userId)

            if (!isInitialized) return
            if (userId.isNullOrEmpty()) {
                if (lastIdentifiedUserId != null) {
                    lastIdentifiedUserId = null
                    PostHog.reset()
                    Log.d(TAG, "Analytics reset (signed out)")
                }
                return
            }
            if (userId == lastIdentifiedUserId) return
            lastIdentifiedUserId = userId

            val userProps = mutableMapOf<String, Any>()
            if (!authProvider.isNullOrEmpty()) {
                userProps["auth_provider"] = authProvider
            }

            PostHog.identify(userId, userProperties = if (userProps.isNotEmpty()) userProps else null)
            Log.d(TAG, "Analytics identify: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "syncAuthIdentity failed", e)
        }
    }

    fun capture(eventName: String, properties: Map<String, Any>? = null) {
        if (!ConsentManager.analyticsConsent.value) return
        val finalProps = mutableMapOf<String, Any>(
            "app_name" to "csi_hymns",
            "app_version" to appVersion,
            "app_build" to buildNumber
        )
        properties?.let { finalProps.putAll(it) }

        // 1. Log to Firebase Analytics
        try {
            firebaseAnalytics?.let { fa ->
                val safeEventName = eventName.replace(" ", "_").replace("-", "_").lowercase().take(40)
                val bundle = Bundle().apply {
                    finalProps.forEach { (key, value) ->
                        val safeKey = key.replace(" ", "_").replace("-", "_").lowercase().take(40)
                        when (value) {
                            is String -> putString(safeKey, value)
                            is Int -> putInt(safeKey, value)
                            is Long -> putLong(safeKey, value)
                            is Double -> putDouble(safeKey, value)
                            is Boolean -> putBoolean(safeKey, value)
                            else -> putString(safeKey, value.toString())
                        }
                    }
                }
                fa.logEvent(safeEventName, bundle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging to Firebase Analytics: $eventName", e)
        }

        // 2. Log to PostHog
        if (isInitialized) {
            try {
                PostHog.capture(eventName, properties = finalProps)
                Log.d(TAG, "capture: $eventName")
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing event in PostHog: $eventName", e)
            }
        }
    }

    fun screen(screenName: String, properties: Map<String, Any>? = null) {
        if (!ConsentManager.analyticsConsent.value) return
        // 1. Log to Firebase Analytics
        try {
            firebaseAnalytics?.let { fa ->
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
                    properties?.forEach { (key, value) ->
                        putString(key.lowercase(), value.toString())
                    }
                }
                fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging screen view to Firebase Analytics", e)
        }

        // 2. Log to PostHog
        if (isInitialized) {
            try {
                PostHog.screen(screenName, properties = properties)
                Log.d(TAG, "screen: $screenName")
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking screen in PostHog: $screenName", e)
            }
        }
    }

    // High-frequency slider seeks — coalesce to reduce volume
    private val seekThrottle = mutableMapOf<String, Long>()
    private const val SEEK_THROTTLE_WINDOW_MS = 900L

    fun captureAudioSeeked(itemType: String, itemNumber: Int, positionMs: Int) {
        val throttleKey = "${itemType}_$itemNumber"
        val now = System.currentTimeMillis()
        val last = seekThrottle[throttleKey]
        
        if (last != null && (now - last) < SEEK_THROTTLE_WINDOW_MS) {
            return
        }
        seekThrottle[throttleKey] = now
        
        capture(
            "Audio Seeked",
            mapOf(
                "item_type" to itemType,
                "item_number" to itemNumber,
                "position_ms" to positionMs
            )
        )
    }
}
