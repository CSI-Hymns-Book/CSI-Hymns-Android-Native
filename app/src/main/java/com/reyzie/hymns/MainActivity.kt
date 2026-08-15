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
import com.adyen.checkout.dropin.DropIn
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
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        com.reyzie.hymns.data.HymnsFirebaseMessagingService.subscribeToDefaultTopics(this)
    }

    val dropInLauncher = DropIn.registerForDropInResult(
        this,
        object : com.adyen.checkout.dropin.SessionDropInCallback {
            override fun onDropInResult(sessionDropInResult: com.adyen.checkout.dropin.SessionDropInResult?) {
                when (sessionDropInResult) {
                    is com.adyen.checkout.dropin.SessionDropInResult.Finished -> {
                        android.widget.Toast.makeText(this@MainActivity, "Thank you for your generous support! May God bless you!", android.widget.Toast.LENGTH_LONG).show()
                    }
                    is com.adyen.checkout.dropin.SessionDropInResult.CancelledByUser -> {
                        android.widget.Toast.makeText(this@MainActivity, "Payment was cancelled.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    is com.adyen.checkout.dropin.SessionDropInResult.Error -> {
                        android.widget.Toast.makeText(this@MainActivity, "Payment error: ${sessionDropInResult.reason}", android.widget.Toast.LENGTH_LONG).show()
                    }
                    null -> {}
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AnalyticsService.init(application)
        if (savedInstanceState == null && OnboardingPrefs.isWelcomeCompleted(this)) {
            OnboardingPrefs.incrementLaunchCount(this)
        }

        if (com.reyzie.hymns.data.ConsentManager.pushConsent.value) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    com.reyzie.hymns.data.HymnsFirebaseMessagingService.subscribeToDefaultTopics(this)
                }
            } else {
                com.reyzie.hymns.data.HymnsFirebaseMessagingService.subscribeToDefaultTopics(this)
            }
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
                    MainScreen(dropInLauncher = dropInLauncher)
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
        setIntent(intent)
        // Handle incoming deep links for OAuth redirects on existing activity
        SupabaseService.getInstance().client.handleDeeplinks(intent)
    }

    private fun maybeRequestNotificationPermissionOnSecondLaunch() {
        if (!com.reyzie.hymns.data.ConsentManager.pushConsent.value) return
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