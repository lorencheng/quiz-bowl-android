package com.quizbowl.app.ui.bonus

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizbowl.app.data.BonusSettings
import com.quizbowl.app.ui.components.BonusSettingsPanel
import com.quizbowl.app.ui.theme.QuizBowlColors
import com.quizbowl.app.ui.theme.qbColors
import com.quizbowl.app.util.PartResult
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusScreen(navController: NavController) {
    val vm: BonusViewModel = viewModel()
    val context = LocalContext.current

    // ── State collection ──────────────────────────────────────────────────────
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

    // ── Permissions ───────────────────────────────────────────────────────────
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

    // focusRequester kept for manual tap-to-type; keyboard no longer auto-raises on ANSWERING
    val focusRequester = remember { FocusRequester() }

    // ── Auto-scroll question to bottom ────────────────────────────────────────
    val scrollState = rememberScrollState()
    LaunchedEffect(wordIndex, currentPart, partResults.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // ── Settings bottom sheet ─────────────────────────────────────────────────
    var showSettings by remember { mutableStateOf(false) }

    // ── Haptic feedback on part result ────────────────────────────────────────
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(partResults.size) {
        if (partResults.isNotEmpty()) {
            val last = partResults.last()
            if (last.correct) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                repeat(2) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(120)
                }
            }
        }
    }

    // ── Screen flash on part result ───────────────────────────────────────────
    var flashActive by remember { mutableStateOf(false) }
    val lastPartCorrect = partResults.lastOrNull()?.correct ?: false
    val flashAlpha by animateFloatAsState(
        targetValue = if (flashActive) 0.18f else 0f,
        animationSpec = if (flashActive) tween(80) else tween(500),
        label = "partFlash",
    )
    LaunchedEffect(partResults.size) {
        if (partResults.isNotEmpty()) {
            flashActive = true
            delay(350)
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
                BonusGameHeader(
                    onBack = { navController.navigateUp() },
                    onSettings = { showSettings = true },
                )

                // ── Score HUD ─────────────────────────────────────────────────
                BonusScoreBoard(score = score, qbColors = qbColors)

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

                // ── Question area ─────────────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = qbColors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    if (bonus != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                        ) {
                            // Category line
                            BonusCategoryLine(
                                category = bonus!!.category,
                                subcategory = bonus!!.subcategory,
                                difficulty = bonus!!.difficulty,
                                setName = bonus!!.set?.name,
                                qbColors = qbColors,
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Scrollable content: leadin + part cards
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Leadin
                                if (phase != BonusPhase.IDLE) {
                                    LeadinText(
                                        words = leadinWords,
                                        wordIndex = wordIndex,
                                        isReading = phase == BonusPhase.READING_LEADIN,
                                        ttsDone = ttsDone && phase == BonusPhase.READING_LEADIN,
                                        qbColors = qbColors,
                                    )
                                }

                                // Part cards
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
                                                qbColors = qbColors,
                                            )
                                        }
                                    }
                                }

                                if (phase == BonusPhase.IDLE) {
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

                                Spacer(modifier = Modifier.height(8.dp))
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

                Spacer(modifier = Modifier.height(16.dp))

                // ── Phase controls (animated on phase change) ─────────────────
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 3 }
                            togetherWith fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 6 })
                            .using(SizeTransform(clip = false))
                    },
                    label = "bonusPhaseControls",
                ) { currentPhase ->
                    BonusPhaseControls(
                        phase = currentPhase,
                        loading = loading,
                        ttsPaused = ttsPaused,
                        answerCountdown = answerCountdown,
                        answer = answer,
                        listening = listening,
                        voiceDisabled = voiceDisabled,
                        speechSupported = vm.speechSupported,
                        directedPrompt = directedPrompt,
                        partResults = partResults,
                        settings = settings,
                        focusRequester = focusRequester,
                        qbColors = qbColors,
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

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Part result flash overlay ─────────────────────────────────────
            if (flashAlpha > 0.01f) {
                val flashColor = if (lastPartCorrect) qbColors.green else qbColors.red
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
            BonusSettingsPanel(
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
private fun BonusGameHeader(
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
            text = "BONUS",
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
// Score HUD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BonusScoreBoard(
    score: com.quizbowl.app.util.BonusScore,
    qbColors: QuizBowlColors,
) {
    val animatedTotal by animateIntAsState(
        targetValue = score.total,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "bonusScoreTotal",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Total — hero number
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = animatedTotal.toString(),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = qbColors.accentAmber,
                lineHeight = 30.sp,
            )
            Text(
                text = "pts",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = qbColors.accentAmber.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        // Stats — right side
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BonusStatColumn(value = score.bonuses.toString(), label = "played", color = qbColors.textMuted)
            BonusStatColumn(value = score.thirties.toString(), label = "30s", color = qbColors.green)
            if (score.bonuses > 0) {
                BonusStatColumn(
                    value = "${"%.1f".format(score.ppb)}",
                    label = "ppb",
                    color = qbColors.accentAmber,
                )
            }
        }
    }
}

@Composable
private fun BonusStatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category line
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BonusCategoryLine(
    category: String?,
    subcategory: String?,
    difficulty: Int?,
    setName: String?,
    qbColors: QuizBowlColors,
) {
    if (category == null && subcategory == null && difficulty == null && setName == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (category != null) {
            val accentColor = bonusCategoryAccentColor(category, qbColors)
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

private fun bonusCategoryAccentColor(category: String, qbColors: QuizBowlColors): Color = when {
    category.contains("Science", ignoreCase = true) -> qbColors.accentTeal
    category.contains("History", ignoreCase = true) -> qbColors.accentAmber
    category.contains("Literature", ignoreCase = true) -> qbColors.accentRose
    category.contains("Fine Arts", ignoreCase = true) -> qbColors.accentRose
    category.contains("Religion", ignoreCase = true) -> qbColors.accentAmber
    category.contains("Mythology", ignoreCase = true) -> qbColors.accentAmber
    else -> qbColors.primary
}

// ─────────────────────────────────────────────────────────────────────────────
// Leadin text
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LeadinText(
    words: List<String>,
    wordIndex: Int,
    isReading: Boolean,
    ttsDone: Boolean,
    qbColors: QuizBowlColors,
) {
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

// ─────────────────────────────────────────────────────────────────────────────
// Bonus part card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BonusPartCard(
    index: Int,
    words: List<String>,
    wordIndex: Int,
    isActiveReading: Boolean,
    result: PartResult?,
    correctAnswer: String,
    qbColors: QuizBowlColors,
) {
    val isAnswered = result != null

    val borderColor by animateColorAsState(
        targetValue = when {
            isActiveReading -> qbColors.primary
            isAnswered && result!!.correct -> qbColors.green
            isAnswered -> qbColors.red
            else -> qbColors.border
        },
        animationSpec = tween(300),
        label = "partBorder$index",
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isAnswered && result!!.correct -> qbColors.greenDim
            isAnswered -> qbColors.redDim
            else -> qbColors.surface
        },
        animationSpec = tween(400),
        label = "partBg$index",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(width = 1.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Part ${index + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = qbColors.textMuted,
                letterSpacing = 1.sp,
            )

            // Part text with word-by-word reveal
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

            // Result row
            if (isAnswered) {
                val r = result!!
                val resultColor = if (r.correct) qbColors.green else qbColors.red
                Text(
                    text = if (r.correct) "Correct! +10" else "Incorrect. 0 pts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = resultColor,
                )
                if (r.userAnswer.isNotEmpty()) {
                    Text(
                        text = "You said: ${r.userAnswer}",
                        fontSize = 12.sp,
                        color = qbColors.textMuted,
                    )
                }
                Text(
                    text = "Answer: $correctAnswer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
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
    settings: BonusSettings,
    focusRequester: FocusRequester,
    qbColors: QuizBowlColors,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAnswerChange: (String) -> Unit,
    onDisableVoice: () -> Unit,
    onStartVoice: () -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
) {
    val primaryGradient = Brush.horizontalGradient(listOf(qbColors.primary, qbColors.accentTeal))

    when (phase) {
        BonusPhase.IDLE -> {
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

        BonusPhase.READING_LEADIN, BonusPhase.READING_PART -> {
            // Pause/Resume — right-aligned circular icon button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
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

        BonusPhase.ANSWERING, BonusPhase.PROMPTING -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Answer countdown timer
                if (answerCountdown != null && settings.answerTimer > 0f) {
                    BonusCountdownTimer(
                        seconds = answerCountdown,
                        maxSeconds = settings.answerTimer,
                        warningThreshold = 3f,
                        qbColors = qbColors,
                    )
                }

                // Prompt card
                if (phase == BonusPhase.PROMPTING && directedPrompt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = qbColors.amberDim),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "More specific: $directedPrompt",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = qbColors.accentAmber,
                            fontSize = 13.sp,
                        )
                    }
                }

                // Mic card — voice auto-starts, this is visual indicator + fallback tap
                if (speechSupported && !voiceDisabled) {
                    BonusMicCard(
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

        BonusPhase.PART_RESULT -> {
            // Auto-advancing — no controls needed
        }

        BonusPhase.DONE -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val total = partResults.sumOf { it.points }

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
                    BonusDoneBanner(total = total, qbColors = qbColors)
                }

                GradientButton(
                    text = "NEXT BONUS",
                    onClick = onNext,
                    gradient = Brush.horizontalGradient(listOf(qbColors.surface2, qbColors.surface2)),
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
// Bonus done banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BonusDoneBanner(total: Int, qbColors: QuizBowlColors) {
    val textColor: Color
    val bgColor: Color
    val borderColor: Color
    val headline: String
    val subline: String

    when {
        total == 30 -> {
            textColor = qbColors.green
            bgColor = qbColors.greenDim
            borderColor = qbColors.green.copy(alpha = 0.3f)
            headline = "30 / 30"
            subline = "Perfect bonus!"
        }
        total == 0 -> {
            textColor = qbColors.red
            bgColor = qbColors.redDim
            borderColor = qbColors.red.copy(alpha = 0.3f)
            headline = "0 / 30"
            subline = "No points"
        }
        else -> {
            textColor = qbColors.accentAmber
            bgColor = qbColors.amberDim
            borderColor = qbColors.accentAmber.copy(alpha = 0.3f)
            headline = "$total / 30"
            subline = "${total / 10} / 3 correct"
        }
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
                text = headline,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
            )
            Text(
                text = subline,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.8f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Countdown timer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BonusCountdownTimer(
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
        label = "bonusTimerColor",
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
// Mic card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BonusMicCard(
    listening: Boolean,
    onTap: () -> Unit,
    qbColors: QuizBowlColors,
) {
    val borderColor by animateColorAsState(
        targetValue = if (listening) qbColors.primary else qbColors.border,
        animationSpec = tween(250),
        label = "bonusMicBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (listening) 2.dp else 1.dp,
        animationSpec = tween(250),
        label = "bonusMicBorderWidth",
    )

    val dotsTransition = rememberInfiniteTransition(label = "bonusMicDots")
    val dotFloat by dotsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Restart),
        label = "bonusDotFloat",
    )
    val dots = ".".repeat((dotFloat.toInt() + 1).coerceIn(1, 3))

    val ringTransition = rememberInfiniteTransition(label = "bonusMicRing")
    val ringScale by ringTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "bonusRingScale",
    )
    val ringAlpha by ringTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "bonusRingAlpha",
    )

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
        Box(contentAlignment = Alignment.Center) {
            if (listening) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(qbColors.primary.copy(alpha = ringAlpha)),
                )
            }
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
