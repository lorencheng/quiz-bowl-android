package com.quizbowl.app.ui.tossup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizbowl.app.ui.components.QuestionText
import com.quizbowl.app.ui.components.ScoreBoard
import com.quizbowl.app.ui.components.SettingsPanel
import androidx.compose.material3.MaterialTheme
import com.quizbowl.app.ui.theme.qbColors
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TossupScreen(navController: NavController) {
    val vm: TossupViewModel = viewModel()
    val context = LocalContext.current

    // ── State collection ──────────────────────────────────────────────────────
    val phase by vm.phase.collectAsStateWithLifecycle()
    val tossup by vm.tossup.collectAsStateWithLifecycle()
    val words by vm.words.collectAsStateWithLifecycle()
    val buzzIndex by vm.buzzIndex.collectAsStateWithLifecycle()
    val wordIndex by vm.tts.wordIndex.collectAsStateWithLifecycle()
    val ttsDone by vm.tts.done.collectAsStateWithLifecycle()
    val ttsPaused by vm.tts.paused.collectAsStateWithLifecycle()
    val answer by vm.answer.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val score by vm.score.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val buzzCountdown by vm.buzzCountdown.collectAsStateWithLifecycle()
    val answerCountdown by vm.answerCountdown.collectAsStateWithLifecycle()
    val listening by vm.listening.collectAsStateWithLifecycle()
    val voiceDisabled by vm.voiceDisabled.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val voices by vm.tts.voices.collectAsStateWithLifecycle()

    // ── Request RECORD_AUDIO permission on screen open ────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Voice will work if granted; buzzing still works if denied */ }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Auto-focus answer field when entering BUZZING ─────────────────────────
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(phase) {
        if (phase == TossupPhase.BUZZING) {
            delay(150)
            try { focusRequester.requestFocus() } catch (_: Exception) { }
        }
    }

    // ── Auto-scroll question text to bottom as words appear ──────────────────
    val questionScrollState = rememberScrollState()
    LaunchedEffect(wordIndex, buzzIndex) {
        questionScrollState.animateScrollTo(questionScrollState.maxValue)
    }

    // ── Answer to display in RESULT ───────────────────────────────────────────
    val correctAnswer = tossup?.let { t ->
        t.answerSanitized ?: t.answer.replace(Regex("<[^>]+>"), "")
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tossup Practice") },
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
            // Settings
            SettingsPanel(
                settings = settings,
                voices = voices,
                onSettingsChange = { vm.saveSettings(it) },
            )

            // Score
            ScoreBoard(score = score)

            // Error banner
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

            // ── Question area (takes remaining vertical space) ────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (tossup != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(questionScrollState),
                    ) {
                        // Metadata chips
                        val metaItems = listOfNotNull(
                            tossup!!.category,
                            tossup!!.subcategory,
                            tossup!!.difficulty?.let { "Diff $it" },
                            tossup!!.set?.name,
                        )
                        if (metaItems.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                metaItems.forEach { item ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(item, fontSize = 11.sp) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Word-by-word question text
                        QuestionText(
                            words = words,
                            wordIndex = wordIndex,
                            buzzIndex = buzzIndex,
                            phase = phase,
                            ttsDone = ttsDone,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Correct answer (RESULT phase)
                        if (phase == TossupPhase.RESULT && correctAnswer != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Answer: $correctAnswer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.qbColors.primary,
                            )
                        }
                    }
                }
            }

            // ── Phase-dependent controls ──────────────────────────────────────
            PhaseControls(
                phase = phase,
                loading = loading,
                ttsPaused = ttsPaused,
                buzzCountdown = buzzCountdown,
                answerCountdown = answerCountdown,
                answer = answer,
                result = result,
                listening = listening,
                voiceDisabled = voiceDisabled,
                speechSupported = vm.speechSupported,
                focusRequester = focusRequester,
                onStart = { vm.fetchTossup() },
                onBuzz = { vm.buzz() },
                onPause = { vm.pauseTts() },
                onResume = { vm.resumeTts() },
                onAnswerChange = { vm.updateAnswer(it) },
                onDisableVoice = { vm.disableVoiceIfActive() },
                onStartVoice = { vm.startVoice() },
                onSubmit = { vm.submitAnswer() },
                onNext = { vm.fetchTossup() },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phase-specific controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhaseControls(
    phase: TossupPhase,
    loading: Boolean,
    ttsPaused: Boolean,
    buzzCountdown: Float?,
    answerCountdown: Float?,
    answer: String,
    result: TossupResult?,
    listening: Boolean,
    voiceDisabled: Boolean,
    speechSupported: Boolean,
    focusRequester: FocusRequester,
    onStart: () -> Unit,
    onBuzz: () -> Unit,
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
        // ── IDLE ──────────────────────────────────────────────────────────────
        TossupPhase.IDLE -> {
            Button(
                onClick = onStart,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (loading) "Loading…" else "Start")
            }
        }

        // ── READING ───────────────────────────────────────────────────────────
        TossupPhase.READING -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onBuzz,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = qbColors.accentTeal,
                        ),
                    ) {
                        Text("Buzz!", fontWeight = FontWeight.Bold)
                    }
                    if (buzzCountdown != null) {
                        Text(
                            text = "%.1f".format(buzzCountdown),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (buzzCountdown <= 2f) qbColors.red else qbColors.accentAmber,
                        )
                    }
                }
                OutlinedButton(
                    onClick = if (ttsPaused) onResume else onPause,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (ttsPaused) "Resume" else "Pause")
                }
            }
        }

        // ── BUZZING ───────────────────────────────────────────────────────────
        TossupPhase.BUZZING -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Answer timer countdown
                if (answerCountdown != null) {
                    Text(
                        text = "Time: ${"%.1f".format(answerCountdown)}s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (answerCountdown <= 1f) qbColors.red else qbColors.accentAmber,
                    )
                }

                // Prompt hint (API asked for more specificity)
                if (result?.directive == "prompt" && result.directedPrompt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = qbColors.amberDim),
                    ) {
                        Text(
                            text = "Prompt: ${result.directedPrompt}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = qbColors.accentAmber,
                            fontSize = 13.sp,
                        )
                    }
                }

                // Answer input + mic button
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
                                    onSubmit()
                                    true
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
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.qbColors.primaryGlow,
                                )
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

        // ── RESULT ────────────────────────────────────────────────────────────
        TossupPhase.RESULT -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result != null) {
                    val isCorrect = result.points > 0
                    val isWrong = result.points < 0
                    val (bannerTextColor, bannerBgColor) = when {
                        isCorrect -> qbColors.green to qbColors.greenDim
                        isWrong -> qbColors.red to qbColors.redDim
                        else -> qbColors.textMuted to qbColors.surface2
                    }
                    val bannerText = when {
                        result.timedOut == "buzz" ->
                            "Didn't buzz in time. 0 points."
                        result.timedOut == "answer" && result.points < 0 ->
                            "Didn't answer in time. ${result.points} points."
                        result.timedOut == "answer" ->
                            "Didn't answer in time. 0 points."
                        isCorrect ->
                            "Correct!${if (result.isPower) " (POWER!)" else ""} +${result.points} points."
                        isWrong ->
                            "Wrong answer. ${result.points} points."
                        else -> "No points."
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = bannerBgColor),
                    ) {
                        Text(
                            text = bannerText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = bannerTextColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }

                    if (result.userAnswer != null) {
                        Text(
                            text = "Your answer: ${result.userAnswer}",
                            fontSize = 13.sp,
                            color = qbColors.textMuted,
                        )
                    }
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Next")
                }
            }
        }
    }
}
