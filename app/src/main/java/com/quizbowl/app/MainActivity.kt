package com.quizbowl.app

import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.quizbowl.app.data.FeedbackManager
import com.quizbowl.app.data.LocalFeedbackManager
import com.quizbowl.app.navigation.NavGraph
import com.quizbowl.app.ui.feedback.FeedbackSheet
import com.quizbowl.app.ui.theme.QuizBowlTheme
import com.quizbowl.app.util.ShakeDetector

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private val feedbackManager = FeedbackManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(
            onShake = {
                runOnUiThread {
                    captureScreenshot { bitmap ->
                        feedbackManager.open(bitmap)
                    }
                }
            },
        )

        setContent {
            QuizBowlTheme {
                CompositionLocalProvider(LocalFeedbackManager provides feedbackManager) {
                    NavGraph()

                    if (feedbackManager.isOpen.value) {
                        FeedbackSheet(
                            screenshot = feedbackManager.screenshot.value,
                            onDismiss = { feedbackManager.dismiss() },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(shakeDetector, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }

    private fun captureScreenshot(callback: (Bitmap?) -> Unit) {
        val decorView = window.decorView
        if (decorView.width == 0 || decorView.height == 0) {
            callback(null)
            return
        }
        val bitmap = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(window, bitmap, { result ->
            callback(if (result == PixelCopy.SUCCESS) bitmap else null)
        }, Handler(Looper.getMainLooper()))
    }
}
