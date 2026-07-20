package com.reyzie.hymns.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.utils.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpToMeterSheet(
    meters: List<String>,
    onMeterSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isMt: Boolean = false,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(meters, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) meters
        else meters.filter { it.lowercase().contains(q) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                if (isMt) "Jump to MT Tune" else "Jump to Meter",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isMt) "Pick an MT tune group to scroll the list instantly." else "Pick a meter group to scroll the list instantly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (isMt) "Search MT tunes…" else "Search meters…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )
            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (isMt) "No MT tunes match your search" else "No meters match your search",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it }) { meter ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    if (meter.isEmpty()) "(No meter)" else if (isMt) "M.T. $meter" else meter,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticFeedbackManager.smoothClick(context)
                                    onMeterSelected(meter)
                                },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
