package com.reyzie.hymns

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.reyzie.hymns.ui.screens.MainScreen
import com.reyzie.hymns.ui.theme.CSIHymnsBookTheme
import com.reyzie.hymns.ui.widgets.ChristmasAmbienceOverlay
import com.reyzie.hymns.BuildConfig
import com.reyzie.hymns.data.SupabaseService
import com.reyzie.hymns.data.AnalyticsService
import com.reyzie.hymns.data.InAppUpdateManager
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reyzie.hymns.ui.viewmodels.SettingsViewModel
import com.reyzie.hymns.ui.viewmodels.ThemeMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import com.reyzie.hymns.data.OnboardingPrefs
import io.github.jan.supabase.gotrue.handleDeeplinks

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* OneSignal handles opt-in after permission */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AnalyticsService.init(application)
        if (savedInstanceState == null && OnboardingPrefs.isWelcomeCompleted(this)) {
            OnboardingPrefs.incrementLaunchCount(this)
        }
        
        // Initialize Supabase
        val supabase = SupabaseService.getInstance()
        supabase.init(
            url = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY
        )
        
        // Handle incoming deep links for OAuth redirects
        supabase.client.handleDeeplinks(intent)

        lifecycleScope.launch {
            InAppUpdateManager.checkSilentlyOnLaunch(this@MainActivity)
        }
        
        val settingsViewModel: SettingsViewModel by viewModels()

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val isAmoledBlack by settingsViewModel.isAmoledBlack.collectAsState()
            val themeColor by settingsViewModel.themeColor.collectAsState()
            val isChristmasMode by settingsViewModel.isChristmasMode.collectAsState()
            
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            CSIHymnsBookTheme(
                darkTheme = darkTheme,
                amoledBlack = isAmoledBlack,
                seedColor = Color(themeColor),
                dynamicColor = false,
                isChristmasMode = isChristmasMode
            ) {
                Box(Modifier.fillMaxSize()) {
                    MainScreen()
                    if (isChristmasMode) {
                        ChristmasAmbienceOverlay(
                            modifier = Modifier.fillMaxSize(),
                            intensity = com.reyzie.hymns.ui.widgets.SnowIntensity.Medium,
                            showEasterEggs = true,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        maybeRequestNotificationPermissionOnSecondLaunch()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Handle incoming deep links for OAuth redirects on existing activity
        SupabaseService.getInstance().client.handleDeeplinks(intent)
    }

    private fun maybeRequestNotificationPermissionOnSecondLaunch() {
        if (!OnboardingPrefs.isWelcomeCompleted(this)) return
        if (OnboardingPrefs.isNotificationPromptDone(this)) return
        val launchCount = OnboardingPrefs.getLaunchCount(this)
        if (launchCount < 1) return

        OnboardingPrefs.markNotificationPromptDone(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}