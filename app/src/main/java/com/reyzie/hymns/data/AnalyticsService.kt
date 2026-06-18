package com.reyzie.hymns.data

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.posthog.PostHog
import com.reyzie.hymns.BuildConfig

object AnalyticsService {
    private const val TAG = "Analytics"
    private var isInitialized = false
    private var lastIdentifiedUserId: String? = null

    private var appVersion: String = "Unknown"
    private var buildNumber: String = "Unknown"

    fun init(application: Application) {
        val apiKey = BuildConfig.POSTHOG_API_KEY
        val host = BuildConfig.POSTHOG_HOST

        if (apiKey.isEmpty() || apiKey == "YOUR_POSTHOG_API_KEY") {
            Log.w(TAG, "PostHog API Key is missing or default. Tracking disabled.")
            return
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

        val config = PostHogAndroidConfig(
            apiKey = apiKey,
            host = host
        ).apply {
            captureApplicationLifecycleEvents = true
            captureScreenViews = true
            preloadFeatureFlags = false
            sendFeatureFlagEvent = false
        }

        PostHogAndroid.setup(application, config)
        isInitialized = true
        Log.i(TAG, "PostHog ready ($host, v$appVersion+$buildNumber)")
    }

    fun syncAuthIdentity(userId: String?, authProvider: String? = null) {
        if (!isInitialized) return
        try {
            if (userId.isNullOrEmpty()) {
                if (lastIdentifiedUserId != null) {
                    lastIdentifiedUserId = null
                    PostHog.reset()
                    Log.d(TAG, "PostHog reset (signed out)")
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
            Log.d(TAG, "PostHog identify: \$userId")
        } catch (e: Exception) {
            Log.e(TAG, "syncAuthIdentity failed", e)
        }
    }

    fun capture(eventName: String, properties: Map<String, Any>? = null) {
        if (!isInitialized) return
        try {
            val finalProps = mutableMapOf<String, Any>(
                "app_name" to "csi_hymns",
                "app_version" to appVersion,
                "app_build" to buildNumber
            )
            properties?.let { finalProps.putAll(it) }

            PostHog.capture(eventName, properties = finalProps)
            Log.d(TAG, "capture: \$eventName")
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing event: \$eventName", e)
        }
    }

    fun screen(screenName: String, properties: Map<String, Any>? = null) {
        if (!isInitialized) return
        try {
            PostHog.screen(screenName, properties = properties)
            Log.d(TAG, "screen: \$screenName")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking screen: \$screenName", e)
        }
    }

    // High-frequency slider seeks — coalesce to reduce volume and cost
    private val seekThrottle = mutableMapOf<String, Long>()
    private const val SEEK_THROTTLE_WINDOW_MS = 900L

    fun captureAudioSeeked(itemType: String, itemNumber: Int, positionMs: Int) {
        val throttleKey = "\${itemType}_\$itemNumber"
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
