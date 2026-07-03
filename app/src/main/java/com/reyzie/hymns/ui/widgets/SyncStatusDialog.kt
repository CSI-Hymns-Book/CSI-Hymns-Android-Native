package com.reyzie.hymns.ui.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

sealed interface SyncState {
    object Idle : SyncState
    object Loading : SyncState
    object Success : SyncState
    data class Error(val message: String) : SyncState
}

@Composable
fun SyncStatusDialog(
    syncState: SyncState,
    onDismiss: () -> Unit
) {
    if (syncState is SyncState.Idle) return

    Dialog(
        onDismissRequest = {
            if (syncState is SyncState.Error || syncState is SyncState.Loading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = syncState is SyncState.Error || syncState is SyncState.Loading,
            dismissOnClickOutside = syncState is SyncState.Error || syncState is SyncState.Loading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (syncState) {
                        is SyncState.Loading -> {
                            ExpressiveCircularProgress(size = 56.dp)
                        }
                        is SyncState.Success -> {
                            AnimatedSuccessCheckmark()
                        }
                        is SyncState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        is SyncState.Idle -> {}
                    }
                }

                val titleText = when (syncState) {
                    is SyncState.Loading -> "Fetching and Syncing Data"
                    is SyncState.Success -> "Sync completed"
                    is SyncState.Error -> "Sync failed"
                    is SyncState.Idle -> ""
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (syncState is SyncState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                } else {
                    val subtitleText = when (syncState) {
                        is SyncState.Loading -> "Fetching latest hymns & keerthanes from cloud..."
                        is SyncState.Success -> "Everything is up-to-date!"
                        is SyncState.Idle, is SyncState.Error -> ""
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (syncState is SyncState.Loading) {
                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSuccessCheckmark() {
    val pathPortion = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pathPortion.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)
        )
    }

    val greenColor = Color(0xFF4CAF50)
    Canvas(modifier = Modifier.size(56.dp)) {
        val width = size.width
        val height = size.height

        // Draw soft green background circle
        drawCircle(
            color = greenColor.copy(alpha = 0.15f),
            radius = width / 2f
        )

        // Draw solid green outline circle
        drawCircle(
            color = greenColor,
            radius = (width / 2f) - 2.dp.toPx(),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw path checkmark
        val startX = width * 0.28f
        val startY = height * 0.5f
        val midX = width * 0.44f
        val midY = height * 0.66f
        val endX = width * 0.72f
        val endY = height * 0.36f

        val p = pathPortion.value
        val path = Path()
        if (p > 0f) {
            path.moveTo(startX, startY)
            if (p <= 0.4f) {
                val currentX = startX + (midX - startX) * (p / 0.4f)
                val currentY = startY + (midY - startY) * (p / 0.4f)
                path.lineTo(currentX, currentY)
            } else {
                path.lineTo(midX, midY)
                val p2 = (p - 0.4f) / 0.6f
                val currentX = midX + (endX - midX) * p2
                val currentY = midY + (endY - midY) * p2
                path.lineTo(currentX, currentY)
            }

            drawPath(
                path = path,
                color = greenColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
