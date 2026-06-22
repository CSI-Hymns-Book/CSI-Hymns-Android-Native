package com.reyzie.hymns.carols.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.domain.splitBilingualLyrics
import com.reyzie.hymns.ui.widgets.ChristmasScreenBackground
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarolSongDetailScreen(
    song: CarolSong,
    churchName: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
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
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    churchName,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF90CAF9),
                )
                song.songNumber?.let {
                    Text(
                        "No. $it · ${song.scale}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (english != null) {
                    StandardButtonGroup(
                        buttonCount = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (language) {
                        "English" -> english.orEmpty()
                        else -> kannada
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}
