package com.reyzie.hymns.carols.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.domain.splitBilingualLyrics
import com.reyzie.hymns.ui.widgets.ChristmasScreenBackground
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.ui.widgets.rememberChristmasScreenColors
import com.reyzie.hymns.utils.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarolSongDetailScreen(
    song: CarolSong,
    churchName: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val colors = rememberChristmasScreenColors()
    val (kannada, english) = remember(song.id) { splitBilingualLyrics(song.lyrics) }
    var language by remember { mutableStateOf(if (english != null) "Kannada" else "Lyrics") }

    ChristmasScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            song.title,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.onBackground,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            },
        ) { padding ->
            val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            churchName,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accent,
                        )
                        song.songNumber?.let {
                            Text(
                                "No. $it · ${song.scale}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onBackgroundMuted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    if (english != null && !isLandscape) {
                        StandardButtonGroup(
                            buttonCount = 2,
                            modifier = Modifier
                                .width(200.dp)
                                .padding(vertical = 4.dp),
                            highContrast = true,
                        ) {
                            Button(
                                index = 0,
                                onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                    language = "Kannada"
                                },
                                label = "ಕನ್ನಡ",
                                isSelected = language == "Kannada",
                            )
                            Button(
                                index = 1,
                                onClick = {
                                    HapticFeedbackManager.smoothClick(context)
                                    language = "English"
                                },
                                label = "English",
                                isSelected = language == "English",
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (english != null && isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ಕನ್ನಡ (Kannada)", style = MaterialTheme.typography.titleMedium, color = colors.accent, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(kannada, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("English", style = MaterialTheme.typography.titleMedium, color = colors.accent, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(english, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                        }
                    }
                } else {
                    Text(
                        text = when (language) {
                            "English" -> english.orEmpty()
                            else -> kannada
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBackground,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}
