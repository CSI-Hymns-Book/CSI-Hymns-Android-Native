package com.reyzie.hymns.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reyzie.hymns.data.ChangelogEntryData
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlin.random.Random

@Composable
fun WelcomeChangelogDialog(
    entry: ChangelogEntryData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                        } else {
                            listOf(Color.White, Color(0xFFF5F7FA), Color(0xFFE8ECF1))
                        }
                    )
                )
        ) {
            ChangelogSparkleBackground(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = scheme.primaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎉", fontSize = 32.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Welcome!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.primary
                        )
                        Text(
                            text = "Version ${entry.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = scheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.5.dp, scheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎊", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = scheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "What's New:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.heightIn(max = 280.dp)) {
                    entry.changes.take(12).forEach { change ->
                        ChangelogChangeRow(change = change)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        HapticFeedbackManager.mediumClick(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Let's Go! 🎫", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
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
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = emojiForChange(change),
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 2.dp, end = 12.dp)
        )
        Text(
            text = change,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            lineHeight = 22.sp
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

@Composable
private fun ChangelogSparkleBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sparkle")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparklePhase"
    )
    val dots = remember {
        List(18) {
            Triple(Random(42).nextFloat(), Random(43).nextFloat(), 2f + Random(44).nextFloat() * 3f)
        }
    }
    Canvas(modifier = modifier) {
        val alpha = 0.08f * (1f - kotlin.math.abs(phase - 0.5f) * 2f)
        dots.forEach { (xR, yR, radius) ->
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = Offset(xR * size.width, yR * size.height)
            )
        }
    }
}
