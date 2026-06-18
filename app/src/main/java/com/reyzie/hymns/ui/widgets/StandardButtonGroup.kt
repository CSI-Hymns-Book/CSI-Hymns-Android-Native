@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.reyzie.hymns.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.utils.MotionSpecs

/** M3 button roles in a group — see [buttons specs](https://m3.material.io/components/buttons/specs). */
enum class GroupButtonVariant {
    /** Filled / primary role (e.g. Number, Play) */
    Filled,
    /** Filled tonal (e.g. Meter) */
    Tonal,
    /** Tertiary / accent role (e.g. Refresh, Speed) */
    Accent
}

internal val LocalGroupButtonHighContrast = staticCompositionLocalOf { false }

private val LocalGroupButtonCount = staticCompositionLocalOf { 1 }

/**
 * M3 expressive connected button group — Essentials-style [ToggleButton] row on [surfaceBright].
 */
@Composable
fun StandardButtonGroup(
    buttonCount: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    highContrast: Boolean = false,
    content: @Composable StandardButtonGroupScope.() -> Unit
) {
    val count = buttonCount.coerceAtLeast(1)

    CompositionLocalProvider(
        LocalGroupButtonHighContrast provides highContrast,
        LocalGroupButtonCount provides count
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val scope = remember { StandardButtonGroupScope(this) }
            scope.content()
        }
    }
}

class StandardButtonGroupScope(
    private val rowScope: RowScope
) {
    @Composable
    fun Button(
        index: Int,
        onClick: () -> Unit,
        label: String,
        icon: ImageVector? = null,
        isSelected: Boolean = false,
        modifier: Modifier = Modifier,
        containerColor: Color? = null,
        contentColor: Color? = null,
        showLabelWhenUnselected: Boolean = true,
        forceShowLabel: Boolean = false,
        variant: GroupButtonVariant = GroupButtonVariant.Filled,
        circleWhenIdle: Boolean = false,
        alwaysCircle: Boolean = false,
        roundedSquare: Boolean = false,
        autoSizeLabel: Boolean = false,
        compact: Boolean = false,
        compactSize: Dp = 48.dp,
        iconSize: Dp? = null
    ) {
        rowScope.ExpressiveGroupToggle(
            index = index,
            onClick = onClick,
            label = label,
            icon = icon,
            isSelected = isSelected,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            showLabelWhenUnselected = showLabelWhenUnselected,
            forceShowLabel = forceShowLabel,
            variant = variant,
            circleWhenIdle = circleWhenIdle,
            alwaysCircle = alwaysCircle,
            roundedSquare = roundedSquare,
            autoSizeLabel = autoSizeLabel,
            compact = compact,
            compactSize = compactSize,
            iconSize = iconSize
        )
    }
}

@Composable
fun RowScope.ExpressiveGroupToggle(
    index: Int,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    showLabelWhenUnselected: Boolean = true,
    forceShowLabel: Boolean = false,
    variant: GroupButtonVariant = GroupButtonVariant.Filled,
    circleWhenIdle: Boolean = false,
    alwaysCircle: Boolean = false,
    roundedSquare: Boolean = false,
    autoSizeLabel: Boolean = false,
    compact: Boolean = false,
    compactSize: Dp = 48.dp,
    iconSize: Dp? = null
) {
    val count = LocalGroupButtonCount.current
    val scheme = MaterialTheme.colorScheme

    val showLabel = forceShowLabel || showLabelWhenUnselected || isSelected
    val iconOnly = !showLabel && icon != null
    val resolvedIconSize = iconSize ?: if (iconOnly) 24.dp else 20.dp

    val shapes = when {
        roundedSquare ->
            ToggleButtonDefaults.shapes(shape = RoundedCornerShape(12.dp))
        alwaysCircle || (circleWhenIdle && !isSelected) ->
            ToggleButtonDefaults.shapes(shape = CircleShape)
        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }

    val colorQuad = resolveColorQuad(
        variant = variant,
        scheme = scheme,
        containerColor = containerColor,
        contentColor = contentColor
    )

    val colors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = colorQuad.uncheckedContainer,
        contentColor = colorQuad.uncheckedContent,
        checkedContainerColor = colorQuad.checkedContainer,
        checkedContentColor = colorQuad.checkedContent
    )

    val targetForeground = if (isSelected) colorQuad.checkedContent else colorQuad.uncheckedContent
    val foregroundColor by animateColorAsState(
        targetValue = targetForeground,
        animationSpec = MotionSpecs.ColorSpec,
        label = "toggleForeground"
    )

    val sizeModifier = if (compact) {
        Modifier.size(compactSize)
    } else {
        Modifier
            .weight(1f)
            .height(if (iconOnly) 44.dp else 48.dp)
    }

    ToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick() },
        modifier = modifier
            .then(sizeModifier)
            .semantics { role = Role.RadioButton },
        shapes = shapes,
        colors = colors
    ) {
        if (iconOnly) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(resolvedIconSize),
                    tint = foregroundColor
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(resolvedIconSize),
                        tint = foregroundColor
                    )
                    if (showLabel) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                if (showLabel) {
                    if (autoSizeLabel) {
                        AutoFitGroupLabel(
                            text = label,
                            isSelected = isSelected,
                            color = foregroundColor
                        )
                    } else {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = foregroundColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun resolveColorQuad(
    variant: GroupButtonVariant,
    scheme: androidx.compose.material3.ColorScheme,
    containerColor: Color?,
    contentColor: Color?
): Quad {
    if (containerColor != null || contentColor != null) {
        val uncheckedBg = containerColor ?: scheme.surfaceContainerHigh
        val checkedBg = containerColor ?: when (variant) {
            GroupButtonVariant.Tonal -> scheme.secondaryContainer
            GroupButtonVariant.Accent -> scheme.tertiaryContainer
            GroupButtonVariant.Filled -> scheme.primary
        }
        val uncheckedFg = contentColor ?: scheme.onSurfaceVariant
        val checkedFg = contentColor ?: when (variant) {
            GroupButtonVariant.Tonal -> scheme.onSecondaryContainer
            GroupButtonVariant.Accent -> scheme.onTertiaryContainer
            GroupButtonVariant.Filled -> scheme.onPrimary
        }
        return Quad(uncheckedBg, uncheckedFg, checkedBg, checkedFg)
    }
    return variantColorQuad(variant, scheme)
}

@Composable
private fun variantColorQuad(
    variant: GroupButtonVariant,
    scheme: androidx.compose.material3.ColorScheme
): Quad {
    return when (variant) {
        GroupButtonVariant.Filled -> Quad(
            uncheckedContainer = scheme.surfaceBright,
            uncheckedContent = scheme.onSurfaceVariant,
            checkedContainer = scheme.primary,
            checkedContent = scheme.onPrimary
        )
        GroupButtonVariant.Tonal -> Quad(
            uncheckedContainer = scheme.surfaceContainerHighest,
            uncheckedContent = scheme.onSurfaceVariant,
            checkedContainer = scheme.secondaryContainer,
            checkedContent = scheme.onSecondaryContainer
        )
        GroupButtonVariant.Accent -> Quad(
            uncheckedContainer = scheme.surfaceContainerHighest,
            uncheckedContent = scheme.onSurfaceVariant,
            checkedContainer = scheme.tertiaryContainer,
            checkedContent = scheme.onTertiaryContainer
        )
    }
}

private data class Quad(
    val uncheckedContainer: Color,
    val uncheckedContent: Color,
    val checkedContainer: Color,
    val checkedContent: Color
)

@Composable
private fun AutoFitGroupLabel(
    text: String,
    isSelected: Boolean,
    color: Color
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        var fontSize by remember(text, maxWidth) { mutableStateOf(14.sp) }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = fontSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            onTextLayout = { result ->
                if (result.didOverflowWidth && fontSize > 9.sp) {
                    fontSize = (fontSize.value - 0.5f).sp
                }
            }
        )
    }
}
