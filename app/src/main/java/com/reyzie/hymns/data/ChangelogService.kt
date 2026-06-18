package com.reyzie.hymns.data

import android.content.Context
import com.reyzie.hymns.BuildConfig
import org.json.JSONArray

data class ChangelogEntryData(
    val title: String,
    val version: String,
    val date: String,
    val changes: List<String>
)

class ChangelogService(private val context: Context) {
    companion object {
        private const val LAST_SHOWN_VERSION = "last_shown_changelog_version"
        private const val IS_FIRST_LAUNCH = "is_first_app_launch"
    }

    private val prefs = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)

    fun getCurrentVersion(): String = BuildConfig.VERSION_NAME

    fun normalizeVersion(version: String): String = version.split("+").first()

    fun shouldShowChangelog(): Boolean {
        val lastShown = prefs.getString(LAST_SHOWN_VERSION, null)
        val current = normalizeVersion(getCurrentVersion())
        if (prefs.getBoolean(IS_FIRST_LAUNCH, true)) {
            prefs.edit().putBoolean(IS_FIRST_LAUNCH, false).apply()
            return true
        }
        return lastShown == null || normalizeVersion(lastShown) != current
    }

    fun getLatestChangelog(): ChangelogEntryData? {
        return try {
            val json = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            if (array.length() == 0) return null
            val obj = array.getJSONObject(0)
            val changes = mutableListOf<String>()
            val changesArray = obj.getJSONArray("changes")
            for (i in 0 until changesArray.length()) {
                changes.add(changesArray.getString(i))
            }
            ChangelogEntryData(
                title = obj.getString("title"),
                version = obj.getString("version"),
                date = obj.getString("date"),
                changes = changes
            )
        } catch (_: Exception) {
            null
        }
    }

    fun markChangelogAsShown() {
        prefs.edit()
            .putString(LAST_SHOWN_VERSION, normalizeVersion(getCurrentVersion()))
            .apply()
    }

    fun markFirstLaunchHandled() {
        prefs.edit().putBoolean(IS_FIRST_LAUNCH, false).apply()
    }
}
