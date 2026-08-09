package com.quizbowl.app.ui.tossup

import android.app.Application
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizbowl.app.api.QbReaderService
import com.quizbowl.app.api.models.Tossup
import com.quizbowl.app.data.SettingsRepository
import com.quizbowl.app.data.TossupSettings
import com.quizbowl.app.engine.SoundEffects
import com.quizbowl.app.engine.SpeechEngine
import com.quizbowl.app.engine.TtsEngine
import com.quizbowl.app.util.TossupScore
import com.quizbowl.app.util.calcTossupPoints
import com.quizbowl.app.util.findPowerIndex
import com.quizbowl.app.util.stripPowerMarker
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

enum class TossupPhase { IDLE, READING, BUZZING, RESULT }

data class TossupResult(
    val directive: String,
    val directedPrompt: String? = null,
    val points: Int,
    val isPower: Boolean = false,
    val userAnswer: String? = null,
    val timedOut: String? = null, // "buzz" or "answer"
)

class TossupViewModel(application: Application) : AndroidViewModel(application) {

    val tts = TtsEngine(application)
    val speechSupported: Boolean = SpeechRecognizer.isRecognitionAvailable(application)

    private val settingsRepo = SettingsRepository(application)
    val settings: StateFlow<TossupSettings> = settingsRepo.tossupSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, TossupSettings())

    private val _phase = MutableStateFlow(TossupPhase.IDLE)
    val phase: StateFlow<TossupPhase> = _phase.asStateFlow()

    private val _tossup = MutableStateFlow<Tossup?>(null)
    val tossup: StateFlow<Tossup?> = _tossup.asStateFlow()

    private val _words = MutableStateFlow<List<String>>(emptyList())
    val words: StateFlow<List<String>> = _words.asStateFlow()

    private val _powerIndex = MutableStateFlow(-1)
    val powerIndex: StateFlow<Int> = _powerIndex.asStateFlow()

    private val _buzzIndex = MutableStateFlow(-1)
    val buzzIndex: StateFlow<Int> = _buzzIndex.asStateFlow()

    private val _answer = MutableStateFlow("")
    val answer: StateFlow<String> = _answer.asStateFlow()

    private val _result = MutableStateFlow<TossupResult?>(null)
    val result: StateFlow<TossupResult?> = _result.asStateFlow()

    private val _score = MutableStateFlow(TossupScore())
    val score: StateFlow<TossupScore> = _score.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _buzzCountdown = MutableStateFlow<Float?>(null)
    val buzzCountdown: StateFlow<Float?> = _buzzCountdown.asStateFlow()

    private val _answerCountdown = MutableStateFlow<Float?>(null)
    val answerCountdown: StateFlow<Float?> = _answerCountdown.asStateFlow()

    private val _voiceDisabled = MutableStateFlow(false)
    val voiceDisabled: StateFlow<Boolean> = _voiceDisabled.asStateFlow()

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    @Volatile private var buzzedAfterDone = false
    @Volatile private var answerStarted = false
    private val submitting = AtomicBoolean(false)

    private var buzzTimerJob: Job? = null
    private var answerTimerJob: Job? = null
    private var listeningJob: Job? = null
    private var speechEngine: SpeechEngine? = null

    init {
        // Start buzz timer when TTS finishes while still in READING phase
        viewModelScope.launch {
            tts.done.collect { done ->
                if (done && _phase.value == TossupPhase.READING) {
                    val buzzTimer = settings.value.buzzTimer
                    if (buzzTimer > 0f) startBuzzTimer(buzzTimer)
                }
            }
        }
        // Apply persisted settings to TTS whenever they change
        viewModelScope.launch {
            settings.collect { s ->
                tts.setRate(s.rate)
                tts.setVoice(s.voiceName)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun fetchTossup() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            tts.reset()
            cancelTimers()
            destroySpeech()
            _phase.value = TossupPhase.IDLE
            _result.value = null
            _answer.value = ""
            _buzzIndex.value = -1
            _voiceDisabled.value = false
            buzzedAfterDone = false
            answerStarted = false
            submitting.set(false)

            try {
                val s = settings.value
                val resp = QbReaderService.getRandomTossup(
                    categories = s.categories,
                    difficulties = s.difficulties,
                )
                val t = resp.tossups.firstOrNull()
                if (t == null) {
                    _error.value = "No tossups found. Try different filters."
                    _loading.value = false
                    return@launch
                }
                _tossup.value = t
                val text = t.questionSanitized ?: t.question
                val rawWords = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
                val rawQ = t.question.split(Regex("\\s+")).filter { it.isNotEmpty() }
                _powerIndex.value = findPowerIndex(rawQ)
                _words.value = rawWords
                _phase.value = TossupPhase.READING
                _loading.value = false
                tts.speak(stripPowerMarker(rawWords))
            } catch (e: Exception) {
                _error.value = "Failed to fetch tossup: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Called directly from a button onClick (must be on the main thread so that
     * SpeechRecognizer.createSpeechRecognizer() is called from the UI thread).
     */
    fun buzz() {
        if (_phase.value != TossupPhase.READING) return
        buzzedAfterDone = tts.done.value
        cancelBuzzTimer()
        val stoppedAt = tts.stop()
        _buzzIndex.value = stoppedAt
        _phase.value = TossupPhase.BUZZING
        startSpeechEngine()
        val answerTimer = settings.value.answerTimer
        if (answerTimer > 0f) startAnswerTimer(answerTimer)
    }

    /** Update answer text (from TextField onValueChange — user typed). */
    fun updateAnswer(text: String) {
        answerStarted = true
        _answer.value = text
    }

    /** Stop voice recognition and disable it for this buzz (user started typing). */
    fun disableVoiceIfActive() {
        if (!_voiceDisabled.value) {
            destroySpeech()
            _voiceDisabled.value = true
        }
    }

    /**
     * Restart voice recognition. Call from a button onClick (main thread user gesture).
     * Only has effect while in BUZZING phase and voice is not disabled.
     */
    fun startVoice() {
        if (_voiceDisabled.value || _phase.value != TossupPhase.BUZZING || _listening.value) return
        startSpeechEngine()
    }

    /** Submit the current answer (or the supplied text override). */
    fun submitAnswer(answerText: String = _answer.value) {
        if (_phase.value != TossupPhase.BUZZING || answerText.isBlank()) return
        submitAnswerCandidates(listOf(answerText))
    }

    /**
     * Try each candidate in order against the API and accept the first one that returns
     * "accept" or "prompt". Falls back to the first candidate's result if all reject.
     * Used for voice input where the top STT result may be a homophone of the answer.
     */
    private fun submitAnswerCandidates(candidates: List<String>) {
        if (candidates.isEmpty()) return
        if (!submitting.compareAndSet(false, true)) return
        cancelAnswerTimer()
        destroySpeech()
        viewModelScope.launch {
            try {
                val t = _tossup.value ?: return@launch
                val expectedAnswer = t.answerSanitized ?: t.answer
                var usedAnswer = candidates.first()
                var res = QbReaderService.checkAnswer(usedAnswer, expectedAnswer)
                for (candidate in candidates.drop(1)) {
                    if (res.directive == "accept" || res.directive == "prompt") break
                    delay(50)
                    usedAnswer = candidate
                    res = QbReaderService.checkAnswer(candidate, expectedAnswer)
                }
                if (res.directive == "accept") _answer.value = usedAnswer
                val points = calcTossupPoints(res.directive, _powerIndex.value, _buzzIndex.value)
                _result.value = TossupResult(
                    directive = res.directive,
                    directedPrompt = res.directedPrompt,
                    points = points,
                    isPower = points == 15,
                    userAnswer = usedAnswer,
                )
                if (res.directive != "prompt") {
                    _score.value = _score.value.update(points)
                    _phase.value = TossupPhase.RESULT
                    if (points > 0) SoundEffects.playCorrect() else if (points < 0) SoundEffects.playWrong()
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

    fun saveSettings(s: TossupSettings) {
        viewModelScope.launch { settingsRepo.saveTossupSettings(s) }
    }

    // -------------------------------------------------------------------------
    // Timers
    // -------------------------------------------------------------------------

    private fun startBuzzTimer(seconds: Float) {
        cancelBuzzTimer()
        var remaining = (seconds * 10).toInt()
        _buzzCountdown.value = remaining / 10f
        buzzTimerJob = viewModelScope.launch {
            while (remaining > 0) {
                delay(100)
                remaining--
                _buzzCountdown.value = remaining / 10f
                if (remaining <= 0) {
                    _buzzCountdown.value = null
                    _result.value = TossupResult(directive = "reject", points = 0, timedOut = "buzz")
                    _score.value = _score.value.update(0)
                    _phase.value = TossupPhase.RESULT
                    return@launch
                }
            }
        }
    }

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
                    val points = if (buzzedAfterDone) 0 else -5
                    _result.value = TossupResult(directive = "reject", points = points, timedOut = "answer")
                    _score.value = _score.value.update(points)
                    _phase.value = TossupPhase.RESULT
                    return@launch
                }
            }
        }
    }

    private fun cancelBuzzTimer() {
        buzzTimerJob?.cancel()
        buzzTimerJob = null
        _buzzCountdown.value = null
    }

    private fun cancelAnswerTimer() {
        answerTimerJob?.cancel()
        answerTimerJob = null
        _answerCountdown.value = null
    }

    private fun cancelTimers() {
        cancelBuzzTimer()
        cancelAnswerTimer()
    }

    // -------------------------------------------------------------------------
    // Speech engine management
    // -------------------------------------------------------------------------

    private fun startSpeechEngine() {
        destroySpeech()
        if (!speechSupported) return
        val app = getApplication<Application>()
        val engine = SpeechEngine(
            context = app,
            onInterimResult = { text ->
                if (_phase.value == TossupPhase.BUZZING && !_voiceDisabled.value) {
                    answerStarted = true
                    _answer.value = text
                }
            },
            onFinalResults = { alternatives ->
                if (_phase.value == TossupPhase.BUZZING && alternatives.isNotEmpty()) {
                    _answer.value = alternatives.first()
                    submitAnswerCandidates(alternatives)
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
