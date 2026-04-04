package com.quizbowl.app

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.quizbowl.app.navigation.NavGraph
import com.quizbowl.app.ui.theme.QuizBowlTheme
import com.quizbowl.app.ui.theme.qbColors
import com.quizbowl.app.util.ShakeDetector

private const val FEEDBACK_URL = "https://feedback.example.com"  // TODO: replace with real URL

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private val showShakeDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(
            onShake = { runOnUiThread { showShakeDialog.value = true } },
        )

        setContent {
            QuizBowlTheme {
                NavGraph()

                // Shake feedback dialog — composited on top of everything
                if (showShakeDialog.value) {
                    ShakeFeedbackDialog(
                        onFeedback = {
                            showShakeDialog.value = false
                            openUrl(this, FEEDBACK_URL)
                        },
                        onDismiss = { showShakeDialog.value = false },
                    )
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
}

@Composable
private fun ShakeFeedbackDialog(
    onFeedback: () -> Unit,
    onDismiss: () -> Unit,
) {
    val qbColors = MaterialTheme.qbColors
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.BugReport,
                contentDescription = null,
                tint = qbColors.accentAmber,
            )
        },
        title = { Text("Hit a snag?") },
        text = {
            Text(
                text = "Shake detected — want to send feedback?",
                color = qbColors.textMuted,
            )
        },
        confirmButton = {
            TextButton(onClick = onFeedback) {
                Text("Send Feedback", color = qbColors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = qbColors.textMuted)
            }
        },
        containerColor = qbColors.surface,
    )
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
