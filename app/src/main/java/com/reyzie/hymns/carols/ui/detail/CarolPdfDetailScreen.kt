package com.reyzie.hymns.carols.ui.detail

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.reyzie.hymns.ui.widgets.ChristmasScreenBackground
import com.reyzie.hymns.ui.widgets.PdfSongViewer
import com.reyzie.hymns.ui.widgets.rememberChristmasScreenColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarolPdfDetailScreen(
    title: String,
    pdfUrl: String,
    onBackClick: () -> Unit,
) {
    val colors = rememberChristmasScreenColors()

    ChristmasScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
            PdfSongViewer(
                pdfPath = pdfUrl,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
