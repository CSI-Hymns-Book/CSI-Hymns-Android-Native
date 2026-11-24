package com.reyzie.hymns.ui.christmas.lists

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.ChristmasMotion
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.LocalChristmasAmbience
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import com.reyzie.hymns.ui.christmas.landing.christmasEntrance
import com.reyzie.hymns.ui.christmas.snowCap
import com.reyzie.hymns.ui.widgets.ChristmasScreenBackground
import com.reyzie.hymns.utils.HapticFeedbackManager

/** Shape shared by every Christmas list row. */
const val CHRISTMAS_ROW_CORNER_DP = 22f
val ChristmasRowShape = RoundedCornerShape(CHRISTMAS_ROW_CORNER_DP.dp)

/** Shape of the search field on Christmas list screens. */
const val CHRISTMAS_FIELD_CORNER_DP = 28f
val ChristmasFieldShape = RoundedCornerShape(CHRISTMAS_FIELD_CORNER_DP.dp)

/** True while Christmas mode is on, so ordinary screens can switch to the Christmas look. */
@Composable
fun isChristmasLook(): Boolean = LocalChristmasAmbience.current != null

/** Puts the night-sky (or frosty-dawn) backdrop behind a list screen in Christmas mode. */
@Composable
fun ChristmasListBackdrop(content: @Composable () -> Unit) {
    if (isChristmasLook()) ChristmasScreenBackground { content() } else content()
}

/**
 * Frosted-glass surface with a thin gold edge and a pile of snow on top. Used for search
 * fields, group headers and rows so every Christmas list shares one look.
 */
@Composable
fun Modifier.christmasGlass(
    shape: Shape,
    cornerDp: Float,
    capped: Boolean = true,
    high: Boolean = false,
): Modifier {
    val palette = rememberChristmasPalette()
    return this
        .then(if (capped) Modifier.snowCap(cornerDp = cornerDp) else Modifier)
        .clip(shape)
        .background(if (high) palette.glassHigh else palette.glass)
        .border(1.dp, palette.edge, shape)
}

/**
 * One song in a Christmas list: a tinted badge with a Christmas icon, a gold label
 * ("Hymn 12"), the title in the display face, an optional subtitle, and a trailing slot.
 * Presses with a soft spring and a gold edge, like the landing cards.
 */
@Composable
fun ChristmasSongRow(
    label: String,
    title: String,
    subtitle: String?,
    badgeBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.(ChristmasPalette) -> Unit = { palette -> DefaultChevron(palette) },
    badge: @Composable () -> Unit,
) {
    val palette = rememberChristmasPalette()
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) ChristmasMotion.PRESSED_SCALE else 1f,
        animationSpec = ChristmasMotion.pressSpring(),
        label = "christmasRowPress",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .christmasEntrance(index = 0, animate = isChristmasLook())
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .snowCap(cornerDp = CHRISTMAS_ROW_CORNER_DP)
            .clip(ChristmasRowShape)
            .background(palette.glass)
            .border(1.dp, if (pressed) palette.gold else palette.edge, ChristmasRowShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = palette.gold),
                role = Role.Button,
                onClickLabel = "Open $title",
            ) {
                HapticFeedbackManager.smoothClick(context)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ChristmasBadge(badgeBackground, BADGE_SIZE) { badge() }
        Column(Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                color = palette.gold,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Text(
                title,
                color = palette.ink,
                fontFamily = ChristmasDisplayFont,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    color = palette.inkMuted,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing(palette)
    }
}

/** A rounded, tinted square holding a small Christmas icon. */
@Composable
fun ChristmasBadge(background: Color, size: Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .background(background, RoundedCornerShape(size * BADGE_CORNER_FRACTION)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DefaultChevron(palette: ChristmasPalette) {
    Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        tint = palette.gold.copy(alpha = 0.8f),
        modifier = Modifier.size(22.dp),
    )
}

private val BADGE_SIZE = 48.dp
private const val BADGE_CORNER_FRACTION = 0.3f
