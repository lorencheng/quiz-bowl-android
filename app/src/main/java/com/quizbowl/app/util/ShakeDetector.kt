package com.quizbowl.app.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects shake gestures via the accelerometer.
 * [onShake] is called on the sensor callback thread — callers must dispatch to the main thread
 * before touching UI state (e.g. `runOnUiThread { ... }`).
 *
 * [threshold] — minimum G-force multiple to count as a shake (default 2.5×G).
 * [cooldownMs] — minimum ms between triggers so one shake doesn't fire multiple times (default 30s).
 */
class ShakeDetector(
    private val onShake: () -> Unit,
    private val threshold: Float = 2.5f,
    private val cooldownMs: Long = 30_000L,
) : SensorEventListener {

    private var lastShakeTime = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (gForce > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > cooldownMs) {
                lastShakeTime = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
