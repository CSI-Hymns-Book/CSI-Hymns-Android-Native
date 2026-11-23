package com.reyzie.hymns.ui.christmas.landing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** The scene is designed on a 390 x 352 dp canvas and stretched to fit. */
internal const val HERO_DESIGN_WIDTH = 390f
internal const val HERO_DESIGN_HEIGHT = 352f

/** Where the Star of Bethlehem sits, in design units. */
internal val STAR_CENTER = Offset(300f, 172f)

private const val NANOS_PER_SECOND = 1_000_000_000f
private const val CARAVAN_PERIOD_SECONDS = 60f
private const val CARAVAN_START_SECONDS = 6f
private const val CARAVAN_SPEED = 15f
private const val TWO_PI = (2 * PI).toFloat()
private const val STAR_HIT_RADIUS = 34f
private const val SNOW_TAP_MAX_X = 120f
private const val SNOW_TAP_HEIGHT = 62f
private const val SNOWMAN_X = 58f
private const val SHOOTING_STAR_SECONDS = 1.3f
private const val STAR_FLARE_DECAY = 1.6f
private const val EPIPHANY_STOP_X = 250f
private const val PIECE_GROW_SECONDS = 0.45f
private val WarmWindow = Color(0xFFFFC96B)
private val StableGlow = Color(0xFFFFBE64)
private val StarCore = Color(0xFFFFF6DC)

private data class SkyStar(val x: Float, val y: Float, val radius: Float, val phase: Float, val speed: Float)

private enum class BuildingKind { HOUSE, TOWER, DOME, STABLE }

private data class Building(val x: Float, val width: Float, val height: Float, val kind: BuildingKind, val litWindows: List<Boolean>)

private fun farHill(x: Float) = 290f + 9f * sin(x / 70f + 1f) + 5f * sin(x / 23f)
private fun nearHill(x: Float) = 312f + 7f * sin(x / 90f + 2f) + 3f * sin(x / 31f)

/** Seconds since the scene appeared, ticking only while [animate] is true. */
@Composable
fun rememberSceneClock(animate: Boolean): State<Float> {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var last = -1L
        while (isActive) {
            withFrameNanos { now ->
                if (last >= 0) clock.floatValue += ((now - last) / NANOS_PER_SECOND).coerceAtMost(0.05f)
                last = now
            }
        }
    }
    return clock
}

/**
 * The night sky over Bethlehem at the top of the Christmas landing screen: twinkling
 * stars, the Star of Bethlehem shining down on the stable, the town skyline with a CSI
 * church tower, and the wise men crossing the hills about once a minute.
 */
@Composable
fun ChristmasHeroScene(
    palette: ChristmasPalette,
    clock: State<Float>,
    eggs: HeroEggState,
    tapsEnabled: Boolean,
    onStarTap: (rootPosition: Offset) -> Unit,
    onSnowTap: (rootPosition: Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val starTap by rememberUpdatedState(onStarTap)
    val snowTap by rememberUpdatedState(onSnowTap)
    val stars = remember {
        val random = Random(7)
        List(85) {
            SkyStar(
                x = random.nextFloat() * HERO_DESIGN_WIDTH,
                y = random.nextFloat() * 262f,
                radius = 0.4f + random.nextFloat() * 0.9f,
                phase = random.nextFloat() * TWO_PI,
                speed = 0.6f + random.nextFloat() * 1.6f,
            )
        }
    }
    val buildings = remember {
        val random = Random(11)
        fun lit() = List(6) { random.nextFloat() > 0.3f }
        listOf(
            Building(112f, 26f, 22f, BuildingKind.HOUSE, lit()),
            Building(141f, 18f, 36f, BuildingKind.TOWER, lit()),
            Building(162f, 34f, 18f, BuildingKind.HOUSE, lit()),
            Building(199f, 24f, 32f, BuildingKind.DOME, lit()),
            Building(226f, 30f, 20f, BuildingKind.HOUSE, lit()),
            Building(273f, 52f, 30f, BuildingKind.STABLE, emptyList()),
            Building(330f, 24f, 26f, BuildingKind.HOUSE, lit()),
            Building(357f, 30f, 18f, BuildingKind.HOUSE, lit()),
        )
    }

    Canvas(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(tapsEnabled) {
                if (!tapsEnabled) return@pointerInput
                detectTapGestures { tap ->
                    val sx = size.width / HERO_DESIGN_WIDTH
                    val sy = size.height / HERO_DESIGN_HEIGHT
                    val x = tap.x / sx
                    val y = tap.y / sy
                    when {
                        hypot(x - STAR_CENTER.x, y - STAR_CENTER.y) < STAR_HIT_RADIUS ->
                            starTap(origin + Offset(STAR_CENTER.x * sx, STAR_CENTER.y * sy))
                        x < SNOW_TAP_MAX_X && y > HERO_DESIGN_HEIGHT - SNOW_TAP_HEIGHT ->
                            snowTap(origin + Offset(SNOWMAN_X * sx, (HERO_DESIGN_HEIGHT - 30f) * sy))
                    }
                }
            },
    ) {
        val t = clock.value
        val scene = SceneScale(this)
        drawRect(
            brush = Brush.verticalGradient(
                0f to palette.skyTop,
                0.43f to palette.skyMid,
                0.72f to palette.skyLow,
                0.91f to palette.horizon,
                1f to palette.ground,
            ),
        )
        val starAlpha = if (palette.isNight) 1f else 0.22f
        stars.forEach { s ->
            val twinkle = 0.3f + 0.7f * (0.5f + 0.5f * sin(t * s.speed + s.phase))
            drawCircle(Color(0xFFFFFAEB).copy(alpha = starAlpha * twinkle), s.radius * scene.unit, scene.at(s.x, s.y))
        }
        val flare = (1f - (t - eggs.starTappedAt) * STAR_FLARE_DECAY).coerceIn(0f, 1f)
        drawStarOfBethlehem(scene, t, palette, flare)
        eggs.shootingStarAt?.let { drawShootingStar(scene, (t - it) / SHOOTING_STAR_SECONDS) }
        drawHill(scene, ::farHill, if (palette.isNight) Color(0xFF1C1A3D) else Color(0xFF8193BB))
        drawCaravan(scene, t, cityColor(palette), stopUnderStar = eggs.moment == ChristmasMoment.EPIPHANY)
        ShepherdWalk.walkers(t).forEach { drawShepherdWalker(scene, it, t, cityColor(palette), palette.isNight) }
        drawHill(scene, ::nearHill, if (palette.isNight) Color(0xFF11132C) else Color(0xFF5D7097))
        val candles = eggs.moment == ChristmasMoment.CHRISTMAS_EVE
        buildings.forEach { drawBuilding(scene, it, cityColor(palette), t, candles) }
        drawSnowMound(scene, t, palette.capSnow)
        drawSnowman(scene, t, eggs, palette)
    }
}

private fun cityColor(palette: ChristmasPalette) = if (palette.isNight) Color(0xFF0A0C20) else Color(0xFF3A4768)

/** Converts design units to pixels for the current canvas size. */
private class SceneScale(scope: DrawScope) {
    val unit = scope.density
    val sx = scope.size.width / (HERO_DESIGN_WIDTH * unit)
    val sy = scope.size.height / (HERO_DESIGN_HEIGHT * unit)
    fun x(v: Float) = v * unit * sx
    fun y(v: Float) = v * unit * sy
    fun at(x: Float, y: Float) = Offset(x(x), y(y))
}

private fun DrawScope.drawStarOfBethlehem(scene: SceneScale, t: Float, palette: ChristmasPalette, flare: Float) {
    val center = scene.at(STAR_CENTER.x, STAR_CENTER.y)
    val glowColor = if (palette.isNight) Color(0xFFE9C27B) else Color(0xFFDEA046)
    val glowRadius = (46f * (1f + 0.06f * sin(t * 1.4f)) + flare * 34f) * scene.unit
    drawCircle(
        brush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.5f + 0.35f * flare), Color.Transparent), center, glowRadius),
        radius = glowRadius,
        center = center,
    )
    val rayLengths = floatArrayOf(40f, 15f, 32f, 15f, 104f, 15f, 32f, 15f)
    rayLengths.forEachIndexed { i, baseLength ->
        val angle = -PI.toFloat() / 2f + i * PI.toFloat() / 4f + sin(t * 0.4f) * 0.03f
        val length = baseLength * scene.unit * (1f + 0.08f * sin(t * 2.3f + i)) * (1f + flare * 0.25f)
        val tip = Offset(center.x + cos(angle) * length, center.y + sin(angle) * length)
        val half = 1.8f * scene.unit
        val perpendicular = Offset(-sin(angle) * half, cos(angle) * half)
        val ray = Path().apply {
            moveTo(center.x + perpendicular.x, center.y + perpendicular.y)
            lineTo(center.x - perpendicular.x, center.y - perpendicular.y)
            lineTo(tip.x, tip.y)
            close()
        }
        drawPath(ray, Brush.linearGradient(listOf(StarCore.copy(alpha = 0.95f), StarCore.copy(alpha = 0f)), center, tip))
    }
    val core = Path().apply {
        repeat(8) { i ->
            val angle = -PI / 2 + i * PI / 4
            val r = (if (i % 2 == 1) 2.6f else 9f + flare * 3f) * scene.unit
            val p = Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    drawCircle(StarCore.copy(alpha = 0.35f), 7f * scene.unit, center)
    drawPath(core, StarCore)
}

private fun DrawScope.drawHill(scene: SceneScale, height: (Float) -> Float, color: Color) {
    val path = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= HERO_DESIGN_WIDTH) {
            lineTo(scene.x(x), scene.y(height(x)))
            x += 6f
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

/** The wise men cross about once a minute; on Epiphany they stop under the star. */
private fun DrawScope.drawCaravan(scene: SceneScale, t: Float, color: Color, stopUnderStar: Boolean) {
    if (stopUnderStar) {
        val walked = -40f + (t - 2f).coerceAtLeast(0f) * CARAVAN_SPEED
        val leadX = walked.coerceAtMost(EPIPHANY_STOP_X)
        repeat(3) { i -> drawCamel(scene, leadX - i * 30f, t, color, walking = walked < EPIPHANY_STOP_X) }
        return
    }
    val cycleTime = (t % CARAVAN_PERIOD_SECONDS) - CARAVAN_START_SECONDS
    if (cycleTime < 0f || t < CARAVAN_START_SECONDS) return
    val leadX = -40f + cycleTime * CARAVAN_SPEED
    if (leadX - 60f > HERO_DESIGN_WIDTH + 30f) return
    repeat(3) { i -> drawCamel(scene, leadX - i * 30f, t, color, walking = true) }
}

private fun DrawScope.drawCamel(scene: SceneScale, x: Float, t: Float, color: Color, walking: Boolean) {
    val base = farHill(x) + 1f
    val u = scene.unit
    listOf(-7f, -4f, 5f, 8f).forEachIndexed { i, offset ->
        val swing = if (walking) sin(t * 6f + i * PI.toFloat()) * 2.4f else 0f
        drawLine(color, scene.at(x + offset, base - 10f), scene.at(x + offset + swing, base), 1.8f * u, StrokeCap.Round)
    }
    drawOval(color, scene.at(x - 10f, base - 18f), Size(20f * u, 10f * u))
    drawOval(color, scene.at(x - 6f, base - 21f), Size(10f * u, 8f * u))
    drawLine(color, scene.at(x + 8f, base - 14f), scene.at(x + 13f, base - 22f), 3f * u, StrokeCap.Round)
    drawOval(color, scene.at(x + 11.4f, base - 24f), Size(7.2f * u, 4f * u))
    val rider = Path().apply {
        moveTo(scene.x(x - 5f), scene.y(base - 18f))
        lineTo(scene.x(x + 2f), scene.y(base - 18f))
        lineTo(scene.x(x - 1.5f), scene.y(base - 29f))
        close()
    }
    drawPath(rider, color)
    drawCircle(color, 2.3f * u, scene.at(x - 1.5f, base - 31f))
    drawRect(Color(0xFFE9C27B), scene.at(x - 3.5f, base - 35.5f), Size(4f * u, 1.8f * u))
}

private fun DrawScope.drawBuilding(scene: SceneScale, b: Building, color: Color, t: Float, candles: Boolean) {
    val base = nearHill(b.x + b.width / 2f) + 3f
    val top = base - b.height
    fun rect(x: Float, y: Float, w: Float, h: Float, c: Color = color) =
        drawRect(c, scene.at(x, y), Size(scene.x(w), scene.y(h)))

    when (b.kind) {
        BuildingKind.STABLE -> {
            drawStable(scene, b, base, top, color, t)
            return
        }
        BuildingKind.DOME -> {
            rect(b.x, top + 9f, b.width, b.height + 12f)
            drawCircle(color, scene.x(b.width / 2f), scene.at(b.x + b.width / 2f, top + 10f))
        }
        BuildingKind.TOWER -> {
            rect(b.x, top, b.width, b.height + 20f)
            rect(b.x + b.width / 2f - 4f, top - 8f, 8f, 8f)
            rect(b.x + b.width / 2f - 0.8f, top - 17f, 1.6f, 9f)
            rect(b.x + b.width / 2f - 3f, top - 14f, 6f, 1.6f)
        }
        BuildingKind.HOUSE -> {
            rect(b.x, top, b.width, b.height + 20f)
            rect(b.x + 2f, top - 3f, 6f, 3f)
        }
    }
    val rows = if (b.height > 24f) 2 else 1
    var windowIndex = 0
    repeat(rows) { row ->
        var wx = b.x + 4f
        while (wx + 4f <= b.x + b.width - 3f) {
            val lit = b.litWindows[windowIndex++ % b.litWindows.size] || candles
            if (lit) {
                val wy = top + (if (b.kind == BuildingKind.DOME) 14f else 6f) + row * 11f
                val flicker = if (candles) 0.85f + 0.15f * sin(t * 9f + wx) else 0.8f
                if (candles) drawCircle(WarmWindow.copy(alpha = 0.22f), 6f * scene.unit, scene.at(wx + 1.5f, wy + 2f))
                rect(wx, wy, 3f, 4f, WarmWindow.copy(alpha = flicker))
            }
            wx += 8f
        }
    }
}

/** The stable under the star: a warm glow, the manger, Mary and Joseph. */
private fun DrawScope.drawStable(scene: SceneScale, b: Building, base: Float, top: Float, color: Color, t: Float) {
    val cx = b.x + b.width / 2f
    val glowCenter = scene.at(cx, base - 6f)
    val glowRadius = 34f * scene.unit
    drawCircle(
        brush = Brush.radialGradient(
            listOf(StableGlow.copy(alpha = 0.55f + 0.08f * sin(t * 2f)), Color.Transparent),
            glowCenter,
            glowRadius,
        ),
        radius = glowRadius,
        center = glowCenter,
    )
    val roof = Path().apply {
        moveTo(scene.x(b.x - 5f), scene.y(top + 7f))
        lineTo(scene.x(cx), scene.y(top - 6f))
        lineTo(scene.x(b.x + b.width + 5f), scene.y(top + 7f))
        lineTo(scene.x(b.x + b.width + 5f), scene.y(top + 10f))
        lineTo(scene.x(b.x - 5f), scene.y(top + 10f))
        close()
    }
    drawPath(roof, color)
    drawRect(color, scene.at(b.x, top + 9f), Size(scene.x(4f), scene.y(b.height + 12f)))
    drawRect(color, scene.at(b.x + b.width - 4f, top + 9f), Size(scene.x(4f), scene.y(b.height + 12f)))
    drawRect(WarmWindow.copy(alpha = 0.55f), scene.at(b.x + 4f, top + 10f), Size(scene.x(b.width - 8f), scene.y(b.height - 8f)))
    drawNativity(scene, cx, base, t)
    drawRect(color, scene.at(b.x - 2f, base - 2f), Size(scene.x(b.width + 4f), scene.y(24f)))
}

/** Snow along the bottom of the scene that slowly piles up after the screen opens. */
private fun DrawScope.drawSnowMound(scene: SceneScale, t: Float, color: Color) {
    val depth = (5f + t * 0.12f).coerceAtMost(12f)
    val path = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= HERO_DESIGN_WIDTH) {
            val bump = depth * (0.65f + 0.35f * sin(x / 37f + 0.5f)) * (0.8f + 0.2f * sin(x / 11f))
            lineTo(scene.x(x), scene.y(HERO_DESIGN_HEIGHT - 3f - bump))
            x += 8f
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

/** A shooting star streaking from the top right across the sky; [progress] runs 0..1. */
private fun DrawScope.drawShootingStar(scene: SceneScale, progress: Float) {
    if (progress < 0f || progress >= 1f) return
    val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
    val head = scene.at(384f - 330f * eased, 46f + 112f * eased)
    val length = 110f * (1f - progress * 0.6f)
    val tail = Offset(head.x + scene.x(length * 0.948f), head.y - scene.y(length * 0.322f))
    drawLine(
        brush = Brush.linearGradient(listOf(StarCore.copy(alpha = 1f - progress), StarCore.copy(alpha = 0f)), head, tail),
        start = head,
        end = tail,
        strokeWidth = 2.2f * scene.unit,
        cap = StrokeCap.Round,
    )
    drawCircle(StarCore.copy(alpha = 1f - progress), 2.4f * scene.unit, head)
}

/** The snowman that builds himself one piece per tap, then waves. */
private fun DrawScope.drawSnowman(scene: SceneScale, t: Float, eggs: HeroEggState, palette: ChristmasPalette) {
    if (eggs.snowmanStage == 0) return
    val ground = HERO_DESIGN_HEIGHT - 9f
    val shade = if (palette.isNight) Color(0xFFAAB6D0) else Color(0xFFC7D3E6)
    fun grow(index: Int): Float {
        val p = ((t - eggs.pieceStartedAt(index)) / PIECE_GROW_SECONDS).coerceIn(0f, 1f)
        return 1f - (1f - p) * (1f - p) * (1f - p) + sin(p * PI.toFloat()) * 0.12f
    }
    fun ball(cy: Float, r: Float, scale: Float) {
        val center = scene.at(SNOWMAN_X, cy)
        drawCircle(palette.capSnow, r * scale * scene.unit, center)
        drawCircle(shade, r * scale * scene.unit, center, style = Stroke(scene.unit))
    }
    ball(ground - 12f, 13f, grow(0))
    if (eggs.snowmanStage >= 2) ball(ground - 31f, 9f, grow(1))
    if (eggs.snowmanStage < 3) return
    val headScale = grow(2)
    ball(ground - 45f, 6.8f, headScale)
    if (headScale < 0.9f) return
    val x = SNOWMAN_X
    val coal = Color(0xFF1A1A22)
    drawRect(coal, scene.at(x - 7f, ground - 52f), Size(scene.x(14f), scene.y(2.6f)))
    drawRect(coal, scene.at(x - 4.5f, ground - 61f), Size(scene.x(9f), scene.y(9.5f)))
    drawRect(Color(0xFFD6493D), scene.at(x - 4.5f, ground - 54.5f), Size(scene.x(9f), scene.y(2f)))
    drawCircle(coal, 0.9f * scene.unit, scene.at(x - 2.4f, ground - 46.5f))
    drawCircle(coal, 0.9f * scene.unit, scene.at(x + 2.4f, ground - 46.5f))
    val nose = Path().apply {
        moveTo(scene.x(x), scene.y(ground - 44.5f))
        lineTo(scene.x(x + 7f), scene.y(ground - 43.5f))
        lineTo(scene.x(x), scene.y(ground - 42.6f))
        close()
    }
    drawPath(nose, Color(0xFFEF8A2F))
    drawRect(Color(0xFFD6493D), scene.at(x - 7f, ground - 39.5f), Size(scene.x(14f), scene.y(3f)))
    drawRect(Color(0xFFD6493D), scene.at(x + 3f, ground - 39f), Size(scene.x(3f), scene.y(8f)))
    val stick = Color(0xFF5B4030)
    drawLine(stick, scene.at(x - 8f, ground - 32f), scene.at(x - 20f, ground - 40f), 1.5f * scene.unit, StrokeCap.Round)
    val wave = -0.7f + 0.45f * sin(t * 6f)
    drawLine(stick, scene.at(x + 8f, ground - 32f), scene.at(x + 8f + cos(wave) * 14f, ground - 32f + sin(wave) * 14f), 1.5f * scene.unit, StrokeCap.Round)
}

private val MaryRobe = Color(0xFF3E5BA9)
private val MaryVeil = Color(0xFF2B4180)
private val JosephRobe = Color(0xFFA8702F)
private val SkinTone = Color(0xFFF1C79E)
private val WoodBrown = Color(0xFF6B4423)
private val HayGold = Color(0xFFE8C46A)
private val SwaddleCream = Color(0xFFFFF3DA)

/**
 * The nativity inside the stable, drawn in colour so it reads against the warm glow:
 * Mary kneeling on the left, Joseph standing with his staff on the right, and the baby
 * Jesus asleep in the manger between them with a soft halo that gently pulses.
 */
private fun DrawScope.drawNativity(scene: SceneScale, cx: Float, base: Float, t: Float) {
    val u = scene.unit
    // Mary, kneeling and leaning toward the manger.
    val mx = cx - 13.5f
    val robe = Path().apply {
        moveTo(scene.x(mx - 5.5f), scene.y(base - 1f))
        quadraticTo(scene.x(mx - 4f), scene.y(base - 9f), scene.x(mx + 0.5f), scene.y(base - 11f))
        quadraticTo(scene.x(mx + 4.5f), scene.y(base - 9f), scene.x(mx + 5.5f), scene.y(base - 1f))
        close()
    }
    drawPath(robe, MaryRobe)
    drawCircle(MaryVeil, 3f * u, scene.at(mx + 1f, base - 13f))
    drawCircle(SkinTone, 1.5f * u, scene.at(mx + 2f, base - 12.6f))
    drawLine(MaryRobe, scene.at(mx + 3f, base - 8f), scene.at(mx + 7.5f, base - 5.5f), 1.5f * u, StrokeCap.Round)

    // Joseph, standing with a staff.
    val jx = cx + 14.5f
    val tunic = Path().apply {
        moveTo(scene.x(jx - 4.5f), scene.y(base - 1f))
        lineTo(scene.x(jx - 2.6f), scene.y(base - 14f))
        lineTo(scene.x(jx + 2.6f), scene.y(base - 14f))
        lineTo(scene.x(jx + 4.5f), scene.y(base - 1f))
        close()
    }
    drawPath(tunic, JosephRobe)
    drawCircle(SkinTone, 2.3f * u, scene.at(jx - 0.4f, base - 16.4f))
    drawArc(WoodBrown, 180f, 180f, true, scene.at(jx - 2.9f, base - 19.3f), Size(5.8f * u, 4f * u))
    drawLine(WoodBrown, scene.at(jx + 6f, base - 1f), scene.at(jx + 6f, base - 19f), 1.1f * u, StrokeCap.Round)
    drawLine(WoodBrown, scene.at(jx + 6f, base - 19f), scene.at(jx + 4.3f, base - 20.2f), 1.1f * u, StrokeCap.Round)

    // The manger with hay, and the baby Jesus with his halo.
    val halo = 0.5f + 0.12f * sin(t * 2.2f)
    val babyCenter = scene.at(cx, base - 8.8f)
    drawCircle(
        brush = Brush.radialGradient(listOf(SwaddleCream.copy(alpha = halo), Color.Transparent), babyCenter, 10f * u),
        radius = 10f * u,
        center = babyCenter,
    )
    val trough = Path().apply {
        moveTo(scene.x(cx - 7.5f), scene.y(base - 7f))
        lineTo(scene.x(cx + 7.5f), scene.y(base - 7f))
        lineTo(scene.x(cx + 5f), scene.y(base - 0.8f))
        lineTo(scene.x(cx - 5f), scene.y(base - 0.8f))
        close()
    }
    drawPath(trough, WoodBrown)
    drawLine(WoodBrown, scene.at(cx - 5f, base - 0.8f), scene.at(cx - 7f, base), 1.2f * u, StrokeCap.Round)
    drawLine(WoodBrown, scene.at(cx + 5f, base - 0.8f), scene.at(cx + 7f, base), 1.2f * u, StrokeCap.Round)
    drawOval(HayGold, scene.at(cx - 6.5f, base - 9f), Size(13f * u, 3.6f * u))
    drawOval(SwaddleCream, scene.at(cx - 3f, base - 10.2f), Size(7f * u, 3.4f * u))
    drawCircle(SkinTone, 1.6f * u, scene.at(cx - 3.4f, base - 9.4f))
}

/** One shepherd on the far hill, heading left toward the stable's side of town, with a lantern. */
private fun DrawScope.drawShepherdWalker(
    scene: SceneScale,
    walker: ShepherdWalk.Walker,
    t: Float,
    color: Color,
    isNight: Boolean,
) {
    val x = HERO_DESIGN_WIDTH + SHEPHERD_MARGIN - walker.progress * (HERO_DESIGN_WIDTH + 2 * SHEPHERD_MARGIN)
    val base = farHill(x) + 1f
    val u = scene.unit
    val cloth = ShepherdCloths[walker.variant]
    val step = sin(t * 7f) * 2.2f
    // Legs (walking toward -x), then robe, head cloth and face.
    drawLine(color, scene.at(x - 1.5f, base - 6f), scene.at(x - 1.5f + step, base), 1.6f * u, StrokeCap.Round)
    drawLine(color, scene.at(x + 1.5f, base - 6f), scene.at(x + 1.5f - step, base), 1.6f * u, StrokeCap.Round)
    val robe = Path().apply {
        moveTo(scene.x(x - 3.4f), scene.y(base - 4f))
        lineTo(scene.x(x - 1.6f), scene.y(base - 15f))
        lineTo(scene.x(x + 1.6f), scene.y(base - 15f))
        lineTo(scene.x(x + 3.4f), scene.y(base - 4f))
        close()
    }
    drawPath(robe, color)
    drawCircle(color, 2.1f * u, scene.at(x, base - 17f))
    drawRect(cloth, scene.at(x - 2.4f, base - 19.4f), Size(4.8f * u, 1.5f * u))
    // Staff held in front.
    drawLine(color, scene.at(x - 5.2f, base), scene.at(x - 5.2f, base - 20f), 1f * u, StrokeCap.Round)
    drawLine(color, scene.at(x - 5.2f, base - 20f), scene.at(x - 3.6f, base - 21.2f), 1f * u, StrokeCap.Round)
    // Lantern swinging from the other hand.
    val sway = sin(t * 3.5f + walker.index) * 1.2f
    val lamp = scene.at(x + 5.2f + sway, base - 9f)
    drawLine(color, scene.at(x + 2f, base - 12f), lamp, 0.8f * u)
    val flicker = 0.8f + 0.2f * sin(t * 9f + walker.index * 2f)
    drawCircle(WarmWindow.copy(alpha = (if (isNight) 0.3f else 0.12f) * flicker), 6f * u, lamp)
    drawCircle(WarmWindow.copy(alpha = flicker), 1.3f * u, lamp)
    when (walker.variant) {
        1 -> drawOval(SheepWool, scene.at(x - 3.6f, base - 17f), Size(6.4f * u, 3.2f * u)) // a lamb on the shoulders
        else -> drawSheep(scene, x + SHEEP_FOLLOW_GAP, farHill(x + SHEEP_FOLLOW_GAP) + 1f, t, color)
    }
}

private fun DrawScope.drawSheep(scene: SceneScale, x: Float, base: Float, t: Float, dark: Color) {
    val u = scene.unit
    val step = sin(t * 7f + 1.5f) * 1.2f
    drawLine(dark, scene.at(x - 2f, base - 3f), scene.at(x - 2f + step, base), 1.1f * u, StrokeCap.Round)
    drawLine(dark, scene.at(x + 2f, base - 3f), scene.at(x + 2f - step, base), 1.1f * u, StrokeCap.Round)
    drawOval(SheepWool, scene.at(x - 4.6f, base - 8.6f), Size(9.2f * u, 6f * u))
    drawCircle(dark, 1.6f * u, scene.at(x - 5.6f, base - 6.6f))
}

private const val SHEPHERD_MARGIN = 26f
private const val SHEEP_FOLLOW_GAP = 15f
private val SheepWool = Color(0xFFE6E0D2)
private val ShepherdCloths = listOf(Color(0xFFC9553F), Color(0xFF5E8F6B), Color(0xFFD9A441))
