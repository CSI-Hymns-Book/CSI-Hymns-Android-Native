package com.reyzie.hymns.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.data.JiraTicket
import com.reyzie.hymns.data.TicketsRepository
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TicketsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TicketsRepository(context) }
    val scope = rememberCoroutineScope()
    
    var tickets by remember { mutableStateOf<List<JiraTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusFilter by remember { mutableStateOf("all") }

    fun loadTickets(syncStatuses: Boolean = false) {
        scope.launch {
            isLoading = true
            if (syncStatuses) {
                repository.syncActiveTicketStatuses()
            }
            tickets = repository.getMyTickets().sortedByDescending { it.createdAt }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadTickets(syncStatuses = true)
    }

    val normalizedStatusGroup = { status: String ->
        val s = status.lowercase().trim()
        when {
            s in listOf("done", "resolved", "closed") -> "done"
            s in listOf("work in progress", "in progress", "in development") -> "in_progress"
            else -> "open"
        }
    }

    val filteredTickets = if (statusFilter == "all") tickets else tickets.filter { normalizedStatusGroup(it.jiraStatus) == statusFilter }

    val getStatusColor = { status: String ->
        val s = status.lowercase().trim()
        when {
            s in listOf("done", "resolved", "closed") -> Color(0xFF4CAF50) // Green
            s in listOf("work in progress", "in progress", "in development") -> Color(0xFF2196F3) // Blue
            s == "email sent" -> Color(0xFF3F51B5) // Indigo
            s == "pending" -> Color(0xFFFFC107) // Amber
            s in listOf("open", "to do") -> Color(0xFFFF9800) // Orange
            else -> Color.Gray
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tickets Submitted") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadTickets(syncStatuses = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Statuses")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ExpressiveCircularProgress(size = 56.dp)
            }
        } else if (tickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No tickets submitted yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Submit a ticket from a hymn or keerthane detail screen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Filters
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == "all",
                        onClick = { statusFilter = "all" },
                        label = { Text("All (${tickets.size})", maxLines = 1) }
                    )
                    FilterChip(
                        selected = statusFilter == "open",
                        onClick = { statusFilter = "open" },
                        label = {
                            val count = tickets.count { normalizedStatusGroup(it.jiraStatus) == "open" }
                            Text("Open ($count)", maxLines = 1)
                        }
                    )
                    FilterChip(
                        selected = statusFilter == "in_progress",
                        onClick = { statusFilter = "in_progress" },
                        label = {
                            val count = tickets.count { normalizedStatusGroup(it.jiraStatus) == "in_progress" }
                            Text("In Progress ($count)", maxLines = 1)
                        }
                    )
                    FilterChip(
                        selected = statusFilter == "done",
                        onClick = { statusFilter = "done" },
                        label = {
                            val count = tickets.count { normalizedStatusGroup(it.jiraStatus) == "done" }
                            Text("Done ($count)", maxLines = 1)
                        }
                    )
                }

                // List
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredTickets) { ticket ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ticket.ticketUrl))
                                    context.startActivity(intent)
                                },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = ticket.ticketKey,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        modifier = Modifier.wrapContentHeight(Alignment.CenterVertically),
                                        color = getStatusColor(ticket.jiraStatus).copy(alpha = 0.2f),
                                        shape = MaterialTheme.shapes.small,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, getStatusColor(ticket.jiraStatus))
                                    ) {
                                        Text(
                                            text = ticket.jiraStatus,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = getStatusColor(ticket.jiraStatus)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (ticket.songType == "Hymn") Icons.Default.MusicNote else Icons.Default.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${ticket.songType} ${ticket.songNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (ticket.songTitle.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ticket.songTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!ticket.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = ticket.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tap to view in Jira",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
