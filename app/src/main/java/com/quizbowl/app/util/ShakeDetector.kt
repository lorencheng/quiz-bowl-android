package com.quizbowl.app.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects shake gestures via the accelerometer.
 * Requires [minPeaks] G-force threshold crossings within [windowMs] milliseconds to fire,
 * which eliminates false positives from taps (single-spike events).
 * [onShake] is called on the sensor callback thread — callers must dispatch to main thread.
 */
class ShakeDetector(
    private val onShake: () -> Unit,
    private val threshold: Float = 3.2f,
    private val minPeaks: Int = 3,
    private val windowMs: Long = 600L,
    private val cooldownMs: Long = 30_000L,
) : SensorEventListener {

    private val peakTimes = ArrayDeque<Long>()
    private var lastShakeTime = 0L
    private var lastPeakTime = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

        if (gForce > threshold) {
            val now = System.currentTimeMillis()
            // Debounce: a single physical spike produces many adjacent sensor readings
            if (now - lastPeakTime < 80) return
            lastPeakTime = now

            peakTimes.addLast(now)
            while (peakTimes.isNotEmpty() && now - peakTimes.first() > windowMs) {
                peakTimes.removeFirst()
            }
            if (peakTimes.size >= minPeaks && now - lastShakeTime > cooldownMs) {
                lastShakeTime = now
                peakTimes.clear()
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
