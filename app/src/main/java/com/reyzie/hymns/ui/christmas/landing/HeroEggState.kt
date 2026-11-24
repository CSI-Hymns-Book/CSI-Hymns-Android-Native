package com.reyzie.hymns.ui.christmas.landing

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Easter-egg state for the hero scene, with times in seconds on the scene clock.
 */
@Stable
class HeroEggState(val moment: ChristmasMoment) {
    /** When the star was last tapped; it flares briefly afterwards. */
    var starTappedAt by mutableFloatStateOf(NEVER)

    /** When the shooting star was sent, or null. */
    var shootingStarAt by mutableStateOf<Float?>(null)

    /** How many snowman pieces are built (0..3). */
    var snowmanStage by mutableIntStateOf(0)
        private set
    private val pieceStartTimes = FloatArray(SNOWMAN_PIECES) { NEVER }

    val isSnowmanDone: Boolean get() = snowmanStage >= SNOWMAN_PIECES

    fun pieceStartedAt(index: Int): Float = pieceStartTimes[index]

    /** Adds the next snowman piece; returns true when this finished him. */
    fun buildSnowman(now: Float): Boolean {
        if (isSnowmanDone) return false
        pieceStartTimes[snowmanStage] = now
        snowmanStage += 1
        return isSnowmanDone
    }

    companion object {
        const val NEVER = -1_000f
        const val SNOWMAN_PIECES = 3
    }
}
