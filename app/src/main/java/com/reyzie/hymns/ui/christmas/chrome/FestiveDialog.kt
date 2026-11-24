package com.reyzie.hymns.ui.christmas.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reyzie.hymns.ui.christmas.ChristmasEffectsLevel
import com.reyzie.hymns.ui.christmas.LocalChristmasAmbience
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DIALOG_CORNER_DP = 28f
private const val CAP_WIDTH_DP = 64f
private const val CAP_HEIGHT_DP = 45f
private const val CAP_OVERLAP_DP = 8f
private const val CAP_ROOM_DP = 40f
private const val CAP_LEFT_DP = 14f
private const val CAP_RESTING_TILT = -10f
private const val TIP_DELAY_MILLIS = 450L
private const val TIP_MILLIS = 900
private const val DROP_DISTANCE_DP = 70f
private const val GOLD_EDGE_ALPHA = 0.55f

/** How far the cap is tilted [t] (0..1) of the way through its single tip: lean, swing back, settle. */
private fun capTilt(t: Float): Float = when {
    t < 0.3f -> lerp(CAP_RESTING_TILT, -22f, t / 0.3f)
    t < 0.6f -> lerp(-22f, 0f, (t - 0.3f) / 0.3f)
    else -> lerp(0f, CAP_RESTING_TILT, (t - 0.6f) / 0.4f)
}

private fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t.coerceIn(0f, 1f)

/**
 * The festive look around a dialog: it drops in with a spring, a snow trim lies along its top
 * edge, a gold line runs around it and a Santa cap sits on the top-left corner and tips once.
 * Put it inside a [Dialog]. The cap needs room above the dialog, so this adds a transparent strip.
 * With Christmas effects off, the cap and trim are drawn but nothing moves.
 */
@Composable
fun FestiveDialogFrame(
    shape: Shape = RoundedCornerShape(DIALOG_CORNER_DP.dp),
    cornerDp: Float = DIALOG_CORNER_DP,
    content: @Composable () -> Unit,
) {
    val palette = rememberChristmasPalette()
    val animate = LocalChristmasAmbience.current?.effectiveLevel.let { it != null && it != ChristmasEffectsLevel.OFF }
    val entrance = remember { Animatable(if (animate) 0f else 1f) }
    val tip = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        coroutineScope {
            launch { entrance.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
            launch { delay(TIP_DELAY_MILLIS); tip.animateTo(1f, tween(TIP_MILLIS)) }
        }
    }
    Box(
        Modifier
            .padding(top = CAP_ROOM_DP.dp)
            .graphicsLayer {
                val e = entrance.value
                translationY = (1f - e) * -DROP_DISTANCE_DP.dp.toPx()
                scaleX = 0.9f + 0.1f * e
                scaleY = 0.9f + 0.1f * e
                alpha = (e * 3f).coerceIn(0f, 1f)
            },
    ) {
        Box(
            Modifier
                .festiveSnowTrim(cornerDp)
                .border(1.dp, palette.gold.copy(alpha = GOLD_EDGE_ALPHA), shape),
        ) { content() }
        SantaCap(
            Modifier
                .align(Alignment.TopStart)
                .offset(CAP_LEFT_DP.dp, -(CAP_HEIGHT_DP - CAP_OVERLAP_DP).dp)
                .graphicsLayer {
                    rotationZ = if (tip.value == 0f) CAP_RESTING_TILT else capTilt(tip.value)
                    transformOrigin = TransformOrigin(0.22f, 0.92f)
                },
            width = CAP_WIDTH_DP.dp,
        )
    }
}

/** Wraps the content of a custom [Dialog] in the festive frame, but only in Christmas mode. */
@Composable
fun FestiveDialogDecor(content: @Composable () -> Unit) {
    if (LocalChristmasAmbience.current == null) content() else FestiveDialogFrame(content = content)
}

/**
 * A drop-in for [AlertDialog]. Outside Christmas mode it is exactly [AlertDialog]. In Christmas
 * mode it shows the same title, text and buttons inside the festive frame, with the main button
 * dressed by role: a bow, or plain red when [destructive] is true. The dismiss button gets holly.
 */
@Composable
fun FestiveAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
    destructive: Boolean = false,
) {
    if (LocalChristmasAmbience.current == null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            shape = shape,
            containerColor = containerColor,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
            tonalElevation = tonalElevation,
            properties = properties,
        )
        return
    }
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        FestiveDialogFrame(shape = shape) {
            Surface(
                modifier = modifier.widthIn(min = 280.dp, max = 560.dp),
                shape = shape,
                color = containerColor,
                tonalElevation = tonalElevation,
            ) {
                Column(Modifier.padding(24.dp)) {
                    if (icon != null) {
                        Box(Modifier.align(Alignment.CenterHorizontally)) {
                            CompositionLocalProvider(LocalContentColor provides iconContentColor, content = icon)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    if (title != null) {
                        Box(Modifier.align(if (icon != null) Alignment.CenterHorizontally else Alignment.Start)) {
                            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                                ProvideTextStyle(MaterialTheme.typography.headlineSmall, title)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    if (text != null) {
                        Box(Modifier.align(Alignment.Start)) {
                            CompositionLocalProvider(LocalContentColor provides textContentColor) {
                                ProvideTextStyle(MaterialTheme.typography.bodyMedium, text)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    Row(
                        Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (dismissButton != null) FestiveButtonFrame(FestiveButtonRole.SECONDARY, content = dismissButton)
                        FestiveButtonFrame(if (destructive) FestiveButtonRole.DESTRUCTIVE else FestiveButtonRole.MAIN, content = confirmButton)
                    }
                }
            }
        }
    }
}
