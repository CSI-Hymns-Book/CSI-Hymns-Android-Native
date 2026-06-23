package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.ui.viewmodels.SettingsViewModel
import com.reyzie.hymns.ui.widgets.rememberChristmasScreenColors
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlin.math.min

@Composable
fun ChristmasLandingScreen(
    onOpenHymns: () -> Unit,
    onOpenKeerthanes: () -> Unit,
    onOpenCarols: () -> Unit,
    onMenuClick: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val isChristmasMode by settingsViewModel.isChristmasMode.collectAsState()

    LaunchedEffect(isChristmasMode) {
        if (isChristmasMode) {
            settingsViewModel.refreshAppConfig()
        }
    }

    val christmasColors = rememberChristmasScreenColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = christmasColors.gradient),
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            val compact = maxHeight < 640.dp
            val heroIconSize: Dp = if (compact) 56.dp else (maxWidth * 0.18f).coerceAtMost(72.dp)
            val heroEmojiSize: TextUnit = if (compact) 30.sp else min(maxWidth.value * 0.1f, 38f).sp
            val cardHeight: Dp = if (compact) 96.dp else (maxHeight * 0.14f).coerceAtMost(108.dp)
            val cardEmojiSize: TextUnit = if (compact) 24.sp else 28.sp
            val topPad = if (compact) 0.dp else 4.dp
            val heroBottomPad = if (compact) 10.dp else 16.dp
            val cardGap = if (compact) 8.dp else 10.dp
            val bottomPad = if (compact) 96.dp else 112.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = if (compact) 6.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                onMenuClick()
                            },
                            modifier = Modifier.size(if (compact) 44.dp else 48.dp),
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = christmasColors.onBackground)
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = heroBottomPad),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFB22222).copy(alpha = 0.28f),
                            modifier = Modifier.size(heroIconSize),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🎄", fontSize = heroEmojiSize)
                            }
                        }
                        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                        Text(
                            "Merry Christmas!",
                            color = christmasColors.onBackground,
                            style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
                        Text(
                            "Glory to God in the highest",
                            color = christmasColors.onBackgroundMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                        )
                        if (!compact) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "✦  🔔  ⭐  🕯️  ✦",
                                color = Color(0xFFFFD700).copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 3.sp,
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Choose a collection",
                        color = christmasColors.onBackgroundMuted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = if (compact) 8.dp else 10.dp, start = 4.dp),
                    )
                }

                item {
                    ChristmasCategoryCard(
                        title = "Hymns",
                        subtitle = "Traditional hymns from the CSI hymn book",
                        emoji = "🎵",
                        gradient = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
                        height = cardHeight,
                        emojiSize = cardEmojiSize,
                        onTap = onOpenHymns,
                    )
                }
                item { Spacer(Modifier.height(cardGap)) }
                item {
                    ChristmasCategoryCard(
                        title = "Keerthane",
                        subtitle = "Kannada devotional songs and lyrics",
                        emoji = "🎶",
                        gradient = listOf(Color(0xFF1976D2), Color(0xFF0D47A1)),
                        height = cardHeight,
                        emojiSize = cardEmojiSize,
                        onTap = onOpenKeerthanes,
                    )
                }
                item { Spacer(Modifier.height(cardGap)) }
                item {
                    ChristmasCategoryCard(
                        title = "Community Carols",
                        subtitle = "Churches, lyrics & sheet-music PDFs",
                        emoji = "🎄",
                        gradient = listOf(Color(0xFFC62828), Color(0xFF8E0000)),
                        height = cardHeight,
                        emojiSize = cardEmojiSize,
                        highlighted = true,
                        onTap = onOpenCarols,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChristmasCategoryCard(
    title: String,
    subtitle: String,
    emoji: String,
    gradient: List<Color>,
    onTap: () -> Unit,
    height: Dp = 112.dp,
    emojiSize: TextUnit = 28.sp,
    highlighted: Boolean = false,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable {
                HapticFeedbackManager.smoothClick(context)
                onTap()
            },
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 10.dp else 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.size(if (height <= 96.dp) 48.dp else 56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = emojiSize)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
