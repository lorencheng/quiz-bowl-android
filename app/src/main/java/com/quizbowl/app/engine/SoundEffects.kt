package com.quizbowl.app.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

object SoundEffects {

    private const val SAMPLE_RATE = 44100

    // Bright ascending arpeggio: D5 → F#5 → A5
    private val correctSamples: ShortArray by lazy {
        concat(
            tone(587.33, 65),
            silence(12),
            tone(739.99, 65),
            silence(12),
            tone(880.00, 110, fadeOutPct = 0.45f),
        )
    }

    // Gentle descending interval: G4 → D4 (neutral, not harsh)
    private val wrongSamples: ShortArray by lazy {
        concat(
            tone(392.00, 80),
            silence(12),
            tone(293.66, 120, fadeOutPct = 0.5f),
        )
    }

    fun playCorrect() = play(correctSamples)
    fun playWrong() = play(wrongSamples)

    private fun tone(
        freqHz: Double,
        durationMs: Int,
        volume: Float = 0.72f,
        fadeInPct: Float = 0.08f,
        fadeOutPct: Float = 0.22f,
    ): ShortArray {
        val n = durationMs * SAMPLE_RATE / 1000
        val fadeIn = (n * fadeInPct).toInt()
        val fadeOutStart = n - (n * fadeOutPct).toInt()
        return ShortArray(n) { i ->
            val raw = sin(2 * PI * freqHz * i / SAMPLE_RATE)
            val env = when {
                i < fadeIn -> i.toDouble() / fadeIn
                i > fadeOutStart -> (n - i).toDouble() / (n - fadeOutStart)
                else -> 1.0
            }
            (raw * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun silence(durationMs: Int) = ShortArray(durationMs * SAMPLE_RATE / 1000)

    private fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var offset = 0
        for (part in parts) { part.copyInto(out, offset); offset += part.size }
        return out
    }

    private fun play(samples: ShortArray) {
        thread(isDaemon = true) {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(samples.size * 1000L / SAMPLE_RATE + 50)
            track.stop()
            track.release()
        }
    }
}
