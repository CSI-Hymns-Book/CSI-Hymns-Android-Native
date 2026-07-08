package com.reyzie.hymns.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.OpenInNew
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
import com.reyzie.hymns.data.TicketMessage
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TicketsScreen(
    initialTicketKey: String? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TicketsRepository(context) }
    val scope = rememberCoroutineScope()
    
    var tickets by remember { mutableStateOf<List<JiraTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusFilter by remember { mutableStateOf("all") }

    var activeChatTicket by remember { mutableStateOf<JiraTicket?>(null) }
    
    LaunchedEffect(initialTicketKey, tickets) {
        if (!initialTicketKey.isNullOrEmpty() && tickets.isNotEmpty() && activeChatTicket == null) {
            val matching = tickets.find { it.ticketKey == initialTicketKey }
            if (matching != null) {
                activeChatTicket = matching
            }
        }
    }

    var messages by remember { mutableStateOf<List<TicketMessage>>(emptyList()) }
    var isMessagesLoading by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

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

    LaunchedEffect(activeChatTicket) {
        val ticket = activeChatTicket
        if (ticket != null) {
            isMessagesLoading = true
            repository.syncTicketComments(ticket.id, ticket.ticketKey)
            messages = repository.getTicketMessages(ticket.ticketKey)
            isMessagesLoading = false
        }
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
                title = {
                    if (activeChatTicket != null) {
                        Text(activeChatTicket!!.ticketKey, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Tickets Submitted", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        if (activeChatTicket != null) {
                            activeChatTicket = null
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeChatTicket != null) {
                        IconButton(onClick = {
                            scope.launch {
                                isMessagesLoading = true
                                repository.syncTicketComments(activeChatTicket!!.id, activeChatTicket!!.ticketKey)
                                messages = repository.getTicketMessages(activeChatTicket!!.ticketKey)
                                isMessagesLoading = false
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Chat")
                        }
                        IconButton(onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeChatTicket!!.ticketUrl))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "View in Jira")
                        }
                    } else {
                        IconButton(onClick = { loadTickets(syncStatuses = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Statuses")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (activeChatTicket != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                TicketChatPane(
                    ticket = activeChatTicket!!,
                    messages = messages,
                    isLoading = isMessagesLoading,
                    replyText = replyText,
                    onReplyTextChange = { replyText = it },
                    onSendMessage = {
                        scope.launch {
                            val sent = repository.sendTicketMessage(
                                activeChatTicket!!.id,
                                activeChatTicket!!.ticketKey,
                                replyText
                            )
                            if (sent != null) {
                                messages = messages + sent
                                replyText = ""
                            }
                        }
                    }
                )
            }
        } else if (isLoading) {
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
                                    HapticFeedbackManager.smoothClick(context)
                                    activeChatTicket = ticket
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
                                    text = "Tap to chat / view details",
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

@Composable
private fun TicketChatPane(
    ticket: JiraTicket,
    messages: List<TicketMessage>,
    isLoading: Boolean,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ExpressiveCircularProgress(size = 56.dp)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                // Ticket Summary Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (ticket.songType == "Hymn") Icons.Default.MusicNote else Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${ticket.songType} ${ticket.songNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (ticket.songTitle.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ticket.songTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (!ticket.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Correction Requested:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ticket.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status: ${ticket.jiraStatus}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = ticket.createdAt.take(10),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Message Bubbles
                items(messages) { msg ->
                    val isUser = msg.sender == "user"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        val bubbleBg = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        val bubbleTextColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        val shape = if (isUser) {
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
                        } else {
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
                        }

                        Surface(
                            color = bubbleBg,
                            shape = shape,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.message,
                                color = bubbleTextColor,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Input row
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = onReplyTextChange,
                        placeholder = { Text("Type a reply...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSendMessage,
                        enabled = replyText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
