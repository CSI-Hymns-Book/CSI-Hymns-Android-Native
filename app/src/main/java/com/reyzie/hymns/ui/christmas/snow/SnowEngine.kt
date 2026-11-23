package com.reyzie.hymns.ui.christmas.snow

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** One depth band of snow. Sizes and speeds are in dp and dp per second. */
enum class SnowLayer(
    val count: Int,
    val minRadius: Float,
    val maxRadius: Float,
    val minSpeed: Float,
    val maxSpeed: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
    val parallax: Float,
    val isFlake: Boolean,
) {
    FAR(70, 0.6f, 1.1f, 12f, 20f, 0.22f, 0.40f, 0.35f, isFlake = false),
    MID(46, 1.1f, 1.9f, 22f, 34f, 0.35f, 0.55f, 0.65f, isFlake = false),
    NEAR(24, 5f, 8.5f, 38f, 56f, 0.50f, 0.85f, 1f, isFlake = true),
}

/** A card top that falling snow can land on, in pixels relative to the snow layer. */
data class SnowSurface(val key: Any, val left: Float, val right: Float, val top: Float)

/** Inputs for one simulation step. */
data class SnowStepParams(
    val densityFraction: Float,
    val tiltDegrees: Float = 0f,
    val isFlurry: Boolean = false,
)

class SnowParticle internal constructor(val layer: SnowLayer, val rank: Float) {
    var x = 0f; internal set
    var y = 0f; internal set
    var radius = 0f; internal set
    var alpha = 0f; internal set
    var rotation = 0f; internal set
    internal var speed = 0f
    internal var phase = 0f
    internal var amplitude = 0f
    internal var frequency = 0f
    internal var spin = 0f

    /** Whether this particle is drawn at the given density. */
    fun isVisible(densityFraction: Float): Boolean = rank < densityFraction
}

class SettledFlake internal constructor(
    val surfaceKey: Any,
    x: Float,
    y: Float,
    val radius: Float,
    val isFlake: Boolean,
    val rotation: Float,
    internal var life: Float,
) {
    var x = x; internal set
    var y = y; internal set
    val maxLife = life

    /** Fades in quickly, then out over the last second. */
    val alpha: Float get() = life.coerceIn(0f, 1f)
}

class SnowChunk internal constructor(x: Float, y: Float, vx: Float, vy: Float, val radius: Float) {
    var x = x; internal set
    var y = y; internal set
    internal var vx = vx
    internal var vy = vy
    internal var life = SnowEngine.CHUNK_LIFE_SECONDS
    val alpha: Float get() = (life * 3f).coerceIn(0f, 1f)
}

class EmojiDrifter internal constructor(val index: Int) {
    var x = 0f; internal set
    var y = 0f; internal set
    var tilt = 0f; internal set
    internal var speed = 0f
    internal var phase = 0f
    internal var hiddenSeconds = 0f

    /** True for a moment after the emoji is popped. */
    val isHidden: Boolean get() = hiddenSeconds > 0f
}

/**
 * Particle simulation for the Christmas snowfall.
 *
 * This class is deliberately mutable: it moves about 140 particles every frame, and
 * allocating fresh objects per frame would churn the garbage collector and drop frames.
 * Callers only read the particle lists; all writes happen inside the engine.
 *
 * Time runs on a continuous clock, so unlike the old 18-second looping animation there
 * is no restart and nothing jumps.
 */
class SnowEngine(private val random: Random = Random.Default) {
    private val _particles = ArrayList<SnowParticle>()
    private val _settled = ArrayList<SettledFlake>()
    private val _chunks = ArrayList<SnowChunk>()
    private val _emojis = ArrayList<EmojiDrifter>()

    val particles: List<SnowParticle> get() = _particles
    val settled: List<SettledFlake> get() = _settled
    val chunks: List<SnowChunk> get() = _chunks
    val emojis: List<EmojiDrifter> get() = _emojis

    var width = 0f; private set
    var height = 0f; private set

    /** Pixels per dp. */
    var unit = 1f; private set

    var timeSeconds = 0f; private set
    var wind = 0f; private set
    private var windTarget = 6f
    private var nextGustAt = 4f

    /** Called when a settled flake melts into the snow cap it landed on. */
    var onAbsorb: (surfaceKey: Any) -> Unit = {}

    fun resize(widthPx: Float, heightPx: Float, pxPerDp: Float) {
        if (widthPx <= 0f || heightPx <= 0f) return
        val needsSeed = _particles.isEmpty() || pxPerDp != unit
        width = widthPx
        height = heightPx
        unit = pxPerDp
        if (needsSeed) seed() else _particles.forEach { it.x = it.x.coerceIn(0f, width) }
    }

    fun step(dt: Float, params: SnowStepParams, surfaces: List<SnowSurface> = emptyList()) {
        if (width <= 0f || dt <= 0f) return
        timeSeconds += dt
        updateWind(dt, params.isFlurry)

        val tiltOn = abs(params.tiltDegrees) > SLIDE_TILT_DEGREES
        val drift = wind + params.tiltDegrees * TILT_DRIFT
        val speedMultiplier = if (params.isFlurry) FLURRY_SPEED else 1f
        val density = if (params.isFlurry) 1f else params.densityFraction

        for (p in _particles) {
            val previousY = p.y
            p.y += p.speed * speedMultiplier * dt
            p.x += (sin(timeSeconds * p.frequency + p.phase) * p.amplitude * 0.6f + drift * unit * p.layer.parallax) * dt
            p.rotation += p.spin * dt
            if (p.y > height + EDGE_DP * unit) respawnAtTop(p)
            wrapX(p)
            if (!tiltOn && p.layer != SnowLayer.FAR && p.isVisible(density)) {
                tryLand(p, previousY, surfaces)
            }
        }
        stepEmojis(dt, speedMultiplier)
        stepSettled(dt, params.tiltDegrees, surfaces)
        stepChunks(dt, params.tiltDegrees)
    }

    /** Throws one clump of snow off the [direction] side (-1 left, +1 right) of [surface]. */
    fun spill(surface: SnowSurface, direction: Float) {
        val x = if (direction > 0) surface.right - random.between(0f, 14f) * unit else surface.left + random.between(0f, 14f) * unit
        spawnChunk(x, surface.top, direction * random.between(40f, 90f) * unit, random.between(-30f, 0f) * unit, random.between(1.4f, 3f) * unit)
    }

    /** Knocks all snow off [surface], scaled by how deep it was (0..1). */
    fun knockOff(surface: SnowSurface, depth: Float) {
        val pieces = (depth.coerceIn(0f, 1f) * KNOCK_OFF_PIECES).toInt()
        repeat(pieces) {
            spawnChunk(
                x = random.between(surface.left + 6f * unit, surface.right - 6f * unit),
                y = surface.top,
                vx = random.between(-70f, 70f) * unit,
                vy = random.between(-150f, -40f) * unit,
                radius = random.between(1.4f, 3.2f) * unit,
            )
        }
        val iterator = _settled.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next()
            if (s.surfaceKey == surface.key) {
                spawnChunk(s.x, s.y, random.between(-60f, 60f) * unit, random.between(-120f, -40f) * unit, s.radius)
                iterator.remove()
            }
        }
    }

    private fun seed() {
        _particles.clear()
        SnowLayer.entries.forEach { layer ->
            repeat(layer.count) { i ->
                _particles += SnowParticle(layer, rank = i / layer.count.toFloat()).apply {
                    x = random.nextFloat() * width
                    y = random.nextFloat() * height
                    radius = random.between(layer.minRadius, layer.maxRadius) * unit
                    speed = random.between(layer.minSpeed, layer.maxSpeed) * unit
                    alpha = random.between(layer.minAlpha, layer.maxAlpha)
                    phase = random.nextFloat() * TAU
                    amplitude = random.between(6f, 16f) * unit
                    frequency = random.between(0.4f, 1.2f)
                    rotation = random.nextFloat() * TAU
                    spin = random.between(-0.6f, 0.6f)
                }
            }
        }
        _emojis.clear()
        repeat(EMOJI_SLOTS) { i ->
            _emojis += EmojiDrifter(i).apply {
                x = random.between(24f * unit, width - 24f * unit)
                y = random.nextFloat() * height
                speed = random.between(10f, 17f) * unit
                phase = random.nextFloat() * TAU
            }
        }
    }

    private fun updateWind(dt: Float, isFlurry: Boolean) {
        if (timeSeconds > nextGustAt) {
            windTarget = random.between(-10f, 16f)
            nextGustAt = timeSeconds + random.between(6f, 12f)
        }
        val target = if (isFlurry) FLURRY_WIND else windTarget
        wind += (target - wind) * (dt * 0.9f).coerceAtMost(1f)
    }

    private fun respawnAtTop(p: SnowParticle) {
        p.y = random.between(-30f, -8f) * unit
        p.x = random.nextFloat() * width
    }

    private fun wrapX(p: SnowParticle) {
        val margin = EDGE_DP * unit
        if (p.x < -margin) p.x += width + 2 * margin else if (p.x > width + margin) p.x -= width + 2 * margin
    }

    private fun tryLand(p: SnowParticle, previousY: Float, surfaces: List<SnowSurface>) {
        if (_settled.size >= MAX_SETTLED) return
        val inset = SURFACE_INSET_DP * unit
        val surface = surfaces.firstOrNull {
            previousY < it.top && p.y >= it.top && p.x > it.left + inset && p.x < it.right - inset
        } ?: return
        if (random.nextFloat() >= LAND_CHANCE) return
        val radius = if (p.layer.isFlake) p.radius * 0.7f else maxOf(1.3f * unit, p.radius)
        _settled += SettledFlake(
            surfaceKey = surface.key,
            x = p.x,
            y = surface.top,
            radius = radius,
            isFlake = p.layer.isFlake,
            rotation = p.rotation,
            life = random.between(2.5f, 6f),
        )
        respawnAtTop(p)
    }

    /**
     * Pops the first of the [visibleCount] emojis within [radius] of the tap, hiding it for a
     * moment before it falls again from the top. Returns the popped emoji, or null.
     */
    fun popEmojiAt(x: Float, y: Float, radius: Float, visibleCount: Int): EmojiDrifter? {
        val hit = _emojis.take(visibleCount).firstOrNull {
            !it.isHidden && (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) <= radius * radius
        } ?: return null
        hit.hiddenSeconds = EMOJI_HIDDEN_SECONDS
        return hit
    }

    private fun stepEmojis(dt: Float, speedMultiplier: Float) {
        for (e in _emojis) {
            if (e.isHidden) {
                e.hiddenSeconds -= dt
                if (!e.isHidden) {
                    e.y = -30f * unit
                    e.x = random.between(24f * unit, width - 24f * unit)
                }
                continue
            }
            e.y += e.speed * speedMultiplier * dt
            e.x = (e.x + (sin(timeSeconds * 0.5f + e.phase) * 10f + wind * 0.5f) * unit * dt)
                .coerceIn(14f * unit, width - 14f * unit)
            e.tilt = sin(timeSeconds * 0.8f + e.phase) * 0.25f
            if (e.y > height + 30f * unit) {
                e.y = -30f * unit
                e.x = random.between(24f * unit, width - 24f * unit)
            }
        }
    }

    private fun stepSettled(dt: Float, tilt: Float, surfaces: List<SnowSurface>) {
        val tiltOn = abs(tilt) > SLIDE_TILT_DEGREES
        val iterator = _settled.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next()
            val surface = surfaces.firstOrNull { it.key == s.surfaceKey }
            if (surface == null) {
                iterator.remove()
                continue
            }
            s.y = surface.top
            if (tiltOn) {
                s.x += tilt * SETTLED_SLIDE * unit * dt
                val margin = 6f * unit
                if (s.x < surface.left - margin || s.x > surface.right + margin) {
                    spawnChunk(s.x, s.y, tilt * 3f * unit, -10f * unit, s.radius)
                    iterator.remove()
                    continue
                }
            }
            s.life -= dt
            if (s.life <= 0f) {
                onAbsorb(s.surfaceKey)
                iterator.remove()
            }
        }
    }

    private fun stepChunks(dt: Float, tilt: Float) {
        val iterator = _chunks.iterator()
        while (iterator.hasNext()) {
            val c = iterator.next()
            c.vy += GRAVITY_DP * unit * dt
            c.vx += tilt * 3f * unit * dt
            c.x += c.vx * dt
            c.y += c.vy * dt
            c.life -= dt
            if (c.life <= 0f || c.y > height + 10f * unit) iterator.remove()
        }
    }

    private fun spawnChunk(x: Float, y: Float, vx: Float, vy: Float, radius: Float) {
        if (_chunks.size < MAX_CHUNKS) _chunks += SnowChunk(x, y, vx, vy, radius)
    }

    companion object {
        const val SLIDE_TILT_DEGREES = 12f
        const val CHUNK_LIFE_SECONDS = 2.2f
        const val MAX_SETTLED = 90
        const val EMOJI_SLOTS = 8
        private const val EMOJI_HIDDEN_SECONDS = 2.2f
        private const val MAX_CHUNKS = 260
        private const val TILT_DRIFT = 1.6f
        private const val FLURRY_SPEED = 2.3f
        private const val FLURRY_WIND = 80f
        private const val LAND_CHANCE = 0.55f
        private const val SURFACE_INSET_DP = 8f
        private const val EDGE_DP = 12f
        private const val GRAVITY_DP = 520f
        private const val SETTLED_SLIDE = 4f
        private const val KNOCK_OFF_PIECES = 16
        private const val TAU = (2 * PI).toFloat()
    }
}

private fun Random.between(from: Float, until: Float): Float = from + nextFloat() * (until - from)
