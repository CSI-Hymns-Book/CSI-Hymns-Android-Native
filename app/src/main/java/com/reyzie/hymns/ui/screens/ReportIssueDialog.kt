package com.reyzie.hymns.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.data.JiraService
import com.reyzie.hymns.data.TicketsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueDialog(
    songType: String,
    songNumber: Int,
    songTitle: String,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jiraService = remember { JiraService() }
    val ticketsRepository = remember { TicketsRepository(context) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Report Issue") },
        text = {
            Column {
                Text(
                    text = "Reporting issue for $songType $songNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !isSubmitting
                )
                if (isSubmitting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    com.reyzie.hymns.ui.widgets.ExpressiveLinearProgress(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isSubmitting = true
                        val appVersion = try {
                            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            pInfo.versionName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                        
                        val result = jiraService.createTicket(
                            songType = songType,
                            songNumber = songNumber,
                            songTitle = songTitle,
                            description = description,
                            appVersion = appVersion,
                            guestDeviceId = ticketsRepository.getDeviceIdForGuest()
                        )
                        
                        isSubmitting = false
                        if (result.success) {
                            Toast.makeText(context, "Issue reported! Ticket ${result.ticketKey}", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}
