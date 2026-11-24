package com.reyzie.hymns.ui.christmas.eggs

import com.reyzie.hymns.ui.christmas.snow.SnowSurface
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/*
 * Santa's gift drop: gifts fall from the sleigh, land on the cards (or fall off the screen),
 * and open into one of five small surprises when tapped. Pure logic, no Compose, so it can be
 * tested on the JVM. The numbers match the iOS app so both behave the same.
 */

/** What a gift turns into when it opens. [weight] is its chance out of 100. */
enum class GiftSurprise(val weight: Int, val title: String) {
    CONFETTI(30, "Confetti pop"),
    CANDY(25, "Candy-cane shower"),
    BELLS(20, "Jingle rings"),
    SNOWMAN(15, "Pop-up snowman"),
    STAR(10, "Golden star");

    companion object {
        /** [roll] is a random number in 0..<1; each surprise owns a slice as wide as its weight. */
        fun pick(roll: Float): GiftSurprise {
            var point = (roll.coerceIn(0f, 0.999_999f) * 100).toInt()
            for (surprise in entries) {
                if (point < surprise.weight) return surprise
                point -= surprise.weight
            }
            return CONFETTI
        }
    }
}

/** How many gifts open the "Gift collector" secret. */
const val GIFT_COLLECTOR_GOAL = 5

enum class GiftState { FALLING, LANDED, OPENING, SINKING }

/** One gift. Positions are pixels; [rotation] is the sway in degrees. */
class Gift internal constructor(
    val id: Int,
    var x: Float,
    var y: Float,
    internal var vx: Float,
    val paletteIndex: Int,
    internal var phase: Float,
) {
    internal var vy = 0f
    var rotation = 0f; internal set
    var age = 0f; internal set
    var state = GiftState.FALLING; internal set
    internal var surfaceKey: Any? = null
}

/**
 * Every gift on screen. Mutable on purpose, like the snow engine: it runs each frame and
 * must not allocate per frame.
 */
class GiftDrops {
    private val _gifts = ArrayList<Gift>()
    val gifts: List<Gift> get() = _gifts
    val isEmpty: Boolean get() = _gifts.isEmpty()

    private var nextId = 0
    private val droppedPoints = HashSet<Int>()

    /** Call when a new sleigh flight begins. */
    fun startFlight() = droppedPoints.clear()

    /**
     * Call every frame of the flight. Drops a gift at ([x], [y]) when the flight crosses a drop
     * point and [allowed] is true. A drop point that is skipped is not made up for later.
     */
    fun flight(progress: Float, x: Float, y: Float, allowed: Boolean, paletteSeed: Int = 0, unit: Float = 1f) {
        DROP_POINTS.forEachIndexed { index, point ->
            if (progress < point || index in droppedPoints) return@forEachIndexed
            droppedPoints += index
            if (!allowed || _gifts.size >= MAX_GIFTS) return@forEachIndexed
            _gifts += Gift(nextId, x, y, START_VX * unit, (nextId + paletteSeed) % PALETTE_COUNT, nextId * 1.7f)
            nextId += 1
        }
    }

    /**
     * Advances every gift. A falling gift lands on the highest surface it crosses; a landed gift
     * rides its card as the list scrolls and falls again if the card goes away.
     * [onLanded] gets the landing point.
     */
    fun step(dt: Float, surfaces: List<SnowSurface>, height: Float, unit: Float = 1f, onLanded: (Float, Float) -> Unit = { _, _ -> }) {
        val half = HALF_SIZE * unit
        val iterator = _gifts.iterator()
        while (iterator.hasNext()) {
            val gift = iterator.next()
            gift.age += dt
            when (gift.state) {
                GiftState.FALLING -> {
                    val previousBottom = gift.y + half
                    gift.vy = (gift.vy + GRAVITY * unit * dt).coerceAtMost(TERMINAL_SPEED * unit)
                    gift.vx *= VX_DECAY.pow(dt)
                    gift.phase += dt * SWAY_SPEED
                    gift.x += (gift.vx + sin(gift.phase) * SWAY * unit) * dt
                    gift.y += gift.vy * dt
                    gift.rotation = sin(gift.phase) * ROTATION_DEGREES
                    val surface = landingSurface(gift.x, previousBottom, gift.y + half, surfaces)
                    if (surface != null) {
                        gift.state = GiftState.LANDED
                        gift.surfaceKey = surface.key
                        gift.y = surface.top - half
                        gift.vx = 0f; gift.vy = 0f; gift.age = 0f
                        onLanded(gift.x, surface.top)
                    } else if (gift.y > height + FALL_OFF_MARGIN * unit) {
                        iterator.remove()
                    }
                }
                GiftState.LANDED -> {
                    val surface = surfaces.firstOrNull { it.key == gift.surfaceKey }
                    if (surface != null) {
                        gift.y = surface.top - half
                    } else {
                        gift.state = GiftState.FALLING; gift.surfaceKey = null; gift.vy = 0f
                    }
                    gift.rotation *= 0.02f.pow(dt)
                    if (gift.age > LANDED_GRACE_SECONDS) { gift.state = GiftState.SINKING; gift.age = 0f }
                }
                GiftState.SINKING -> if (gift.age > SINK_SECONDS) iterator.remove()
                GiftState.OPENING -> if (gift.age > OPEN_SECONDS) iterator.remove()
            }
        }
    }

    /** The nearest falling or landed gift within [radius] pixels of the tap starts opening and is returned. */
    fun open(x: Float, y: Float, radius: Float = TAP_RADIUS): Gift? {
        var best: Gift? = null
        var bestDistance = Float.MAX_VALUE
        for (gift in _gifts) {
            if (gift.state != GiftState.FALLING && gift.state != GiftState.LANDED) continue
            val distance = hypot(gift.x - x, gift.y - y)
            if (distance <= radius && distance < bestDistance) { best = gift; bestDistance = distance }
        }
        best?.let { it.state = GiftState.OPENING; it.age = 0f }
        return best
    }

    fun clear() = _gifts.clear()

    private fun landingSurface(x: Float, previousBottom: Float, bottom: Float, surfaces: List<SnowSurface>): SnowSurface? {
        var best: SnowSurface? = null
        for (s in surfaces) {
            if (x < s.left || x > s.right || previousBottom > s.top + LAND_SLACK || bottom < s.top) continue
            if (best == null || s.top < best.top) best = s
        }
        return best
    }

    companion object {
        /** Santa drops a gift each time his flight passes one of these fractions. */
        val DROP_POINTS = floatArrayOf(0.22f, 0.38f, 0.54f)
        const val MAX_GIFTS = 4
        const val HALF_SIZE = 14f
        const val LANDED_GRACE_SECONDS = 3.2f
        const val SINK_SECONDS = 0.5f
        const val OPEN_SECONDS = 1.0f
        const val TAP_RADIUS = 30f
        const val PALETTE_COUNT = 4
        private const val START_VX = 70f
        private const val GRAVITY = 150f
        private const val TERMINAL_SPEED = 120f
        private const val VX_DECAY = 0.35f
        private const val SWAY = 26f
        private const val SWAY_SPEED = 2.6f
        private const val ROTATION_DEGREES = 14f
        private const val FALL_OFF_MARGIN = 40f
        private const val LAND_SLACK = 6f
    }
}
