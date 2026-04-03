package com.quizbowl.app.ui.bonus

import android.app.Application
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizbowl.app.api.QbReaderService
import com.quizbowl.app.api.models.Bonus
import com.quizbowl.app.data.BonusSettings
import com.quizbowl.app.data.SettingsRepository
import com.quizbowl.app.engine.SpeechEngine
import com.quizbowl.app.engine.TtsEngine
import com.quizbowl.app.util.BonusScore
import com.quizbowl.app.util.PartResult
import com.quizbowl.app.util.calcBonusTotal
import com.quizbowl.app.util.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class BonusPhase { IDLE, READING_LEADIN, READING_PART, ANSWERING, PROMPTING, PART_RESULT, DONE }

class BonusViewModel(application: Application) : AndroidViewModel(application) {

    val tts = TtsEngine(application)
    val speechSupported: Boolean = SpeechRecognizer.isRecognitionAvailable(application)

    private val settingsRepo = SettingsRepository(application)
    val settings: StateFlow<BonusSettings> = settingsRepo.bonusSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, BonusSettings())

    private val _phase = MutableStateFlow(BonusPhase.IDLE)
    val phase: StateFlow<BonusPhase> = _phase.asStateFlow()

    private val _bonus = MutableStateFlow<Bonus?>(null)
    val bonus: StateFlow<Bonus?> = _bonus.asStateFlow()

    private val _leadinWords = MutableStateFlow<List<String>>(emptyList())
    val leadinWords: StateFlow<List<String>> = _leadinWords.asStateFlow()

    private val _currentPart = MutableStateFlow(0)
    val currentPart: StateFlow<Int> = _currentPart.asStateFlow()

    // Words for the CURRENTLY ACTIVE part (as passed to tts.speak — must match for wordIndex)
    private val _partWords = MutableStateFlow<List<String>>(emptyList())
    val partWords: StateFlow<List<String>> = _partWords.asStateFlow()

    private val _partResults = MutableStateFlow<List<PartResult>>(emptyList())
    val partResults: StateFlow<List<PartResult>> = _partResults.asStateFlow()

    private val _score = MutableStateFlow(BonusScore())
    val score: StateFlow<BonusScore> = _score.asStateFlow()

    private val _answer = MutableStateFlow("")
    val answer: StateFlow<String> = _answer.asStateFlow()

    private val _answerCountdown = MutableStateFlow<Float?>(null)
    val answerCountdown: StateFlow<Float?> = _answerCountdown.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _voiceDisabled = MutableStateFlow(false)
    val voiceDisabled: StateFlow<Boolean> = _voiceDisabled.asStateFlow()

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    private val _directedPrompt = MutableStateFlow<String?>(null)
    val directedPrompt: StateFlow<String?> = _directedPrompt.asStateFlow()

    private val submitting = AtomicBoolean(false)
    @Volatile private var answerStarted = false
    private var answerTimerJob: Job? = null
    private var partAdvanceJob: Job? = null
    private var listeningJob: Job? = null
    private var speechEngine: SpeechEngine? = null

    init {
        // When TTS finishes in READING_LEADIN → read part 0
        // When TTS finishes in READING_PART → start answering phase
        viewModelScope.launch {
            tts.done.collect { done ->
                if (!done) return@collect
                when (_phase.value) {
                    BonusPhase.READING_LEADIN -> readPart(0)
                    BonusPhase.READING_PART -> startAnswering()
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            settings.collect { s ->
                tts.setRate(s.rate)
                tts.setVoice(s.voiceName)
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun fetchBonus() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            tts.reset()
            cancelTimers()
            destroySpeech()
            _phase.value = BonusPhase.IDLE
            _bonus.value = null
            _leadinWords.value = emptyList()
            _currentPart.value = 0
            _partWords.value = emptyList()
            _partResults.value = emptyList()
            _answer.value = ""
            _voiceDisabled.value = false
            _directedPrompt.value = null
            answerStarted = false
            submitting.set(false)

            try {
                val s = settings.value
                val resp = QbReaderService.getRandomBonus(
                    categories = s.categories,
                    difficulties = s.difficulties,
                )
                val b = resp.bonuses.firstOrNull()
                if (b == null) {
                    _error.value = "No bonuses found. Try different filters."
                    _loading.value = false
                    return@launch
                }
                _bonus.value = b
                val leadin = b.leadinSanitized ?: b.leadin
                _leadinWords.value = leadin.split(Regex("\\s+")).filter { it.isNotEmpty() }
                _phase.value = BonusPhase.READING_LEADIN
                _loading.value = false
                tts.speak(_leadinWords.value)
            } catch (e: Exception) {
                _error.value = "Failed to fetch bonus: ${e.message}"
                _loading.value = false
            }
        }
    }

    fun updateAnswer(text: String) {
        answerStarted = true
        _answer.value = text
    }

    fun disableVoiceIfActive() {
        if (!_voiceDisabled.value) {
            destroySpeech()
            _voiceDisabled.value = true
        }
    }

    /** Re-start voice recognition. Call from a button onClick (main thread user gesture). */
    fun startVoice() {
        val p = _phase.value
        if (_voiceDisabled.value || (p != BonusPhase.ANSWERING && p != BonusPhase.PROMPTING) || _listening.value) return
        startSpeechEngine()
    }

    fun submitAnswer(answerText: String = _answer.value) {
        val phase = _phase.value
        if (phase != BonusPhase.ANSWERING && phase != BonusPhase.PROMPTING) return
        if (answerText.isBlank()) return
        if (!submitting.compareAndSet(false, true)) return
        val isFromPrompt = (phase == BonusPhase.PROMPTING)
        cancelAnswerTimer()
        destroySpeech()
        viewModelScope.launch {
            try {
                val b = _bonus.value ?: return@launch
                val part = _currentPart.value
                val expectedAnswer = b.answersSanitized?.getOrNull(part) ?: b.answers.getOrNull(part) ?: ""
                val res = QbReaderService.checkAnswer(answerText.trim(), expectedAnswer)
                when {
                    res.directive == "prompt" && !isFromPrompt -> {
                        // Surface the prompt — let user re-answer (no nested prompting)
                        _directedPrompt.value = res.directedPrompt
                        _answer.value = ""
                        _voiceDisabled.value = false
                        _phase.value = BonusPhase.PROMPTING
                        startSpeechEngine()
                    }
                    else -> {
                        val correct = res.directive == "accept"
                        val points = if (correct) 10 else 0
                        val newResults = _partResults.value + PartResult(
                            correct = correct,
                            points = points,
                            userAnswer = answerText.trim(),
                        )
                        _partResults.value = newResults
                        _directedPrompt.value = null
                        _phase.value = BonusPhase.PART_RESULT
                        schedulePartAdvance(newResults)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to check answer: ${e.message}"
            } finally {
                submitting.set(false)
            }
        }
    }

    fun pauseTts() = tts.pause()
    fun resumeTts() = tts.resume()

    fun saveSettings(s: BonusSettings) {
        viewModelScope.launch { settingsRepo.saveBonusSettings(s) }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun readPart(partIndex: Int) {
        val b = _bonus.value ?: return
        _currentPart.value = partIndex
        val partText = b.partsSanitized?.getOrNull(partIndex) ?: b.parts.getOrNull(partIndex) ?: return
        val words = partText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        _partWords.value = words
        _phase.value = BonusPhase.READING_PART
        tts.speak(words)
    }

    private fun startAnswering() {
        destroySpeech()
        _voiceDisabled.value = false
        _answer.value = ""
        answerStarted = false
        _phase.value = BonusPhase.ANSWERING
        startSpeechEngine()
        val timer = settings.value.answerTimer
        if (timer > 0f) startAnswerTimer(timer)
    }

    private fun schedulePartAdvance(results: List<PartResult>) {
        partAdvanceJob?.cancel()
        partAdvanceJob = viewModelScope.launch {
            delay(1500)
            val part = _currentPart.value
            if (part < 2) {
                readPart(part + 1)
            } else {
                val bonusTotal = calcBonusTotal(results)
                _score.value = _score.value.update(bonusTotal)
                _phase.value = BonusPhase.DONE
            }
        }
    }

    // ── Answer timer ──────────────────────────────────────────────────────────

    private fun startAnswerTimer(seconds: Float) {
        cancelAnswerTimer()
        answerStarted = false
        var remaining = (seconds * 10).toInt()
        _answerCountdown.value = remaining / 10f
        answerTimerJob = viewModelScope.launch {
            while (remaining > 0) {
                if (answerStarted) {
                    _answerCountdown.value = null
                    return@launch
                }
                delay(100)
                remaining--
                _answerCountdown.value = remaining / 10f
                if (remaining <= 0) {
                    _answerCountdown.value = null
                    destroySpeech()
                    // Timeout → 0 pts (no penalty in bonus)
                    val newResults = _partResults.value + PartResult(
                        correct = false,
                        points = 0,
                        userAnswer = "",
                    )
                    _partResults.value = newResults
                    _phase.value = BonusPhase.PART_RESULT
                    schedulePartAdvance(newResults)
                    return@launch
                }
            }
        }
    }

    private fun cancelAnswerTimer() {
        answerTimerJob?.cancel()
        answerTimerJob = null
        _answerCountdown.value = null
    }

    private fun cancelTimers() {
        cancelAnswerTimer()
        partAdvanceJob?.cancel()
        partAdvanceJob = null
    }

    // ── Speech engine ─────────────────────────────────────────────────────────

    private fun startSpeechEngine() {
        destroySpeech()
        if (!speechSupported) return
        val app = getApplication<Application>()
        val engine = SpeechEngine(
            context = app,
            onInterimResult = { text ->
                val p = _phase.value
                if ((p == BonusPhase.ANSWERING || p == BonusPhase.PROMPTING) && !_voiceDisabled.value) {
                    answerStarted = true
                    _answer.value = text
                }
            },
            onFinalResult = { text ->
                val p = _phase.value
                if (p == BonusPhase.ANSWERING || p == BonusPhase.PROMPTING) {
                    _answer.value = text
                    submitAnswer(text)
                }
            },
        )
        speechEngine = engine
        listeningJob = viewModelScope.launch {
            engine.listening.collect { _listening.value = it }
        }
        engine.start()
    }

    private fun destroySpeech() {
        listeningJob?.cancel()
        listeningJob = null
        speechEngine?.destroy()
        speechEngine = null
        _listening.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tts.shutdown()
        destroySpeech()
        cancelTimers()
    }
}
