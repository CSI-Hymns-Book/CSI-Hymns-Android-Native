package com.reyzie.hymns.ui.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun SquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    isPlaying: Boolean = true,
    activeColor: Color = Color.Red,
    inactiveColor: Color = Color.Gray,
    waveHeight: Dp = 6.dp,
    waveLength: Dp = 20.dp,
    strokeWidth: Dp = 3.dp
) {
    val density = LocalDensity.current
    val baseWaveHeightPx = with(density) { waveHeight.toPx() }
    val waveLengthPx = with(density) { waveLength.toPx() }
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    var dragValue by remember { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!isDragging) {
            dragValue = value
        }
    }

    val squish by animateFloatAsState(
        targetValue = if (isDragging) 1.12f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "squish"
    )

    val waveAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "waveAmplitude"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "squiggly")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val liveWaveHeightPx = baseWaveHeightPx * squish * waveAmplitude

    val smoothProgress by animateFloatAsState(
        targetValue = if (isDragging) dragValue else value,
        animationSpec = if (isDragging) snap() else tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "smoothProgress"
    )

    var componentWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .height(40.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (componentWidth > 0) {
                        val v = (offset.x / componentWidth).coerceIn(0f, 1f)
                        isDragging = true
                        dragValue = v
                        onValueChange(v)
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    }
                ) { change, _ ->
                    change.consume()
                    if (componentWidth > 0) {
                        val v = (change.position.x / componentWidth).coerceIn(0f, 1f)
                        dragValue = v
                        onValueChange(v)
                    }
                }
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            componentWidth = size.width.toInt()
            val centerY = size.height / 2f
            val activeWidth = size.width * smoothProgress.coerceIn(0f, 1f)
            val phase = if (isPlaying) phaseShift else 0f

            if (activeWidth > 0f) {
                if (liveWaveHeightPx > 0.5f) {
                    val path = Path()
                    path.moveTo(0f, centerY)
                    var x = 0f
                    val step = 3f
                    while (x <= activeWidth) {
                        val angle = (x / waveLengthPx) * (2 * Math.PI) + phase
                        val y = centerY + sin(angle).toFloat() * liveWaveHeightPx
                        path.lineTo(x, y)
                        x += step
                    }
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = strokeWidthPx * squish, cap = StrokeCap.Round)
                    )
                } else {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, centerY),
                        end = Offset(activeWidth, centerY),
                        strokeWidth = strokeWidthPx * squish,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (activeWidth < size.width) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(activeWidth, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = strokeWidthPx * 0.85f,
                    cap = StrokeCap.Round
                )
            }

            val thumbW = strokeWidthPx * 2.6f
            val thumbH = size.height * 0.5f * squish
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(activeWidth - thumbW / 2f, centerY - thumbH / 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f)
            )
        }
    }
}
