package com.reyzie.hymns.ui.widgets

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.data.Hymn
import kotlinx.coroutines.launch
import com.reyzie.hymns.presentation.FreeShowFormatter
import com.reyzie.hymns.presentation.LyricsLanguageMode
import com.reyzie.hymns.utils.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeShowExportSheet(
    hymn: Hymn,
    isKeerthane: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasKannada = !hymn.kannadaLyrics.isNullOrBlank()
    var languageMode by remember {
        mutableStateOf(if (hasKannada) LyricsLanguageMode.KANNADA else LyricsLanguageMode.ENGLISH)
    }
    var showBeamSheet by remember { mutableStateOf(false) }

    if (showBeamSheet) {
        FreeShowRemoteSheet(
            hymn = hymn,
            isKeerthane = isKeerthane,
            languageMode = languageMode,
            onDismiss = { showBeamSheet = false }
        )
    }

    val slides = remember(hymn, isKeerthane, languageMode) {
        FreeShowFormatter.parseSlides(
            number = hymn.number,
            title = hymn.title,
            isKeerthane = isKeerthane,
            kannadaLyrics = hymn.kannadaLyrics,
            englishLyrics = hymn.lyrics,
            mode = languageMode,
            churchName = null,
            includeCreditSlide = true
        )
    }

    val quickLyricsText = remember(slides, hymn, isKeerthane) {
        FreeShowFormatter.formatQuickLyricsText(
            number = hymn.number,
            title = hymn.title,
            isKeerthane = isKeerthane,
            slides = slides
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CoPresent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Project on Screen (FreeShow)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (isKeerthane) "Keerthane" else "Hymn"} ${hymn.number} • ${hymn.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Centered Language Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = languageMode == LyricsLanguageMode.KANNADA,
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        languageMode = LyricsLanguageMode.KANNADA
                    },
                    label = { Text("ಕನ್ನಡ") },
                    enabled = hasKannada
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = languageMode == LyricsLanguageMode.ENGLISH,
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        languageMode = LyricsLanguageMode.ENGLISH
                    },
                    label = { Text("English") }
                )
                if (hasKannada) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = languageMode == LyricsLanguageMode.BILINGUAL,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            languageMode = LyricsLanguageMode.BILINGUAL
                        },
                        label = { Text("Both") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Entire Slide Deck Preview - Scrollable
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Slide Deck (${slides.size} slides)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Scroll to review all",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inner Scrollable Slides List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slides.forEachIndexed { index, slide ->
                            val isChorus = slide.group.startsWith("Chorus", ignoreCase = true)

                            val cardBg = if (isChorus) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }

                            val tagColor = if (isChorus) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = cardBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "[${slide.group}]",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = tagColor
                                        )
                                        Text(
                                            text = "Slide ${index + 1} of ${slides.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = slide.content,
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "By CSI Hymns Book App",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Wireless Wi-Fi Beam to FreeShow
            FilledTonalButton(
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    showBeamSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Beam to FreeShow (Live Wi-Fi)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons - Project Only (No .show)
            Button(
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    try {
                        val file = FreeShowFormatter.createProjectFile(
                            context = context,
                            number = hymn.number,
                            title = hymn.title,
                            isKeerthane = isKeerthane,
                            slides = slides
                        )
                        val intent = FreeShowFormatter.createShareShowIntent(context, file)
                        context.startActivity(Intent.createChooser(intent, "Share FreeShow Project"))

                        scope.launch {
                            com.reyzie.hymns.data.SupabaseService.getInstance().logFreeShowDownload(
                                downloadType = "single",
                                category = if (isKeerthane) "keerthane" else "hymn",
                                songNumber = hymn.number,
                                songTitle = hymn.title,
                                language = languageMode.name.lowercase(),
                                slideCount = slides.size,
                                source = "android"
                            )
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Export FreeShow (.project)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        clipboardManager.setText(AnnotatedString(quickLyricsText))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Text", maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        val title = "${if (isKeerthane) "Keerthane" else "Hymn"} ${hymn.number} - ${hymn.title}"
                        val intent = FreeShowFormatter.createShareTextIntent(title, quickLyricsText)
                        context.startActivity(Intent.createChooser(intent, "Share Slides"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Text", maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
