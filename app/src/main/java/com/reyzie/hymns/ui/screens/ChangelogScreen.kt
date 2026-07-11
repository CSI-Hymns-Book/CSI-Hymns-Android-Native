package com.reyzie.hymns.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class ChangelogEntry(
    val title: String,
    val version: String,
    val date: String,
    val changes: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var changelogData by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val jsonString = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val entries = mutableListOf<ChangelogEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val changesArray = obj.getJSONArray("changes")
                val changes = mutableListOf<String>()
                for (j in 0 until changesArray.length()) {
                    changes.add(changesArray.getString(j))
                }
                entries.add(
                    ChangelogEntry(
                        title = obj.getString("title"),
                        version = obj.getString("version"),
                        date = obj.getString("date"),
                        changes = changes
                    )
                )
            }
            changelogData = entries
        } catch (e: Exception) {
            changelogData = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changelog") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (changelogData.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    items(changelogData) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Version: ${entry.version}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Date: ${entry.date}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                entry.changes.forEach { change ->
                                    Text(
                                        text = "• $change",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
