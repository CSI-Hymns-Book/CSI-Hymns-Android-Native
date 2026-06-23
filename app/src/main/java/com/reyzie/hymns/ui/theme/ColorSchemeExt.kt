package com.reyzie.hymns.ui.theme

import androidx.compose.ui.graphics.Color

private val ReadableDarkOnLight = Color(0xFF111318)
private val ReadableLightOnDark = Color(0xFFE8EAED)

/** Dark text on light backgrounds, light text on dark backgrounds. */
fun Color.contentOn(): Color =
    if (luminance() > 0.5f) ReadableDarkOnLight else ReadableLightOnDark

private fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else Math.pow(((red + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else Math.pow(((green + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else Math.pow(((blue + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
