package com.reyzie.hymns.ui.christmas.eggs

/** The hidden Christmas easter eggs. [name] is what gets stored in settings. */
enum class ChristmasSecret(val hint: String) {
    STAR("Tap the star 7 times"),
    BELL("Ring the hanging bell"),
    SNOWMAN("Tap the snow at the bottom-left 3 times"),
    POP("Tap a drifting emoji"),
    LIGHTS("Press and hold “Merry Christmas”"),
    SHAKE("Shake the phone"),
    TILT("Tilt the phone"),
    SANTA("Tap Santa’s sleigh as it flies past");

    companion object {
        /** Reads stored names, ignoring any that no longer exist. */
        fun fromStored(names: Set<String>): Set<ChristmasSecret> =
            names.mapNotNull { name -> entries.firstOrNull { it.name == name } }.toSet()
    }
}

/** What a tap on the star led to. */
enum class StarTapResult { TAP, HINT, SHOOTING_STAR }

/**
 * Counts quick taps on the Star of Bethlehem. Taps more than [windowSeconds] apart start
 * the count again. A hint comes at [hintAt] taps and the shooting star at [triggerAt].
 */
class StarTapCounter(
    private val windowSeconds: Float = 2.5f,
    private val hintAt: Int = 4,
    private val triggerAt: Int = 7,
) {
    private var count = 0
    private var lastTapSeconds: Float? = null

    fun tap(nowSeconds: Float): StarTapResult {
        val last = lastTapSeconds
        if (last == null || nowSeconds - last > windowSeconds) count = 0
        lastTapSeconds = nowSeconds
        count += 1
        return when (count) {
            triggerAt -> {
                count = 0
                StarTapResult.SHOOTING_STAR
            }
            hintAt -> StarTapResult.HINT
            else -> StarTapResult.TAP
        }
    }
}
