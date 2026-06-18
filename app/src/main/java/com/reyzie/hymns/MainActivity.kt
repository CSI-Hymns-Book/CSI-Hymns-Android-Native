package com.reyzie.hymns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.reyzie.hymns.ui.screens.MainScreen
import com.reyzie.hymns.ui.theme.CSIHymnsBookTheme
import com.reyzie.hymns.BuildConfig
import com.reyzie.hymns.data.SupabaseService
import com.reyzie.hymns.data.AnalyticsService
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.reyzie.hymns.ui.viewmodels.SettingsViewModel
import com.reyzie.hymns.ui.viewmodels.ThemeMode

import io.github.jan.supabase.gotrue.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AnalyticsService.init(application)
        
        // Initialize Supabase
        val supabase = SupabaseService.getInstance()
        supabase.init(
            url = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY
        )
        
        // Handle incoming deep links for OAuth redirects
        supabase.client.handleDeeplinks(intent)
        
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
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Handle incoming deep links for OAuth redirects on existing activity
        SupabaseService.getInstance().client.handleDeeplinks(intent)
    }
}