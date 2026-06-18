package com.reyzie.hymns.ui.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class FlipDirection { Forward, Backward }

/**
 * Interactive page-curl lyrics reader.
 * Drag right→left for next page, left→right for previous. Vertical drags scroll.
 */
@Composable
fun PageFlipLyrics(
    lyrics: String,
    fontSize: TextUnit,
    isKeerthane: Boolean = false,
    textAlign: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") stanzasPerPage: Int = 2
) {
    val layout = remember(lyrics, isKeerthane, fontSize) {
        buildLyricsPages(lyrics, isKeerthane, fontSize)
    }
    val pages = layout.pages
    if (pages.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pageColor = MaterialTheme.colorScheme.surfaceBright
    val screenBackground = MaterialTheme.colorScheme.background

    var currentPage by remember(lyrics, isKeerthane, fontSize) { mutableIntStateOf(0) }
    var flipProgress by remember { mutableFloatStateOf(0f) }
    var flipDirection by remember { mutableStateOf<FlipDirection?>(null) }
    var fingerYNorm by remember { mutableFloatStateOf(0.5f) }
    var isAnimating by remember { mutableStateOf(false) }
    val animatedProgress = remember { Animatable(0f) }

    val captureLayer = rememberGraphicsLayer()
    var curlSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(lyrics, isKeerthane, fontSize) {
        currentPage = 0
        flipProgress = 0f
        flipDirection = null
        curlSnapshot = null
        animatedProgress.snapTo(0f)
    }

    fun canGoForward() = currentPage < pages.lastIndex
    fun canGoBack() = currentPage > 0

    val activeProgress = if (isAnimating) animatedProgress.value else flipProgress
    val isFlipping = activeProgress > 0.001f && flipDirection != null

    val underPageIndex = when (flipDirection) {
        FlipDirection.Forward -> (currentPage + 1).coerceAtMost(pages.lastIndex)
        FlipDirection.Backward -> (currentPage - 1).coerceAtLeast(0)
        null -> currentPage
    }

    suspend fun captureSnapshot() {
        curlSnapshot = captureLayer.toImageBitmap()
    }

    suspend fun animateFlip(complete: Boolean, direction: FlipDirection) {
        isAnimating = true
        animatedProgress.snapTo(flipProgress.coerceIn(0f, 1f))
        val target = if (complete) 1f else 0f
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = if (complete) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        if (complete) {
            HapticFeedbackManager.lightClick(context)
            currentPage = when (direction) {
                FlipDirection.Forward -> (currentPage + 1).coerceAtMost(pages.lastIndex)
                FlipDirection.Backward -> (currentPage - 1).coerceAtLeast(0)
            }
        }
        flipProgress = 0f
        flipDirection = null
        curlSnapshot = null
        animatedProgress.snapTo(0f)
        isAnimating = false
    }

    fun commitFlip(complete: Boolean) {
        val direction = flipDirection ?: return
        if (isAnimating) return
        scope.launch { animateFlip(complete, direction) }
    }

    fun settleFlip(velocityX: Float) {
        val direction = flipDirection ?: return
        val threshold = 0.28f
        val velocitySnap = 900f
        val complete = when (direction) {
            FlipDirection.Forward ->
                flipProgress > threshold || velocityX < -velocitySnap
            FlipDirection.Backward ->
                flipProgress > threshold || velocityX > velocitySnap
        } && when (direction) {
            FlipDirection.Forward -> canGoForward()
            FlipDirection.Backward -> canGoBack()
        }
        commitFlip(complete)
    }

    fun tapForward() {
        if (!canGoForward() || isAnimating) return
        scope.launch {
            flipDirection = FlipDirection.Forward
            captureSnapshot()
            HapticFeedbackManager.lightClick(context)
            animateFlip(complete = true, direction = FlipDirection.Forward)
        }
    }

    fun tapBackward() {
        if (!canGoBack() || isAnimating) return
        scope.launch {
            flipDirection = FlipDirection.Backward
            captureSnapshot()
            HapticFeedbackManager.lightClick(context)
            animateFlip(complete = true, direction = FlipDirection.Backward)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        val widthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val heightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }

        Box(Modifier.fillMaxSize()) {
            if (isFlipping) {
                BookPage(
                    text = pages[underPageIndex],
                    fontSize = fontSize,
                    textAlign = textAlign,
                    scrollEnabled = false,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    flipEnabled = false,
                    onFlipDrag = { _, _, _ -> },
                    onFlipEnd = { },
                    onTapEdge = { },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (!isFlipping || curlSnapshot == null) {
                CapturedBookPage(
                    layer = captureLayer,
                    text = pages[currentPage],
                    fontSize = fontSize,
                    textAlign = textAlign,
                    scrollEnabled = !isFlipping,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    flipEnabled = !isAnimating,
                    canFlipForward = canGoForward(),
                    canFlipBackward = canGoBack(),
                    onFlipDrag = { progress, direction, yNorm ->
                        fingerYNorm = yNorm
                        if (flipDirection == null) {
                            flipDirection = direction
                            scope.launch { captureSnapshot() }
                            HapticFeedbackManager.lightClick(context)
                        }
                        flipProgress = progress
                    },
                    onFlipEnd = { velocityX -> settleFlip(velocityX) },
                    onTapEdge = { fraction ->
                        when {
                            fraction < 0.22f -> tapBackward()
                            fraction > 0.78f -> tapForward()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            val snapshot = curlSnapshot
            if (isFlipping && snapshot != null) {
                PageCurlCanvas(
                    image = snapshot,
                    progress = activeProgress,
                    forward = flipDirection == FlipDirection.Forward,
                    fingerYNorm = fingerYNorm,
                    pageColor = pageColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 4.dp
        ) {
            Text(
                text = "Page ${currentPage + 1} of ${pages.size}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CapturedBookPage(
    layer: GraphicsLayer,
    text: String,
    fontSize: TextUnit,
    textAlign: TextAlign,
    scrollEnabled: Boolean,
    widthPx: Float,
    heightPx: Float,
    flipEnabled: Boolean,
    canFlipForward: Boolean = false,
    canFlipBackward: Boolean = false,
    onFlipDrag: (Float, FlipDirection, Float) -> Unit,
    onFlipEnd: (Float) -> Unit,
    onTapEdge: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawWithContent {
            layer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(layer)
        }
    ) {
        BookPage(
            text = text,
            fontSize = fontSize,
            textAlign = textAlign,
            scrollEnabled = scrollEnabled,
            widthPx = widthPx,
            heightPx = heightPx,
            flipEnabled = flipEnabled,
            canFlipForward = canFlipForward,
            canFlipBackward = canFlipBackward,
            onFlipDrag = onFlipDrag,
            onFlipEnd = onFlipEnd,
            onTapEdge = onTapEdge,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BookPage(
    text: String,
    fontSize: TextUnit,
    textAlign: TextAlign,
    scrollEnabled: Boolean,
    widthPx: Float,
    heightPx: Float,
    flipEnabled: Boolean,
    canFlipForward: Boolean = false,
    canFlipBackward: Boolean = false,
    onFlipDrag: (Float, FlipDirection, Float) -> Unit,
    onFlipEnd: (Float) -> Unit,
    onTapEdge: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageColor = MaterialTheme.colorScheme.surfaceBright
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        color = pageColor,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        val interactionModifier = Modifier
            .pointerInput(flipEnabled, widthPx) {
                if (!flipEnabled) return@pointerInput
                detectTapGestures { offset ->
                    onTapEdge(offset.x / size.width)
                }
            }
            .pointerInput(
                flipEnabled,
                canFlipForward,
                canFlipBackward,
                widthPx,
                heightPx
            ) {
                if (!flipEnabled) return@pointerInput

                var dragDirection: FlipDirection? = null
                var totalDrag = 0f
                var lastVelocityX = 0f
                var engaged = false

                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        lastVelocityX = 0f
                        dragDirection = null
                        engaged = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        lastVelocityX = dragAmount
                        val yNorm = (change.position.y / heightPx).coerceIn(0f, 1f)

                        if (dragDirection == null) {
                            dragDirection = when {
                                totalDrag < 0f && canFlipForward -> FlipDirection.Forward
                                totalDrag > 0f && canFlipBackward -> FlipDirection.Backward
                                else -> return@detectHorizontalDragGestures
                            }
                            engaged = true
                        }

                        val direction = dragDirection ?: return@detectHorizontalDragGestures
                        val pull = (abs(totalDrag) / widthPx).coerceIn(0f, 1f)
                        onFlipDrag(pull, direction, yNorm)
                    },
                    onDragEnd = {
                        if (engaged) onFlipEnd(lastVelocityX)
                    },
                    onDragCancel = {
                        if (engaged) onFlipEnd(0f)
                    }
                )
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageColor)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            val scrollModifier = if (scrollEnabled) {
                Modifier.verticalScroll(scrollState)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier)
                    .then(interactionModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.7f,
                    textAlign = textAlign,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    softWrap = true
                )
            }
        }
    }
}
