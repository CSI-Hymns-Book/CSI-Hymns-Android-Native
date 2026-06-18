package com.reyzie.hymns.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.utils.MotionSpecs
import com.reyzie.hymns.utils.connectedResponse
import com.reyzie.hymns.utils.expressiveClick

/**
 * M3 **connected** button group — single surface, shared background.
 * Neighbors do NOT react (per M3). For alive sibling motion use [StandardButtonGroup].
 */
@Composable
fun ConnectedButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp).fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
fun RowScope.ConnectedButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    label: String? = null,
    isSelected: Boolean = false,
    isNeighborPressed: Boolean = false,
    onPressedChange: ((Boolean) -> Unit)? = null,
    showLabelAlways: Boolean = true,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Morphing corner radius: pill-shaped when selected, subtle round when not
    val cornerSize by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 12.dp,
        animationSpec = MotionSpecs.DpSpec,
        label = "cornerSize"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = MotionSpecs.ColorSpec,
        label = "color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MotionSpecs.ColorSpec,
        label = "contentColor"
    )

    val scale by animateFloatAsState(
        targetValue = 1.0f, // Removed exaggerated scale-up
        label = "scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .connectedResponse(isNeighborPressed)
            .expressiveClick(onPressedChange = onPressedChange),
        shape = RoundedCornerShape(cornerSize),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                if (label != null && (showLabelAlways || isSelected)) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
            if (label != null && (showLabelAlways || isSelected)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
