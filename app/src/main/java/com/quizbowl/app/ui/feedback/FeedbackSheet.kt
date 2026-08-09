package com.quizbowl.app.ui.feedback

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizbowl.app.data.FeedbackRepository
import com.quizbowl.app.ui.theme.qbColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    screenshot: Bitmap?,
    onDismiss: () -> Unit,
) {
    val qbColors = MaterialTheme.qbColors
    val scope = rememberCoroutineScope()
    var feedbackText by remember { mutableStateOf("") }
    var includeScreenshot by remember { mutableStateOf(screenshot != null) }
    var submitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = qbColors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = null,
                    tint = qbColors.accentAmber,
                )
                Text("Send Feedback", style = MaterialTheme.typography.titleMedium)
            }

            if (submitted) {
                Text("Thanks! Your feedback was sent.", color = qbColors.green, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            } else {
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    placeholder = { Text("What happened? What were you doing?", fontSize = 14.sp) },
                    maxLines = 6,
                )

                if (screenshot != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Include screenshot", fontSize = 14.sp, color = qbColors.textMuted)
                        Switch(checked = includeScreenshot, onCheckedChange = { includeScreenshot = it })
                    }
                    if (includeScreenshot) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            colors = CardDefaults.cardColors(containerColor = qbColors.surface2),
                        ) {
                            Image(
                                bitmap = screenshot.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = qbColors.red, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        scope.launch {
                            submitting = true
                            errorMsg = null
                            FeedbackRepository.submit(
                                feedbackText = feedbackText,
                                screenshot = if (includeScreenshot) screenshot else null,
                            ).onSuccess {
                                submitted = true
                            }.onFailure {
                                errorMsg = "Failed to send. Please try again."
                            }
                            submitting = false
                        }
                    },
                    enabled = feedbackText.isNotBlank() && !submitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = qbColors.bg,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (submitting) "Sending…" else "Send")
                }
            }
        }
    }
}
