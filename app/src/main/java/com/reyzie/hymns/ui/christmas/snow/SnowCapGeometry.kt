package com.reyzie.hymns.ui.christmas.snow

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random

/** One drawing step of a snow cap outline, in dp relative to the cap's top-left corner. */
sealed interface CapCommand {
    data class MoveTo(val x: Float, val y: Float) : CapCommand
    data class LineTo(val x: Float, val y: Float) : CapCommand
    data class QuadTo(val controlX: Float, val controlY: Float, val x: Float, val y: Float) : CapCommand
}

/** Cap geometry, in dp. The cap box is one dp wider than the card on each side. */
object SnowCapMetrics {
    const val OVERHANG = 1f
    const val RISE = 8f
    const val HEIGHT = 20f

    /** The line in the cap that lines up with the card's top edge; depth scales around it. */
    const val BASELINE = 11f

    /** How far the mound top sits above the card at full depth. */
    const val MOUND_ABOVE_CARD = 3f

    /** Corner radius assumed when a card does not say. */
    const val DEFAULT_CORNER = 22f
}

private const val TIP_INSET = 0.22f
private const val SHOULDER_INSET = 0.55f
private const val TIP_DROP = 0.374f
private const val MOUND_TOP = 5f
private const val MAX_TIP_Y = 19.5f

/**
 * Builds the outline of a snow cap for a card [cardWidthDp] wide whose corners have radius
 * [cornerDp]. The snow stays inside the card's width: it rises from the rounded corners,
 * forms a lumpy mound along the top, and hangs a few icicles under the edge. The ends
 * follow the corner curve, so no snow sticks out into the air beside the card.
 * The same [seed] always gives the same shape, so a card keeps its cap between frames.
 */
fun buildSnowCapOutline(
    cardWidthDp: Float,
    seed: Int,
    cornerDp: Float = SnowCapMetrics.DEFAULT_CORNER,
): List<CapCommand> {
    val random = Random(seed)
    val o = SnowCapMetrics.OVERHANG
    val base = SnowCapMetrics.BASELINE
    val w = cardWidthDp + 2 * o
    val r = cornerDp.coerceIn(0f, cardWidthDp / 2f)
    val tipY = min(MAX_TIP_Y, base + TIP_DROP * r)
    val leftTip = o + TIP_INSET * r
    val rightTip = w - o - TIP_INSET * r
    val leftShoulder = o + SHOULDER_INSET * r + 4f
    val rightShoulder = w - o - SHOULDER_INSET * r - 4f

    val commands = ArrayList<CapCommand>()
    commands += CapCommand.MoveTo(leftTip, tipY)
    commands += CapCommand.QuadTo(o + 0.1f * r, MOUND_TOP + 2f, leftShoulder, MOUND_TOP)

    var x = leftShoulder
    while (x < rightShoulder - 6f) {
        val next = min(rightShoulder, x + random.nextFloat() * 10f + 12f)
        commands += CapCommand.QuadTo((x + next) / 2f, random.nextFloat() * 3.5f, next, 4f + random.nextFloat() * 2f)
        x = next
    }
    commands += CapCommand.QuadTo(w - o - 0.1f * r, MOUND_TOP + 2f, rightTip, tipY)
    commands += CapCommand.QuadTo(rightTip - 2f, base, rightShoulder, base)

    val span = (rightShoulder - leftShoulder - 16f).coerceAtLeast(0f)
    val dripCount = maxOf(2, (cardWidthDp / 70f).roundToInt())
    val drips = List(dripCount) { leftShoulder + 8f + random.nextFloat() * span }.sortedDescending()
    for (dx in drips) {
        commands += CapCommand.LineTo(dx + 3.5f, base)
        commands += CapCommand.LineTo(dx, base + 4f + random.nextFloat() * 4f)
        commands += CapCommand.LineTo(dx - 3.5f, base)
    }
    commands += CapCommand.LineTo(leftShoulder, base)
    commands += CapCommand.QuadTo(leftTip + 2f, base, leftTip, tipY)
    return commands
}

/** How deep a card's snow is (0..1) and how far it has slid sideways (dp). */
data class SnowCapValues(val depth: Float, val slideDp: Float = 0f)

/** Result of one step: the new values and, when snow fell off, which side (-1 or +1). */
data class SnowCapStep(val values: SnowCapValues, val spillDirection: Int = 0)

private const val GROW_PER_SECOND = 0.03f
private const val MELT_PER_SECOND_WHEN_TILTED = 0.9f
private const val SPILL_CHANCE_PER_LOST_DEPTH = 40f
private const val MAX_SLIDE_DP = 18f
private const val SLIDE_RESPONSE = 6f
private const val SLIDE_SNAP_DP = 0.05f

/**
 * Advances a snow cap by [dt] seconds. Tilting past the slide angle makes the snow slide
 * toward the low side and fall off; otherwise it slowly builds back up while it is snowing.
 * [roll] is a 0..1 random number used to decide whether a clump falls this frame.
 */
fun advanceSnowCap(
    values: SnowCapValues,
    dt: Float,
    tiltDegrees: Float,
    isSnowing: Boolean,
    roll: Float,
): SnowCapStep {
    val tiltOn = abs(tiltDegrees) > SnowEngine.SLIDE_TILT_DEGREES
    val direction = sign(tiltDegrees).toInt()
    var depth = values.depth
    var spill = 0
    if (tiltOn && depth > 0.02f) {
        val loss = min(depth, dt * MELT_PER_SECOND_WHEN_TILTED)
        depth -= loss
        if (roll < loss * SPILL_CHANCE_PER_LOST_DEPTH) spill = direction
    } else if (isSnowing) {
        depth = min(1f, depth + dt * GROW_PER_SECOND)
    }
    val target = if (tiltOn) direction * (1f - depth) * MAX_SLIDE_DP else 0f
    val eased = values.slideDp + (target - values.slideDp) * min(1f, dt * SLIDE_RESPONSE)
    val slide = if (abs(target - eased) < SLIDE_SNAP_DP) target else eased
    return SnowCapStep(SnowCapValues(depth.coerceIn(0f, 1f), slide), spill)
}
