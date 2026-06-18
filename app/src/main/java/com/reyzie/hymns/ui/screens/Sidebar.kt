@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.ui.viewmodels.AuthViewModel
import com.reyzie.hymns.utils.HapticFeedbackManager
import io.github.jan.supabase.gotrue.SessionStatus

@Composable
fun Sidebar(
    navController: NavHostController,
    onCloseDrawer: () -> Unit,
    onSettingsClick: () -> Unit,
    onPraiseAppClick: () -> Unit = {},
    onTicketsClick: () -> Unit = {},
    onAboutDeveloperClick: () -> Unit = {},
    onProfileEditClick: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    val isLoggedIn = sessionStatus is SessionStatus.Authenticated
    val accountLabel by authViewModel.accountDisplayName.collectAsState()
    var resolvedName by remember { mutableStateOf(accountLabel) }
    LaunchedEffect(isLoggedIn, accountLabel) {
        if (isLoggedIn) {
            resolvedName = authViewModel.resolveAccountDisplayName()
        }
    }

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.width(320.dp)
    ) {
        val scrollState = rememberScrollState()
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Church,
                                contentDescription = "App Icon",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "CSI Hymns & Lyrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Praise The Lord!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            SidebarSectionLabel("Account")
            SidebarListBlock {
                if (!isLoggedIn) {
                    SidebarDrawerItem(
                        icon = Icons.AutoMirrored.Filled.Login,
                        label = "Login / Sign up",
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            onCloseDrawer()
                            navController.navigate("auth")
                        }
                    )
                } else {
                    SidebarDrawerItem(
                        icon = Icons.Default.AccountCircle,
                        label = resolvedName,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            onCloseDrawer()
                            onProfileEditClick()
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    SidebarDrawerItem(
                        icon = Icons.Default.Edit,
                        label = "Edit profile",
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            onCloseDrawer()
                            onProfileEditClick()
                        }
                    )
                }
            }

            SidebarSectionLabel("Explore")
            SidebarListBlock {
                SidebarDrawerItem(
                    icon = Icons.Default.Storefront,
                    label = "Praise and Worship App",
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onCloseDrawer()
                        onPraiseAppClick()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                SidebarDrawerItem(
                    icon = Icons.Default.GridView,
                    label = "Categories",
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onCloseDrawer()
                        navController.navigate("categories")
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                SidebarDrawerItem(
                    icon = Icons.Default.ConfirmationNumber,
                    label = "Tickets Submitted",
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onCloseDrawer()
                        onTicketsClick()
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SidebarSectionLabel("More")
            SidebarListBlock {
                SidebarDrawerItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onCloseDrawer()
                        onSettingsClick()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                SidebarDrawerItem(
                    icon = Icons.Default.Person,
                    label = "About Developer",
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onCloseDrawer()
                        onAboutDeveloperClick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SidebarSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SidebarDrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.large
    )
}

@Composable
private fun SidebarListBlock(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

// Legacy alias
@Composable
fun SidebarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) = SidebarDrawerItem(icon = icon, label = label, onClick = onClick)
