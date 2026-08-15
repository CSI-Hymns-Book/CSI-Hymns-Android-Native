package com.reyzie.hymns.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Local + remote consent artefact aligned with DPDP Act, 2023 and DPDP Rules, 2025.
 * Required purposes cannot be skipped. Analytics and push are separate opt-in purposes.
 */
object ConsentManager {
    const val CURRENT_POLICY_VERSION = "2026-08-15.1"
    const val GRIEVANCE_EMAIL = "reynoldclare02@gmail.com"
    const val DATA_FIDUCIARY_NAME = "Reynold Clare (CSI Hymns)"
    const val DATA_FIDUCIARY_REGION = "Bengaluru, Karnataka, India"
    const val ANALYTICS_STORAGE_KEY = "csi_consent_analytics"
    const val PUSH_STORAGE_KEY = "csi_consent_push"

    enum class LegalLanguage(val code: String, val label: String) {
        ENGLISH("en", "English"),
        KANNADA("kn", "ಕನ್ನಡ");

        companion object {
            fun fromCode(code: String?): LegalLanguage =
                entries.firstOrNull { it.code == code } ?: ENGLISH
        }
    }

    private object Keys {
        const val LANGUAGE = "csi_legal_language"
        const val VERSION = "csi_consent_policy_version"
        const val REQUIRED = "csi_consent_required_accepted"
        const val TERMS = "csi_consent_terms_accepted"
        const val AGE = "csi_consent_age_confirmed"
        const val ANALYTICS = ANALYTICS_STORAGE_KEY
        const val PUSH = PUSH_STORAGE_KEY
        const val RECORDED_AT = "csi_consent_recorded_at"
        const val LEGACY_PRIVACY = OnboardingPrefs.PRIVACY_ACCEPTED_LOCAL
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private val _hasValidRequiredConsent = MutableStateFlow(false)
    val hasValidRequiredConsent: StateFlow<Boolean> = _hasValidRequiredConsent.asStateFlow()

    private val _analyticsConsent = MutableStateFlow(false)
    val analyticsConsent: StateFlow<Boolean> = _analyticsConsent.asStateFlow()

    private val _pushConsent = MutableStateFlow(false)
    val pushConsent: StateFlow<Boolean> = _pushConsent.asStateFlow()

    private val _language = MutableStateFlow(LegalLanguage.ENGLISH)
    val language: StateFlow<LegalLanguage> = _language.asStateFlow()

    private val _acceptedVersion = MutableStateFlow<String?>(null)
    val acceptedVersion: StateFlow<String?> = _acceptedVersion.asStateFlow()

    private val _recordedAtEpochMs = MutableStateFlow<Long?>(null)
    val recordedAtEpochMs: StateFlow<Long?> = _recordedAtEpochMs.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
        val storedLang = prefs?.getString(Keys.LANGUAGE, LegalLanguage.ENGLISH.code)
        _language.value = LegalLanguage.fromCode(storedLang)
        refreshValidity()
    }

    fun setLanguage(language: LegalLanguage) {
        _language.value = language
        prefs?.edit()?.putString(Keys.LANGUAGE, language.code)?.apply()
    }

    fun refreshValidity() {
        val p = prefs ?: return
        val versionOk = p.getString(Keys.VERSION, null) == CURRENT_POLICY_VERSION
        val required = p.getBoolean(Keys.REQUIRED, false)
        val terms = p.getBoolean(Keys.TERMS, false)
        val age = p.getBoolean(Keys.AGE, false)
        _hasValidRequiredConsent.value = versionOk && required && terms && age
        if (!p.contains(Keys.ANALYTICS) && _hasValidRequiredConsent.value) {
            p.edit().putBoolean(Keys.ANALYTICS, true).apply()
        }
        _analyticsConsent.value = p.getBoolean(Keys.ANALYTICS, false)
        _pushConsent.value = p.getBoolean(Keys.PUSH, false)
        _acceptedVersion.value = p.getString(Keys.VERSION, null)
        _recordedAtEpochMs.value = if (p.contains(Keys.RECORDED_AT)) p.getLong(Keys.RECORDED_AT, 0L) else null
    }

    /** Agree to the current Privacy Policy and Terms. Analytics is enabled; push uses the OS prompt. */
    fun acceptCurrentPolicy() {
        acceptRequiredConsent(
            analytics = true,
            push = _pushConsent.value,
            ageConfirmed = true,
            privacyAccepted = true,
            termsAccepted = true
        )
    }

    fun acceptRequiredConsent(
        analytics: Boolean,
        push: Boolean,
        ageConfirmed: Boolean,
        privacyAccepted: Boolean,
        termsAccepted: Boolean
    ) {
        if (!ageConfirmed || !privacyAccepted || !termsAccepted) return
        val now = System.currentTimeMillis()
        prefs?.edit()
            ?.putString(Keys.VERSION, CURRENT_POLICY_VERSION)
            ?.putBoolean(Keys.REQUIRED, true)
            ?.putBoolean(Keys.TERMS, true)
            ?.putBoolean(Keys.AGE, true)
            ?.putBoolean(Keys.ANALYTICS, analytics)
            ?.putBoolean(Keys.PUSH, push)
            ?.putLong(Keys.RECORDED_AT, now)
            ?.putInt(Keys.LEGACY_PRIVACY, 1)
            ?.apply()
        refreshValidity()
        applySideEffects(analytics = analytics, push = push)
        scope.launch { syncToProfile() }
    }

    fun setAnalyticsConsent(enabled: Boolean) {
        prefs?.edit()?.putBoolean(Keys.ANALYTICS, enabled)?.apply()
        _analyticsConsent.value = enabled
        AnalyticsService.applyAnalyticsConsent(enabled)
        scope.launch { syncToProfile() }
    }

    fun setPushConsent(enabled: Boolean) {
        prefs?.edit()?.putBoolean(Keys.PUSH, enabled)?.apply()
        _pushConsent.value = enabled
        applyPushSideEffect(enabled)
        scope.launch { syncToProfile() }
    }

    fun hasOsNotificationPermission(context: Context): Boolean {
        val managerEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!managerEnabled) return false
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun syncPushConsentWithOsPermission(context: Context) {
        val granted = hasOsNotificationPermission(context)
        if (granted != _pushConsent.value) {
            setPushConsent(granted)
        }
    }

    fun withdrawRequiredConsent() {
        prefs?.edit()
            ?.putBoolean(Keys.REQUIRED, false)
            ?.putBoolean(Keys.TERMS, false)
            ?.putBoolean(Keys.AGE, false)
            ?.putBoolean(Keys.ANALYTICS, false)
            ?.putBoolean(Keys.PUSH, false)
            ?.putInt(Keys.LEGACY_PRIVACY, 0)
            ?.remove(Keys.VERSION)
            ?.apply()
        refreshValidity()
        applySideEffects(analytics = false, push = false)
        scope.launch {
            val supabase = SupabaseService.getInstance()
            supabase.setPrivacyPolicyAcceptedInProfile(0)
            if (supabase.currentUser != null) {
                runCatching { supabase.signOut() }
            }
        }
    }

    fun artefactMap(): Map<String, Any> {
        val recorded = _recordedAtEpochMs.value?.let { Instant.ofEpochMilli(it).toString() }
            ?: Instant.now().toString()
        return mapOf(
            "policy_version" to CURRENT_POLICY_VERSION,
            "recorded_at" to recorded,
            "language" to _language.value.code,
            "privacy_accepted" to _hasValidRequiredConsent.value,
            "terms_accepted" to _hasValidRequiredConsent.value,
            "age_confirmed" to _hasValidRequiredConsent.value,
            "analytics" to _analyticsConsent.value,
            "push_notifications" to _pushConsent.value,
            "notice" to "DPDP Act 2023 / DPDP Rules 2025 in-app notice"
        )
    }

    suspend fun syncToProfile() {
        prefs?.edit()?.putInt(Keys.LEGACY_PRIVACY, if (_hasValidRequiredConsent.value) 1 else 0)?.apply()
        val recordedIso = _recordedAtEpochMs.value?.let { Instant.ofEpochMilli(it).toString() }
        SupabaseService.getInstance().syncConsentArtefact(
            requiredAccepted = _hasValidRequiredConsent.value,
            analytics = _analyticsConsent.value,
            push = _pushConsent.value,
            version = if (_hasValidRequiredConsent.value) CURRENT_POLICY_VERSION else null,
            recordedAtIso = recordedIso,
            artefact = artefactMap()
        )
    }

    private fun applySideEffects(analytics: Boolean, push: Boolean) {
        AnalyticsService.applyAnalyticsConsent(analytics)
        applyPushSideEffect(push)
    }

    private fun applyPushSideEffect(enabled: Boolean) {
        if (enabled) {
            HymnsFirebaseMessagingService.subscribeToDefaultTopics(appContext)
        } else {
            HymnsFirebaseMessagingService.optOutPush()
        }
    }
}
