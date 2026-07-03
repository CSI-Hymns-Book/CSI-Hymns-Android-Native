package com.reyzie.hymns.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.utils.MotionSpecs
import com.reyzie.hymns.ui.theme.contentOn
import com.reyzie.hymns.utils.connectedResponse
import com.reyzie.hymns.utils.expressiveClick

/**
 * Coordinated Expressive Action Button.
 * Derived entirely from semantic Material 3 tokens for adaptive accessibility.
 * Supports both ImageVector (material icons) and Painter (custom vectors/drawables).
 */
@Composable
fun ExpressiveActionButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    label: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isNeighborPressed: Boolean = false,
    onPressedChange: ((Boolean) -> Unit)? = null,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val cornerSize by animateDpAsState(
        targetValue = when {
            isSelected -> 12.dp
            isNeighborPressed -> 20.dp
            else -> 28.dp
        },
        animationSpec = MotionSpecs.DpSpec,
        label = "corner"
    )

    val widthScale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.12f
            isNeighborPressed -> 0.92f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "widthScale"
    )

    // Semantic Role Selection
    val targetContainerColor = containerColor ?: if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val targetContentColor = contentColor ?: targetContainerColor.contentOn()

    val finalContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = MotionSpecs.ColorSpec,
        label = "bg"
    )

    val finalContentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = MotionSpecs.ColorSpec,
        label = "fg"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = widthScale * (if (isNeighborPressed) 0.98f else 1f)
                scaleY = if (isNeighborPressed) 0.98f else 1f
            }
            .connectedResponse(isNeighborPressed)
            .expressiveClick(onPressedChange = onPressedChange),
        shape = RoundedCornerShape(cornerSize),
        color = finalContainerColor,
        contentColor = finalContentColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
            } else if (iconPainter != null) {
                Icon(iconPainter, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Visible // Prioritize readability as requested
            )
        }
    }
}
