package com.reyzie.hymns.data

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ForceUpdateDecision(
    val requiresUpdate: Boolean,
    val currentVersion: String,
    val minimumVersion: String? = null,
    val currentBuildNumber: Long? = null,
    val minimumBuildNumber: Long? = null,
    val message: String? = null,
    val androidStoreUrl: String? = null
)

class ForceUpdateService(
    private val context: Context,
    private val appConfigRepository: AppConfigRepository = AppConfigRepository(context = context)
) {

    suspend fun getDecision(): ForceUpdateDecision = withContext(Dispatchers.IO) {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersion = packageInfo.versionName ?: "1.0.0"
        val currentBuild = packageInfo.longVersionCode

        try {
            val remote = appConfigRepository.fetchRemoteConfig()
            val minVersion = remote.forceUpdateMinVersion
            val minBuild = remote.forceUpdateMinBuildNumber
            val enabled = remote.forceUpdateEnabled ?: (minVersion != null || minBuild != null)

            val requiresByVersion = if (!minVersion.isNullOrEmpty()) isVersionLower(currentVersion, minVersion) else false
            val requiresByBuild = minBuild != null && currentBuild < minBuild
            
            val requiresUpdate = enabled && (requiresByVersion || requiresByBuild)

            ForceUpdateDecision(
                requiresUpdate = requiresUpdate,
                currentVersion = currentVersion,
                minimumVersion = minVersion,
                currentBuildNumber = currentBuild,
                minimumBuildNumber = minBuild,
                message = remote.forceUpdateMessage,
                androidStoreUrl = remote.forceUpdateAndroidStoreUrl
            )
        } catch (e: Exception) {
            Log.e("ForceUpdateService", "Remote config fetch failed", e)
            ForceUpdateDecision(
                requiresUpdate = false,
                currentVersion = currentVersion,
                currentBuildNumber = currentBuild
            )
        }
    }

    private fun isVersionLower(current: String, minimum: String): Boolean {
        val a = extractVersionParts(current)
        val b = extractVersionParts(minimum)
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av < bv) return true
            if (av > bv) return false
        }
        return false
    }

    private fun extractVersionParts(input: String): List<Int> {
        val normalized = input.split("-").first().trim()
        if (normalized.isEmpty()) return listOf(0)
        return normalized.split(".").map { it.toIntOrNull() ?: 0 }
    }
}
