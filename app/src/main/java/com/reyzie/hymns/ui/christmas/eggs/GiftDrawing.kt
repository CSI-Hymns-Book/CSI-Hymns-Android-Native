package com.reyzie.hymns.ui.christmas.eggs

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform

/** Body and ribbon colours of the four gift wrappings. */
private val GiftPalettes = listOf(
    Color(0xFFC8352B) to Color(0xFFF2D58A),
    Color(0xFF2F7D55) to Color(0xFFF4F7FF),
    Color(0xFF3C63B8) to Color(0xFFF2D58A),
    Color(0xFF8E3FB0) to Color(0xFFFFD9E8),
)
private val RingGold = Color(0xFFE9C27B)
private const val GIFT_DESIGN_SIZE = 32f
private const val GIFT_SIZE_DP = 34f
private const val SQUASH_SECONDS = 0.42f
private const val OPEN_LID_SECONDS = 0.62f

private fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t.coerceIn(0f, 1f)

/** Draws every gift in [drops]. A gift is drawn on a 32 x 32 grid and scaled to about 34dp. */
fun DrawScope.drawGifts(drops: GiftDrops) {
    drops.gifts.forEach { drawGift(it) }
}

private fun DrawScope.drawGift(gift: Gift) {
    val (body, ribbon) = GiftPalettes[gift.paletteIndex % GiftPalettes.size]
    var scaleX = 1f
    var scaleY = 1f
    var alpha = 1f
    var lidLift = 0f
    var lidAngle = 0f
    var lidAlpha = 1f
    var dy = 0f
    when (gift.state) {
        GiftState.FALLING -> Unit
        GiftState.LANDED -> {
            val t = (gift.age / SQUASH_SECONDS).coerceIn(0f, 1f)
            if (t < 1f) {
                if (t < 0.5f) { scaleX = lerp(1.2f, 0.92f, t / 0.5f); scaleY = lerp(0.75f, 1.1f, t / 0.5f) }
                else { scaleX = lerp(0.92f, 1f, (t - 0.5f) / 0.5f); scaleY = lerp(1.1f, 1f, (t - 0.5f) / 0.5f) }
            }
            // A ring that keeps pulsing says "tap me" until the gift sinks.
            val pulse = (gift.age * 0.9f) % 1f
            drawCircle(RingGold.copy(alpha = 0.9f * (1f - pulse)), (12f + 10f * pulse) * density, Offset(gift.x, gift.y), style = Stroke(2f * density))
        }
        GiftState.OPENING -> {
            val a = gift.age
            val lid = (a / OPEN_LID_SECONDS).coerceIn(0f, 1f)
            lidLift = -34f * lid
            lidAngle = -60f * lid
            lidAlpha = if (lid < 0.45f) 1f else (1f - (lid - 0.45f) / 0.55f)
            when {
                a < 0.22f -> { scaleX = lerp(1f, 0.82f, a / 0.22f); scaleY = lerp(1f, 1.18f, a / 0.22f) }
                a < 0.45f -> { scaleX = lerp(0.82f, 1.15f, (a - 0.22f) / 0.23f); scaleY = lerp(1.18f, 0.9f, (a - 0.22f) / 0.23f) }
                a < 0.63f -> { scaleX = lerp(1.15f, 1f, (a - 0.45f) / 0.18f); scaleY = lerp(0.9f, 1f, (a - 0.45f) / 0.18f) }
                else -> { val f = ((a - 0.63f) / 0.27f).coerceIn(0f, 1f); scaleX = lerp(1f, 0.6f, f); scaleY = scaleX; alpha = 1f - f }
            }
        }
        GiftState.SINKING -> {
            val f = (gift.age / GiftDrops.SINK_SECONDS).coerceIn(0f, 1f)
            alpha = 1f - f
            dy = 10f * density * f
        }
    }
    val k = GIFT_SIZE_DP * density / GIFT_DESIGN_SIZE
    withTransform({
        translate(gift.x, gift.y + dy)
        rotate(gift.rotation, Offset.Zero)
        scale(scaleX * k, scaleY * k, Offset.Zero)
        translate(-GIFT_DESIGN_SIZE / 2f, -GIFT_DESIGN_SIZE / 2f)
    }) {
        drawRoundRect(body.copy(alpha = alpha), Offset(5f, 14f), Size(22f, 15f), CornerRadius(1.5f))
        drawRect(ribbon.copy(alpha = alpha), Offset(14f, 14f), Size(4f, 15f))
        // The lid, ribbon and bow lift off together when the gift opens.
        withTransform({
            translate(0f, lidLift)
            rotate(lidAngle, Offset(16f, 11f))
        }) {
            val lidA = alpha * lidAlpha
            drawRoundRect(body.copy(alpha = lidA), Offset(3f, 8f), Size(26f, 6f), CornerRadius(1.5f))
            drawRect(ribbon.copy(alpha = lidA), Offset(14f, 8f), Size(4f, 6f))
            drawPath(bowLoops(), ribbon.copy(alpha = lidA))
        }
    }
}

private fun bowLoops(): Path = Path().apply {
    moveTo(16f, 8f); cubicTo(10f, 0f, 4f, 4f, 10f, 8f); close()
    moveTo(16f, 8f); cubicTo(22f, 0f, 28f, 4f, 22f, 8f); close()
}
