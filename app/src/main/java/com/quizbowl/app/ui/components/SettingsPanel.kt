package com.quizbowl.app.ui.components

import android.speech.tts.Voice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.quizbowl.app.ui.theme.qbColors
import java.util.Locale

private val DIFF_CHIP_LABELS = listOf(
    0 to "Unrated", 1 to "MS", 2 to "E.HS", 3 to "HS", 4 to "H.HS",
    5 to "E.Col", 6 to "Col", 7 to "H.Col", 8 to "E.Open", 9 to "Open", 10 to "Nat'l",
)

/**
 * Convert a raw TTS voice name (e.g. "en-us-x-iom-local") into something
 * a human can read (e.g. "English (US) · HD").
 *
 * Rules:
 *  - Base: language + country from voice.locale
 *  - Append "· Online" if network required
 *  - Append "· HD" if quality >= QUALITY_HIGH and local
 *  - Append index suffix if multiple voices share the same base label
 */
private fun buildVoiceDisplayNames(voices: List<Voice>): Map<Voice, String> {
    // Build the base label for each voice
    fun Voice.baseLabel(): String {
        val lang = locale.getDisplayLanguage(Locale.ENGLISH)
        val country = locale.getDisplayCountry(Locale.ENGLISH)
        val base = if (country.isNotEmpty()) "$lang ($country)" else lang
        return when {
            isNetworkConnectionRequired -> "$base · Online"
            quality >= Voice.QUALITY_HIGH -> "$base · HD"
            else -> base
        }
    }

    // Count how many voices share each base label
    val labelCount = mutableMapOf<String, Int>()
    voices.forEach { labelCount[it.baseLabel()] = (labelCount[it.baseLabel()] ?: 0) + 1 }

    // Assign unique display names, appending "2", "3", … for duplicates
    val labelIndex = mutableMapOf<String, Int>()
    return voices.associateWith { voice ->
        val base = voice.baseLabel()
        if ((labelCount[base] ?: 0) > 1) {
            val n = (labelIndex[base] ?: 1)
            labelIndex[base] = n + 1
            "$base $n"
        } else {
            base
        }
    }
}

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

    // Local slider state — updates instantly on drag, persists only on release
    var localRate by remember(settings.rate) { mutableFloatStateOf(settings.rate) }
    var localBuzzTimer by remember(settings.buzzTimer) { mutableFloatStateOf(settings.buzzTimer) }
    var localAnswerTimer by remember(settings.answerTimer) { mutableFloatStateOf(settings.answerTimer) }

    // Build readable voice names once per voice list change
    val englishVoices = remember(voices) {
        voices.filter { it.locale.language.startsWith("en") }
    }
    val voiceDisplayNames = remember(englishVoices) {
        buildVoiceDisplayNames(englishVoices)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = qbColors.surface),
        border = BorderStroke(1.dp, qbColors.border),
    ) {
        Column {
            // ── Header ────────────────────────────────────────────────────────
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

            // ── Expanded content — scrollable so it never overflows ───────────
            // Use if() instead of AnimatedVisibility to avoid animation-clipping bugs
            if (expanded) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)          // bound height
                        .verticalScroll(scrollState)     // scroll if content overflows
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {

                    // ── Voice ─────────────────────────────────────────────────
                    if (englishVoices.isNotEmpty()) {
                        val currentDisplay = voiceDisplayNames[
                            englishVoices.find { it.name == settings.voiceName }
                        ] ?: "Default"
                        SectionLabel("Voice")
                        var voiceMenuOpen by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { voiceMenuOpen = !voiceMenuOpen },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = currentDisplay,
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
                                        text = {
                                            Text(
                                                voiceDisplayNames[voice] ?: voice.name,
                                                fontSize = 13.sp,
                                            )
                                        },
                                        onClick = {
                                            onSettingsChange(settings.copy(voiceName = voice.name))
                                            voiceMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // ── Speed ─────────────────────────────────────────────────
                    CompactSlider(
                        label = "Speed",
                        valueText = "${"%.1f".format(localRate)}×",
                        value = localRate,
                        onValueChange = { localRate = it },
                        onValueChangeFinished = { onSettingsChange(settings.copy(rate = localRate)) },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                    )

                    // ── Buzz timer ────────────────────────────────────────────
                    CompactSlider(
                        label = "Buzz timer",
                        valueText = if (localBuzzTimer == 0f) "Off" else "${"%.1f".format(localBuzzTimer)}s",
                        value = localBuzzTimer,
                        onValueChange = { localBuzzTimer = it },
                        onValueChangeFinished = { onSettingsChange(settings.copy(buzzTimer = localBuzzTimer)) },
                        valueRange = 0f..15f,
                        steps = 29,
                    )

                    // ── Answer timer ──────────────────────────────────────────
                    CompactSlider(
                        label = "Answer timer",
                        valueText = if (localAnswerTimer == 0f) "Off" else "${"%.1f".format(localAnswerTimer)}s",
                        value = localAnswerTimer,
                        onValueChange = { localAnswerTimer = it },
                        onValueChangeFinished = { onSettingsChange(settings.copy(answerTimer = localAnswerTimer)) },
                        valueRange = 0f..15f,
                        steps = 29,
                    )

                    // ── Categories ────────────────────────────────────────────
                    SectionLabel(
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

                    // ── Difficulties ──────────────────────────────────────────
                    SectionLabel(
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

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.qbColors.textMuted)
}

/** Label + current value on one line, slider on the next — compact vertical footprint. */
@Composable
private fun CompactSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    val textMuted = MaterialTheme.qbColors.textMuted
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontSize = 12.sp, color = textMuted)
            Text(valueText, fontSize = 12.sp, color = textMuted)
        }
        // Disable the 48dp minimum touch-target enforcement so we can set a short height.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
            )
        }
    }
}
