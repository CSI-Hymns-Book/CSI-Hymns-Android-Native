@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.reyzie.hymns.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.ui.viewmodels.SettingsViewModel
import com.reyzie.hymns.ui.viewmodels.ThemeMode
import com.reyzie.hymns.ui.viewmodels.AuthViewModel
import com.reyzie.hymns.ui.widgets.ExpressiveSwitch
import com.reyzie.hymns.ui.widgets.GroupButtonVariant
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import io.github.jan.supabase.gotrue.SessionStatus

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onNavigateUp: () -> Unit,
    onPrivacyPolicyClick: () -> Unit = {},
    onChangelogClick: () -> Unit = {},
    onAboutAppClick: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val isAmoledBlack by viewModel.isAmoledBlack.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val isPageFlipEnabled by viewModel.isPageFlipEnabled.collectAsState()
    val isChristmasMode by viewModel.isChristmasMode.collectAsState()
    val remoteAppConfig by viewModel.remoteAppConfig.collectAsState()
    val isPageFlipOptionVisible = remoteAppConfig.pageFlipVisible == true
    val scope = rememberCoroutineScope()
    
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    val user = (sessionStatus as? SessionStatus.Authenticated)?.session?.user
    val context = LocalContext.current

    val themeColors = listOf(
        0xFF6750A4, 0xFFD32F2F, 0xFFC62828, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
        0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
        0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722,
        0xFF795548, 0xFF9E9E9E, 0xFF607D8B
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onNavigateUp()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)
            SettingsExpressiveCard {
                StandardButtonGroup(
                    buttonCount = 3,
                    modifier = Modifier.fillMaxWidth(),
                    highContrast = true
                ) {
                    Button(
                        index = 0,
                        label = "System",
                        isSelected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                    )
                    Button(
                        index = 1,
                        label = "Light",
                        isSelected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        variant = GroupButtonVariant.Tonal
                    )
                    Button(
                        index = 2,
                        label = "Dark",
                        isSelected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        variant = GroupButtonVariant.Tonal
                    )
                }

                if (themeMode == ThemeMode.DARK || themeMode == ThemeMode.SYSTEM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSwitchTile(
                        title = "AMOLED Black Mode",
                        subtitle = "Pure black backgrounds for OLED screens",
                        icon = Icons.Default.FormatPaint,
                        checked = isAmoledBlack,
                        onCheckedChange = { viewModel.setAmoledBlack(it) }
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)) {
                    Text(
                        "Accent Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Choose a color that matches your mood",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val columns = 6
                    val chunkedColors = themeColors.chunked(columns)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chunkedColors.forEach { rowColors ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowColors.forEach { color ->
                                    val isSelected = themeColor == color.toInt()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(Color(color))
                                            .clickable {
                                                HapticFeedbackManager.smoothClick(context)
                                                viewModel.setThemeColor(color.toInt())
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                if (rowColors.size < columns) {
                                    repeat(columns - rowColors.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SettingsSectionHeader(title = "Experience", icon = Icons.Default.AutoAwesome)
            SettingsExpressiveCard {
                if (isPageFlipOptionVisible) {
                    SettingsSwitchTile(
                        title = "Page Flip Animation",
                        subtitle = "Tactile book-like page turning",
                        icon = Icons.Default.MenuBook,
                        checked = isPageFlipEnabled,
                        onCheckedChange = { viewModel.setPageFlipEnabled(it) }
                    )
                }
                SettingsSwitchTile(
                    title = "Christmas Mode",
                    subtitle = "Seasonal theme and carols",
                    icon = Icons.Default.AcUnit,
                    checked = isChristmasMode,
                    onCheckedChange = { viewModel.setChristmasMode(it) }
                )
            }

            SettingsSectionHeader(title = "Account", icon = Icons.Default.AccountCircle)
            SettingsExpressiveCard {
                if (user != null) {
                    SettingsActionTile(
                        title = user.email ?: "Logged In",
                        subtitle = "Profile synced",
                        leadingContent = { 
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = user.email?.take(1)?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsActionTile(
                        title = "Sign Out",
                        icon = Icons.Default.Logout,
                        onClick = { authViewModel.signOut() },
                        textColor = MaterialTheme.colorScheme.error,
                        iconTint = MaterialTheme.colorScheme.onErrorContainer,
                        iconBackground = MaterialTheme.colorScheme.errorContainer
                    )
                } else {
                    SettingsActionTile(
                        title = "Sign In",
                        subtitle = "Cloud sync for favorites",
                        icon = Icons.Default.Login,
                        onClick = onSignInClick
                    )
                }
            }

            SettingsSectionHeader(title = "Information", icon = Icons.Default.Info)
            SettingsExpressiveCard {
                SettingsActionTile(
                    title = "Privacy Policy",
                    icon = Icons.Default.Security,
                    onClick = onPrivacyPolicyClick
                )
                SettingsActionTile(
                    title = "Check for Updates",
                    icon = Icons.Default.CloudDownload,
                    onClick = {
                        scope.launch {
                            val service = com.reyzie.hymns.data.ForceUpdateService(context)
                            val decision = service.getDecision()
                            if (!decision.requiresUpdate) {
                                android.widget.Toast.makeText(context, "You are on the latest version!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(decision.androidStoreUrl ?: "https://play.google.com/store/apps/details?id=${context.packageName}"))
                                context.startActivity(intent)
                            }
                        }
                    }
                )
                SettingsActionTile(
                    title = "About & Changelog",
                    icon = Icons.Default.HelpCenter,
                    onClick = onAboutAppClick
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "CSI Kannada Hymns v${com.reyzie.hymns.BuildConfig.VERSION_NAME}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SettingsExpressiveCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsSwitchTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = subtitle?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        leadingContent = { 
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        },
        trailingContent = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = {
                    HapticFeedbackManager.performHapticFeedback(android.view.View(context), android.view.HapticFeedbackConstants.CLOCK_TICK)
                    onCheckedChange(it)
                }
            )
        },
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { 
                HapticFeedbackManager.smoothClick(context)
                onCheckedChange(!checked) 
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsActionTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable {
                HapticFeedbackManager.smoothClick(context)
                onClick()
            }
    } else Modifier

    ListItem(
        headlineContent = { 
            Text(text = title, fontWeight = FontWeight.Bold, color = textColor) 
        },
        supportingContent = subtitle?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        leadingContent = leadingContent ?: icon?.let {
            {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = iconBackground
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = it, 
                            contentDescription = null, 
                            modifier = Modifier.size(22.dp), 
                            tint = iconTint
                        )
                    }
                }
            }
        },
        trailingContent = trailingContent ?: if (onClick != null) {
            { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        modifier = clickableModifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// Legacy aliases for any external references
@Composable
fun SectionHeader(title: String, icon: ImageVector) = SettingsSectionHeader(title, icon)

@Composable
fun SwitchListTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) = SettingsSwitchTile(title, subtitle, icon, checked, onCheckedChange)

@Composable
fun SettingsListTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) = SettingsActionTile(title, subtitle, icon, leadingContent, onClick, trailingContent, iconTint, iconBackground, textColor)
