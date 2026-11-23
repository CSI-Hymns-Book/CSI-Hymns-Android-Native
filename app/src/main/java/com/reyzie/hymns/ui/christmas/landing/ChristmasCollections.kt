package com.reyzie.hymns.ui.christmas.landing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.ChristmasPalette

/** What happens when each collection card is tapped. */
data class ChristmasCollectionActions(
    val onOpenHymns: () -> Unit,
    val onOpenKeerthanes: () -> Unit,
    val onOpenCarols: () -> Unit,
    val onOpenMtHymns: () -> Unit,
)

private val BigCardShape = RoundedCornerShape(24.dp)
private val CarolsShape = RoundedCornerShape(22.dp)
private val RowShape = RoundedCornerShape(18.dp)
private const val SHEEN_PERIOD_MILLIS = 7_000
private const val SHEEN_ACTIVE_FRACTION = 0.2f

/**
 * The collection cards: Hymns and Keerthane as the two big cards (most used),
 * Community Carols as a slimmer feature card, and M.T. Hymns as a small row.
 * [firstIndex] is where these cards start in the screen's entrance order.
 */
@Composable
fun ChristmasCollections(
    palette: ChristmasPalette,
    animate: Boolean,
    capsEnabled: Boolean,
    actions: ChristmasCollectionActions,
    firstIndex: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigCollectionCard(
                palette = palette,
                title = "Hymns",
                subtitle = "CSI hymn book",
                footer = "Kannada Sangeetha",
                tintBackground = palette.pineBackground,
                index = firstIndex,
                animate = animate,
                capsEnabled = capsEnabled,
                onClick = actions.onOpenHymns,
                modifier = Modifier.weight(1f),
            ) { HymnBookHollyIcon(palette.pine, palette.hollyLeaf, 31.dp, animate) }
            BigCollectionCard(
                palette = palette,
                title = "Keerthane",
                subtitle = "ಕೀರ್ತನೆಗಳು",
                footer = "Kannada Sankeerthane",
                tintBackground = palette.iceBackground,
                index = firstIndex + 1,
                animate = animate,
                capsEnabled = capsEnabled,
                onClick = actions.onOpenKeerthanes,
                modifier = Modifier.weight(1f),
            ) { StarLanternIcon(palette.ice, 34.dp, animate) }
        }
        CarolsCard(palette, firstIndex + 2, animate, capsEnabled, actions.onOpenCarols)
        MtHymnsRow(palette, firstIndex + 3, animate, capsEnabled, actions.onOpenMtHymns)
    }
}

@Composable
private fun BigCollectionCard(
    palette: ChristmasPalette,
    title: String,
    subtitle: String,
    footer: String,
    tintBackground: Color,
    index: Int,
    animate: Boolean,
    capsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .christmasCard(
                palette = palette,
                shape = BigCardShape,
                cornerDp = 24f,
                index = index,
                animate = animate,
                capsEnabled = capsEnabled,
                tint = Brush.verticalGradient(0f to tintBackground, 0.75f to Color.Transparent),
                contentLabel = title,
                onClick = onClick,
            )
            .heightIn(min = 140.dp)
            .padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(tintBackground, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.weight(1f).heightIn(min = 8.dp))
        Text(title, color = palette.ink, fontFamily = ChristmasDisplayFont, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, color = palette.inkMuted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                footer,
                color = palette.gold,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                // Shrinks on narrow phones so both big cards keep the same height.
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = palette.gold, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CarolsCard(palette: ChristmasPalette, index: Int, animate: Boolean, capsEnabled: Boolean, onClick: () -> Unit) {
    val sheen = if (animate) {
        rememberInfiniteTransition(label = "carolsSheen").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(SHEEN_PERIOD_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "carolsSheenProgress",
        ).value
    } else {
        1f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .christmasCard(
                palette = palette,
                shape = CarolsShape,
                cornerDp = 22f,
                index = index,
                animate = animate,
                capsEnabled = capsEnabled,
                tint = Brush.linearGradient(listOf(palette.featureStart, palette.featureEnd)),
                contentLabel = "Community Carols",
                onClick = onClick,
            )
            .drawWithContent {
                drawContent()
                val sweep = sheen / SHEEN_ACTIVE_FRACTION
                if (!animate || sweep > 1f) return@drawWithContent
                val center = -size.width * 0.3f + sweep * size.width * 1.6f
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color(0xFFFFE9B9).copy(alpha = 0.2f), Color.Transparent),
                        start = Offset(center - size.width * 0.15f, 0f),
                        end = Offset(center + size.width * 0.15f, size.height),
                    ),
                )
            }
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(palette.holly.copy(alpha = 0.18f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { ChristmasTreeIcon(palette.hollyLeaf, 32.dp, animate) }
        Column(Modifier.weight(1f)) {
            Text("THIS SEASON", color = palette.gold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text(
                "Community Carols",
                color = palette.ink,
                fontFamily = ChristmasDisplayFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Lyrics and sheet music from churches",
                color = palette.inkMuted,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BobbingStave(palette.gold, Modifier.size(width = 52.dp, height = 34.dp), animate)
    }
}

@Composable
private fun MtHymnsRow(palette: ChristmasPalette, index: Int, animate: Boolean, capsEnabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .christmasCard(
                palette = palette,
                shape = RowShape,
                cornerDp = 18f,
                index = index,
                animate = animate,
                capsEnabled = capsEnabled,
                tint = null,
                contentLabel = "M.T. Hymns",
                onClick = onClick,
            )
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(palette.emberBackground, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { PianoBowIcon(palette.ember, 24.dp) }
        Column(Modifier.weight(1f)) {
            Text("M.T. Hymns", color = palette.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            Text("Mangalore tunes", color = palette.inkMuted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.ink)
    }
}
