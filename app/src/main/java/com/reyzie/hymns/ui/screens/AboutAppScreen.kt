package com.reyzie.hymns.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.reyzie.hymns.R
import com.reyzie.hymns.ui.widgets.ExpressiveActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    val context = LocalContext.current

    fun launchUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    var tapCount by remember { mutableIntStateOf(0) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var passcodeText by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About This App", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.playstore_icon),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = "CSI Hymns Book",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        tapCount++
                        if (tapCount >= 8) {
                            tapCount = 0
                            passcodeText = ""
                            passcodeError = false
                            showPasscodeDialog = true
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "A Kannada CSI Hymns and Keerthane Lyrics Book, with modern minimal UI and functionalities.",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.clickable { 
                        launchUrl("https://t.me/Reynold29") 
                    }
                ) {
                    Text(
                        text = "Developed By",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Reynold (@Reynold29)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Contribute",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This app is Open Source! Contribute to the project's code and let's enhance it ! ;)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpressiveActionButton(
                        onClick = { launchUrl("https://github.com/Reynold29/CSI-Hymns-and-Lyrics/") },
                        iconPainter = painterResource(id = R.drawable.ic_github),
                        label = "GitHub",
                        modifier = Modifier.weight(1f)
                    )
                    ExpressiveActionButton(
                        onClick = { launchUrl("https://play.google.com/store/apps/details?id=com.reyzie.hymns") },
                        iconPainter = painterResource(id = R.drawable.ic_googleplay),
                        label = "PlayStore",
                        modifier = Modifier.weight(1f)
                    )
                    ExpressiveActionButton(
                        onClick = { launchUrl("https://apps.apple.com/in/app/worship-companion/id6759990066") },
                        iconPainter = painterResource(id = R.drawable.ic_apple),
                        label = "App Store",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Support",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Get Support Here and also find out how we Prioritize User Privacy and Data!",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpressiveActionButton(
                        onClick = { launchUrl("https://t.me/Reynold29") },
                        iconPainter = painterResource(id = R.drawable.ic_telegram),
                        label = "Telegram",
                        modifier = Modifier.weight(1f)
                    )
                    ExpressiveActionButton(
                        onClick = onPrivacyPolicyClick,
                        icon = Icons.Default.Policy,
                        label = "Privacy Policy",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showPasscodeDialog = false },
            title = { Text("Root Admin Authentication", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the Sudo Master Passcode to unlock Root Admin rights on this device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passcodeText,
                        onValueChange = {
                            passcodeText = it
                            passcodeError = false
                        },
                        label = { Text("Passcode") },
                        isError = passcodeError,
                        supportingText = if (passcodeError) {
                            @Composable { Text("Invalid Passcode!", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val isCorrect = com.reyzie.hymns.data.AdminPrefs.verifyPasscode(context, passcodeText)
                            if (isCorrect) {
                                com.reyzie.hymns.data.AdminPrefs.setSudoAdminEnabled(context, true)
                                android.widget.Toast.makeText(
                                    context,
                                    "⚡ Sudo Root Admin Mode Activated!",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                showPasscodeDialog = false
                                onBackClick()
                            } else {
                                passcodeError = true
                            }
                        }
                    }
                ) {
                    Text("Authorize", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasscodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
