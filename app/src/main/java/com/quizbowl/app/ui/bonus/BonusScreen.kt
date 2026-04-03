package com.quizbowl.app.ui.bonus

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizbowl.app.ui.components.BonusSettingsPanel
import com.quizbowl.app.ui.theme.qbColors
import com.quizbowl.app.util.PartResult
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BonusScreen(navController: NavController) {
    val vm: BonusViewModel = viewModel()
    val context = LocalContext.current

    val phase by vm.phase.collectAsStateWithLifecycle()
    val bonus by vm.bonus.collectAsStateWithLifecycle()
    val leadinWords by vm.leadinWords.collectAsStateWithLifecycle()
    val currentPart by vm.currentPart.collectAsStateWithLifecycle()
    val partWords by vm.partWords.collectAsStateWithLifecycle()
    val partResults by vm.partResults.collectAsStateWithLifecycle()
    val score by vm.score.collectAsStateWithLifecycle()
    val wordIndex by vm.tts.wordIndex.collectAsStateWithLifecycle()
    val ttsDone by vm.tts.done.collectAsStateWithLifecycle()
    val ttsPaused by vm.tts.paused.collectAsStateWithLifecycle()
    val answer by vm.answer.collectAsStateWithLifecycle()
    val answerCountdown by vm.answerCountdown.collectAsStateWithLifecycle()
    val listening by vm.listening.collectAsStateWithLifecycle()
    val voiceDisabled by vm.voiceDisabled.collectAsStateWithLifecycle()
    val directedPrompt by vm.directedPrompt.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val voices by vm.tts.voices.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(phase) {
        if (phase == BonusPhase.ANSWERING || phase == BonusPhase.PROMPTING) {
            delay(150)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(wordIndex, currentPart, partResults.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bonus Practice") },
                navigationIcon = {
                    TextButton(onClick = { navController.navigateUp() }) {
                        Text("←", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.qbColors.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BonusSettingsPanel(
                settings = settings,
                voices = voices,
                onSettingsChange = { vm.saveSettings(it) },
            )

            // Scoreboard
            BonusScoreRow(score = score)

            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.qbColors.redDim),
                ) {
                    Text(
                        text = error!!,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.qbColors.red,
                        fontSize = 13.sp,
                    )
                }
            }

            // Question area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (bonus != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Metadata chips
                        val metaItems = listOfNotNull(
                            bonus!!.category,
                            bonus!!.subcategory,
                            bonus!!.difficulty?.let { "Diff $it" },
                            bonus!!.set?.name,
                        )
                        if (metaItems.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                metaItems.forEach { item ->
                                    SuggestionChip(onClick = {}, label = { Text(item, fontSize = 11.sp) })
                                }
                            }
                        }

                        // Leadin (shown from READING_LEADIN onwards)
                        if (phase != BonusPhase.IDLE) {
                            LeadinText(
                                words = leadinWords,
                                wordIndex = wordIndex,
                                isReading = phase == BonusPhase.READING_LEADIN,
                                ttsDone = ttsDone && phase == BonusPhase.READING_LEADIN,
                            )
                        }

                        // Part cards (show parts as they're reached)
                        val partsReached = phase != BonusPhase.IDLE && phase != BonusPhase.READING_LEADIN
                        if (partsReached) {
                            repeat(3) { i ->
                                val isCurrentPart = currentPart == i
                                val isReached = i < partResults.size || isCurrentPart
                                if (isReached) {
                                    val b = bonus!!
                                    val thisPartWords = if (isCurrentPart) {
                                        partWords
                                    } else {
                                        val text = b.partsSanitized?.getOrNull(i) ?: b.parts.getOrNull(i) ?: ""
                                        text.split(Regex("\\s+")).filter { it.isNotEmpty() }
                                    }
                                    val result = partResults.getOrNull(i)
                                    val correctAnswer = b.answersSanitized?.getOrNull(i) ?: b.answers.getOrNull(i) ?: ""
                                    BonusPartCard(
                                        index = i,
                                        words = thisPartWords,
                                        wordIndex = if (isCurrentPart) wordIndex else thisPartWords.size - 1,
                                        isActiveReading = isCurrentPart && phase == BonusPhase.READING_PART,
                                        result = result,
                                        correctAnswer = correctAnswer,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Phase controls
            BonusPhaseControls(
                phase = phase,
                loading = loading,
                ttsPaused = ttsPaused,
                answerCountdown = answerCountdown,
                answer = answer,
                listening = listening,
                voiceDisabled = voiceDisabled,
                speechSupported = vm.speechSupported,
                directedPrompt = directedPrompt,
                partResults = partResults,
                focusRequester = focusRequester,
                onStart = { vm.fetchBonus() },
                onPause = { vm.pauseTts() },
                onResume = { vm.resumeTts() },
                onAnswerChange = { vm.updateAnswer(it) },
                onDisableVoice = { vm.disableVoiceIfActive() },
                onStartVoice = { vm.startVoice() },
                onSubmit = { vm.submitAnswer() },
                onNext = { vm.fetchBonus() },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BonusScoreRow(score: com.quizbowl.app.util.BonusScore) {
    val qbColors = MaterialTheme.qbColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = qbColors.surface),
        border = BorderStroke(1.dp, qbColors.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatItem("Total", "${score.total}")
            StatItem("Bonuses", "${score.bonuses}")
            StatItem("30s", "${score.thirties}")
            if (score.bonuses > 0) StatItem("PPB", "${"%.1f".format(score.ppb)}")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.qbColors.textMuted)
    }
}

@Composable
private fun LeadinText(
    words: List<String>,
    wordIndex: Int,
    isReading: Boolean,
    ttsDone: Boolean,
) {
    val qbColors = MaterialTheme.qbColors
    val visibleUpTo = when {
        isReading && !ttsDone -> wordIndex
        else -> words.size - 1
    }
    val annotated = buildAnnotatedString {
        words.forEachIndexed { i, word ->
            if (i <= visibleUpTo) {
                val isHighlighted = i == wordIndex && isReading && !ttsDone
                if (isHighlighted) {
                    withStyle(SpanStyle(background = qbColors.primaryGlow, color = qbColors.primary)) {
                        append(word)
                    }
                } else {
                    append(word)
                }
                append(' ')
            }
        }
    }
    Text(
        text = annotated,
        fontSize = 15.sp,
        fontStyle = FontStyle.Italic,
        lineHeight = 24.sp,
        color = qbColors.textMuted,
    )
}

@Composable
private fun BonusPartCard(
    index: Int,
    words: List<String>,
    wordIndex: Int,
    isActiveReading: Boolean,
    result: PartResult?,
    correctAnswer: String,
) {
    val qbColors = MaterialTheme.qbColors
    val isAnswered = result != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAnswered && result!!.correct -> qbColors.greenDim
                isAnswered -> qbColors.redDim
                else -> qbColors.surface
            }
        ),
        border = BorderStroke(
            width = if (!isAnswered) 1.dp else 0.dp,
            color = if (isActiveReading || (!isAnswered && wordIndex >= 0)) qbColors.primary else qbColors.border,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Part ${index + 1} (10 pts)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = qbColors.textMuted,
            )

            // Part text
            val visibleUpTo = when {
                isActiveReading -> wordIndex
                else -> words.size - 1
            }
            if (visibleUpTo >= 0) {
                val annotated = buildAnnotatedString {
                    words.forEachIndexed { i, word ->
                        if (i <= visibleUpTo) {
                            val isHighlighted = i == wordIndex && isActiveReading
                            if (isHighlighted) {
                                withStyle(SpanStyle(background = qbColors.primaryGlow, color = qbColors.primary)) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                            append(' ')
                        }
                    }
                }
                Text(text = annotated, fontSize = 15.sp, lineHeight = 24.sp)
            }

            // Result
            if (isAnswered) {
                val r = result!!
                Text(
                    text = if (r.correct) "Correct! +10" else "Incorrect. 0 pts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (r.correct) qbColors.green else qbColors.red,
                )
                if (r.userAnswer.isNotEmpty()) {
                    Text(
                        text = "Your answer: ${r.userAnswer}",
                        fontSize = 12.sp,
                        color = qbColors.textMuted,
                    )
                }
                Text(
                    text = "Answer: $correctAnswer",
                    fontSize = 12.sp,
                    color = qbColors.primary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phase controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BonusPhaseControls(
    phase: BonusPhase,
    loading: Boolean,
    ttsPaused: Boolean,
    answerCountdown: Float?,
    answer: String,
    listening: Boolean,
    voiceDisabled: Boolean,
    speechSupported: Boolean,
    directedPrompt: String?,
    partResults: List<PartResult>,
    focusRequester: FocusRequester,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAnswerChange: (String) -> Unit,
    onDisableVoice: () -> Unit,
    onStartVoice: () -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
) {
    val qbColors = MaterialTheme.qbColors

    when (phase) {
        BonusPhase.IDLE -> {
            Button(
                onClick = onStart,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (loading) "Loading…" else "Start")
            }
        }

        BonusPhase.READING_LEADIN, BonusPhase.READING_PART -> {
            OutlinedButton(
                onClick = if (ttsPaused) onResume else onPause,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ttsPaused) "Resume" else "Pause")
            }
        }

        BonusPhase.ANSWERING, BonusPhase.PROMPTING -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (answerCountdown != null) {
                    Text(
                        text = "Time: ${"%.1f".format(answerCountdown)}s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (answerCountdown <= 1f) qbColors.red else qbColors.accentAmber,
                    )
                }

                if (phase == BonusPhase.PROMPTING && directedPrompt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = qbColors.amberDim),
                    ) {
                        Text(
                            text = "Prompt: $directedPrompt",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = qbColors.accentAmber,
                            fontSize = 13.sp,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { newText ->
                            onAnswerChange(newText)
                            onDisableVoice()
                        },
                        placeholder = {
                            Text(
                                text = if (listening) "Listening… (or type)" else "Type your answer…",
                                fontSize = 14.sp,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                    onSubmit(); true
                                } else false
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    )
                    if (speechSupported && !voiceDisabled) {
                        OutlinedButton(
                            onClick = { if (!listening) onStartVoice() },
                            modifier = Modifier.size(56.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = if (listening)
                                ButtonDefaults.outlinedButtonColors(containerColor = qbColors.primaryGlow)
                            else ButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text("🎤", fontSize = 20.sp)
                        }
                    }
                }

                Button(
                    onClick = onSubmit,
                    enabled = answer.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Submit")
                }
            }
        }

        BonusPhase.PART_RESULT -> {
            // Auto-advancing — nothing needed
        }

        BonusPhase.DONE -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val total = partResults.sumOf { it.points }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            total == 30 -> qbColors.greenDim
                            total == 0 -> qbColors.redDim
                            else -> qbColors.surface
                        }
                    ),
                ) {
                    Text(
                        text = "Bonus Score: $total / 30",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = when {
                            total == 30 -> qbColors.green
                            total == 0 -> qbColors.red
                            else -> qbColors.primary
                        },
                    )
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Next Bonus")
                }
            }
        }
    }
}
