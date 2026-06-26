package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reyzie.hymns.data.ChangelogEntryData
import com.reyzie.hymns.ui.theme.contentOn
import com.reyzie.hymns.utils.HapticFeedbackManager

@Composable
fun WelcomeChangelogDialog(
    entry: ChangelogEntryData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxDialogHeight = screenHeight * 0.82f

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = maxDialogHeight)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight),
                shape = RoundedCornerShape(28.dp),
                color = scheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeBg = scheme.primaryContainer
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = badgeBg,
                            modifier = Modifier.size(52.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Text("🎉", fontSize = 26.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "What's new",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "Version ${entry.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = scheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primaryContainer.contentOn(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = scheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Changes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        entry.changes.forEach { change ->
                            ChangelogChangeRow(change = change)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            HapticFeedbackManager.mediumClick(context)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.primary.contentOn()
                        )
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Let's Go", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogChangeRow(change: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emojiForChange(change),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 1.dp, end = 10.dp)
        )
        Text(
            text = change,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun emojiForChange(change: String): String {
    val lower = change.lowercase()
    return when {
        "new" in lower || "added" in lower -> "✨"
        "improved" in lower || "better" in lower -> "🚀"
        "fixed" in lower || "bug" in lower -> "🐛"
        "christmas" in lower || "carol" in lower -> "🎄"
        "audio" in lower || "music" in lower -> "🎵"
        "theme" in lower || "color" in lower -> "🎨"
        "login" in lower || "auth" in lower -> "🔐"
        "pdf" in lower || "document" in lower -> "📄"
        "search" in lower || "filter" in lower -> "🔍"
        else -> "•"
    }
}
