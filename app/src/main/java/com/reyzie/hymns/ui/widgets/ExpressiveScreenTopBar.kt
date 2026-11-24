package com.reyzie.hymns.ui.widgets

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.lists.isChristmasLook
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import com.reyzie.hymns.ui.christmas.snowCap
import com.reyzie.hymns.utils.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveScreenTopBar(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Menu,
    actions: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val christmasLook = isChristmasLook()
    TopAppBar(
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars),
        title = {
            if (christmasLook) {
                ChristmasTitle(title)
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        },
        navigationIcon = {
            IconButton(
                modifier = if (christmasLook) Modifier.snowCap(cornerDp = 24f) else Modifier,
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    onMenuClick()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            ) {
                Icon(navigationIcon, contentDescription = "Navigation Icon")
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/** Christmas title: the display serif with a small gold star before it. */
@Composable
private fun ChristmasTitle(title: String) {
    val palette = rememberChristmasPalette()
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = palette.gold)) { append("✦  ") }
            append(title)
        },
        style = MaterialTheme.typography.titleLarge,
        fontFamily = ChristmasDisplayFont,
        fontWeight = FontWeight.Medium,
        color = palette.ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
