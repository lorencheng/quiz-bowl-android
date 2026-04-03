package com.quizbowl.app.ui.tossup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizbowl.app.data.TossupSettings
import com.quizbowl.app.ui.components.QuestionText
import com.quizbowl.app.ui.components.ScoreBoard
import com.quizbowl.app.ui.components.SettingsPanel
import com.quizbowl.app.ui.theme.QuizBowlColors
import com.quizbowl.app.ui.theme.qbColors
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

    // ── Correct answer for RESULT display ────────────────────────────────────
    val correctAnswer = tossup?.let { t ->
        t.answerSanitized ?: t.answer.replace(Regex("<[^>]+>"), "")
    }

    // ── Settings bottom sheet ─────────────────────────────────────────────────
    var showSettings by remember { mutableStateOf(false) }

    // ── Screen flash on RESULT ────────────────────────────────────────────────
    var flashActive by remember { mutableStateOf(false) }
    val isCorrectResult = result != null && (result!!.points > 0)
    val flashAlpha by animateFloatAsState(
        targetValue = if (flashActive) 0.22f else 0f,
        animationSpec = tween(350),
        label = "resultFlash",
    )
    LaunchedEffect(phase) {
        if (phase == TossupPhase.RESULT) {
            flashActive = true
            delay(550)
            flashActive = false
        }
    }

    val qbColors = MaterialTheme.qbColors

    Scaffold(
        containerColor = qbColors.bg,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // ── Custom header ─────────────────────────────────────────────
                GameHeader(
                    onBack = { navController.navigateUp() },
                    onSettings = { showSettings = true },
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )

                // ── Score strip ───────────────────────────────────────────────
                ScoreBoard(score = score)

                Spacer(modifier = Modifier.height(8.dp))

                // ── Error banner ──────────────────────────────────────────────
                if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = qbColors.redDim),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = error!!,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = qbColors.red,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Question card (fills remaining vertical space) ────────────
                val cardBorderBrush = Brush.linearGradient(
                    listOf(qbColors.primary, qbColors.accentTeal)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.5.dp, cardBorderBrush, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = qbColors.surface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        if (tossup != null) {
                            // Category + subcategory + difficulty badges
                            val category = tossup!!.category
                            val subcategory = tossup!!.subcategory
                            val difficulty = tossup!!.difficulty

                            if (category != null || subcategory != null || difficulty != null) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (category != null) {
                                        val badgeColor = categoryAccentColor(category, qbColors)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(badgeColor.copy(alpha = 0.18f))
                                                .border(
                                                    1.dp,
                                                    badgeColor.copy(alpha = 0.55f),
                                                    RoundedCornerShape(100.dp),
                                                )
                                                .padding(horizontal = 10.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = category.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor,
                                                letterSpacing = 1.2.sp,
                                            )
                                        }
                                    }
                                    if (subcategory != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(qbColors.surface2)
                                                .padding(horizontal = 10.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = subcategory,
                                                fontSize = 10.sp,
                                                color = qbColors.textMuted,
                                            )
                                        }
                                    }
                                    if (difficulty != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(qbColors.surface2)
                                                .padding(horizontal = 10.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = "Diff $difficulty",
                                                fontSize = 10.sp,
                                                color = qbColors.textMuted,
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Scrollable question text
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(questionScrollState),
                            ) {
                                QuestionText(
                                    words = words,
                                    wordIndex = wordIndex,
                                    buzzIndex = buzzIndex,
                                    phase = phase,
                                    ttsDone = ttsDone,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                // Correct answer reveal in RESULT
                                if (phase == TossupPhase.RESULT && correctAnswer != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Answer: ",
                                            fontSize = 14.sp,
                                            color = qbColors.textMuted,
                                        )
                                        Text(
                                            text = correctAnswer,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = qbColors.primary,
                                        )
                                    }
                                }
                            }
                        } else {
                            // Empty / idle placeholder
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Tap Start to begin!",
                                    color = qbColors.textMuted,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Phase-dependent controls ──────────────────────────────────
                PhaseControls(
                    phase = phase,
                    loading = loading,
                    ttsPaused = ttsPaused,
                    ttsDone = ttsDone,
                    buzzCountdown = buzzCountdown,
                    answerCountdown = answerCountdown,
                    answer = answer,
                    result = result,
                    listening = listening,
                    voiceDisabled = voiceDisabled,
                    speechSupported = vm.speechSupported,
                    focusRequester = focusRequester,
                    settings = settings,
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

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Screen flash overlay ──────────────────────────────────────────
            if (flashAlpha > 0.01f) {
                val flashColor = if (isCorrectResult) qbColors.green else qbColors.red
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(flashColor.copy(alpha = flashAlpha)),
                )
            }
        }
    }

    // ── Settings bottom sheet ─────────────────────────────────────────────────
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = qbColors.surface,
        ) {
            SettingsPanel(
                settings = settings,
                voices = voices,
                onSettingsChange = { vm.saveSettings(it) },
                initiallyExpanded = true,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Game header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GameHeader(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qbColors = MaterialTheme.qbColors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text("←", fontSize = 22.sp, color = qbColors.textMuted)
        }
        Text(
            text = "TOSSUP PRACTICE",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = qbColors.primary,
        )
        TextButton(onClick = onSettings) {
            Text("⚙", fontSize = 20.sp, color = qbColors.textMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phase controls
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PhaseControls(
    phase: TossupPhase,
    loading: Boolean,
    ttsPaused: Boolean,
    ttsDone: Boolean,
    buzzCountdown: Float?,
    answerCountdown: Float?,
    answer: String,
    result: TossupResult?,
    listening: Boolean,
    voiceDisabled: Boolean,
    speechSupported: Boolean,
    focusRequester: FocusRequester,
    settings: TossupSettings,
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
    val gradientBrush = Brush.horizontalGradient(listOf(qbColors.primary, qbColors.accentTeal))

    when (phase) {
        // ── IDLE ──────────────────────────────────────────────────────────────
        TossupPhase.IDLE -> {
            GradientButton(
                text = if (loading) "Loading…" else "▶  START",
                onClick = onStart,
                enabled = !loading,
                gradient = gradientBrush,
                height = 60.dp,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        // ── READING ───────────────────────────────────────────────────────────
        TossupPhase.READING -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Buzz timer progress bar (shown only when timer is configured)
                if (buzzCountdown != null && settings.buzzTimer > 0f) {
                    TimerBar(
                        progress = buzzCountdown / settings.buzzTimer,
                        color = if (buzzCountdown <= 3f) qbColors.red else qbColors.accentAmber,
                        timeText = "%.1f".format(buzzCountdown),
                    )
                }

                // Giant pulsing buzz button
                BuzzButton(
                    onClick = onBuzz,
                    shouldPulse = ttsDone || buzzCountdown != null,
                    qbColors = qbColors,
                )

                // Pause/Resume as a subtle text button
                TextButton(
                    onClick = if (ttsPaused) onResume else onPause,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (ttsPaused) "▶  Resume" else "⏸  Pause",
                        color = qbColors.textMuted,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        // ── BUZZING ───────────────────────────────────────────────────────────
        TossupPhase.BUZZING -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Answer timer progress bar
                if (answerCountdown != null && settings.answerTimer > 0f) {
                    TimerBar(
                        progress = answerCountdown / settings.answerTimer,
                        color = if (answerCountdown <= 1f) qbColors.red else qbColors.accentAmber,
                        timeText = "%.1f".format(answerCountdown),
                    )
                }

                // Prompt hint card
                if (result?.directive == "prompt" && result.directedPrompt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = qbColors.amberDim),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "Be more specific: ${result.directedPrompt}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = qbColors.accentAmber,
                            fontSize = 13.sp,
                        )
                    }
                }

                // Mic card
                if (speechSupported && !voiceDisabled) {
                    MicCard(
                        listening = listening,
                        onTap = { if (!listening) onStartVoice() },
                        qbColors = qbColors,
                    )
                }

                // Text answer field
                OutlinedTextField(
                    value = answer,
                    onValueChange = { newText ->
                        onAnswerChange(newText)
                        onDisableVoice()
                    },
                    placeholder = {
                        Text("Type your answer here…", fontSize = 14.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = qbColors.primary,
                        unfocusedBorderColor = qbColors.border,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )

                GradientButton(
                    text = "✓  SUBMIT",
                    onClick = onSubmit,
                    enabled = answer.isNotBlank(),
                    gradient = gradientBrush,
                    height = 56.dp,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // ── RESULT ────────────────────────────────────────────────────────────
        TossupPhase.RESULT -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (result != null) {
                    val isCorrect = result.points > 0
                    val isWrong = result.points < 0
                    val (bannerTextColor, bannerBgColor) = when {
                        isCorrect -> qbColors.green to qbColors.greenDim
                        isWrong -> qbColors.red to qbColors.redDim
                        else -> qbColors.textMuted to qbColors.surface2
                    }
                    val icon = when {
                        isCorrect -> "✅"
                        isWrong -> "❌"
                        else -> "⏱"
                    }
                    val bannerText = when {
                        result.timedOut == "buzz" ->
                            "Didn't buzz in time — 0 pts"
                        result.timedOut == "answer" && result.points < 0 ->
                            "Time's up! ${result.points} pts"
                        result.timedOut == "answer" ->
                            "Time's up — 0 pts"
                        isCorrect ->
                            "Correct!${if (result.isPower) "  ⚡ POWER!" else ""}  +${result.points} pts"
                        isWrong ->
                            "Wrong answer — ${result.points} pts"
                        else -> "No points"
                    }

                    var bannerVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { bannerVisible = true }

                    AnimatedVisibility(
                        visible = bannerVisible,
                        enter = fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.92f),
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = bannerBgColor),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, bannerTextColor.copy(alpha = 0.35f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(icon, fontSize = 28.sp)
                                Text(
                                    text = bannerText,
                                    color = bannerTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }

                    if (result.userAnswer != null) {
                        Text(
                            text = "You answered: ${result.userAnswer}",
                            fontSize = 13.sp,
                            color = qbColors.textMuted,
                        )
                    }
                }

                GradientButton(
                    text = "NEXT  →",
                    onClick = onNext,
                    gradient = gradientBrush,
                    height = 56.dp,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    gradient: Brush,
    height: Dp,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val disabledBrush = Brush.horizontalGradient(
        listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f))
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) gradient else disabledBrush)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
            fontSize = fontSize,
            fontWeight = fontWeight,
        )
    }
}

@Composable
private fun BuzzButton(
    onClick: () -> Unit,
    shouldPulse: Boolean,
    qbColors: QuizBowlColors,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buzzPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "buzzScale",
    )
    val appliedScale = if (shouldPulse) pulseScale else 1f
    val buzzGradient = Brush.horizontalGradient(listOf(qbColors.accentTeal, qbColors.primary))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(appliedScale)
            .clip(RoundedCornerShape(28.dp))
            .background(buzzGradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "⚡  BUZZ!",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun MicCard(
    listening: Boolean,
    onTap: () -> Unit,
    qbColors: QuizBowlColors,
) {
    val listeningTransition = rememberInfiniteTransition(label = "micDots")
    val dotFloat by listeningTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Restart),
        label = "dotFloat",
    )
    val dots = ".".repeat((dotFloat.toInt() + 1).coerceIn(1, 3))

    val micBackground = if (listening)
        Brush.horizontalGradient(
            listOf(
                qbColors.primary.copy(alpha = 0.25f),
                qbColors.accentTeal.copy(alpha = 0.25f),
            )
        )
    else
        Brush.horizontalGradient(
            listOf(qbColors.surface2, qbColors.surface2)
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(micBackground)
            .border(
                1.dp,
                if (listening) qbColors.primary.copy(alpha = 0.6f) else qbColors.border,
                RoundedCornerShape(12.dp),
            )
            .clickable(enabled = !listening, onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("🎤", fontSize = 26.sp)
            Text(
                text = if (listening) "Listening$dots" else "Tap to speak your answer",
                fontSize = 15.sp,
                fontWeight = if (listening) FontWeight.SemiBold else FontWeight.Normal,
                color = if (listening) qbColors.primary else qbColors.textMuted,
            )
        }
    }
}

@Composable
private fun TimerBar(
    progress: Float,
    color: Color,
    timeText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.qbColors.surface2,
        )
        Text(
            text = "${timeText}s",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun categoryAccentColor(category: String, qbColors: QuizBowlColors): Color = when {
    category.contains("Science", ignoreCase = true) -> qbColors.accentTeal
    category.contains("History", ignoreCase = true) -> qbColors.accentAmber
    category.contains("Literature", ignoreCase = true) -> qbColors.accentRose
    category.contains("Fine Arts", ignoreCase = true) -> qbColors.accentRose
    category.contains("Religion", ignoreCase = true) -> qbColors.accentAmber
    category.contains("Mythology", ignoreCase = true) -> qbColors.accentAmber
    else -> qbColors.primary
}
