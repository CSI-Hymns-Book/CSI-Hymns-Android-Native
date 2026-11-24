package com.reyzie.hymns.ui.christmas.eggs

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Kinds of one-off effect particles. */
enum class EffectKind { SPARK, CONFETTI, RING, ROCKET, GIFT, EMOJI }

/** How an emoji particle moves. Only FALL is integrated; the others are drawn from their age. */
enum class EmojiStyle { FALL, SWING, POP, RISE }

/** The emoji the gift surprises draw. Rendered once into bitmaps by the snowfall layer. */
const val CANDY_EMOJI = "🍭"
const val BELL_EMOJI = "🔔"
const val SNOWMAN_EMOJI = "⛄"
const val STAR_EMOJI = "⭐"
val SurpriseEmojis = listOf(CANDY_EMOJI, BELL_EMOJI, SNOWMAN_EMOJI, STAR_EMOJI)

/** Particle colours as ARGB, kept out of Compose so this file stays testable. */
object EffectColors {
    const val GOLD = 0xFFE9C27B.toInt()
    const val PALE_GOLD = 0xFFFFF3D1.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val ICE = 0xFFCFE0FF.toInt()
    const val HOLLY = 0xFFD6493D.toInt()
    const val PINE = 0xFF5FAE7F.toInt()
    const val EMBER = 0xFFF3B26B.toInt()
    val FIREWORKS = intArrayOf(0xFFFFD36E.toInt(), 0xFFFF7B6B.toInt(), 0xFF9FD8FF.toInt(), 0xFFC9A7FF.toInt(), WHITE, 0xFF8FE3B0.toInt())
    val CONFETTI = intArrayOf(GOLD, HOLLY, WHITE, PINE, EMBER)
}

class EffectParticle internal constructor(
    val kind: EffectKind,
    x: Float,
    y: Float,
    internal var vx: Float,
    internal var vy: Float,
    private val gravity: Float,
    val maxLife: Float,
    val color: Int,
    val size: Float,
    internal var delay: Float = 0f,
    val emoji: String = "",
    val style: EmojiStyle = EmojiStyle.FALL,
) {
    var x = x; internal set
    var y = y; internal set
    var life = maxLife; internal set
    var rotation = 0f; internal set
    internal var spin = 0f
    internal var flutterPhase = 0f

    /** 1 when new, falling to 0 as the particle dies. */
    val fade: Float get() = (life / maxLife).coerceIn(0f, 1f)
    val isWaiting: Boolean get() = delay > 0f

    internal fun step(dt: Float, time: Float, unit: Float) {
        when (kind) {
            EffectKind.CONFETTI -> {
                vy = (vy + gravity * dt).coerceAtMost(CONFETTI_MAX_FALL * unit)
                x += (vx + sin(time * 4f + flutterPhase) * 22f * unit) * dt
                y += vy * dt
            }
            EffectKind.RING -> Unit
            EffectKind.EMOJI -> if (style == EmojiStyle.FALL) {
                vy += gravity * dt
                x += vx * dt
                y += vy * dt
            }
            else -> {
                vy += gravity * dt
                vx *= SPARK_DRAG
                x += vx * dt
                y += vy * dt
            }
        }
        rotation += spin * dt
        life -= dt
    }

    private companion object {
        const val CONFETTI_MAX_FALL = 160f
        const val SPARK_DRAG = 0.985f
    }
}

/**
 * One-off celebration effects drawn over the app: sparkles, confetti, bell rings,
 * fireworks and falling gifts. Deliberately mutable for the same reason as the snow
 * engine: it runs every frame and must not allocate per frame.
 */
class ChristmasEffects(private val random: Random = Random.Default) {
    private val _particles = ArrayList<EffectParticle>()
    val particles: List<EffectParticle> get() = _particles

    /** Pixels per dp; set by the snowfall layer. */
    var unit = 1f
    private var time = 0f

    fun step(dt: Float) {
        time += dt
        val bursts = ArrayList<EffectParticle>(0)
        val iterator = _particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            if (p.delay > 0f) {
                p.delay -= dt
                continue
            }
            p.step(dt, time, unit)
            if (p.life <= 0f) {
                if (p.kind == EffectKind.ROCKET) bursts += p
                iterator.remove()
            }
        }
        bursts.forEach { burst(it.x, it.y, it.color) }
    }

    fun sparkle(
        x: Float,
        y: Float,
        count: Int = 16,
        colors: IntArray = intArrayOf(EffectColors.GOLD, EffectColors.PALE_GOLD, EffectColors.WHITE),
        delay: Float = 0f,
    ) {
        repeat(count) { i ->
            val angle = random.nextFloat() * TAU
            val speed = random.between(40f, 120f) * unit
            add(EffectKind.SPARK, x, y, cos(angle) * speed, sin(angle) * speed, 25f * unit, random.between(0.6f, 1f), colors[i % colors.size], random.between(1.2f, 2.6f) * unit, delay)
        }
    }

    fun snowPuff(x: Float, y: Float) = sparkle(x, y, 10, intArrayOf(EffectColors.WHITE, EffectColors.ICE))

    /** Plays what a gift turned into, at ([x], [y]) on a screen [width] pixels wide. */
    fun surprise(kind: GiftSurprise, x: Float, y: Float, width: Float) {
        when (kind) {
            GiftSurprise.CONFETTI -> confettiBurst(x, y)
            GiftSurprise.CANDY -> candyShower(width)
            GiftSurprise.BELLS -> bells(x, y)
            GiftSurprise.SNOWMAN -> snowman(x, y)
            GiftSurprise.STAR -> goldStar(x, y, width)
        }
    }

    /** A pop of confetti from a point, as when a gift opens. */
    fun confettiBurst(x: Float, y: Float, count: Int = BURST_CONFETTI_PIECES) {
        repeat(count) { i ->
            add(
                kind = EffectKind.CONFETTI,
                x = x,
                y = y,
                vx = random.between(-130f, 130f) * unit,
                vy = random.between(-150f, -40f) * unit,
                gravity = 380f * unit,
                life = random.between(1f, 1.7f),
                color = EffectColors.CONFETTI[i % EffectColors.CONFETTI.size],
                size = random.between(6f, 9f) * unit,
            ).apply {
                rotation = random.nextFloat() * TAU
                spin = random.between(-9f, 9f)
                flutterPhase = random.nextFloat() * TAU
            }
        }
    }

    /** Candy canes dropping across the whole [width] of the screen. */
    fun candyShower(width: Float) {
        repeat(CANDY_COUNT) { i ->
            add(
                kind = EffectKind.EMOJI,
                x = (i + 0.5f) / CANDY_COUNT * width + random.between(-10f, 10f) * unit,
                y = -20f * unit,
                vx = 0f,
                vy = random.between(90f, 140f) * unit,
                gravity = 300f * unit,
                life = 2.2f,
                color = EffectColors.WHITE,
                size = 24f * unit,
                delay = i * 0.07f,
                emoji = CANDY_EMOJI,
            ).apply { spin = random.between(-4f, 4f) }
        }
    }

    /** Two bells swinging, with gold rings spreading out. */
    fun bells(x: Float, y: Float) {
        for (side in intArrayOf(-1, 1)) {
            add(EffectKind.EMOJI, x + side * 18f * unit, y - 8f * unit, 0f, 0f, 0f, 1.5f, EffectColors.WHITE, 30f * unit, emoji = BELL_EMOJI, style = EmojiStyle.SWING)
                .also { it.spin = side.toFloat() }
        }
        rings(x, y)
    }

    /** A snowman popping up where the gift was. */
    fun snowman(x: Float, y: Float) {
        add(EffectKind.EMOJI, x, y, 0f, 0f, 0f, 2.2f, EffectColors.WHITE, 40f * unit, emoji = SNOWMAN_EMOJI, style = EmojiStyle.POP)
        sparkle(x, y - 30f * unit, 6)
    }

    /** A golden star that rises, spins and drifts off to the right, leaving sparkles behind. */
    fun goldStar(x: Float, y: Float, width: Float) {
        val rise = -(y - 70f * unit).coerceAtLeast(60f * unit)
        // vx is how far it drifts at the end and vy is how far it rises; neither is integrated.
        add(EffectKind.EMOJI, x, y, width * 0.6f, rise, 0f, 1.7f, EffectColors.GOLD, 40f * unit, emoji = STAR_EMOJI, style = EmojiStyle.RISE)
        repeat(STAR_TRAIL_SPARKS) { i ->
            val along = (i + 1f) / (STAR_TRAIL_SPARKS + 1f)
            sparkle(x + random.between(-12f, 12f) * unit, y + rise * along, count = 3, delay = i * 0.09f)
        }
    }

    fun confetti(width: Float) {
        repeat(CONFETTI_PIECES) { i ->
            add(
                kind = EffectKind.CONFETTI,
                x = random.nextFloat() * width,
                y = random.between(-260f, -10f) * unit,
                vx = random.between(-25f, 25f) * unit,
                vy = random.between(70f, 150f) * unit,
                gravity = 25f * unit,
                life = 7f,
                color = EffectColors.CONFETTI[i % EffectColors.CONFETTI.size],
                size = random.between(5f, 8f) * unit,
            ).apply {
                rotation = random.nextFloat() * TAU
                spin = random.between(-6f, 6f)
                flutterPhase = random.nextFloat() * TAU
            }
        }
    }

    /** Three gold rings spreading out from a ringing bell. */
    fun rings(x: Float, y: Float) {
        repeat(3) { i -> add(EffectKind.RING, x, y, 0f, 0f, 0f, 1.1f, EffectColors.GOLD, 6f * unit, delay = i * 0.22f) }
    }

    /** One firework launched from [launchY] at a random point across [width]. */
    fun rocket(width: Float, launchY: Float) {
        val color = EffectColors.FIREWORKS[random.nextInt(EffectColors.FIREWORKS.size)]
        add(EffectKind.ROCKET, random.between(50f * unit, width - 50f * unit), launchY, random.between(-12f, 12f) * unit, random.between(-270f, -230f) * unit, 95f * unit, random.between(0.8f, 1f), color, 1.6f * unit)
    }

    fun gifts(x: Float, y: Float) {
        repeat(3) {
            add(EffectKind.GIFT, x + random.between(-10f, 10f) * unit, y, random.between(-25f, 25f) * unit, random.between(-60f, -20f) * unit, 220f * unit, 2.4f, EffectColors.WHITE, 18f * unit)
                .apply { spin = random.between(-3f, 3f) }
        }
    }

    private fun burst(x: Float, y: Float, color: Int) {
        repeat(BURST_PIECES) { i ->
            val angle = random.nextFloat() * TAU
            val speed = random.between(50f, 125f) * unit
            add(EffectKind.SPARK, x, y, cos(angle) * speed, sin(angle) * speed, 38f * unit, random.between(1f, 1.5f), if (i % 2 == 0) color else EffectColors.WHITE, random.between(1.2f, 2.6f) * unit)
        }
    }

    private fun add(
        kind: EffectKind,
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        gravity: Float,
        life: Float,
        color: Int,
        size: Float,
        delay: Float = 0f,
        emoji: String = "",
        style: EmojiStyle = EmojiStyle.FALL,
    ): EffectParticle {
        val particle = EffectParticle(kind, x, y, vx, vy, gravity, life, color, size, delay, emoji, style)
        if (_particles.size < MAX_PARTICLES) _particles += particle
        return particle
    }

    private companion object {
        const val MAX_PARTICLES = 600
        const val CONFETTI_PIECES = 150
        const val BURST_PIECES = 46
        const val BURST_CONFETTI_PIECES = 44
        const val CANDY_COUNT = 9
        const val STAR_TRAIL_SPARKS = 10
        const val TAU = (2 * PI).toFloat()
    }
}

private fun Random.between(from: Float, until: Float): Float = from + nextFloat() * (until - from)
