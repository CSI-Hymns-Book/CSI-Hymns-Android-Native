package com.reyzie.hymns.ui.christmas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val TOAST_MILLIS = 2_400L
private val ToastBackground = Color(0xFF0C0E1E).copy(alpha = 0.86f)
private val ToastText = Color(0xFFF6EFE2)
private val ToastEdge = Color(0xFFE9C27B).copy(alpha = 0.4f)

/**
 * Lets the Christmas effects react to taps anywhere in the app (catching Santa, popping
 * emojis). Taps that hit nothing pass straight through to the screen underneath.
 */
fun Modifier.christmasTapCatcher(controller: ChristmasAmbienceController): Modifier =
    pointerInput(controller) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (controller.handleTap(down.position.x, down.position.y)) down.consume()
        }
    }

/** Short Christmas messages ("Secret found · 3 of 8") at the bottom of the screen. */
@Composable
fun BoxScope.ChristmasToastHost(controller: ChristmasAmbienceController) {
    val toast = controller.toast
    var lastText by remember { mutableStateOf("") }
    val text = toast?.text ?: lastText
    SideEffect { lastText = text }
    LaunchedEffect(toast?.id) {
        val id = toast?.id ?: return@LaunchedEffect
        delay(TOAST_MILLIS)
        controller.dismissToast(id)
    }
    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn() + slideInVertically(spring(dampingRatio = 0.6f)) { it },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 104.dp),
    ) {
        Box(
            Modifier
                .background(ToastBackground, CircleShape)
                .border(1.dp, ToastEdge, CircleShape)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(text, color = ToastText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
