package com.quizbowl.app.ui.components

import android.speech.tts.Voice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizbowl.app.api.QbReaderService
import com.quizbowl.app.data.TossupSettings
import androidx.compose.material3.MaterialTheme
import com.quizbowl.app.ui.theme.qbColors

private val DIFF_CHIP_LABELS = listOf(
    0 to "Unrated", 1 to "MS", 2 to "E.HS", 3 to "HS", 4 to "H.HS",
    5 to "E.Col", 6 to "Col", 7 to "H.Col", 8 to "E.Open", 9 to "Open", 10 to "Nat'l",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(
    settings: TossupSettings,
    voices: List<Voice>,
    onSettingsChange: (TossupSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val qbColors = MaterialTheme.qbColors
    var expanded by remember { mutableStateOf(false) }

    // Local slider state for smooth dragging (save to DataStore only on release)
    var localRate by remember(settings.rate) { mutableFloatStateOf(settings.rate) }
    var localBuzzTimer by remember(settings.buzzTimer) { mutableFloatStateOf(settings.buzzTimer) }
    var localAnswerTimer by remember(settings.answerTimer) { mutableFloatStateOf(settings.answerTimer) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = qbColors.surface),
        border = BorderStroke(1.dp, qbColors.border),
    ) {
        Column {
            // ── Header row (tap to expand/collapse) ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Settings", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 11.sp,
                    color = qbColors.textMuted,
                )
            }

            // ── Expanded content ──────────────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    // Voice selector
                    val englishVoices = voices.filter { it.locale.language.startsWith("en") }
                    if (englishVoices.isNotEmpty()) {
                        val currentVoice = englishVoices.find { it.name == settings.voiceName }
                        SettingLabel("Voice")
                        var voiceMenuOpen by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { voiceMenuOpen = !voiceMenuOpen },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = currentVoice?.name ?: "Default",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 13.sp,
                                )
                            }
                            DropdownMenu(
                                expanded = voiceMenuOpen,
                                onDismissRequest = { voiceMenuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Default") },
                                    onClick = {
                                        onSettingsChange(settings.copy(voiceName = null))
                                        voiceMenuOpen = false
                                    },
                                )
                                englishVoices.forEach { voice ->
                                    DropdownMenuItem(
                                        text = { Text(voice.name, fontSize = 13.sp) },
                                        onClick = {
                                            onSettingsChange(settings.copy(voiceName = voice.name))
                                            voiceMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Speed
                    SettingLabel("Speed: ${"%.1f".format(localRate)}×")
                    Slider(
                        value = localRate,
                        onValueChange = { localRate = it },
                        onValueChangeFinished = { onSettingsChange(settings.copy(rate = localRate)) },
                        valueRange = 0.5f..2.0f,
                        steps = 14, // 16 snap points: 0.5, 0.6, … 2.0
                    )

                    // Buzz timer
                    SettingLabel(
                        "Buzz timer: ${if (localBuzzTimer == 0f) "Off" else "${"%.1f".format(localBuzzTimer)}s"}"
                    )
                    Slider(
                        value = localBuzzTimer,
                        onValueChange = { localBuzzTimer = it },
                        onValueChangeFinished = { onSettingsChange(settings.copy(buzzTimer = localBuzzTimer)) },
                        valueRange = 0f..15f,
                        steps = 29, // 0.5-step increments
                    )

                    // Answer timer
                    SettingLabel(
                        "Answer timer: ${if (localAnswerTimer == 0f) "Off" else "${"%.1f".format(localAnswerTimer)}s"}"
                    )
                    Slider(
                        value = localAnswerTimer,
                        onValueChange = { localAnswerTimer = it },
                        onValueChangeFinished = { onSettingsChange(settings.copy(answerTimer = localAnswerTimer)) },
                        valueRange = 0f..15f,
                        steps = 29,
                    )

                    // Categories
                    SettingLabel(
                        "Categories${if (settings.categories.isEmpty()) " (all)" else " (${settings.categories.size})"}"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        QbReaderService.categories.forEach { cat ->
                            val selected = cat in settings.categories
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) settings.categories.filter { it != cat }
                                               else settings.categories + cat
                                    onSettingsChange(settings.copy(categories = next))
                                },
                                label = { Text(cat, fontSize = 12.sp) },
                            )
                        }
                    }

                    // Difficulties
                    SettingLabel(
                        "Difficulties${if (settings.difficulties.isEmpty()) " (all)" else " (${settings.difficulties.size})"}"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        DIFF_CHIP_LABELS.forEach { (value, label) ->
                            val selected = value in settings.difficulties
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) settings.difficulties.filter { it != value }
                                               else settings.difficulties + value
                                    onSettingsChange(settings.copy(difficulties = next))
                                },
                                label = { Text(label, fontSize = 12.sp) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, fontSize = 13.sp, color = MaterialTheme.qbColors.textMuted)
}
