package com.reyzie.hymns.ui.christmas

/**
 * How much Christmas motion the user wants. Stored in settings as [name].
 *
 * [snowDensity] is the share of snow particles drawn (0..1) and [emojiCount] is how many
 * drifting emoji easter eggs fall with the snow.
 */
enum class ChristmasEffectsLevel(
    val snowDensity: Float,
    val emojiCount: Int,
    val label: String,
    val description: String,
) {
    OFF(
        snowDensity = 0f,
        emojiCount = 0,
        label = "Off",
        description = "No snow or moving effects",
    ),
    GENTLE(
        snowDensity = 0.5f,
        emojiCount = 5,
        label = "Gentle",
        description = "Light snowfall, snow piles on cards, tilt, shake and Santa",
    );

    companion object {
        val DEFAULT = GENTLE

        /**
         * Reads a stored setting, falling back to [DEFAULT] for missing or unknown values.
         * The old "FULL" setting was retired, so it now reads as Gentle.
         */
        fun fromKey(key: String?): ChristmasEffectsLevel =
            entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
