package com.reyzie.hymns.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reyzie.hymns.carols.data.repository.CarolsRepository
import com.reyzie.hymns.cast.CastService
import com.reyzie.hymns.data.AppConfigRepository
import com.reyzie.hymns.data.RemoteAppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val carolsRepository = CarolsRepository.getInstance(application)
    private val appConfigRepository = AppConfigRepository(context = application)

    private val _remoteAppConfig = MutableStateFlow(appConfigRepository.getCachedRemoteConfig())
    val remoteAppConfig: StateFlow<RemoteAppConfig> = _remoteAppConfig.asStateFlow()

    init {
        refreshAppConfig()
        if (prefs.getBoolean("christmas_mode", false)) {
            syncChristmasCarols()
        }
    }

    fun refreshAppConfig() {
        viewModelScope.launch {
            try {
                val remote = appConfigRepository.fetchRemoteConfig()
                _remoteAppConfig.value = remote
                CastService.getInstance().applyRemoteConfig(getApplication(), remote)
                applyChristmasFromRemote(remote.isChristmasTime)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error fetching app_config, falling back to local/cached", e)
                val fallback = appConfigRepository.getCachedRemoteConfig()
                _remoteAppConfig.value = fallback
                loadLocalChristmasMode()
            }
        }
    }

    private fun applyChristmasFromRemote(remoteValue: Boolean?) {
        if (appConfigRepository.hasManualChristmasOverride()) {
            _isChristmasMode.value = prefs.getBoolean("christmas_mode", false)
            return
        }
        if (remoteValue != null) {
            setChristmasModeLocal(remoteValue, fromRemote = true)
        } else {
            loadLocalChristmasMode()
        }
    }

    private fun loadLocalChristmasMode() {
        val cached = appConfigRepository.cachedChristmasRemote()
        if (!appConfigRepository.hasManualChristmasOverride() && cached != null) {
            setChristmasModeLocal(cached, fromRemote = true)
            return
        }
        val hasKey = prefs.contains("christmas_mode")
        if (!hasKey) {
            val calendar = java.util.Calendar.getInstance()
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val isSeason = month == 12 || (month == 1 && day <= 6)
            setChristmasModeLocal(isSeason, fromRemote = false)
        } else {
            _isChristmasMode.value = prefs.getBoolean("christmas_mode", false)
        }
    }

    private fun setChristmasModeLocal(enabled: Boolean, fromRemote: Boolean) {
        prefs.edit().putBoolean("christmas_mode", enabled).apply()
        _isChristmasMode.value = enabled
        if (enabled) {
            syncChristmasCarols()
        }
    }

    private fun syncChristmasCarols() {
        viewModelScope.launch {
            try {
                carolsRepository.refresh()
            } catch (e: Exception) {
                android.util.Log.w("SettingsViewModel", "Christmas carols sync failed", e)
            }
        }
    }

    private val _themeMode = MutableStateFlow(ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isAmoledBlack = MutableStateFlow(prefs.getBoolean("amoled_black", false))
    val isAmoledBlack: StateFlow<Boolean> = _isAmoledBlack.asStateFlow()

    private val _themeColor = MutableStateFlow(
        prefs.getInt("theme_color", AppConfigRepository.DEFAULT_THEME_COLOR)
    )
    val themeColor: StateFlow<Int> = _themeColor.asStateFlow()

    private val _isPageFlipEnabled = MutableStateFlow(prefs.getBoolean("page_flip", false))
    val isPageFlipEnabled: StateFlow<Boolean> = _isPageFlipEnabled.asStateFlow()

    private val _isChristmasMode = MutableStateFlow(prefs.getBoolean("christmas_mode", false))
    val isChristmasMode: StateFlow<Boolean> = _isChristmasMode.asStateFlow()

    private val _privacyAccepted = MutableStateFlow(prefs.getInt("privacy_accepted", -1))
    val privacyAccepted: StateFlow<Int> = _privacyAccepted.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setAmoledBlack(enabled: Boolean) {
        prefs.edit().putBoolean("amoled_black", enabled).apply()
        _isAmoledBlack.value = enabled
    }

    fun setThemeColor(color: Int) {
        prefs.edit().putInt("theme_color", color).apply()
        _themeColor.value = color
    }

    fun setPageFlipEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("page_flip", enabled).apply()
        _isPageFlipEnabled.value = enabled
    }

    fun setChristmasMode(enabled: Boolean) {
        appConfigRepository.setManualChristmasOverride(true)
        prefs.edit().putBoolean("christmas_mode", enabled).apply()
        _isChristmasMode.value = enabled
        if (enabled) {
            syncChristmasCarols()
        }
    }

    fun setPrivacyAccepted(accepted: Int) {
        prefs.edit().putInt("privacy_accepted", accepted).apply()
        _privacyAccepted.value = accepted
    }

    private val _isLocalOverridesEnabled = MutableStateFlow(appConfigRepository.isLocalOverridesEnabled())
    val isLocalOverridesEnabled: StateFlow<Boolean> = _isLocalOverridesEnabled.asStateFlow()

    fun setLocalOverridesEnabled(enabled: Boolean) {
        appConfigRepository.setLocalOverridesEnabled(enabled)
        _isLocalOverridesEnabled.value = enabled
        refreshAppConfig()
    }

    fun saveConfigValue(key: String, value: Any?, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                appConfigRepository.saveConfigValue(key, value)
                refreshAppConfig()
                onComplete(null)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to save config value for key=$key", e)
                onComplete(e.localizedMessage ?: e.message ?: e.toString())
            }
        }
    }

    fun clearLocalOverrides() {
        appConfigRepository.clearOverrides()
        _isLocalOverridesEnabled.value = false
        appConfigRepository.setLocalOverridesEnabled(false)
        refreshAppConfig()
    }

    private val _midiInstrument = MutableStateFlow(prefs.getInt("midi_instrument", 16))
    val midiInstrument: StateFlow<Int> = _midiInstrument.asStateFlow()

    fun onMidiInstrumentChanged(instrument: Int) {
        _midiInstrument.value = instrument
        prefs.edit().putInt("midi_instrument", instrument).apply()
    }
}
