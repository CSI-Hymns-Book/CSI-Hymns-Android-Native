package com.reyzie.hymns.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.data.ConsentManager
import com.reyzie.hymns.data.LegalDocumentKind
import com.reyzie.hymns.ui.widgets.ExpressiveSwitch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCentreScreen(
    onBackClick: () -> Unit,
    onNestedChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val version by ConsentManager.acceptedVersion.collectAsState()
    val recordedAt by ConsentManager.recordedAtEpochMs.collectAsState()
    val analytics by ConsentManager.analyticsConsent.collectAsState()
    val push by ConsentManager.pushConsent.collectAsState()
    var showWithdrawConfirm by remember { mutableStateOf(false) }
    var showPushDeclineDialog by remember { mutableStateOf(false) }
    var pushDialogFromSystemDeny by remember { mutableStateOf(false) }
    var openDocument by remember { mutableStateOf<LegalDocumentKind?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        ConsentManager.setPushConsent(granted)
        if (granted) {
            com.reyzie.hymns.data.HymnsFirebaseMessagingService.subscribeToDefaultTopics(context)
        } else {
            pushDialogFromSystemDeny = true
            showPushDeclineDialog = true
        }
    }

    androidx.compose.runtime.DisposableEffect(openDocument) {
        onNestedChanged(openDocument != null)
        onDispose { onNestedChanged(false) }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                ConsentManager.syncPushConsentWithOsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        ConsentManager.syncPushConsentWithOsPermission(context)
    }

    if (openDocument != null) {
        LegalDocumentScreen(
            kind = openDocument!!,
            onBackClick = { openDocument = null }
        )
        return
    }

    val recordedLabel = recordedAt?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
    } ?: "—"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Centre", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Consent record", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Policy version: ${version ?: "—"}", modifier = Modifier.padding(top = 8.dp))
            Text("Recorded: $recordedLabel", modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            Text(
                "This record is stored on your device and, if you are signed in, on your profile in India.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsActionTile(
                title = "Privacy Policy",
                icon = Icons.Default.Policy,
                onClick = { openDocument = LegalDocumentKind.PRIVACY }
            )
            SettingsActionTile(
                title = "Terms of Use",
                icon = Icons.Default.Description,
                onClick = { openDocument = LegalDocumentKind.TERMS }
            )

            Text(
                "Optional processing",
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingsToggleRow(
                title = "Product analytics",
                icon = Icons.Default.ShowChart,
                checked = analytics,
                onCheckedChange = { ConsentManager.setAnalyticsConsent(it) }
            )
            SettingsToggleRow(
                title = "Push notifications",
                icon = Icons.Default.Notifications,
                checked = push && ConsentManager.hasOsNotificationPermission(context),
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !ConsentManager.hasOsNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            ConsentManager.setPushConsent(true)
                        }
                    } else {
                        pushDialogFromSystemDeny = false
                        showPushDeclineDialog = true
                    }
                }
            )
            Text(
                "These help us improve the app. You can turn them off any time. Hymn reading still works.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text("Your rights", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingsActionTile(
                title = "Request access, correction, or erasure",
                icon = Icons.Default.Email,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${ConsentManager.GRIEVANCE_EMAIL}?subject=${Uri.encode("DPDP rights request")}")
                    }
                    context.startActivity(intent)
                }
            )
            SettingsActionTile(
                title = "MeitY / Data Protection Board",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.meity.gov.in/")))
                }
            )
            Text(
                "Grievance contact: ${ConsentManager.GRIEVANCE_EMAIL}. You may also complain to the Data Protection Board of India.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            TextButton(
                onClick = { showWithdrawConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Withdraw consent", fontWeight = FontWeight.Bold)
            }
            Text(
                "This stops optional analytics and notifications immediately, signs you out, and shows the privacy notice again. Bundled hymns stay on the device after you accept a current notice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPushDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showPushDeclineDialog = false },
            title = { Text("Notifications are important") },
            text = {
                Text("They help keep you updated. We do not send unwanted content.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPushDeclineDialog = false
                        if (pushDialogFromSystemDeny) {
                            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text(if (pushDialogFromSystemDeny) "Open settings" else "Keep on")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPushDeclineDialog = false
                        ConsentManager.setPushConsent(false)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (pushDialogFromSystemDeny) "Not now" else "Turn off")
                }
            }
        )
    }

    if (showWithdrawConfirm) {
        AlertDialog(
            onDismissRequest = { showWithdrawConfirm = false },
            title = { Text("Withdraw consent?") },
            text = {
                Text("You will be signed out and asked to review the notice again. Optional analytics and notifications stop immediately.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWithdrawConfirm = false
                        ConsentManager.withdrawRequiredConsent()
                    }
                ) { Text("Withdraw") }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        ExpressiveSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
