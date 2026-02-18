package com.quizbowl.app.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android speech-to-text engine wrapping SpeechRecognizer.
 * Port of useSpeechRecognition.js.
 *
 * - Call start() from a user gesture (Button onClick) — required on Android.
 * - Shows interim results via onInterimResult callback.
 * - Calls onFinalResult when the user pauses speaking.
 * - Call stop() to cancel recognition (e.g., when user starts typing).
 *
 * Usage:
 *   val speech = SpeechEngine(context,
 *       onInterimResult = { text -> ... },
 *       onFinalResult = { text -> ... }
 *   )
 *   speech.start()   // in Button onClick
 *   speech.stop()
 *   speech.destroy() // in onDestroy
 */
class SpeechEngine(
    private val context: Context,
    private val onInterimResult: (String) -> Unit = {},
    private val onFinalResult: (String) -> Unit = {},
) {

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    val supported: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private var recognizer: SpeechRecognizer? = null

    fun start() {
        if (!supported) return
        stop() // Clean up any existing instance

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _listening.value = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _listening.value = false
                }

                override fun onError(error: Int) {
                    _listening.value = false
                    // SpeechRecognizer.ERROR_NO_MATCH and ERROR_SPEECH_TIMEOUT are expected
                }

                override fun onResults(results: Bundle?) {
                    _listening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: return
                    onInterimResult(text)
                    onFinalResult(text.trim())
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: return
                    onInterimResult(text)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            startListening(intent)
        }
    }

    fun stop() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        _listening.value = false
    }

    fun destroy() {
        stop()
    }
}
