package com.reyzie.hymns.interop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import com.reyzie.hymns.utils.HapticFeedbackManager

private val ChipShape = RoundedCornerShape(50)
private const val ICON_PX = 64

/**
 * "Back to <partner app>" button, shown only when a partner app opened Carols. The name and
 * icon come from that app itself, so a whitelabeled partner shows its own. Draws nothing
 * otherwise.
 */
@Composable
fun ReturnToCallerChip(modifier: Modifier = Modifier) {
    val caller by CarolHandoff.caller.collectAsState()
    val current = caller ?: return
    val context = LocalContext.current
    val palette = rememberChristmasPalette()
    val icon = remember(current) { current.icon?.toBitmap(ICON_PX, ICON_PX)?.asImageBitmap() }
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(palette.glassHigh)
            .border(1.dp, palette.edge, ChipShape)
            .clickable(role = Role.Button, onClickLabel = "Go back to ${current.label}") {
                HapticFeedbackManager.smoothClick(context)
                CarolHandoff.returnToCaller(context)
            }
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = palette.gold, modifier = Modifier.size(18.dp))
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))) }
        Text(
            "Back to ${current.label}",
            color = palette.ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
