package com.reyzie.hymns.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.reyzie.hymns.data.SupabaseService
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    onAccountDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supabase = remember { SupabaseService.getInstance() }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var deactivating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        name = supabase.getProfileName().orEmpty()
        email = supabase.currentUser?.email
        loading = false
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!deactivating) showDeleteConfirm = false },
            title = { Text("Deactivate this account?") },
            text = {
                Text(
                    "You will be signed out. The account stays in our records as deactivated. This is not a permanent erase."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        deactivating = true
                        scope.launch {
                            HapticFeedbackManager.mediumClick(context)
                            try {
                                supabase.deleteAccount()
                            } catch (_: Exception) {
                            }
                            deactivating = false
                            onAccountDeleted()
                        }
                    },
                    enabled = !deactivating
                ) { Text("Deactivate", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !deactivating
                ) { Text("Cancel") }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Couldn’t complete") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).padding(32.dp))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !saving && !exporting && !deactivating,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email.orEmpty(),
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                    enabled = false,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Email comes from your sign-in and cannot be changed here. We do not store a profile picture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        saving = true
                        scope.launch {
                            HapticFeedbackManager.mediumClick(context)
                            supabase.upsertProfile(name.trim())
                            saving = false
                        }
                    },
                    enabled = !saving && !exporting && !deactivating && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(if (saving) "Saving…" else "Save name")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        exporting = true
                        scope.launch {
                            HapticFeedbackManager.mediumClick(context)
                            try {
                                val zipFile = supabase.exportMyDataZipFile(context.cacheDir)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    zipFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "CSI Hymns — my information")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    clipData = ClipData.newRawUri("", uri)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share your information")
                                )
                            } catch (e: Exception) {
                                errorMessage = if (supabase.isDataExportRateLimited(e)) {
                                    "Please wait 15 minutes before downloading your information again."
                                } else {
                                    e.localizedMessage ?: "Could not download your information."
                                }
                            }
                            exporting = false
                        }
                    },
                    enabled = !saving && !exporting && !deactivating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(if (exporting) "Preparing…" else "Download my information")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We’ll pack the data we store about you into a zip file. You can download this once every 15 minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = !saving && !exporting && !deactivating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (deactivating) "Deactivating…" else "Deactivate account",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "This signs you out and marks the account as deactivated. Your record is kept internally and is not permanently erased.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
