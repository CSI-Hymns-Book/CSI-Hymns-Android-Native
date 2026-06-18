package com.reyzie.hymns.ui.widgets

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val SEGMENT_COUNT = 32

/**
 * Renders a page snapshot as a cylindrical curl using per-strip 3D transforms.
 */
@Composable
fun PageCurlCanvas(
    image: ImageBitmap,
    progress: Float,
    forward: Boolean,
    fingerYNorm: Float,
    pageColor: Color,
    modifier: Modifier = Modifier
) {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0.001f) return

    val fingerY = fingerYNorm.coerceIn(0f, 1f)
    val cornerTorque = (fingerY - 0.5f) * 2f
    val androidBitmap = image.asAndroidBitmap()

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val segmentWidth = width / SEGMENT_COUNT
        val foldX = if (forward) width * (1f - p) else width * p
        val pivotY = height * fingerY

        val scaleX = image.width.toFloat() / width
        val scaleY = image.height.toFloat() / height
        val srcHeight = (height * scaleY).roundToInt().coerceIn(1, image.height)

        // Opaque page base — no see-through gaps.
        drawRect(color = pageColor, size = Size(width, height))

        if (forward) {
            val flatRight = foldX.coerceIn(0f, width)
            if (flatRight > 1f) {
                val srcRight = (flatRight * scaleX).roundToInt().coerceIn(1, image.width)
                drawIntoCanvas { composeCanvas ->
                    composeCanvas.nativeCanvas.drawBitmap(
                        androidBitmap,
                        Rect(0, 0, srcRight, srcHeight),
                        RectF(0f, 0f, flatRight, height),
                        null
                    )
                }
            }
        } else {
            val flatLeft = foldX.coerceIn(0f, width)
            if (flatLeft < width - 1f) {
                val srcLeft = (flatLeft * scaleX).roundToInt().coerceIn(0, image.width - 1)
                drawIntoCanvas { composeCanvas ->
                    composeCanvas.nativeCanvas.drawBitmap(
                        androidBitmap,
                        Rect(srcLeft, 0, image.width, srcHeight),
                        RectF(flatLeft, 0f, width, height),
                        null
                    )
                }
            }
        }

        for (index in 0 until SEGMENT_COUNT) {
            val left = index * segmentWidth
            val right = left + segmentWidth
            val mid = (left + right) * 0.5f

            val participates = if (forward) mid >= foldX else mid <= foldX
            if (!participates) continue

            val span = if (forward) (width - foldX).coerceAtLeast(1f) else foldX.coerceAtLeast(1f)
            val t = if (forward) {
                ((mid - foldX) / span).coerceIn(0f, 1f)
            } else {
                ((foldX - mid) / span).coerceIn(0f, 1f)
            }

            val eased = curlEase(t)
            val angleY = if (forward) -eased * 178f else eased * 178f
            val angleX = cornerTorque * eased * 12f
            val lift = sin(eased * PI.toFloat()) * 8f

            val srcLeft = (left * scaleX).roundToInt().coerceIn(0, image.width - 1)
            val srcRight = (right * scaleX).roundToInt().coerceIn(srcLeft + 1, image.width)

            clipRect(left = left, top = 0f, right = right, bottom = height) {
                drawIntoCanvas { composeCanvas ->
                    val native = composeCanvas.nativeCanvas
                    native.save()
                    val camera = Camera()
                    val matrix = Matrix()
                    camera.save()
                    camera.rotateX(-angleX)
                    camera.rotateY(angleY)
                    camera.getMatrix(matrix)
                    camera.restore()
                    matrix.preTranslate(-foldX, -(pivotY + lift))
                    matrix.postTranslate(foldX, pivotY + lift)
                    native.concat(matrix)
                    native.drawBitmap(
                        androidBitmap,
                        Rect(srcLeft, 0, srcRight, srcHeight),
                        RectF(left, 0f, right, height),
                        null
                    )
                    native.restore()
                }
            }
        }

        drawCurlShadow(
            foldX = foldX,
            forward = forward,
            progress = p,
            height = height,
            width = width
        )
    }
}

private fun curlEase(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return 1f - (1f - clamped) * (1f - clamped) * (1f - clamped)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurlShadow(
    foldX: Float,
    forward: Boolean,
    progress: Float,
    width: Float,
    height: Float
) {
    val shadowWidth = 140f + 100f * progress
    val startX = if (forward) (foldX - shadowWidth).coerceAtLeast(0f) else foldX
    val endX = if (forward) foldX else (foldX + shadowWidth).coerceAtMost(width)

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.35f * progress),
                Color.Black.copy(alpha = 0.55f * progress)
            ),
            startX = startX,
            endX = endX
        ),
        topLeft = Offset(startX, 0f),
        size = Size((endX - startX).coerceAtLeast(0f), height)
    )

    val highlightStart = if (forward) foldX else (foldX - 20f).coerceAtLeast(0f)
    val highlightEnd = if (forward) (foldX + 18f).coerceAtMost(width) else foldX
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f * progress),
                Color.Transparent
            ),
            startX = highlightStart,
            endX = highlightEnd
        ),
        topLeft = Offset(highlightStart, 0f),
        size = Size((highlightEnd - highlightStart).coerceAtLeast(0f), height)
    )
}
