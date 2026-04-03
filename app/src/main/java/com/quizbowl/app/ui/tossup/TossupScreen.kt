package com.quizbowl.app.ui.tossup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.text.style.TextAlign
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

    // ── Permissions ───────────────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // focusRequester kept for manual tap-to-type; keyboard no longer auto-raises on Buzz
    val focusRequester = remember { FocusRequester() }

    // ── Auto-scroll question to bottom ────────────────────────────────────────
    val questionScrollState = rememberScrollState()
    LaunchedEffect(wordIndex, buzzIndex) {
        questionScrollState.animateScrollTo(questionScrollState.maxValue)
    }

    val correctAnswer = tossup?.let { t ->
        t.answerSanitized ?: t.answer.replace(Regex("<[^>]+>"), "")
    }

    // ── Settings bottom sheet ─────────────────────────────────────────────────
    var showSettings by remember { mutableStateOf(false) }

    // ── Result screen flash ───────────────────────────────────────────────────
    var flashActive by remember { mutableStateOf(false) }
    val isCorrectResult = result != null && result!!.points > 0
    val flashAlpha by animateFloatAsState(
        targetValue = if (flashActive) 0.18f else 0f,
        animationSpec = tween(400),
        label = "resultFlash",
    )
    LaunchedEffect(phase) {
        if (phase == TossupPhase.RESULT) {
            flashActive = true
            delay(600)
            flashActive = false
        }
    }

    val qbColors = MaterialTheme.qbColors

    Scaffold(containerColor = qbColors.bg) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                // ── Header ────────────────────────────────────────────────────
                GameHeader(
                    onBack = { navController.navigateUp() },
                    onSettings = { showSettings = true },
                )

                // ── Score HUD ─────────────────────────────────────────────────
                ScoreBoard(score = score)

                Spacer(modifier = Modifier.height(8.dp))

                // ── Error banner ──────────────────────────────────────────────
                if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = qbColors.redDim),
                        shape = RoundedCornerShape(10.dp),
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

                // ── Question card ─────────────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = qbColors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                    ) {
                        if (tossup != null) {
                            // Category line — one pill for category+subcategory, separate diff badge
                            CategoryLine(
                                category = tossup!!.category,
                                subcategory = tossup!!.subcategory,
                                difficulty = tossup!!.difficulty,
                                qbColors = qbColors,
                            )
                            Spacer(modifier = Modifier.height(14.dp))

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

                                if (phase == TossupPhase.RESULT && correctAnswer != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Answer: ",
                                            fontSize = 13.sp,
                                            color = qbColors.textMuted,
                                        )
                                        Text(
                                            text = correctAnswer,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = qbColors.primary,
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Tap Start to begin",
                                    color = qbColors.textMuted,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Controls ──────────────────────────────────────────────────
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

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Result flash overlay (behind controls, over question) ─────────
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
) {
    val qbColors = MaterialTheme.qbColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = qbColors.textMuted,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "TOSSUP",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
            color = qbColors.primary,
        )
        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = qbColors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category line
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryLine(
    category: String?,
    subcategory: String?,
    difficulty: Int?,
    qbColors: QuizBowlColors,
) {
    if (category == null && subcategory == null && difficulty == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (category != null) {
            val accentColor = categoryAccentColor(category, qbColors)
            val label = if (subcategory != null) "$category · $subcategory" else category

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
            }
        }

        if (difficulty != null) {
            Text(
                text = "D$difficulty",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = qbColors.textMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(qbColors.surface2)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phase controls
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val primaryGradient = Brush.horizontalGradient(listOf(qbColors.primary, qbColors.accentTeal))

    when (phase) {

        // ── IDLE ──────────────────────────────────────────────────────────────
        TossupPhase.IDLE -> {
            GradientButton(
                text = if (loading) "Loading…" else "START",
                onClick = onStart,
                enabled = !loading,
                gradient = primaryGradient,
                height = 58.dp,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
            )
        }

        // ── READING ───────────────────────────────────────────────────────────
        TossupPhase.READING -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Countdown timer — big number + bar
                if (buzzCountdown != null && settings.buzzTimer > 0f) {
                    CountdownTimer(
                        seconds = buzzCountdown,
                        maxSeconds = settings.buzzTimer,
                        warningThreshold = 3f,
                        qbColors = qbColors,
                    )
                }

                // Buzz button + pause icon in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BuzzButton(
                        onClick = onBuzz,
                        shouldPulse = ttsDone || buzzCountdown != null,
                        qbColors = qbColors,
                        modifier = Modifier.weight(1f),
                    )
                    // Pause/Resume — small circular icon button
                    IconButton(
                        onClick = if (ttsPaused) onResume else onPause,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(qbColors.surface2),
                    ) {
                        Icon(
                            imageVector = if (ttsPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (ttsPaused) "Resume" else "Pause",
                            tint = qbColors.textMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        // ── BUZZING ───────────────────────────────────────────────────────────
        TossupPhase.BUZZING -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Answer countdown
                if (answerCountdown != null && settings.answerTimer > 0f) {
                    CountdownTimer(
                        seconds = answerCountdown,
                        maxSeconds = settings.answerTimer,
                        warningThreshold = 3f,
                        qbColors = qbColors,
                    )
                }

                // Prompt hint
                if (result?.directive == "prompt" && result.directedPrompt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = qbColors.amberDim),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "More specific: ${result.directedPrompt}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = qbColors.accentAmber,
                            fontSize = 13.sp,
                        )
                    }
                }

                // Mic card — animated border, static background
                if (speechSupported && !voiceDisabled) {
                    MicCard(
                        listening = listening,
                        onTap = { if (!listening) onStartVoice() },
                        qbColors = qbColors,
                    )
                }

                // Answer text field
                OutlinedTextField(
                    value = answer,
                    onValueChange = { newText ->
                        onAnswerChange(newText)
                        onDisableVoice()
                    },
                    placeholder = { Text("Type your answer…", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                onSubmit(); true
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
                    text = "SUBMIT",
                    onClick = onSubmit,
                    enabled = answer.isNotBlank(),
                    gradient = primaryGradient,
                    height = 54.dp,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
            }
        }

        // ── RESULT ────────────────────────────────────────────────────────────
        TossupPhase.RESULT -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (result != null) {
                    val isCorrect = result.points > 0
                    val isWrong = result.points < 0

                    var bannerVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { bannerVisible = true }

                    AnimatedVisibility(
                        visible = bannerVisible,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            initialOffsetY = { it / 2 },
                        ) + fadeIn(tween(200)),
                    ) {
                        ResultBanner(
                            result = result,
                            isCorrect = isCorrect,
                            isWrong = isWrong,
                            qbColors = qbColors,
                        )
                    }
                }

                GradientButton(
                    text = "NEXT",
                    onClick = onNext,
                    gradient = Brush.horizontalGradient(
                        listOf(qbColors.surface2, qbColors.surface2)
                    ),
                    height = 50.dp,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultBanner(
    result: TossupResult,
    isCorrect: Boolean,
    isWrong: Boolean,
    qbColors: QuizBowlColors,
) {
    val (textColor, bgColor, borderColor) = when {
        isCorrect -> Triple(qbColors.green, qbColors.greenDim, qbColors.green.copy(alpha = 0.3f))
        isWrong -> Triple(qbColors.red, qbColors.redDim, qbColors.red.copy(alpha = 0.3f))
        else -> Triple(qbColors.textMuted, qbColors.surface2, qbColors.border)
    }

    val headlineText = when {
        result.timedOut == "buzz" -> "No buzz"
        result.timedOut == "answer" -> "Time's up"
        isCorrect && result.isPower -> "POWER!"
        isCorrect -> "Correct!"
        isWrong -> "Wrong"
        else -> "0 pts"
    }

    val ptsText = when {
        result.timedOut != null -> "0"
        result.points > 0 -> "+${result.points}"
        else -> "${result.points}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = headlineText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
            )
            Text(
                text = ptsText,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
            )
        }
        if (result.userAnswer != null) {
            Text(
                text = "You said: ${result.userAnswer}",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Countdown timer — large number + bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CountdownTimer(
    seconds: Float,
    maxSeconds: Float,
    warningThreshold: Float,
    qbColors: QuizBowlColors,
) {
    val progress = (seconds / maxSeconds).coerceIn(0f, 1f)
    val isWarning = seconds <= warningThreshold
    val timerColor by animateColorAsState(
        targetValue = if (isWarning) qbColors.red else qbColors.accentAmber,
        animationSpec = tween(300),
        label = "timerColor",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "%.1f".format(seconds),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = timerColor,
            letterSpacing = (-1).sp,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = timerColor,
            trackColor = qbColors.surface2,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Buzz button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BuzzButton(
    onClick: () -> Unit,
    shouldPulse: Boolean,
    qbColors: QuizBowlColors,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buzzPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "buzzScale",
    )
    val appliedScale = if (shouldPulse) pulseScale else 1f

    val interactionSource = remember { MutableInteractionSource() }
    val buzzGradient = Brush.horizontalGradient(listOf(qbColors.accentTeal, qbColors.primary))

    Box(
        modifier = modifier
            .height(72.dp)
            .scale(appliedScale)
            .clip(RoundedCornerShape(20.dp))
            .background(buzzGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = qbColors.accentAmber,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = "BUZZ",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mic card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MicCard(
    listening: Boolean,
    onTap: () -> Unit,
    qbColors: QuizBowlColors,
) {
    // Animate border width and color — not background — on active state
    val borderColor by animateColorAsState(
        targetValue = if (listening) qbColors.primary else qbColors.border,
        animationSpec = tween(250),
        label = "micBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (listening) 2.dp else 1.dp,
        animationSpec = tween(250),
        label = "micBorderWidth",
    )

    // Animated dots string
    val dotsTransition = rememberInfiniteTransition(label = "micDots")
    val dotFloat by dotsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Restart),
        label = "dotFloat",
    )
    val dots = ".".repeat((dotFloat.toInt() + 1).coerceIn(1, 3))

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(qbColors.surface2)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = qbColors.primary, bounded = true),
                enabled = !listening,
                onClick = onTap,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (listening) qbColors.primary.copy(alpha = 0.15f) else qbColors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = if (listening) qbColors.primary else qbColors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = if (listening) "Listening$dots" else "Tap to speak your answer",
            fontSize = 14.sp,
            fontWeight = if (listening) FontWeight.SemiBold else FontWeight.Normal,
            color = if (listening) qbColors.primary else qbColors.textMuted,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gradient button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    gradient: Brush,
    height: Dp,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    letterSpacing: TextUnit = 0.sp,
    enabled: Boolean = true,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    val disabledBrush = Brush.horizontalGradient(
        listOf(Color.Gray.copy(alpha = 0.35f), Color.Gray.copy(alpha = 0.35f))
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) gradient else disabledBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    color = if (enabled) Color.White else Color.Transparent,
                    bounded = true,
                ),
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.4f),
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
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
