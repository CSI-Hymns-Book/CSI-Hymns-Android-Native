package com.reyzie.hymns.ui.christmas.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.reyzie.hymns.ui.christmas.ChristmasAmbienceController
import com.reyzie.hymns.ui.christmas.ChristmasEffectsLevel

private const val NANOS_PER_MILLI = 1_000_000L

/**
 * Feeds phone tilt and shakes into the Christmas effects. Only listens when effects are on
 * setting, and only while the app is in the foreground, to save battery.
 */
@Composable
fun ChristmasDeviceMotion(controller: ChristmasAmbienceController) {
    if (controller.effectiveLevel == ChristmasEffectsLevel.OFF) return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return@DisposableEffect onDispose { }
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val shakeDetector = ShakeDetector()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                when (event.sensor.type) {
                    Sensor.TYPE_GRAVITY -> controller.setDeviceTilt(rollDegrees(x, y, z))
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (gravity == null) controller.setDeviceTilt(rollDegrees(x, y, z))
                        if (shakeDetector.onAcceleration(x, y, z, event.timestamp / NANOS_PER_MILLI)) {
                            controller.shake()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun start() {
            gravity?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }

        fun stop() {
            sensorManager.unregisterListener(listener)
            controller.setDeviceTilt(0f)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }
}
