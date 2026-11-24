package com.reyzie.hymns.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.reyzie.hymns.ui.christmas.landing.SecretsChip
import com.reyzie.hymns.ui.christmas.landing.SecretsDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.ChristmasEffectsLevel
import com.reyzie.hymns.ui.christmas.ChristmasNightPalette
import com.reyzie.hymns.ui.christmas.ChristmasStatusBar
import com.reyzie.hymns.ui.christmas.ChristmasMotion
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.LocalChristmasAmbience
import com.reyzie.hymns.ui.christmas.landing.ChristmasCalendar
import com.reyzie.hymns.ui.christmas.landing.ChristmasCollectionActions
import com.reyzie.hymns.ui.christmas.landing.ChristmasCollections
import com.reyzie.hymns.ui.christmas.landing.ChristmasHeroScene
import com.reyzie.hymns.ui.christmas.landing.ChristmasMoment
import com.reyzie.hymns.ui.christmas.landing.HangingBell
import com.reyzie.hymns.ui.christmas.landing.HeroEggState
import com.reyzie.hymns.ui.christmas.landing.rememberSceneClock
import com.reyzie.hymns.ui.christmas.landing.rememberToday
import com.reyzie.hymns.ui.christmas.eggs.ChristmasSecret
import com.reyzie.hymns.ui.christmas.eggs.StarTapCounter
import com.reyzie.hymns.ui.christmas.eggs.StarTapResult
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.reyzie.hymns.ui.christmas.landing.christmasEntrance
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import com.reyzie.hymns.ui.viewmodels.SettingsViewModel
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.delay
import java.time.LocalDate

private const val HERO_MIN_DP = 300
private const val HERO_MAX_DP = 400
private const val COLLECTIONS_ESTIMATE_DP = 470
private const val CHIP_BLOCK_DP = 64
private const val CHIP_BOTTOM_GAP_DP = 14
private const val GLORIA_MILLIS = 4_200L
private const val GLORIA_INDEX = -1
private const val CHRISTMAS_DAY_DELAY_MILLIS = 600L
private const val TITLE_TEXT = "Merry\nChristmas"
private const val LIGHTS_OFF = -100f
private const val LIGHT_SPREAD = 6f
private const val LIGHTS_MILLIS = 1_700

/**
 * "Merry Christmas" with a band of gold light that travels letter by letter while
 * [lightPosition] sweeps from 0 to the end of the text.
 */
private fun litTitle(palette: ChristmasPalette, lightPosition: Float) = buildAnnotatedString {
    TITLE_TEXT.forEachIndexed { i, char ->
        val glow = (1f - abs(lightPosition - i - LIGHT_SPREAD / 2f) / (LIGHT_SPREAD / 2f)).coerceIn(0f, 1f)
        val italic = i > TITLE_TEXT.indexOf('\n')
        withStyle(
            SpanStyle(
                color = lerp(palette.ink, palette.gold, glow),
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                fontWeight = if (italic) FontWeight.Normal else FontWeight.Medium,
                shadow = if (glow > 0f) Shadow(palette.gold.copy(alpha = 0.9f * glow), blurRadius = 26f * glow) else null,
            ),
        ) { append(char) }
    }
}
private const val BOTTOM_TOOLBAR_SPACE_DP = 96

/** Short Christmas verses (KJV) that fade from one to the next under the title. */
private val ChristmasVerses = listOf(
    "Glory to God in the highest, and on earth peace, good will toward men." to "Luke 2:14",
    "For unto you is born this day in the city of David a Saviour." to "Luke 2:11",
    "For unto us a child is born, unto us a son is given." to "Isaiah 9:6",
    "When they saw the star, they rejoiced with exceeding great joy." to "Matthew 2:10",
)

/** Entrance order: top bar, greeting, title, verse, section heading, then the cards. */
private object EntranceOrder {
    const val TOP_BAR = 0
    const val GREETING = 1
    const val TITLE = 2
    const val VERSE = 3
    const val SECTION = 4
    const val FIRST_CARD = 5
}

/**
 * Christmas home screen: a live night sky over Bethlehem, a countdown, rotating verses,
 * and the song collections as snow-topped cards.
 *
 * Pass [isVisible] = false while another screen covers this one, so snow stops landing
 * on cards the user cannot see.
 */
@Composable
fun ChristmasLandingScreen(
    onOpenHymns: () -> Unit,
    onOpenKeerthanes: () -> Unit,
    onOpenCarols: () -> Unit,
    onOpenMtHymns: () -> Unit,
    onMenuClick: () -> Unit = {},
    isVisible: Boolean = true,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val isChristmasMode by settingsViewModel.isChristmasMode.collectAsState()
    LaunchedEffect(isChristmasMode) {
        if (isChristmasMode) settingsViewModel.refreshAppConfig()
    }

    val palette = rememberChristmasPalette()
    val ambience = LocalChristmasAmbience.current
    val animate = ambience?.effectiveLevel != ChristmasEffectsLevel.OFF
    val today = rememberToday()
    val moment = ChristmasCalendar.moment(today)
    val eggs = remember(moment) { HeroEggState(moment) }
    LaunchedEffect(moment) { ambience?.updateMoment(moment) }
    val actions = ChristmasCollectionActions(onOpenHymns, onOpenKeerthanes, onOpenCarols, onOpenMtHymns)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    NightSkyOverHome(isActive = isVisible, needsLightIcons = !palette.isNight)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.ground, palette.groundDeep))),
    ) {
        if (isLandscape) {
            Row(Modifier.fillMaxSize()) {
                ChristmasHero(ChristmasNightPalette, animate, today, eggs, onMenuClick, Modifier.weight(1f).fillMaxHeight())
                CollectionsSection(
                    palette = palette,
                    animate = animate,
                    capsEnabled = isVisible,
                    today = today,
                    actions = actions,
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(top = 16.dp),
                )
            }
        } else {
            PortraitHome(palette, animate, isVisible, today, eggs, actions, onMenuClick)
        }
    }
}

/**
 * The portrait home. It is built to fit the screen without scrolling, so the Christmas scene
 * stays put: the hero grows or shrinks to use the space the cards leave. Only if the screen
 * is too small for everything (tiny phone, huge font) does it fall back to scrolling, so
 * nothing is ever unreachable.
 */
@Composable
private fun PortraitHome(
    palette: ChristmasPalette,
    animate: Boolean,
    capsEnabled: Boolean,
    today: LocalDate,
    eggs: HeroEggState,
    actions: ChristmasCollectionActions,
    onMenuClick: () -> Unit,
) {
    val ambience = LocalChristmasAmbience.current
    val density = LocalDensity.current
    var showSecrets by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        var collectionsHeight by remember { mutableStateOf(COLLECTIONS_ESTIMATE_DP.dp) }
        val navBar = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
        val heroHeight = (maxHeight - collectionsHeight - CHIP_BLOCK_DP.dp - navBar).coerceIn(HERO_MIN_DP.dp, HERO_MAX_DP.dp)
        val scroll = rememberScrollState()
        Column(Modifier.fillMaxSize().verticalScroll(scroll, enabled = scroll.maxValue > 0)) {
            ChristmasHero(ChristmasNightPalette, animate, today, eggs, onMenuClick, Modifier.fillMaxWidth().height(heroHeight))
            CollectionsSection(
                palette = palette,
                animate = animate,
                capsEnabled = capsEnabled,
                today = today,
                actions = actions,
                bottomSpace = 0.dp,
                useNavigationInset = false,
                modifier = Modifier.onSizeChanged { collectionsHeight = with(density) { it.height.toDp() } },
            )
            Spacer(Modifier.height(CHIP_BLOCK_DP.dp + navBar))
        }
        if (ambience?.eggsActive == true) {
            val found = ambience.foundSecrets
            SecretsChip(
                palette = palette,
                found = found,
                onClick = { showSecrets = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = CHIP_BOTTOM_GAP_DP.dp)
                    .christmasEntrance(EntranceOrder.FIRST_CARD + 4, animate),
            )
            if (showSecrets) {
                SecretsDialog(
                    palette = palette,
                    found = found,
                    onDismiss = { showSecrets = false },
                    onReset = { ambience.resetSecrets() },
                )
            }
        }
    }
}

@Composable
private fun ChristmasHero(
    palette: ChristmasPalette,
    animate: Boolean,
    today: LocalDate,
    eggs: HeroEggState,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ambience = LocalChristmasAmbience.current
    val eggsActive = ambience?.eggsActive == true
    val clock = rememberSceneClock(animate)
    val starCounter = remember { StarTapCounter() }
    var gloria by remember { mutableStateOf(false) }
    var bellSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(gloria) {
        if (gloria) {
            delay(GLORIA_MILLIS)
            gloria = false
        }
    }
    LaunchedEffect(Unit) {
        if (eggs.moment == ChristmasMoment.CHRISTMAS_DAY && ambience != null) {
            delay(CHRISTMAS_DAY_DELAY_MILLIS)
            ambience.celebrateChristmasDay()
            bellSignal += 1
        }
    }

    Box(modifier) {
        ChristmasHeroScene(
            palette = palette,
            clock = clock,
            eggs = eggs,
            tapsEnabled = eggsActive,
            onStarTap = { at ->
                eggs.starTappedAt = clock.value
                ambience?.effects?.sparkle(at.x, at.y, count = 6)
                when (starCounter.tap(clock.value)) {
                    StarTapResult.HINT -> ambience?.showToast("The star is getting brighter…")
                    StarTapResult.SHOOTING_STAR -> {
                        eggs.shootingStarAt = clock.value
                        gloria = true
                        HapticFeedbackManager.success(context)
                        ambience?.discover(ChristmasSecret.STAR)
                    }
                    StarTapResult.TAP -> HapticFeedbackManager.lightClick(context)
                }
            },
            onSnowTap = { at ->
                ambience?.effects?.snowPuff(at.x, at.y)
                HapticFeedbackManager.lightClick(context)
                if (eggs.buildSnowman(clock.value)) ambience?.discover(ChristmasSecret.SNOWMAN)
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (eggsActive) {
            HangingBell(
                ringSignal = bellSignal,
                onRing = { at ->
                    ambience.effects.rings(at.x, at.y)
                    HapticFeedbackManager.jingle(context)
                    ambience.discover(ChristmasSecret.BELL)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 22.dp),
            )
        }
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp),
        ) {
            HeroTopBar(palette, animate, ChristmasCalendar.chipLabel(today), onMenuClick)
            Spacer(Modifier.height(18.dp))
            TitleBlock(
                palette = palette,
                animate = animate,
                gloria = gloria,
                onLongPress = if (eggsActive) {
                    {
                        HapticFeedbackManager.mediumClick(context)
                        ambience.discover(ChristmasSecret.LIGHTS)
                    }
                } else {
                    null
                },
                modifier = Modifier.padding(start = 8.dp).width(250.dp),
            )
        }
    }
}

@Composable
private fun HeroTopBar(palette: ChristmasPalette, animate: Boolean, countdown: String, onMenuClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.christmasEntrance(EntranceOrder.TOP_BAR, animate),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                HapticFeedbackManager.smoothClick(context)
                onMenuClick()
            },
            modifier = Modifier
                .size(42.dp)
                .background(palette.glass, CircleShape)
                .border(1.dp, palette.edge, CircleShape),
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = palette.ink, modifier = Modifier.size(20.dp))
        }
        CountdownChip(palette, animate, countdown)
    }
}

@Composable
private fun CountdownChip(palette: ChristmasPalette, animate: Boolean, text: String) {
    val pulse = if (animate) {
        rememberInfiniteTransition(label = "countdownPulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2_400), RepeatMode.Restart),
            label = "countdownPulseProgress",
        ).value
    } else {
        1f
    }
    Row(
        modifier = Modifier
            .height(32.dp)
            .background(palette.glass, CircleShape)
            .border(1.dp, palette.edge, CircleShape)
            .padding(start = 11.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .drawBehind {
                    if (animate && pulse < 0.7f) {
                        val p = pulse / 0.7f
                        drawCircle(palette.holly.copy(alpha = 0.55f * (1f - p)), radius = size.minDimension / 2f + p * 8.dp.toPx())
                    }
                    drawCircle(palette.holly)
                },
        )
        Text(text, color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun TitleBlock(
    palette: ChristmasPalette,
    animate: Boolean,
    gloria: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val lights = remember { Animatable(LIGHTS_OFF) }
    val currentLongPress by rememberUpdatedState(onLongPress)
    var verseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(animate) {
        while (animate) {
            delay(ChristmasMotion.VERSE_ROTATE_MILLIS)
            verseIndex = (verseIndex + 1) % ChristmasVerses.size
        }
    }
    Column(modifier) {
        Text(
            "ಕ್ರಿಸ್ಮಸ್ ಹಬ್ಬದ ಶುಭಾಶಯಗಳು",
            color = palette.gold,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.christmasEntrance(EntranceOrder.GREETING, animate),
        )
        Text(
            text = litTitle(palette, lights.value),
            color = palette.ink,
            fontFamily = ChristmasDisplayFont,
            fontWeight = FontWeight.Medium,
            fontSize = 46.sp,
            lineHeight = 46.sp,
            modifier = Modifier
                .padding(top = 2.dp)
                .christmasEntrance(EntranceOrder.TITLE, animate)
                .semantics { contentDescription = "Merry Christmas" }
                .pointerInput(onLongPress != null) {
                    if (onLongPress == null) return@pointerInput
                    detectTapGestures(
                        onLongPress = {
                            currentLongPress?.invoke()
                            scope.launch {
                                lights.snapTo(0f)
                                lights.animateTo(TITLE_TEXT.length + LIGHT_SPREAD, tween(LIGHTS_MILLIS, easing = LinearEasing))
                                lights.snapTo(LIGHTS_OFF)
                            }
                        },
                    )
                },
        )
        AnimatedContent(
            targetState = if (gloria) GLORIA_INDEX else verseIndex,
            transitionSpec = {
                (fadeIn(tween(ChristmasMotion.CROSSFADE_MILLIS)) + slideInVertically(tween(ChristmasMotion.CROSSFADE_MILLIS)) { it / 8 })
                    .togetherWith(fadeOut(tween(ChristmasMotion.CROSSFADE_MILLIS)))
            },
            label = "christmasVerse",
            modifier = Modifier
                .padding(top = 8.dp)
                .christmasEntrance(EntranceOrder.VERSE, animate),
        ) { index ->
            if (index == GLORIA_INDEX) {
                Text(
                    "Gloria in excelsis Deo",
                    color = palette.gold,
                    fontFamily = ChristmasDisplayFont,
                    fontStyle = FontStyle.Italic,
                    fontSize = 21.sp,
                    style = TextStyle(shadow = Shadow(palette.gold.copy(alpha = 0.5f), blurRadius = 24f)),
                    modifier = Modifier.heightIn(min = 57.dp),
                )
                return@AnimatedContent
            }
            val (verse, reference) = ChristmasVerses[index]
            Text(
                text = buildAnnotatedString {
                    append(verse)
                    append("  ")
                    withStyle(SpanStyle(color = palette.gold, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)) {
                        append(reference)
                    }
                },
                color = palette.inkMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.heightIn(min = 57.dp),
            )
        }
    }
}

@Composable
private fun CollectionsSection(
    palette: ChristmasPalette,
    animate: Boolean,
    capsEnabled: Boolean,
    today: LocalDate,
    actions: ChristmasCollectionActions,
    modifier: Modifier = Modifier,
    bottomSpace: Dp = BOTTOM_TOOLBAR_SPACE_DP.dp,
    useNavigationInset: Boolean = true,
) {
    Column(
        modifier
            .then(if (useNavigationInset) Modifier.navigationBarsPadding() else Modifier)
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottomSpace),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .christmasEntrance(EntranceOrder.SECTION, animate),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "COLLECTIONS",
                color = palette.inkMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                modifier = Modifier.weight(1f),
            )
            Text(ChristmasCalendar.adventLabel(today), color = palette.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
        ChristmasCollections(
            palette = palette,
            animate = animate,
            capsEnabled = capsEnabled,
            actions = actions,
            firstIndex = EntranceOrder.FIRST_CARD,
        )
    }
}

/**
 * The home's sky is always a dark night, even when the phone is in light mode. While the
 * home is showing in light mode this switches the status-bar icons to white so the clock
 * and battery stay readable on the dark sky, and tells the snowfall layer (which draws
 * Santa) that a dark sky is behind it. Both are put back when the home is covered or left.
 */
@Composable
private fun NightSkyOverHome(isActive: Boolean, needsLightIcons: Boolean) {
    val ambience = LocalChristmasAmbience.current
    DisposableEffect(isActive, needsLightIcons, ambience) {
        if (!isActive || !needsLightIcons) return@DisposableEffect onDispose {}
        ChristmasStatusBar.forceLightIcons = true
        ambience?.darkSkyBehind = true
        onDispose {
            ChristmasStatusBar.forceLightIcons = false
            ambience?.darkSkyBehind = false
        }
    }
}
