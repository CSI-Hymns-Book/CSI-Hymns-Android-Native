package com.reyzie.hymns.ui.christmas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lets the Christmas home ask for white status-bar icons while a dark night sky is behind
 * them in light mode. The app theme reads this when it sets the icon colour, so the two
 * never fight over it.
 */
object ChristmasStatusBar {
    var forceLightIcons by mutableStateOf(false)
}
