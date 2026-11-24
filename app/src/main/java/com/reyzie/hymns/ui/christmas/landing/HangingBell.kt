package com.reyzie.hymns.ui.christmas.landing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val SWING_MILLIS = 1_700
private val BellGold = Color(0xFFE2B66A)
private val BellRim = Color(0xFFA97C34)
private val StringColor = Color(0xFFC9A15C)
private val BowRed = Color(0xFFD6493D)
private val Shine = Color(0xFFFFF3D6)

/**
 * A gold bell hanging on a string. Tapping it swings it and calls [onRing] with where its
 * mouth is on screen. Changing [ringSignal] rings it without a tap (used on Christmas Day).
 */
@Composable
fun HangingBell(ringSignal: Int, onRing: (rootPosition: Offset) -> Unit, modifier: Modifier = Modifier) {
    val swing = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var mouth by remember { mutableStateOf(Offset.Zero) }
    val bellPath = remember {
        PathParser().parsePathString(
            "M20 54 c-8 0 -11 7 -11 14 c0 6 -2 9 -5 11 h32 c-3 -2 -5 -5 -5 -11 c0 -7 -3 -14 -11 -14z",
        ).toPath()
    }
    val bowPath = remember { PathParser().parsePathString("M13 50 q7 -7 14 0 l-3 3 q-4 -3 -8 0z").toPath() }

    suspend fun ring() {
        swing.snapTo(0f)
        swing.animateTo(
            0f,
            keyframes {
                durationMillis = SWING_MILLIS
                24f at 204
                -19f at 442
                13f at 680
                -8f at 918
                4f at 1_156
                -2f at 1_394
            },
        )
    }

    LaunchedEffect(ringSignal) {
        if (ringSignal > 0) {
            onRing(mouth)
            ring()
        }
    }

    Canvas(
        modifier
            .size(width = 40.dp, height = 92.dp)
            .onGloballyPositioned { mouth = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height * 0.88f) }
            .graphicsLayer {
                rotationZ = swing.value
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Ring the bell",
            ) {
                onRing(mouth)
                scope.launch { ring() }
            },
    ) {
        scale(size.width / 40f, size.height / 92f, pivot = Offset.Zero) {
            drawLine(StringColor, Offset(20f, 0f), Offset(20f, 52f), 1.4f)
            drawPath(bowPath, BowRed)
            drawPath(bellPath, BellGold)
            drawPath(bellPath, BellRim, style = Stroke(1f))
            drawLine(Shine.copy(alpha = 0.7f), Offset(12f, 66f), Offset(17f, 57.5f), 1.6f, StrokeCap.Round)
            drawCircle(BellRim, 3.4f, Offset(20f, 82f))
        }
    }
}
