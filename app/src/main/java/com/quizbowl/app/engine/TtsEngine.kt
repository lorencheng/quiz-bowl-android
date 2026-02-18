package com.quizbowl.app.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Android TTS engine wrapping TextToSpeech.
 * Port of useTTS.js — but cleaner because Android's UtteranceProgressListener.onRangeStart()
 * provides native word-boundary callbacks (no chunking hack needed).
 *
 * Usage:
 *   val tts = TtsEngine(context)
 *   tts.speak(words)
 *   // observe: tts.wordIndex, tts.speaking, tts.paused, tts.done
 *   tts.shutdown() // call in onDestroy
 */
class TtsEngine(context: Context) {

    private val _speaking = MutableStateFlow(false)
    private val _paused = MutableStateFlow(false)
    private val _wordIndex = MutableStateFlow(-1)
    private val _done = MutableStateFlow(false)
    private val _voices = MutableStateFlow<List<android.speech.tts.Voice>>(emptyList())

    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()
    val paused: StateFlow<Boolean> = _paused.asStateFlow()
    val wordIndex: StateFlow<Int> = _wordIndex.asStateFlow()
    val done: StateFlow<Boolean> = _done.asStateFlow()
    val voices: StateFlow<List<android.speech.tts.Voice>> = _voices.asStateFlow()

    private var words: List<String> = emptyList()
    private var currentWordIndex = -1
    private var cancelled = false
    private var speechRate = 1.0f
    private var selectedVoiceName: String? = null
    private var currentUtteranceId: String? = null

    // Character offset → word index map for onRangeStart
    private var charToWord: List<Pair<Int, Int>> = emptyList() // (charOffset, wordIndex)

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            _voices.value = tts.voices?.toList() ?: emptyList()
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (cancelled || utteranceId != currentUtteranceId) return
                advanceWordIndex(0)
            }

            override fun onDone(utteranceId: String?) {
                if (cancelled || utteranceId != currentUtteranceId) return
                advanceWordIndex(words.size - 1)
                _speaking.value = false
                _paused.value = false
                _done.value = true
            }

            override fun onError(utteranceId: String?) {
                if (cancelled) return
                _speaking.value = false
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                if (cancelled || utteranceId != currentUtteranceId) return
                // Map character offset to word index
                var wi = 0
                for ((charOffset, wordIdx) in charToWord.reversed()) {
                    if (start >= charOffset) {
                        wi = wordIdx
                        break
                    }
                }
                advanceWordIndex(wi)
            }
        })
    }

    fun setRate(rate: Float) {
        speechRate = rate
        tts.setSpeechRate(rate)
    }

    fun setVoice(voiceName: String?) {
        selectedVoiceName = voiceName
        if (voiceName != null) {
            val voice = tts.voices?.find { it.name == voiceName }
            if (voice != null) tts.voice = voice
        }
    }

    fun speak(words: List<String>) {
        cancel()
        this.words = words
        cancelled = false
        currentWordIndex = -1
        _wordIndex.value = -1
        _done.value = false
        _speaking.value = true
        _paused.value = false

        if (words.isEmpty()) {
            _speaking.value = false
            _done.value = true
            return
        }

        // Build char offset → word index map
        val map = mutableListOf<Pair<Int, Int>>()
        var charPos = 0
        for ((i, word) in words.withIndex()) {
            map.add(charPos to i)
            charPos += word.length + 1
        }
        charToWord = map

        val text = words.joinToString(" ")
        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId

        val params = Bundle()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /** Pause TTS playback. */
    fun pause() {
        tts.stop()
        _paused.value = true
        _speaking.value = false
    }

    /** Resume (re-speak from current word). */
    fun resume() {
        if (!_paused.value) return
        val remaining = words.drop(currentWordIndex.coerceAtLeast(0))
        if (remaining.isEmpty()) return
        _paused.value = false
        _speaking.value = true

        // Rebuild char map for the remaining words
        val fullOffset = words.take(currentWordIndex.coerceAtLeast(0)).sumOf { it.length + 1 }
        val map = mutableListOf<Pair<Int, Int>>()
        var charPos = 0
        for ((i, word) in remaining.withIndex()) {
            map.add((fullOffset + charPos) to (currentWordIndex + i))
            charPos += word.length + 1
        }
        charToWord = map

        val text = remaining.joinToString(" ")
        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
    }

    /** Stop TTS and return the word index at which it was stopped. */
    fun stop(): Int {
        cancelled = true
        tts.stop()
        val stoppedAt = currentWordIndex
        _speaking.value = false
        _paused.value = false
        return stoppedAt
    }

    /** Reset all state. */
    fun reset() {
        cancel()
        words = emptyList()
        currentWordIndex = -1
        _wordIndex.value = -1
        _done.value = false
        _speaking.value = false
        _paused.value = false
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun cancel() {
        cancelled = true
        tts.stop()
    }

    private fun advanceWordIndex(newIndex: Int) {
        val advanced = maxOf(currentWordIndex, newIndex)
        currentWordIndex = advanced
        _wordIndex.value = advanced
    }
}
