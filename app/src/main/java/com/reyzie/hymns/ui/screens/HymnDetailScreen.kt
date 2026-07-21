package com.reyzie.hymns.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
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
import com.reyzie.hymns.data.RemoteAppConfig
import com.reyzie.hymns.data.AppSection
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
import android.widget.Toast
import com.reyzie.hymns.data.JiraService
import com.reyzie.hymns.data.TicketsRepository
import androidx.activity.compose.rememberLauncherForActivityResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HymnDetailScreen(
    hymn: Hymn,
    isKeerthane: Boolean = false,
    favoritesViewModel: FavoritesViewModel = viewModel(),
    recentSongsViewModel: RecentSongsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    audioViewModel: AudioViewModel = viewModel(),
    isMt: Boolean = false,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val readingProgressService = remember { ReadingProgressService(context) }
    var selectedLanguage by remember { mutableStateOf("Kannada") }
    val prefs = remember { context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) }
    var fontSize by remember { mutableStateOf(prefs.getInt("global_songs_font_size", 18).sp) }
    var isControlsExpanded by remember { mutableStateOf(prefs.getBoolean("detail_controls_expanded", true)) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val rightColumnScrollState = rememberScrollState()
    
    val favoriteHymns by favoritesViewModel.favoriteHymnIds.collectAsState()
    val favoriteKeerthanes by favoritesViewModel.favoriteKeerthaneIds.collectAsState()
    val isFavorite = if (isKeerthane) favoriteKeerthanes.contains(hymn.number) else favoriteHymns.contains(hymn.number)
    
    val isPageFlipEnabled by settingsViewModel.isPageFlipEnabled.collectAsState()
    val audioState by audioViewModel.audioState.collectAsState()
    
    val remoteAppConfig by settingsViewModel.remoteAppConfig.collectAsState()
    val castEnabled = remoteAppConfig.castEnabled == true
    val isPageFlipOptionVisible = remoteAppConfig.pageFlipVisible == true
    
    val repository = remember { com.reyzie.hymns.data.HymnsRepository(context) }
    var csiHymnsMap by remember { mutableStateOf<Map<Int, Hymn>>(emptyMap()) }
    var midiFilesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isMidiFilesLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        launch {
            try {
                val list = repository.loadHymns(AppSection.CSI)
                csiHymnsMap = list.associateBy { it.number }
            } catch (e: Exception) {
                android.util.Log.e("HymnDetailScreen", "Failed to load CSI hymns for options signature lookup", e)
            }
        }
        launch {
            try {
                midiFilesList = repository.getMidiFileNames()
            } catch (e: Exception) {
                android.util.Log.e("HymnDetailScreen", "Failed to load GitHub midi files list", e)
            } finally {
                isMidiFilesLoading = false
            }
        }
    }

    val defaultOption = remember(hymn.number, hymn.signature, isKeerthane, isMt, midiFilesList) {
        extractTuneOptions(hymn.number, hymn.signature, isKeerthane, isMt, midiFilesList).firstOrNull() ?: hymn.number.toString()
    }

    val isMidiMigrated = if (isKeerthane) {
        remoteAppConfig.parsedMidiKeerthanes.contains(hymn.number)
    } else {
        val isMtRef = defaultOption.contains("M.T.", ignoreCase = true) || 
                      defaultOption.contains("Mang.T.B.", ignoreCase = true) || 
                      defaultOption.lowercase().startsWith("mt")
        if (isMtRef) {
            true
        } else {
            val baseMeter = if (defaultOption.contains("_")) defaultOption.substringBefore("_") else defaultOption
            val normalized = MeterUtils.getNormalizedMeter(baseMeter)
            val hasMatchingFiles = midiFilesList.any { filename ->
                val nameWithoutExt = filename.substringBeforeLast(".mid")
                val normalizedName = MeterUtils.getNormalizedMeter(nameWithoutExt)
                normalizedName == normalized || normalizedName.startsWith("${normalized}_")
            }
            hasMatchingFiles || remoteAppConfig.parsedMidiHymns.contains(normalized)
        }
    }

    val targetAudioUrl = hymn.audioUrl ?: when {
        isKeerthane -> {
            if (isMidiMigrated) {
                "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/midi/Keerthane_${hymn.number}.mid"
            } else {
                "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/Keerthane_${hymn.number}.ogg"
            }
        }
        isMt -> {
            val mtNumber = hymn.signature.split(Regex("[,/\\s]+")).firstOrNull()?.trim() ?: hymn.number.toString()
            "https://raw.githubusercontent.com/Reynold29/midi-files/main/Mangalore%20Tunes/mt${mtNumber}.mid"
        }
        else -> {
            val isMtRef = defaultOption.contains("M.T.", ignoreCase = true) || 
                          defaultOption.contains("Mang.T.B.", ignoreCase = true) || 
                          defaultOption.lowercase().startsWith("mt")
            val baseMeter = if (defaultOption.contains("_")) defaultOption.substringBefore("_") else defaultOption
            val normalized = MeterUtils.getNormalizedMeter(baseMeter)
            val hasMatchingFiles = midiFilesList.any { filename ->
                val nameWithoutExt = filename.substringBeforeLast(".mid")
                val normalizedName = MeterUtils.getNormalizedMeter(nameWithoutExt)
                normalizedName == normalized || normalizedName.startsWith("${normalized}_")
            }
            val isOptMigrated = if (isMtRef) true else (hasMatchingFiles || remoteAppConfig.parsedMidiHymns.contains(normalized))
            getUrlForOption(defaultOption, isOptMigrated, hymn.number)
        }
    }
    val isSameSong = audioState.currentAudioUrl == targetAudioUrl
    var showCastSheet by remember { mutableStateOf(false) }
    
    var showReportDialog by remember { mutableStateOf(false) }

    if (showCastSheet && castEnabled) {
        val streamUrl = targetAudioUrl
        CastControlSheet(
            request = SongCastRequest(
                streamUrl = streamUrl,
                title = hymn.title,
                subtitle = when {
                    isKeerthane -> "Keerthane ${hymn.number}"
                    isMt -> "MT ${hymn.number}"
                    else -> "Hymn ${hymn.number}"
                },
                contentType = if (streamUrl.endsWith(".mid", ignoreCase = true) || streamUrl.endsWith(".midi", ignoreCase = true)) "audio/midi" else "audio/ogg"
            ),
            onDismiss = { showCastSheet = false }
        )
    }
    
    LaunchedEffect(hymn.number) {
        val progress = readingProgressService.getProgress(if (isKeerthane) "keerthane" else "hymn", hymn.number.toString()).first()
        fontSize = prefs.getInt("global_songs_font_size", 18).sp
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



    val verifiedTuneOptions = remember { mutableStateListOf<String>() }

    LaunchedEffect(hymn.number, hymn.signature, isKeerthane, isMt, midiFilesList) {
        verifiedTuneOptions.clear()
        val baseOptions = extractTuneOptions(hymn.number, hymn.signature, isKeerthane, isMt, midiFilesList)
        verifiedTuneOptions.addAll(baseOptions)
        
        if (isMt) {
            baseOptions.forEach { baseOpt ->
                val cleanBase = baseOpt.filter { it.isDigit() }
                listOf("a", "b", "c", "d").forEach { suffix ->
                    val candidate = "$cleanBase$suffix"
                    if (candidate != baseOpt) {
                        val urlStr = "https://raw.githubusercontent.com/Reynold29/midi-files/main/Mangalore%20Tunes/mt$candidate.mid"
                        launch {
                            if (checkUrlExists(urlStr)) {
                                if (!verifiedTuneOptions.contains(candidate)) {
                                    verifiedTuneOptions.add(candidate)
                                    val sorted = verifiedTuneOptions.toList().sortedWith(
                                        compareBy<String> { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                                            .thenBy { it.filter { c -> c.isLetter() } }
                                    )
                                    verifiedTuneOptions.clear()
                                    verifiedTuneOptions.addAll(sorted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val showAudioPlayer = audioState.isVisible &&
        audioState.currentSongNumber == hymn.number &&
        audioState.isKeerthane == isKeerthane

    Scaffold { innerPadding ->
        val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isDarkTheme = isSystemInDarkTheme()
        val controlsCardColor = if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceBright
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SongInfoBanner(
                songTypeLabel = if (isKeerthane) "Keerthane" else "Hymn",
                number = hymn.number,
                title = hymn.title,
                subtitle = if (isKeerthane) {
                    null
                } else if (isMt) {
                    "M.T. ${hymn.signature}"
                } else {
                    hymn.signature.takeIf { it.isNotBlank() }
                },
                hint = null,
                onBackClick = {
                    HapticFeedbackManager.smoothClick(context)
                    audioViewModel.stopAndReset()
                    onBackClick()
                },
                actionContent = if (isLandscape) {
                    {
                        AnimatedVisibility(
                            visible = !isControlsExpanded,
                            enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + expandHorizontally(),
                            exit = fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) + shrinkHorizontally()
                        ) {
                            Button(
                                onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                    isControlsExpanded = true
                                    prefs.edit().putBoolean("detail_controls_expanded", true).apply()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Show Controls",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else null
            )

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
                            .weight(1f)
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

                    AnimatedVisibility(
                        visible = isControlsExpanded,
                        enter = slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                    ) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            Column(
                                modifier = Modifier
                                    .width(280.dp)
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .verticalScroll(rightColumnScrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        isControlsExpanded = false
                                        prefs.edit().putBoolean("detail_controls_expanded", false).apply()
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Hide Controls")
                                }
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
                                            val newSize = (fontSize.value - 2f).coerceAtLeast(14f)
                                            fontSize = newSize.sp
                                            prefs.edit().putInt("global_songs_font_size", newSize.toInt()).apply()
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
                                            val newSize = (fontSize.value + 2f).coerceAtMost(44f)
                                            fontSize = newSize.sp
                                            prefs.edit().putInt("global_songs_font_size", newSize.toInt()).apply()
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
                                                    audioViewModel.playSong(
                                                        number = hymn.number,
                                                        title = hymn.title,
                                                        isKeerthane = isKeerthane,
                                                        signature = hymn.signature,
                                                        customAudioUrl = targetAudioUrl
                                                    )
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
                                audioViewModel = audioViewModel,
                                tuneOptions = verifiedTuneOptions,
                                isMt = isMt,
                                remoteAppConfig = remoteAppConfig,
                                midiFilesList = midiFilesList
                            )
                        }
                    }
                }
            }
        }
    } else {
                AnimatedVisibility(
                    visible = isControlsExpanded,
                    enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(300))
                ) {
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
                                        val newSize = (fontSize.value - 2f).coerceAtLeast(14f)
                                        fontSize = newSize.sp
                                        prefs.edit().putInt("global_songs_font_size", newSize.toInt()).apply()
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
                                        val newSize = (fontSize.value + 2f).coerceAtMost(44f)
                                        fontSize = newSize.sp
                                        prefs.edit().putInt("global_songs_font_size", newSize.toInt()).apply()
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
                            val isLoadingMidi = isMidiMigrated && isMidiFilesLoading
                            Button(
                                index = 1,
                                onClick = {
                                    if (!isLoadingMidi) {
                                        HapticFeedbackManager.smoothClick(context)
                                        if (!audioState.isVisible || !isSameSong) {
                                            audioViewModel.playSong(
                                                number = hymn.number,
                                                title = hymn.title,
                                                isKeerthane = isKeerthane,
                                                signature = hymn.signature,
                                                customAudioUrl = targetAudioUrl
                                            )
                                        } else {
                                            audioViewModel.toggleVisibility()
                                        }
                                    }
                                },
                                icon = if (isLoadingMidi) null else if (audioState.isVisible && isSameSong) Icons.Default.KeyboardArrowDown else Icons.Default.MusicNote,
                                label = if (isLoadingMidi) "Loading..." else if (audioState.isVisible && isSameSong) "Hide" else "Audio",
                                isSelected = audioState.isVisible && isSameSong,
                                variant = GroupButtonVariant.Tonal,
                                containerColor = if (isLoadingMidi) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else null,
                                contentColor = if (isLoadingMidi) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else null
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

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                isControlsExpanded = false
                                prefs.edit().putBoolean("detail_controls_expanded", false).apply()
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hide Controls")
                        }
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
                        AnimatedVisibility(
                            visible = !isControlsExpanded,
                            enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(300))
                        ) {
                            Button(
                                onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                    isControlsExpanded = true
                                    prefs.edit().putBoolean("detail_controls_expanded", true).apply()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Show Controls", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

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
                                audioViewModel = audioViewModel,
                                tuneOptions = verifiedTuneOptions,
                                isMt = isMt,
                                remoteAppConfig = remoteAppConfig,
                                midiFilesList = midiFilesList
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
    audioViewModel: AudioViewModel,
    tuneOptions: List<String> = emptyList(),
    isMt: Boolean = false,
    remoteAppConfig: RemoteAppConfig,
    midiFilesList: List<String> = emptyList()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAdvancedMidiBottomSheet by remember { mutableStateOf(false) }
    var showAudioContributionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(audioState.error) {
        if (audioState.error == "AUDIO_NOT_FOUND") {
            showAudioContributionDialog = true
        }
    }

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
                        text = when {
                            audioState.isKeerthane -> "Keerthane ${audioState.currentSongNumber}"
                            isMt -> {
                                val currentMt = audioState.currentAudioUrl?.substringAfterLast("/mt")?.substringBefore(".mid") ?: ""
                                if (currentMt.isNotEmpty()) "M.T. $currentMt" else "Hymn ${audioState.currentSongNumber}"
                            }
                            else -> "Hymn ${audioState.currentSongNumber}"
                        },
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

            if (tuneOptions.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                TuneSelectorDropdown(
                    tuneOptions = tuneOptions,
                    currentSongNum = audioState.currentSongNumber ?: 0,
                    isKeerthane = audioState.isKeerthane,
                    isMt = isMt,
                    audioState = audioState,
                    remoteAppConfig = remoteAppConfig,
                    audioViewModel = audioViewModel,
                    context = context,
                    midiFilesList = midiFilesList
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
                        label = "Speed",
                        showLabelWhenUnselected = false,
                        alwaysCircle = !isLandscape,
                        compact = !isLandscape,
                        iconSize = if (isLandscape) 28.dp else 24.dp,
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

            // Advanced Audio Controls launcher button
            val isMidi = audioState.currentAudioUrl?.endsWith(".mid", ignoreCase = true) == true
            if (isMidi) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { showAdvancedMidiBottomSheet = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Advanced Audio Controls",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Advanced Audio Controls Modal Bottom Sheet
            if (showAdvancedMidiBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAdvancedMidiBottomSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Advanced Audio Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val instruments = listOf(
                            Pair("Church Organ", 19),
                            Pair("Grand Piano", 0),
                            Pair("Drawbar Organ", 16),
                            Pair("Choir", 52),
                            Pair("Flute", 73)
                        )

                        // 1. Default / Set All Instrument override
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Default / Set All Instrument",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            var showInstrumentMenu by remember { mutableStateOf(false) }
                            val currentInstrumentId = remember {
                                val prefs = context.getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
                                mutableStateOf(prefs.getInt("midi_instrument", 16))
                            }
                            val currentInstrumentName = instruments.firstOrNull { it.second == currentInstrumentId.value }?.first ?: "Drawbar Organ"
                            
                            Box {
                                TextButton(onClick = { showInstrumentMenu = true }) {
                                    Text(currentInstrumentName)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = showInstrumentMenu,
                                    onDismissRequest = { showInstrumentMenu = false }
                                ) {
                                    instruments.forEach { (name, id) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                currentInstrumentId.value = id
                                                audioViewModel.setMidiInstrument(id)
                                                audioViewModel.setSatbInstruments(id, id, id, id)
                                                showInstrumentMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 2. Transpose (Pitch)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Transpose (Pitch)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val currentTranspose = audioState.midiTranspose
                                OutlinedIconButton(
                                    onClick = { audioViewModel.setMidiTranspose(currentTranspose - 1) },
                                    modifier = Modifier.size(36.dp),
                                    enabled = currentTranspose > -6
                                ) {
                                    Text("-", style = MaterialTheme.typography.titleMedium)
                                }
                                
                                Text(
                                    text = if (currentTranspose == 0) "Normal" else if (currentTranspose > 0) "+$currentTranspose" else "$currentTranspose",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                OutlinedIconButton(
                                    onClick = { audioViewModel.setMidiTranspose(currentTranspose + 1) },
                                    modifier = Modifier.size(36.dp),
                                    enabled = currentTranspose < 6
                                ) {
                                    Text("+", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        
                        // 2.5. Playback Speed Slider
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Playback Speed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (kotlin.math.abs(audioState.playbackSpeed - 1.0f) > 0.01f) {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "Reset",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                HapticFeedbackManager.smoothClick(context)
                                                audioViewModel.setPlaybackSpeed(1.0f)
                                            }
                                        )
                                    }
                                }
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2fx", audioState.playbackSpeed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedIconButton(
                                    onClick = { 
                                        val newSpeed = (audioState.playbackSpeed - 0.05f).coerceAtLeast(0.5f)
                                        audioViewModel.setPlaybackSpeed(newSpeed)
                                    },
                                    modifier = Modifier.size(36.dp),
                                    enabled = audioState.playbackSpeed > 0.5f
                                ) {
                                    Text("-", style = MaterialTheme.typography.titleMedium)
                                }
                                
                                Slider(
                                    value = audioState.playbackSpeed,
                                    onValueChange = { audioViewModel.setPlaybackSpeed(it) },
                                    valueRange = 0.5f..1.5f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                
                                OutlinedIconButton(
                                    onClick = { 
                                        val newSpeed = (audioState.playbackSpeed + 0.05f).coerceAtMost(1.5f)
                                        audioViewModel.setPlaybackSpeed(newSpeed)
                                    },
                                    modifier = Modifier.size(36.dp),
                                    enabled = audioState.playbackSpeed < 1.5f
                                ) {
                                    Text("+", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        
                        // 3. SATB Routing with Individual Instruments
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "SATB Vocal Routing & Instruments",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = audioState.isSatbRoutingEnabled,
                                    onCheckedChange = { audioViewModel.setSatbRoutingEnabled(it) }
                                )
                            }
                            
                            if (audioState.isSatbRoutingEnabled) {
                                val satbParts = listOf(
                                    SatbPartConfig("Soprano", audioState.isSopranoEnabled, audioState.sopranoInstrument,
                                        onToggle = { enabled -> audioViewModel.setSatbRoute(enabled, audioState.isAltoEnabled, audioState.isTenorEnabled, audioState.isBassEnabled) },
                                        onInstrumentChange = { inst -> audioViewModel.setSatbInstruments(inst, audioState.altoInstrument, audioState.tenorInstrument, audioState.bassInstrument) }
                                    ),
                                    SatbPartConfig("Alto", audioState.isAltoEnabled, audioState.altoInstrument,
                                        onToggle = { enabled -> audioViewModel.setSatbRoute(audioState.isSopranoEnabled, enabled, audioState.isTenorEnabled, audioState.isBassEnabled) },
                                        onInstrumentChange = { inst -> audioViewModel.setSatbInstruments(audioState.sopranoInstrument, inst, audioState.tenorInstrument, audioState.bassInstrument) }
                                    ),
                                    SatbPartConfig("Tenor", audioState.isTenorEnabled, audioState.tenorInstrument,
                                        onToggle = { enabled -> audioViewModel.setSatbRoute(audioState.isSopranoEnabled, audioState.isAltoEnabled, enabled, audioState.isBassEnabled) },
                                        onInstrumentChange = { inst -> audioViewModel.setSatbInstruments(audioState.sopranoInstrument, audioState.altoInstrument, inst, audioState.bassInstrument) }
                                    ),
                                    SatbPartConfig("Bass", audioState.isBassEnabled, audioState.bassInstrument,
                                        onToggle = { enabled -> audioViewModel.setSatbRoute(audioState.isSopranoEnabled, audioState.isAltoEnabled, audioState.isTenorEnabled, enabled) },
                                        onInstrumentChange = { inst -> audioViewModel.setSatbInstruments(audioState.sopranoInstrument, audioState.altoInstrument, audioState.tenorInstrument, inst) }
                                    )
                                )
                                
                                satbParts.forEach { part ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Checkbox(
                                                    checked = part.isEnabled,
                                                    onCheckedChange = { part.onToggle(it) }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = part.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (part.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                            
                                            var showPartMenu by remember { mutableStateOf(false) }
                                            val currentPartInstrumentName = instruments.firstOrNull { it.second == part.currentInstrument }?.first ?: "Drawbar Organ"
                                            
                                            Box {
                                                TextButton(
                                                    onClick = { showPartMenu = true },
                                                    enabled = part.isEnabled
                                                ) {
                                                    Text(
                                                        text = currentPartInstrumentName,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showPartMenu,
                                                    onDismissRequest = { showPartMenu = false }
                                                ) {
                                                    instruments.forEach { (name, id) ->
                                                        DropdownMenuItem(
                                                            text = { Text(name) },
                                                            onClick = {
                                                                part.onInstrumentChange(id)
                                                                showPartMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Audio Contribution Dialog
            if (showAudioContributionDialog) {
                AudioContributionDialog(
                    songNumber = audioState.currentSongNumber ?: 0,
                    songTitle = audioState.currentSongTitle ?: "Unknown",
                    isKeerthane = audioState.isKeerthane,
                    onDismiss = {
                        showAudioContributionDialog = false
                        audioViewModel.stopAndReset()
                    },
                    onSubmit = { name, bytes ->
                        showAudioContributionDialog = false
                        scope.launch {
                            val appVersion = try {
                                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
                            } catch (e: Exception) {
                                "Unknown"
                            }
                            val ticketsRepository = TicketsRepository(context)
                            val jiraService = JiraService()
                            
                            Toast.makeText(context, "Uploading audio contribution...", Toast.LENGTH_SHORT).show()
                            
                            val ticketResult = jiraService.createTicket(
                                songType = if (audioState.isKeerthane) "Keerthane" else "Hymn",
                                songNumber = audioState.currentSongNumber ?: 0,
                                songTitle = audioState.currentSongTitle ?: "Unknown",
                                description = "User contributed audio file: $name for this song. Please review and add to raw assets.",
                                appVersion = appVersion,
                                guestDeviceId = ticketsRepository.getDeviceIdForGuest(),
                                isAudioContribution = true
                            )
                            
                            if (ticketResult.success && ticketResult.ticketKey != null) {
                                val uploadSuccess = jiraService.uploadAttachment(
                                    ticketKey = ticketResult.ticketKey,
                                    fileBytes = bytes,
                                    fileName = name
                                )
                                if (uploadSuccess) {
                                    Toast.makeText(context, "Thank you! Audio contribution ticket ${ticketResult.ticketKey} submitted.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Ticket created (${ticketResult.ticketKey}) but file attachment upload failed.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Failed to submit contribution: ${ticketResult.errorMessage}", Toast.LENGTH_LONG).show()
                            }
                            audioViewModel.stopAndReset()
                        }
                    }
                )
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

fun getUrlForOption(option: String, isOptionMidiMigrated: Boolean, songNumber: Int): String {
    val trimmed = option.trim()
    val isMtRef = trimmed.contains("M.T.", ignoreCase = true) || 
                  trimmed.contains("Mang.T.B.", ignoreCase = true) || 
                  trimmed.lowercase().startsWith("mt")
    
    return when {
        isMtRef -> {
            val mtNumber = trimmed.filter { it.isDigit() || it == 'b' || it == 'c' || it == 'd' || it == 'e' }
            "https://raw.githubusercontent.com/Reynold29/midi-files/main/Mangalore%20Tunes/mt${mtNumber}.mid"
        }
        else -> {
            if (isOptionMidiMigrated) {
                val meterName = MeterUtils.getMeterMidiFileName(trimmed)
                "https://raw.githubusercontent.com/reynold29/midi-files/main/Hymns/midi/${meterName}.mid"
            } else {
                "https://raw.githubusercontent.com/reynold29/midi-files/main/Hymns/Hymn_${songNumber}.ogg"
            }
        }
    }
}

fun extractTuneOptions(
    hymnNumber: Int,
    signature: String,
    isKeerthane: Boolean,
    isMt: Boolean = false,
    midiFilesList: List<String> = emptyList()
): List<String> {
    if (isKeerthane) return listOf(hymnNumber.toString())
    
    val options = mutableListOf<String>()
    
    if (isMt) {
        // For MT hymns, signature is a comma/slash/space separated list of MT numbers
        val regex = Regex("\\b\\d+[b-e]?\\b")
        regex.findAll(signature).forEach { match ->
            options.add(match.value.lowercase())
        }
        if (options.isEmpty()) {
            options.add(hymnNumber.toString())
        }
    } else {
        // CSI Hymns
        val signatures = if (signature.contains("/")) {
            signature.split("/").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listOf(signature.trim())
        }
        
        for (sig in signatures) {
            val isMtRef = sig.contains("M.T.", ignoreCase = true) || 
                          sig.contains("Mang.T.B.", ignoreCase = true) || 
                          sig.lowercase().startsWith("mt")
            
            if (isMtRef) {
                options.add(sig)
            } else {
                val normalizedSig = MeterUtils.getNormalizedMeter(sig)
                
                // Scan midiFilesList for files matching this signature
                val matchedFiles = midiFilesList.filter { filename ->
                    val nameWithoutExt = filename.substringBeforeLast(".mid")
                    val normalizedName = MeterUtils.getNormalizedMeter(nameWithoutExt)
                    normalizedName == normalizedSig || normalizedName.startsWith("${normalizedSig}_")
                }
                
                if (matchedFiles.isNotEmpty()) {
                    options.addAll(matchedFiles.map { it.substringBeforeLast(".mid") })
                } else {
                    options.add(sig)
                }
            }
        }
    }
    
    return options.distinct()
}

suspend fun checkUrlExists(urlStr: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val url = java.net.URL(urlStr)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connectTimeout = 800
        connection.readTimeout = 800
        val responseCode = connection.responseCode
        responseCode == java.net.HttpURLConnection.HTTP_OK
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioContributionDialog(
    songNumber: Int,
    songTitle: String,
    isKeerthane: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (fileName: String, fileBytes: ByteArray) -> Unit
) {
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val name = getFileName(context, uri)
            val ext = name.substringAfterLast('.', "").lowercase()
            val isValid = ext == "mid" || ext == "midi" || ext == "mp3" || ext == "ogg" || ext == "wav" || ext == "m4a" || ext == "aac" || ext == "flac" || context.contentResolver.getType(uri)?.startsWith("audio/") == true
            
            if (isValid) {
                selectedFileUri = uri
                selectedFileName = name
            } else {
                Toast.makeText(context, "Please select an audio file (e.g. .mp3, .ogg, .wav, .mid)", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No Audio Available") },
        text = {
            Column {
                Text(
                    text = "No audio is available for \"$songTitle\". Would you like to submit an audio or MIDI file to help improve the library for everyone?",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (selectedFileName != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Audio File",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedFileName ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedFileUri == null) {
                Button(onClick = { filePickerLauncher.launch("*/*") }) {
                    Text("Select Audio File")
                }
            } else {
                Button(onClick = {
                    val uri = selectedFileUri!!
                    val name = selectedFileName!!
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        if (bytes != null) {
                            onSubmit(name, bytes)
                        } else {
                            Toast.makeText(context, "Could not read file data", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Submit Audio")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getFileName(context: android.content.Context, uri: android.net.Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "audio_contribution"
}

data class SatbPartConfig(
    val name: String,
    val isEnabled: Boolean,
    val currentInstrument: Int,
    val onToggle: (Boolean) -> Unit,
    val onInstrumentChange: (Int) -> Unit
)

@Composable
fun TuneSelectorDropdown(
    tuneOptions: List<String>,
    currentSongNum: Int,
    isKeerthane: Boolean,
    isMt: Boolean,
    audioState: AudioState,
    remoteAppConfig: com.reyzie.hymns.data.RemoteAppConfig,
    audioViewModel: AudioViewModel,
    context: Context,
    midiFilesList: List<String>,
    modifier: Modifier = Modifier
) {
    if (tuneOptions.size <= 1) return
    
    var showTuneDropdown by remember { mutableStateOf(false) }
    
    // Find the currently active tune option or select the first one
    val activeOption = tuneOptions.firstOrNull { option ->
        val isOptionMidiMigrated = if (isKeerthane) {
            remoteAppConfig.parsedMidiKeerthanes.contains(currentSongNum)
        } else {
            val isMtRef = option.contains("M.T.", ignoreCase = true) || 
                          option.contains("Mang.T.B.", ignoreCase = true) || 
                          option.lowercase().startsWith("mt")
            if (isMtRef) {
                true
            } else {
                val baseMeter = if (option.contains("_")) option.substringBefore("_") else option
                val normalized = MeterUtils.getNormalizedMeter(baseMeter)
                val hasMatchingFiles = midiFilesList.any { filename ->
                    val nameWithoutExt = filename.substringBeforeLast(".mid")
                    val normalizedName = MeterUtils.getNormalizedMeter(nameWithoutExt)
                    normalizedName == normalized || normalizedName.startsWith("${normalized}_")
                }
                hasMatchingFiles || remoteAppConfig.parsedMidiHymns.contains(normalized)
            }
        }
        val optionUrl = when {
            isKeerthane -> {
                if (isOptionMidiMigrated) {
                    "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/midi/Keerthane_$option.mid"
                } else {
                    "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/Keerthane_$option.ogg"
                }
            }
            isMt -> "https://raw.githubusercontent.com/Reynold29/midi-files/main/Mangalore%20Tunes/mt$option.mid"
            else -> {
                getUrlForOption(option, isOptionMidiMigrated, currentSongNum)
            }
        }
        audioState.currentAudioUrl == optionUrl
    } ?: tuneOptions.firstOrNull() ?: ""

    val activeDisplayName = if (isKeerthane || isMt) {
        if (activeOption == currentSongNum.toString()) "Default" else activeOption
    } else {
        MeterUtils.getDisplayTuneName(activeOption)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .clickable {
                    HapticFeedbackManager.smoothClick(context)
                    showTuneDropdown = true
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Select Tune: $activeDisplayName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Tune",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        DropdownMenu(
            expanded = showTuneDropdown,
            onDismissRequest = { showTuneDropdown = false }
        ) {
            tuneOptions.forEach { option ->
                val isOptionMidiMigrated = if (isKeerthane) {
                    remoteAppConfig.parsedMidiKeerthanes.contains(currentSongNum)
                } else {
                    val isMtRef = option.contains("M.T.", ignoreCase = true) || 
                                  option.contains("Mang.T.B.", ignoreCase = true) || 
                                  option.lowercase().startsWith("mt")
                    if (isMtRef) {
                        true
                    } else {
                        val baseMeter = if (option.contains("_")) option.substringBefore("_") else option
                        val normalized = MeterUtils.getNormalizedMeter(baseMeter)
                        val hasMatchingFiles = midiFilesList.any { filename ->
                            val nameWithoutExt = filename.substringBeforeLast(".mid")
                            val normalizedName = MeterUtils.getNormalizedMeter(nameWithoutExt)
                            normalizedName == normalized || normalizedName.startsWith("${normalized}_")
                        }
                        hasMatchingFiles || remoteAppConfig.parsedMidiHymns.contains(normalized)
                    }
                }
                val optionUrl = when {
                    isKeerthane -> {
                        if (isOptionMidiMigrated) {
                            "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/midi/Keerthane_$option.mid"
                        } else {
                            "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/Keerthane_$option.ogg"
                        }
                    }
                    isMt -> "https://raw.githubusercontent.com/Reynold29/midi-files/main/Mangalore%20Tunes/mt$option.mid"
                    else -> {
                        getUrlForOption(option, isOptionMidiMigrated, currentSongNum)
                    }
                }
                val isSelected = audioState.currentAudioUrl == optionUrl
                
                val displayName = if (isKeerthane || isMt) {
                    if (option == currentSongNum.toString()) "Default" else option
                } else {
                    MeterUtils.getDisplayTuneName(option)
                }

                DropdownMenuItem(
                    text = { 
                        Text(
                            text = displayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    onClick = {
                        showTuneDropdown = false
                        HapticFeedbackManager.smoothClick(context)
                        audioViewModel.playSong(
                            number = currentSongNum,
                            title = audioState.currentSongTitle.orEmpty().ifBlank { "Audio" },
                            isKeerthane = isKeerthane,
                            signature = option,
                            customAudioUrl = optionUrl
                        )
                    }
                )
            }
        }
    }
}
