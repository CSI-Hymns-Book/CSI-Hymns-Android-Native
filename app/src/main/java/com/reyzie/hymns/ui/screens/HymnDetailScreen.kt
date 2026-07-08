package com.reyzie.hymns.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.data.ReadingProgressService
import com.reyzie.hymns.ui.viewmodels.*
import com.reyzie.hymns.cast.SongCastRequest
import com.reyzie.hymns.ui.motion.expressiveAudioPlayerEnter
import com.reyzie.hymns.ui.motion.expressiveAudioPlayerExit
import com.reyzie.hymns.ui.widgets.CastControlSheet
import com.reyzie.hymns.ui.widgets.GroupButtonVariant
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import com.reyzie.hymns.ui.widgets.PageFlipLyrics
import com.reyzie.hymns.ui.widgets.SongInfoBanner
import com.reyzie.hymns.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HymnDetailScreen(
    hymn: Hymn,
    isKeerthane: Boolean = false,
    favoritesViewModel: FavoritesViewModel = viewModel(),
    recentSongsViewModel: RecentSongsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    audioViewModel: AudioViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val readingProgressService = remember { ReadingProgressService(context) }
    var selectedLanguage by remember { mutableStateOf("Kannada") }
    var fontSize by remember { mutableStateOf(18.sp) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val rightColumnScrollState = rememberScrollState()
    
    val favoriteHymns by favoritesViewModel.favoriteHymnIds.collectAsState()
    val favoriteKeerthanes by favoritesViewModel.favoriteKeerthaneIds.collectAsState()
    val isFavorite = if (isKeerthane) favoriteKeerthanes.contains(hymn.number) else favoriteHymns.contains(hymn.number)
    
    val isPageFlipEnabled by settingsViewModel.isPageFlipEnabled.collectAsState()
    val audioState by audioViewModel.audioState.collectAsState()
    val targetAudioUrl = hymn.audioUrl ?: if (isKeerthane) {
        "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/Keerthane_${hymn.number}.ogg"
    } else {
        "https://raw.githubusercontent.com/reynold29/midi-files/main/Hymns/Hymn_${hymn.number}.ogg"
    }
    val isSameSong = audioState.currentAudioUrl == targetAudioUrl

    val remoteAppConfig by settingsViewModel.remoteAppConfig.collectAsState()
    val castEnabled = remoteAppConfig.castEnabled == true
    val isPageFlipOptionVisible = remoteAppConfig.pageFlipVisible == true
    var showCastSheet by remember { mutableStateOf(false) }
    
    var showReportDialog by remember { mutableStateOf(false) }

    if (showCastSheet && castEnabled) {
        val streamUrl = targetAudioUrl
        CastControlSheet(
            request = SongCastRequest(
                streamUrl = streamUrl,
                title = hymn.title,
                subtitle = if (isKeerthane) "Keerthane ${hymn.number}" else "Hymn ${hymn.number}"
            ),
            onDismiss = { showCastSheet = false }
        )
    }
    
    LaunchedEffect(hymn.number) {
        val progress = readingProgressService.getProgress(if (isKeerthane) "keerthane" else "hymn", hymn.number.toString()).first()
        progress.fontSize?.let { fontSize = it.sp }
        progress.language?.let { selectedLanguage = it }
        progress.scrollOffset?.let { scrollState.scrollTo(it.toInt()) }
        
        recentSongsViewModel.trackViewed(
            itemType = if (isKeerthane) "keerthane" else "hymn",
            itemId = hymn.number.toString(),
            title = hymn.title
        )
    }

    LaunchedEffect(fontSize, selectedLanguage, scrollState.value) {
        delay(1000) // Debounce
        readingProgressService.saveProgress(
            itemType = if (isKeerthane) "keerthane" else "hymn",
            itemId = hymn.number.toString(),
            fontSize = fontSize.value,
            language = selectedLanguage,
            scrollOffset = scrollState.value.toFloat()
        )
    }



    val showAudioPlayer = audioState.isVisible &&
        audioState.currentSongNumber == hymn.number &&
        audioState.isKeerthane == isKeerthane

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SongInfoBanner(
                songTypeLabel = if (isKeerthane) "Keerthane" else "Hymn",
                number = hymn.number,
                title = hymn.title,
                subtitle = if (isKeerthane) null else hymn.signature.takeIf { it.isNotBlank() },
                hint = null,
                onBackClick = {
                    HapticFeedbackManager.smoothClick(context)
                    audioViewModel.stopAndReset()
                    onBackClick()
                }
            )

            val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val isDarkTheme = isSystemInDarkTheme()
            val controlsCardColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceBright
            }

            if (isLandscape) {
                val lyricsText = if (selectedLanguage == "English" || hymn.kannadaLyrics.isNullOrEmpty()) {
                    hymn.lyrics
                } else {
                    hymn.kannadaLyrics!!
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    ) {
                        if (isPageFlipEnabled && isPageFlipOptionVisible) {
                            PageFlipLyrics(
                                lyrics = lyricsText,
                                fontSize = fontSize,
                                isKeerthane = isKeerthane,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = lyricsText,
                                    fontSize = fontSize,
                                    lineHeight = fontSize * 1.6,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .verticalScroll(rightColumnScrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = controlsCardColor
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Font Size",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                StandardButtonGroup(
                                    buttonCount = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    spacing = 6.dp,
                                    highContrast = true
                                ) {
                                    Button(
                                        index = 0,
                                        onClick = {
                                            HapticFeedbackManager.smoothClick(context)
                                            fontSize = (fontSize.value - 2f).coerceAtLeast(14f).sp
                                        },
                                        icon = Icons.Default.Remove,
                                        label = "Smaller",
                                        showLabelWhenUnselected = false,
                                        variant = GroupButtonVariant.Tonal
                                    )
                                    Button(
                                        index = 1,
                                        onClick = {},
                                        label = "${fontSize.value.toInt()}",
                                        forceShowLabel = true,
                                        roundedSquare = true,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        autoSizeLabel = true,
                                        variant = GroupButtonVariant.Tonal
                                    )
                                    Button(
                                        index = 2,
                                        onClick = {
                                            HapticFeedbackManager.smoothClick(context)
                                            fontSize = (fontSize.value + 2f).coerceAtMost(44f).sp
                                        },
                                        icon = Icons.Default.Add,
                                        label = "Bigger",
                                        showLabelWhenUnselected = false,
                                        variant = GroupButtonVariant.Tonal
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    "Lyrics Language",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                StandardButtonGroup(
                                    buttonCount = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    spacing = 6.dp,
                                    highContrast = true
                                ) {
                                    Button(
                                        index = 0,
                                        onClick = {
                                            HapticFeedbackManager.smoothClick(context)
                                            selectedLanguage = "Kannada"
                                        },
                                        label = "ಕನ್ನಡ",
                                        isSelected = selectedLanguage == "Kannada"
                                    )
                                    Button(
                                        index = 1,
                                        onClick = {
                                            HapticFeedbackManager.smoothClick(context)
                                            selectedLanguage = "English"
                                        },
                                        label = "English",
                                        isSelected = selectedLanguage == "English"
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    "Actions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StandardButtonGroup(
                                        buttonCount = 2,
                                        modifier = Modifier.fillMaxWidth(),
                                        spacing = 6.dp,
                                        highContrast = true
                                    ) {
                                        Button(
                                            index = 0,
                                            onClick = {
                                                HapticFeedbackManager.smoothClick(context)
                                                if (isKeerthane) favoritesViewModel.toggleFavoriteKeerthane(hymn.number)
                                                else favoritesViewModel.toggleFavoriteHymn(hymn.number)
                                            },
                                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            label = if (isFavorite) "Saved" else "Save",
                                            isSelected = isFavorite,
                                            variant = GroupButtonVariant.Filled
                                        )
                                        Button(
                                            index = 1,
                                            onClick = {
                                                HapticFeedbackManager.smoothClick(context)
                                                if (!audioState.isVisible || !isSameSong) {
                                                    audioViewModel.playSong(hymn.number, hymn.title, isKeerthane, hymn.audioUrl)
                                                } else {
                                                    audioViewModel.toggleVisibility()
                                                }
                                                scope.launch {
                                                    delay(100)
                                                    rightColumnScrollState.animateScrollTo(rightColumnScrollState.maxValue)
                                                }
                                            },
                                            icon = if (audioState.isVisible && isSameSong) Icons.Default.KeyboardArrowDown else Icons.Default.MusicNote,
                                            label = if (audioState.isVisible && isSameSong) "Hide" else "Audio",
                                            isSelected = audioState.isVisible && isSameSong,
                                            variant = GroupButtonVariant.Tonal
                                        )
                                    }
                                    if (castEnabled) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            StandardButtonGroup(
                                                buttonCount = 1,
                                                modifier = Modifier.weight(1f),
                                                spacing = 6.dp,
                                                highContrast = true
                                            ) {
                                                Button(
                                                    index = 0,
                                                    onClick = {
                                                        HapticFeedbackManager.smoothClick(context)
                                                        showCastSheet = true
                                                    },
                                                    icon = Icons.Default.Cast,
                                                    label = "Cast",
                                                    alwaysCircle = true,
                                                    variant = GroupButtonVariant.Accent
                                                )
                                            }
                                            StandardButtonGroup(
                                                buttonCount = 1,
                                                modifier = Modifier.weight(1f),
                                                spacing = 6.dp,
                                                highContrast = true
                                            ) {
                                                Button(
                                                    index = 0,
                                                    onClick = {
                                                        HapticFeedbackManager.smoothClick(context)
                                                        showReportDialog = true
                                                    },
                                                    icon = Icons.Default.BugReport,
                                                    label = "Report",
                                                    alwaysCircle = true,
                                                    variant = GroupButtonVariant.Accent
                                                )
                                            }
                                        }
                                    } else {
                                        StandardButtonGroup(
                                            buttonCount = 1,
                                            modifier = Modifier.fillMaxWidth(),
                                            spacing = 6.dp,
                                            highContrast = true
                                        ) {
                                            Button(
                                                index = 0,
                                                onClick = {
                                                    HapticFeedbackManager.smoothClick(context)
                                                    showReportDialog = true
                                                },
                                                icon = Icons.Default.BugReport,
                                                label = "Report",
                                                alwaysCircle = true,
                                                variant = GroupButtonVariant.Accent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showAudioPlayer,
                            enter = expressiveAudioPlayerEnter(),
                            exit = expressiveAudioPlayerExit()
                        ) {
                            ExpressiveAudioPlayer(
                                audioState = audioState,
                                audioViewModel = audioViewModel
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = controlsCardColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StandardButtonGroup(
                                buttonCount = 3,
                                modifier = Modifier.weight(0.85f),
                                spacing = 6.dp,
                                highContrast = true
                            ) {
                                Button(
                                    index = 0,
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        fontSize = (fontSize.value - 2f).coerceAtLeast(14f).sp
                                    },
                                    icon = Icons.Default.Remove,
                                    label = "Smaller",
                                    showLabelWhenUnselected = false,
                                    variant = GroupButtonVariant.Tonal
                                )
                                Button(
                                    index = 1,
                                    onClick = {},
                                    label = "${fontSize.value.toInt()}",
                                    forceShowLabel = true,
                                    roundedSquare = true,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    autoSizeLabel = true,
                                    variant = GroupButtonVariant.Tonal
                                )
                                Button(
                                    index = 2,
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        fontSize = (fontSize.value + 2f).coerceAtMost(44f).sp
                                    },
                                    icon = Icons.Default.Add,
                                    label = "Bigger",
                                    showLabelWhenUnselected = false,
                                    variant = GroupButtonVariant.Tonal
                                )
                            }

                            StandardButtonGroup(
                                buttonCount = 2,
                                modifier = Modifier.weight(1f),
                                spacing = 6.dp,
                                highContrast = true
                            ) {
                                Button(
                                    index = 0,
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        selectedLanguage = "Kannada"
                                    },
                                    label = "ಕನ್ನಡ",
                                    isSelected = selectedLanguage == "Kannada"
                                )
                                Button(
                                    index = 1,
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        selectedLanguage = "English"
                                    },
                                    label = "English",
                                    isSelected = selectedLanguage == "English"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        StandardButtonGroup(
                            buttonCount = if (castEnabled) 4 else 3,
                            modifier = Modifier.fillMaxWidth(),
                            spacing = 6.dp,
                            highContrast = true
                        ) {
                            Button(
                                index = 0,
                                onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                    if (isKeerthane) favoritesViewModel.toggleFavoriteKeerthane(hymn.number)
                                    else favoritesViewModel.toggleFavoriteHymn(hymn.number)
                                },
                                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = if (isFavorite) "Saved" else "Save",
                                isSelected = isFavorite,
                                variant = GroupButtonVariant.Filled
                            )
                            Button(
                                index = 1,
                                onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                     if (!audioState.isVisible || !isSameSong) {
                                         audioViewModel.playSong(hymn.number, hymn.title, isKeerthane, hymn.audioUrl)
                                     } else {
                                        audioViewModel.toggleVisibility()
                                    }
                                },
                                icon = if (audioState.isVisible && isSameSong) Icons.Default.KeyboardArrowDown else Icons.Default.MusicNote,
                                label = if (audioState.isVisible && isSameSong) "Hide" else "Audio",
                                isSelected = audioState.isVisible && isSameSong,
                                variant = GroupButtonVariant.Tonal
                            )
                            if (castEnabled) {
                                Button(
                                    index = 2,
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        showCastSheet = true
                                    },
                                    icon = Icons.Default.Cast,
                                    label = "Cast",
                                    variant = GroupButtonVariant.Accent
                                )
                            }
                            val reportIndex = if (castEnabled) 3 else 2
                            Button(
                                index = reportIndex,
                                onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                    showReportDialog = true
                                },
                                icon = Icons.Default.BugReport,
                                label = "Report",
                                variant = GroupButtonVariant.Accent
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val lyricsText = if (selectedLanguage == "English" || hymn.kannadaLyrics.isNullOrEmpty()) {
                        hymn.lyrics
                    } else {
                        hymn.kannadaLyrics!!
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (isPageFlipEnabled && isPageFlipOptionVisible) {
                                PageFlipLyrics(
                                    lyrics = lyricsText,
                                    fontSize = fontSize,
                                    isKeerthane = isKeerthane,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = lyricsText,
                                        fontSize = fontSize,
                                        lineHeight = fontSize * 1.6,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showAudioPlayer,
                            enter = expressiveAudioPlayerEnter(),
                            exit = expressiveAudioPlayerExit()
                        ) {
                            ExpressiveAudioPlayer(
                                audioState = audioState,
                                audioViewModel = audioViewModel
                            )
                        }
                    }
                }
            }
        }
        
        if (showReportDialog) {
            ReportIssueDialog(
                songType = if (isKeerthane) "Keerthane" else "Hymn",
                songNumber = hymn.number,
                songTitle = hymn.title,
                onDismiss = { showReportDialog = false }
            )
        }
    }
}

@Composable
fun ExpressiveButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    isSelected: Boolean = false
) {
    // Morphing corner radius with standardized spec
    val cornerSize by animateDpAsState(
        targetValue = if (isSelected) 32.dp else 16.dp,
        animationSpec = MotionSpecs.DpSpec,
        label = "cornerSize"
    )
    
    val animScaleY by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = MotionSpecs.WeightSpec,
        label = "expressiveSquash"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .graphicsLayer { scaleY = animScaleY },
        shape = RoundedCornerShape(cornerSize),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(24.dp).graphicsLayer { 
                    scaleX = if (isSelected) 1.05f else 1.0f
                    scaleY = if (isSelected) 1.05f else 1.0f
                }
            )
            if (showLabel) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ExpressiveAudioPlayer(
    audioState: AudioState,
    audioViewModel: AudioViewModel
) {
    val context = LocalContext.current
    var showSpeedMenu by remember { mutableStateOf(false) }

    val formatTime: (Long) -> String = { ms ->
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%02d:%02d", mins, secs)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (audioState.isKeerthane) "Keerthane ${audioState.currentSongNumber}" else "Hymn ${audioState.currentSongNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = audioState.currentSongTitle ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        audioViewModel.toggleVisibility()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Player",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (audioState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveCircularProgress(size = 40.dp)
                }
            }

            audioState.error?.let { errorText ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Interactive Squiggly Slider
            var isScrubbing by remember { mutableStateOf(false) }
            var scrubProgress by remember { mutableFloatStateOf(0f) }
            val progress = if (audioState.duration > 0) {
                (audioState.position.toFloat() / audioState.duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val sliderProgress = if (isScrubbing) scrubProgress else progress

            com.reyzie.hymns.ui.widgets.SquigglySlider(
                value = sliderProgress,
                onValueChange = { newValue ->
                    isScrubbing = true
                    scrubProgress = newValue
                    if (audioState.duration > 0) {
                        audioViewModel.seekTo((newValue * audioState.duration).toLong())
                    }
                },
                onValueChangeFinished = {
                    isScrubbing = false
                },
                isPlaying = audioState.isPlaying,
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                waveHeight = 5.dp,
                waveLength = 22.dp,
                strokeWidth = 3.dp
            )

            // Time Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(if (isScrubbing && audioState.duration > 0) {
                        (scrubProgress * audioState.duration).toLong()
                    } else {
                        audioState.position
                    }),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(audioState.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            var controlsBarWidth by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val menuWidthDp = 200.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { controlsBarWidth = it.size.width }
            ) {
                StandardButtonGroup(
                    buttonCount = 5,
                    modifier = Modifier.fillMaxWidth(),
                    spacing = 6.dp
                ) {
                    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    val loopSelected = audioState.isLooping
                    val activeScheme = MaterialTheme.colorScheme

                    Button(
                        index = 0,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            audioViewModel.toggleLoop()
                        },
                        icon = Icons.Default.Repeat,
                        label = "Loop",
                        isSelected = if (isLandscape) false else loopSelected,
                        containerColor = if (isLandscape && loopSelected) activeScheme.secondaryContainer else null,
                        contentColor = if (isLandscape && loopSelected) activeScheme.onSecondaryContainer else null,
                        showLabelWhenUnselected = false,
                        circleWhenIdle = true,
                        iconSize = if (isLandscape) 28.dp else null,
                        variant = GroupButtonVariant.Accent
                    )
                    Button(
                        index = 1,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            val newPos = (audioState.position - 5000).coerceAtLeast(0)
                            audioViewModel.seekTo(newPos)
                        },
                        icon = Icons.Default.FastRewind,
                        label = "Rewind 5 seconds",
                        showLabelWhenUnselected = false,
                        alwaysCircle = !isLandscape,
                        compact = !isLandscape,
                        iconSize = if (isLandscape) 28.dp else 24.dp,
                        variant = GroupButtonVariant.Filled
                    )
                    Button(
                        index = 2,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            audioViewModel.togglePlayback()
                        },
                        icon = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        label = if (audioState.isPlaying) "Pause" else "Play",
                        isSelected = audioState.isPlaying,
                        showLabelWhenUnselected = false,
                        alwaysCircle = !isLandscape,
                        compact = !isLandscape,
                        compactSize = 48.dp,
                        iconSize = if (isLandscape) 28.dp else 24.dp,
                        variant = GroupButtonVariant.Filled
                    )
                    Button(
                        index = 3,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            val newPos = (audioState.position + 5000).coerceAtMost(audioState.duration)
                            audioViewModel.seekTo(newPos)
                        },
                        icon = Icons.Default.FastForward,
                        label = "Forward 5 seconds",
                        showLabelWhenUnselected = false,
                        alwaysCircle = !isLandscape,
                        compact = !isLandscape,
                        iconSize = if (isLandscape) 28.dp else 24.dp,
                        variant = GroupButtonVariant.Filled
                    )
                    Button(
                        index = 4,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            showSpeedMenu = true
                        },
                        icon = Icons.Default.Speed,
                        label = if (isLandscape) "Speed" else "${audioState.playbackSpeed}x",
                        forceShowLabel = !isLandscape,
                        showLabelWhenUnselected = !isLandscape,
                        iconSize = if (isLandscape) 28.dp else null,
                        variant = GroupButtonVariant.Accent
                    )
                }
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false },
                    offset = DpOffset(
                        x = with(density) {
                            (controlsBarWidth - menuWidthDp.toPx()).coerceAtLeast(0f).toDp()
                        },
                        y = (-10).dp
                    ),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.width(menuWidthDp)
                ) {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        val selected = speed == audioState.playbackSpeed
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (speed == 1.0f) "Normal (1.0x)" else "${speed}x",
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                audioViewModel.setPlaybackSpeed(speed)
                                showSpeedMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    Surface(
        onClick = {
            HapticFeedbackManager.smoothClick(context)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.height(36.dp).padding(horizontal = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
