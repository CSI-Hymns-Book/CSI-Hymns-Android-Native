package com.reyzie.hymns.ui.christmas.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.eggs.ChristmasSecret
import com.reyzie.hymns.utils.HapticFeedbackManager

private val ChipShape = RoundedCornerShape(50)

/**
 * A small pill at the bottom of the Christmas home that hints the app is full of hidden
 * surprises. Tapping it opens the list of secrets and how many have been found.
 */
@Composable
fun SecretsChip(palette: ChristmasPalette, found: Set<ChristmasSecret>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(palette.glassHigh)
            .border(1.dp, palette.edge, ChipShape)
            .clickable(role = Role.Button, onClickLabel = "Show hidden Christmas secrets") {
                HapticFeedbackManager.smoothClick(context)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("✨", fontSize = 14.sp)
        Text(
            "Tap, tilt & shake · ${found.size} of ${ChristmasSecret.entries.size} secrets",
            color = palette.ink,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Lists every secret: found ones show how to trigger them, the rest stay a mystery. */
@Composable
fun SecretsDialog(palette: ChristmasPalette, found: Set<ChristmasSecret>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secrets in this season", fontFamily = ChristmasDisplayFont) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Christmas changes how the whole app looks and feels. Tap things, press and hold, tilt and shake your phone, and watch the sky.",
                    color = palette.inkMuted,
                    fontSize = 13.sp,
                )
                ChristmasSecret.entries.forEach { secret ->
                    val isFound = secret in found
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (isFound) "★" else "☆", color = palette.gold)
                        Text(
                            if (isFound) secret.hint else "Still hidden…",
                            color = if (isFound) palette.ink else palette.inkMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}
