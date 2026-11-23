package com.reyzie.hymns.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import com.reyzie.hymns.ui.christmas.motion.SnowHaptics
import com.reyzie.hymns.ui.christmas.motion.SnowHapticEvent
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedbackManager {
    
    fun performHapticFeedback(view: View, type: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        view.performHapticFeedback(type)
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(context: Context, duration: Long) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun lightClick(context: Context) = vibrate(context, 5)

    fun smoothClick(context: Context) = vibrate(context, 10)

    fun mediumClick(context: Context) = vibrate(context, 15)
    
    fun heavyClick(context: Context) = vibrate(context, 20)

    fun selection(context: Context) = vibrate(context, 10)

    fun success(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 15, 40, 5)
            val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    /** A short bell-like jingle: tap, tap, longer tap. */
    fun jingle(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 25, 70, 25, 70, 60)
            val amplitudes = intArrayOf(0, 180, 0, 180, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60)
        }
    }

    fun error(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 20, 45, 15)
            val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    /** Vibrates for snow sliding off a card or being shaken off, scaled by how much snow there was. */
    fun snow(context: Context, event: SnowHapticEvent) {
        val pattern = when (event) {
            is SnowHapticEvent.Slide -> SnowHaptics.slide(event.depth)
            is SnowHapticEvent.Shake -> SnowHaptics.shake(event.totalDepth)
        }
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern.totalMillis.coerceAtMost(LEGACY_MAX_MILLIS))
            return
        }
        if (!vibrator.hasAmplitudeControl()) {
            // Without strength control, a plain on-off pulse of the same length still gives the beat.
            vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings.toLongArray(), -1))
            return
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings.toLongArray(), pattern.amplitudes.toIntArray(), -1))
    }

    private const val LEGACY_MAX_MILLIS = 80L
}
