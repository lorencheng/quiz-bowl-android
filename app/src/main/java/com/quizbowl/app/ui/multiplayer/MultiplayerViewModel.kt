package com.quizbowl.app.ui.multiplayer

import android.app.Application
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.quizbowl.app.api.MultiplayerClient
import com.quizbowl.app.api.QbReaderService
import com.quizbowl.app.api.models.RoomInfo
import com.quizbowl.app.data.SettingsRepository
import com.quizbowl.app.engine.SpeechEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class MultiplayerView { LOBBY, ROOM }

data class MpPlayer(
    val userId: String,
    val username: String,
    val score: Int = 0,
)

data class MpChatMessage(
    val username: String,
    val text: String,
    val isSystem: Boolean = false,
)

data class MpAnswerResult(
    val username: String,
    val givenAnswer: String,
    val directive: String,
    val score: Int,
    val revealedAnswer: String? = null,
)

class MultiplayerViewModel(application: Application) : AndroidViewModel(application) {

    val speechSupported: Boolean = SpeechRecognizer.isRecognitionAvailable(application)

    private val settingsRepo = SettingsRepository(application)

    private var client: MultiplayerClient? = null
    private var eventJob: Job? = null

    // Direct TTS for per-word speaking (no word-tracking needed in multiplayer)
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // ── View ──────────────────────────────────────────────────────────────────

    private val _view = MutableStateFlow(MultiplayerView.LOBBY)
    val view: StateFlow<MultiplayerView> = _view.asStateFlow()

    // ── Lobby state ───────────────────────────────────────────────────────────

    private val _usernameInput = MutableStateFlow("")
    val usernameInput: StateFlow<String> = _usernameInput.asStateFlow()

    private val _roomNameInput = MutableStateFlow("")
    val roomNameInput: StateFlow<String> = _roomNameInput.asStateFlow()

    private val _rooms = MutableStateFlow<List<RoomInfo>>(emptyList())
    val rooms: StateFlow<List<RoomInfo>> = _rooms.asStateFlow()

    private val _roomsLoading = MutableStateFlow(false)
    val roomsLoading: StateFlow<Boolean> = _roomsLoading.asStateFlow()

    // ── Room state ────────────────────────────────────────────────────────────

    private val _connectedRoomName = MutableStateFlow("")
    val connectedRoomName: StateFlow<String> = _connectedRoomName.asStateFlow()

    private val _myUserId = MutableStateFlow<String?>(null)
    val myUserId: StateFlow<String?> = _myUserId.asStateFlow()

    private val _players = MutableStateFlow<List<MpPlayer>>(emptyList())
    val players: StateFlow<List<MpPlayer>> = _players.asStateFlow()

    private val _canBuzz = MutableStateFlow(false)
    val canBuzz: StateFlow<Boolean> = _canBuzz.asStateFlow()

    private val _buzzedPlayer = MutableStateFlow<MpPlayer?>(null)
    val buzzedPlayer: StateFlow<MpPlayer?> = _buzzedPlayer.asStateFlow()

    private val _questionWords = MutableStateFlow<List<String>>(emptyList())
    val questionWords: StateFlow<List<String>> = _questionWords.asStateFlow()

    private val _questionType = MutableStateFlow<String?>(null)
    val questionType: StateFlow<String?> = _questionType.asStateFlow()

    private val _bonusLeadin = MutableStateFlow<String?>(null)
    val bonusLeadin: StateFlow<String?> = _bonusLeadin.asStateFlow()

    private val _bonusParts = MutableStateFlow<List<String>>(emptyList())
    val bonusParts: StateFlow<List<String>> = _bonusParts.asStateFlow()

    private val _answerResult = MutableStateFlow<MpAnswerResult?>(null)
    val answerResult: StateFlow<MpAnswerResult?> = _answerResult.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<MpChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<MpChatMessage>> = _chatMessages.asStateFlow()

    private val _answer = MutableStateFlow("")
    val answer: StateFlow<String> = _answer.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(false)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _ttsRate = MutableStateFlow(1.0f)
    val ttsRate: StateFlow<Float> = _ttsRate.asStateFlow()

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    private val _voiceDisabled = MutableStateFlow(false)
    val voiceDisabled: StateFlow<Boolean> = _voiceDisabled.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    private var speechEngine: SpeechEngine? = null
    private var listeningJob: Job? = null
    private val submitting = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            settingsRepo.username.collect { username ->
                if (_usernameInput.value.isEmpty() && username.isNotEmpty()) {
                    _usernameInput.value = username
                }
            }
        }
        tts = TextToSpeech(application) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    // ── Lobby actions ─────────────────────────────────────────────────────────

    fun updateUsernameInput(text: String) { _usernameInput.value = text }
    fun updateRoomNameInput(text: String) { _roomNameInput.value = text }

    fun fetchRooms() {
        viewModelScope.launch {
            _roomsLoading.value = true
            try {
                _rooms.value = QbReaderService.getRoomList()
            } catch (_: Exception) {
                // Room list is optional — silently ignore failures
            } finally {
                _roomsLoading.value = false
            }
        }
    }

    fun joinRoom(roomName: String = _roomNameInput.value) {
        val username = _usernameInput.value.trim()
        val room = roomName.trim()
        if (username.isEmpty()) { _error.value = "Username is required."; return }
        if (room.isEmpty()) { _error.value = "Room name is required."; return }

        viewModelScope.launch { settingsRepo.saveUsername(username) }

        val userId = MultiplayerClient.generateUserId()
        _myUserId.value = userId
        _connecting.value = true
        _error.value = null

        val c = MultiplayerClient()
        client = c
        _connectedRoomName.value = room

        eventJob = viewModelScope.launch {
            c.events.collect { (type, msg) -> handleEvent(type, msg) }
        }
        c.connect(room, username, userId)
    }

    fun leaveRoom() {
        stopTts()
        destroySpeech()
        eventJob?.cancel()
        eventJob = null
        client?.disconnect()
        client = null
        _view.value = MultiplayerView.LOBBY
        _connecting.value = false
        clearRoomState()
    }

    fun dismissError() { _error.value = null }

    // ── Room actions ──────────────────────────────────────────────────────────

    /** Must be called from a Button onClick (main thread user gesture). */
    fun buzz() {
        if (!_canBuzz.value || _buzzedPlayer.value != null) return
        client?.buzz()
        startSpeechEngine()
    }

    fun updateAnswer(text: String) {
        _answer.value = text
        if (text.isNotEmpty()) client?.giveAnswerLiveUpdate(text)
    }

    fun disableVoiceIfActive() {
        if (!_voiceDisabled.value) {
            destroySpeech()
            _voiceDisabled.value = true
        }
    }

    /** Must be called from a Button onClick (main thread user gesture). */
    fun startVoice() {
        if (_voiceDisabled.value || _listening.value) return
        startSpeechEngine()
    }

    fun submitAnswer(answerText: String = _answer.value) {
        if (answerText.isBlank()) return
        if (!submitting.compareAndSet(false, true)) return
        destroySpeech()
        client?.giveAnswer(answerText.trim())
        _answer.value = ""
        submitting.set(false)
    }

    fun requestNext() = client?.next()

    fun updateChatInput(text: String) { _chatInput.value = text }

    fun sendChat() {
        val msg = _chatInput.value.trim()
        if (msg.isEmpty()) return
        client?.chat(msg)
        _chatInput.value = ""
    }

    fun setTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled
        if (!enabled) stopTts()
    }

    fun setTtsRate(rate: Float) {
        _ttsRate.value = rate
        tts?.setSpeechRate(rate)
    }

    // ── WebSocket event handler ───────────────────────────────────────────────

    private fun handleEvent(type: String, msg: JsonObject) {
        try {
        when (type) {
            "_connected" -> {
                _view.value = MultiplayerView.ROOM
                _connecting.value = false
                _error.value = null
            }
            "_disconnected" -> {
                val reason = msg.get("reason")?.asString ?: ""
                if (_view.value == MultiplayerView.ROOM || _connecting.value) {
                    stopTts()
                    destroySpeech()
                    eventJob?.cancel()
                    eventJob = null
                    client = null
                    _view.value = MultiplayerView.LOBBY
                    _connecting.value = false
                    if (reason.isNotEmpty()) _error.value = "Disconnected: $reason"
                    clearRoomState()
                }
            }
            "_error" -> {
                _connecting.value = false
                _error.value = msg.get("error")?.asString ?: "Connection error"
            }
            "connection-acknowledged" -> {
                val playersArr = msg.getAsJsonArray("players")
                if (playersArr != null) {
                    _players.value = playersArr.mapNotNull { e ->
                        if (e.isJsonObject) parsePlayer(e.asJsonObject) else null
                    }
                }
                _canBuzz.value = msg.get("canBuzz")?.asBoolean ?: false
                _questionType.value = msg.get("currentQuestionType")?.asString
            }
            "connection-acknowledged-question" -> {
                _questionType.value = msg.get("currentQuestionType")?.asString
            }
            "join" -> {
                val userId = msg.get("userId")?.asString ?: return
                val username = msg.get("username")?.asString ?: return
                if (_players.value.none { it.userId == userId }) {
                    _players.value = _players.value + MpPlayer(userId, username)
                }
                addSystemChat("$username joined")
            }
            "leave" -> {
                val userId = msg.get("userId")?.asString ?: return
                val username = msg.get("username")?.asString ?: ""
                _players.value = _players.value.filter { it.userId != userId }
                addSystemChat("$username left")
            }
            "start-next-tossup" -> {
                stopTts()
                _questionWords.value = emptyList()
                _questionType.value = "tossup"
                _canBuzz.value = true
                _buzzedPlayer.value = null
                _answerResult.value = null
                _answer.value = ""
                _voiceDisabled.value = false
            }
            "start-next-bonus" -> {
                stopTts()
                _questionWords.value = emptyList()
                _questionType.value = "bonus"
                _bonusLeadin.value = null
                _bonusParts.value = emptyList()
                _canBuzz.value = false
                _buzzedPlayer.value = null
                _answerResult.value = null
                _answer.value = ""
                _voiceDisabled.value = false
            }
            "update-question" -> {
                val word = msg.get("word")?.asString ?: return
                _questionWords.value = _questionWords.value + word
                speakWord(word)
            }
            "reveal-leadin" -> {
                val leadin = msg.get("leadin")?.asString ?: return
                _bonusLeadin.value = leadin
                speakText(leadin)
            }
            "reveal-next-part" -> {
                val part = msg.get("part")?.asString ?: return
                _bonusParts.value = _bonusParts.value + part
                speakText(part)
            }
            "buzz" -> {
                val userId = msg.get("userId")?.asString ?: return
                val username = msg.get("username")?.asString ?: ""
                val player = _players.value.find { it.userId == userId }
                    ?: MpPlayer(userId, username)
                _buzzedPlayer.value = player
                _canBuzz.value = false
                stopTts()
            }
            "give-tossup-answer" -> {
                val userId = msg.get("userId")?.asString ?: return
                val username = msg.get("username")?.asString ?: ""
                val directive = msg.get("directive")?.asString ?: "reject"
                val score = msg.get("score")?.asInt ?: 0
                val revealedAnswer = msg.get("revealedAnswer")?.asString
                val givenAnswer = msg.get("givenAnswer")?.asString ?: ""
                _answerResult.value = MpAnswerResult(username, givenAnswer, directive, score, revealedAnswer)
                updatePlayerScore(userId, score)
                if (directive != "prompt") {
                    _buzzedPlayer.value = null
                    if (userId == _myUserId.value) destroySpeech()
                }
            }
            "give-bonus-answer" -> {
                val userId = msg.get("userId")?.asString ?: return
                val username = msg.get("username")?.asString ?: ""
                val directive = msg.get("directive")?.asString ?: "reject"
                val score = msg.get("score")?.asInt ?: 0
                val givenAnswer = msg.get("givenAnswer")?.asString ?: ""
                _answerResult.value = MpAnswerResult(username, givenAnswer, directive, score)
                updatePlayerScore(userId, score)
                if (userId == _myUserId.value) destroySpeech()
            }
            "reveal-tossup-answer" -> {
                val revealedAnswer = msg.get("answer")?.asString ?: return
                _answerResult.value = _answerResult.value?.copy(revealedAnswer = revealedAnswer)
            }
            "end-current-tossup", "end-current-bonus" -> {
                _canBuzz.value = false
                _buzzedPlayer.value = null
                destroySpeech()
            }
            "chat" -> {
                val username = msg.get("username")?.asString ?: ""
                val message = msg.get("message")?.asString ?: return
                addChat(username, message)
            }
            "pause" -> {
                val paused = msg.get("paused")?.asBoolean ?: false
                if (paused) stopTts()
            }
            "error" -> {
                _error.value = msg.get("message")?.asString ?: "Server error"
            }
            "enforcing-removal" -> {
                val removalType = msg.get("removalType")?.asString ?: "removed"
                _error.value = "You have been ${removalType}d from the room."
                leaveRoom()
            }
        }
        } catch (e: Exception) {
            // Prevent any unexpected JSON parsing exception from crashing the app
        }
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    private fun speakWord(word: String) {
        if (!_ttsEnabled.value || !ttsReady) return
        tts?.setSpeechRate(_ttsRate.value)
        tts?.speak(word, TextToSpeech.QUEUE_ADD, null, "mp_word")
    }

    private fun speakText(text: String) {
        if (!_ttsEnabled.value || !ttsReady) return
        tts?.setSpeechRate(_ttsRate.value)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "mp_text")
    }

    private fun stopTts() { tts?.stop() }

    // ── Speech engine ─────────────────────────────────────────────────────────

    private fun startSpeechEngine() {
        destroySpeech()
        if (!speechSupported) return
        val app = getApplication<Application>()
        val myId = _myUserId.value
        val engine = SpeechEngine(
            context = app,
            onInterimResult = { text ->
                if (_buzzedPlayer.value?.userId == myId && !_voiceDisabled.value) {
                    _answer.value = text
                    client?.giveAnswerLiveUpdate(text)
                }
            },
            onFinalResult = { text ->
                if (_buzzedPlayer.value?.userId == myId) {
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parsePlayer(obj: JsonObject): MpPlayer? {
        val userId = obj.get("odId")?.asString ?: obj.get("userId")?.asString ?: return null
        val username = obj.get("odUsername")?.asString ?: obj.get("username")?.asString ?: return null
        val score = obj.get("score")?.asInt ?: 0
        return MpPlayer(userId, username, score)
    }

    private fun updatePlayerScore(userId: String, newScore: Int) {
        _players.value = _players.value.map { p ->
            if (p.userId == userId) p.copy(score = newScore) else p
        }
    }

    private fun addChat(username: String, text: String) {
        _chatMessages.value = (_chatMessages.value + MpChatMessage(username, text)).takeLast(50)
    }

    private fun addSystemChat(text: String) {
        _chatMessages.value = (_chatMessages.value + MpChatMessage("", text, isSystem = true)).takeLast(50)
    }

    private fun clearRoomState() {
        _players.value = emptyList()
        _canBuzz.value = false
        _buzzedPlayer.value = null
        _questionWords.value = emptyList()
        _questionType.value = null
        _bonusLeadin.value = null
        _bonusParts.value = emptyList()
        _answerResult.value = null
        _chatMessages.value = emptyList()
        _answer.value = ""
        _chatInput.value = ""
        _voiceDisabled.value = false
        _connectedRoomName.value = ""
        _myUserId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
        tts?.shutdown()
        destroySpeech()
        eventJob?.cancel()
        client?.disconnect()
    }
}
