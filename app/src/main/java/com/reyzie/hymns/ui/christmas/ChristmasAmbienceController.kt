package com.reyzie.hymns.ui.christmas

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import com.reyzie.hymns.ui.christmas.eggs.ChristmasEffects
import com.reyzie.hymns.ui.christmas.eggs.ChristmasSecret
import com.reyzie.hymns.ui.christmas.eggs.SantaFlight
import com.reyzie.hymns.ui.christmas.eggs.SantaPose
import com.reyzie.hymns.ui.christmas.landing.ChristmasCalendar
import com.reyzie.hymns.ui.christmas.landing.ChristmasMoment
import com.reyzie.hymns.ui.christmas.motion.SlideHapticThrottle
import com.reyzie.hymns.ui.christmas.motion.SnowHapticEvent
import com.reyzie.hymns.ui.christmas.snow.SnowCapMetrics
import com.reyzie.hymns.ui.christmas.snow.SnowEngine
import com.reyzie.hymns.ui.christmas.snow.SnowStepParams
import com.reyzie.hymns.ui.christmas.snow.SnowSurface
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlin.math.abs
import java.time.LocalDate
import kotlin.math.min
import kotlin.random.Random

/**
 * Shared state for all Christmas effects on screen: the snowfall, the snow caps on
 * cards, device tilt and shake, and dimming behind reading screens.
 *
 * One instance lives at the top of the app and is handed down through
 * [LocalChristmasAmbience]. It is null when Christmas mode is off.
 */
@Stable
class ChristmasAmbienceController internal constructor(
    level: ChristmasEffectsLevel,
    easterEggsEnabled: Boolean,
    foundSecrets: Set<ChristmasSecret>,
    val moment: ChristmasMoment,
    private val systemAnimationsOff: Boolean,
) {
    var level by mutableStateOf(level)
        internal set
    var easterEggsEnabled by mutableStateOf(easterEggsEnabled)
        internal set

    /** Easter eggs the user has found so far. */
    var foundSecrets by mutableStateOf(foundSecrets)
        internal set

    /** Called the first time each secret is found, so it can be saved. */
    internal var onSecretFound: (ChristmasSecret) -> Unit = {}

    /** Called when the snow does something the phone should vibrate for. */
    internal var onHaptic: (SnowHapticEvent) -> Unit = {}
    private val slideThrottle = SlideHapticThrottle(SLIDE_HAPTIC_GAP_MILLIS)

    /** The message currently shown at the bottom of the screen, if any. */
    var toast by mutableStateOf<ChristmasToast?>(null)
        private set
    private var nextToastId = 0L

    /** True when hidden surprises should respond to taps. */
    val eggsActive: Boolean get() = easterEggsEnabled && effectiveLevel != ChristmasEffectsLevel.OFF

    /** The level actually used: Off whenever the phone's "Remove animations" is on. */
    val effectiveLevel: ChristmasEffectsLevel
        get() = if (systemAnimationsOff) ChristmasEffectsLevel.OFF else level

    /** Smoothed side-to-side tilt in degrees; positive is right edge down. */
    var tiltDegrees by mutableFloatStateOf(0f)
        private set

    /** Goes up by one on every shake; snow caps watch it to know when to fall off. */
    var shakeGeneration by mutableIntStateOf(0)
        private set

    /** Snow opacity multiplier, eased toward [DIMMED_ALPHA] while a reading screen is open. */
    var snowAlpha by mutableFloatStateOf(1f)
        private set

    private var dimRequests by mutableIntStateOf(0)
    private var rawTilt = 0f
    private var flurryUntilSeconds = 0f

    val engine = SnowEngine()
    val effects = ChristmasEffects()

    private var santaStartSeconds: Float? = null
    private var nextSantaAtSeconds = if (moment == ChristmasMoment.CHRISTMAS_EVE) EVE_FIRST_SANTA_SECONDS else FIRST_SANTA_SECONDS
    private var nextFireworkAtSeconds = 0f

    val clockSeconds: Float get() = engine.timeSeconds
    val isFlurry: Boolean get() = clockSeconds < flurryUntilSeconds
    val isSnowing: Boolean get() = effectiveLevel != ChristmasEffectsLevel.OFF || isFlurry

    private val caps = LinkedHashMap<Any, CapEntry>()
    private val surfaceBuffer = ArrayList<SnowSurface>()

    private class CapEntry(val onAbsorb: (Float) -> Unit) {
        var bounds: Rect = Rect.Zero
        var depth: Float = 0f
        var visible: Boolean = true
    }

    init {
        engine.onAbsorb = { key -> caps[key]?.onAbsorb?.invoke(ABSORB_DEPTH) }
    }

    /** Advances everything by one frame. Called by the snowfall layer. */
    internal fun advance(dt: Float) {
        val tiltStep = (rawTilt - tiltDegrees) * min(1f, dt * TILT_RESPONSE)
        if (abs(tiltStep) > 0.001f) tiltDegrees += tiltStep
        val alphaTarget = if (dimRequests > 0) DIMMED_ALPHA else 1f
        val alphaStep = (alphaTarget - snowAlpha) * min(1f, dt * DIM_RESPONSE)
        if (abs(alphaStep) > 0.0005f) snowAlpha += alphaStep
        engine.step(
            dt = dt,
            params = SnowStepParams(
                densityFraction = effectiveLevel.snowDensity,
                tiltDegrees = tiltDegrees,
                isFlurry = isFlurry,
            ),
            surfaces = activeSurfaces(),
        )
        effects.unit = engine.unit
        effects.step(dt)
        if (abs(rawTilt) > SnowEngine.SLIDE_TILT_DEGREES) discover(ChristmasSecret.TILT)
        if (effectiveLevel != ChristmasEffectsLevel.OFF) {
            advanceSanta()
            launchFireworks()
        }
    }

    /** Where Santa is right now, or null when he is not flying. */
    fun santaPose(): SantaPose? {
        val start = santaStartSeconds ?: return null
        return SantaFlight.pose(clockSeconds - start, engine.width, engine.unit)
    }

    /** Sends Santa across the sky now, unless he is already flying. */
    fun callSanta() {
        if (santaStartSeconds == null) santaStartSeconds = clockSeconds
    }

    private fun advanceSanta() {
        val start = santaStartSeconds
        if (start == null) {
            if (clockSeconds >= nextSantaAtSeconds) santaStartSeconds = clockSeconds
            return
        }
        val pose = santaPose()
        if (pose == null) {
            santaStartSeconds = null
            nextSantaAtSeconds = clockSeconds + Random.nextFloat() * SANTA_GAP_SPREAD_SECONDS + SANTA_GAP_SECONDS
            return
        }
        if (Random.nextFloat() < TRAIL_SPARKS_PER_FRAME) {
            effects.sparkle(pose.x - SLEIGH_TAIL_DP * engine.unit, pose.y + 4f * engine.unit, count = 1)
        }
    }

    private fun launchFireworks() {
        if (moment != ChristmasMoment.NEW_YEAR || clockSeconds < nextFireworkAtSeconds || engine.width <= 0f) return
        effects.rocket(engine.width, launchY = FIREWORK_LAUNCH_DP * engine.unit)
        nextFireworkAtSeconds = clockSeconds + 0.8f + Random.nextFloat() * 0.8f
    }

    /** Confetti for Christmas Day. */
    fun celebrateChristmasDay() {
        if (effectiveLevel != ChristmasEffectsLevel.OFF && engine.width > 0f) effects.confetti(engine.width)
    }

    /**
     * Handles a tap anywhere on screen (root coordinates). Returns true when the tap caught
     * Santa or popped an emoji, so the tap should not also reach what is underneath.
     */
    fun handleTap(x: Float, y: Float): Boolean {
        if (!eggsActive) return false
        val pose = santaPose()
        if (pose != null && SantaFlight.isHit(pose, x, y, engine.unit)) {
            effects.gifts(pose.x, pose.y)
            if (!discover(ChristmasSecret.SANTA, "🎅 Ho ho ho! You caught Santa")) showToast("🎅 Ho ho ho! Merry Christmas!")
            return true
        }
        val popped = engine.popEmojiAt(x, y, EMOJI_HIT_RADIUS_DP * engine.unit, effectiveLevel.emojiCount) ?: return false
        effects.sparkle(popped.x, popped.y, count = 18)
        discover(ChristmasSecret.POP)
        return true
    }

    /**
     * Marks [secret] as found. Shows "Secret found" (or [message] with the count) the first
     * time and returns true; returns false if it was already found or eggs are off.
     */
    fun discover(secret: ChristmasSecret, message: String? = null): Boolean {
        if (!easterEggsEnabled || secret in foundSecrets) return false
        foundSecrets = foundSecrets + secret
        val count = "${foundSecrets.size} of ${ChristmasSecret.entries.size}"
        showToast(if (message != null) "$message · $count" else "✨ Secret found · $count")
        onSecretFound(secret)
        return true
    }

    fun showToast(text: String) {
        nextToastId += 1
        toast = ChristmasToast(nextToastId, text)
    }

    internal fun dismissToast(id: Long) {
        if (toast?.id == id) toast = null
    }

    fun setDeviceTilt(degrees: Float) {
        rawTilt = degrees.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
    }

    /** Knocks snow off every visible card and starts a short flurry. */
    fun shake() {
        if (effectiveLevel == ChristmasEffectsLevel.OFF) return
        shakeGeneration += 1
        onHaptic(SnowHapticEvent.Shake(caps.values.filter { it.visible }.sumOf { it.depth.toDouble() }.toFloat()))
        flurryUntilSeconds = clockSeconds + FLURRY_SECONDS
        discover(ChristmasSecret.SHAKE)
    }

    /** Dims the snow until the returned function is called. Safe to call the release twice. */
    fun requestDim(): () -> Unit {
        dimRequests += 1
        var released = false
        return {
            if (!released) {
                released = true
                dimRequests -= 1
            }
        }
    }

    internal fun registerCap(key: Any, onAbsorb: (Float) -> Unit) {
        caps[key] = CapEntry(onAbsorb)
    }

    internal fun unregisterCap(key: Any) {
        caps.remove(key)
    }

    internal fun updateCap(key: Any, bounds: Rect? = null, depth: Float? = null, visible: Boolean? = null) {
        val entry = caps[key] ?: return
        bounds?.let { entry.bounds = it }
        depth?.let { entry.depth = it }
        visible?.let { entry.visible = it }
    }

    internal fun spillFromCap(key: Any, direction: Int) {
        surfaceFor(key)?.let { engine.spill(it, direction.toFloat()) }
        val depth = caps[key]?.depth ?: return
        if (slideThrottle.tryFire((clockSeconds * MILLIS_PER_SECOND).toLong())) onHaptic(SnowHapticEvent.Slide(depth))
    }

    internal fun knockOffCap(key: Any, depth: Float) {
        surfaceFor(key)?.let { engine.knockOff(it, depth) }
    }

    private fun surfaceFor(key: Any): SnowSurface? {
        val entry = caps[key] ?: return null
        if (!entry.visible || entry.bounds.isEmpty) return null
        val lift = SnowCapMetrics.MOUND_ABOVE_CARD * engine.unit * entry.depth
        return SnowSurface(key, entry.bounds.left, entry.bounds.right, entry.bounds.top - lift)
    }

    private fun activeSurfaces(): List<SnowSurface> {
        surfaceBuffer.clear()
        for ((key, entry) in caps) {
            if (entry.depth < MIN_LANDING_DEPTH) continue
            surfaceFor(key)?.let { surfaceBuffer += it }
        }
        return surfaceBuffer
    }

    companion object {
        const val DIMMED_ALPHA = 0.3f
        private const val ABSORB_DEPTH = 0.012f
        private const val TILT_RESPONSE = 5f
        private const val DIM_RESPONSE = 4f
        private const val MAX_TILT_DEGREES = 40f
        private const val FLURRY_SECONDS = 5f
        private const val MIN_LANDING_DEPTH = 0.05f
        private const val FIRST_SANTA_SECONDS = 18f
        private const val EVE_FIRST_SANTA_SECONDS = 4f
        private const val SANTA_GAP_SECONDS = 60f
        private const val SANTA_GAP_SPREAD_SECONDS = 30f
        private const val TRAIL_SPARKS_PER_FRAME = 0.6f
        private const val SLEIGH_TAIL_DP = 18f
        private const val FIREWORK_LAUNCH_DP = 330f
        private const val EMOJI_HIT_RADIUS_DP = 22f
        private const val SLIDE_HAPTIC_GAP_MILLIS = 140L
        private const val MILLIS_PER_SECOND = 1_000f
    }
}

/** A short message shown at the bottom of the screen. */
data class ChristmasToast(val id: Long, val text: String)

/** The app's Christmas effects, or null when Christmas mode is off. */
val LocalChristmasAmbience = staticCompositionLocalOf<ChristmasAmbienceController?> { null }

@Composable
fun rememberChristmasAmbienceController(
    level: ChristmasEffectsLevel,
    easterEggsEnabled: Boolean,
    foundSecrets: Set<ChristmasSecret>,
    onSecretFound: (ChristmasSecret) -> Unit,
): ChristmasAmbienceController {
    val context = LocalContext.current
    val controller = remember {
        val animatorScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        ChristmasAmbienceController(
            level = level,
            easterEggsEnabled = easterEggsEnabled,
            foundSecrets = foundSecrets,
            moment = ChristmasCalendar.moment(LocalDate.now()),
            systemAnimationsOff = animatorScale == 0f,
        )
    }
    SideEffect {
        controller.level = level
        controller.easterEggsEnabled = easterEggsEnabled
        controller.foundSecrets = controller.foundSecrets + foundSecrets
        controller.onSecretFound = onSecretFound
        controller.onHaptic = { event -> HapticFeedbackManager.snow(context, event) }
    }
    return controller
}

/** Dims the snow while this is on screen. Put it in reading screens so lyrics stay clear. */
@Composable
fun DimChristmasSnow() {
    val controller = LocalChristmasAmbience.current ?: return
    DisposableEffect(controller) {
        val release = controller.requestDim()
        onDispose { release() }
    }
}
