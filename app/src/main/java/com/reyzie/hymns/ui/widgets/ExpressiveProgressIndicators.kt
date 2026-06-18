package com.reyzie.hymns.ui.widgets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimum size — [ContainedLoadingIndicator] does not render reliably below this. */
private val ContainedIndicatorMinSize = 48.dp

/**
 * M3 expressive contained loading indicator — morphing shape inside a tonal container.
 * Uses [primaryContainer] so it stays visible on [surfaceContainerHigh] backgrounds (e.g. audio player).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveCircularProgress(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val resolvedSize = if (size < ContainedIndicatorMinSize) ContainedIndicatorMinSize else size
    ContainedLoadingIndicator(
        modifier = modifier.size(resolvedSize),
        containerColor = containerColor,
        indicatorColor = indicatorColor,
        containerShape = LoadingIndicatorDefaults.containerShape
    )
}

/** M3 expressive linear progress — rounded caps, full-width track. */
@Composable
fun ExpressiveLinearProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
    }
}
