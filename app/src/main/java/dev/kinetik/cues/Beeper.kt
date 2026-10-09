package dev.kinetik.cues

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/** A sine tone with 10 ms fades so it doesn't click. */
fun beepPcm(freqHz: Double, seconds: Double, sampleRate: Int = 44_100): ShortArray {
    val n = (sampleRate * seconds).toInt()
    val fade = sampleRate / 100.0
    return ShortArray(n) { i ->
        val env = minOf(1.0, i / fade, (n - 1 - i) / fade)
        (sin(2 * PI * freqHz * i / sampleRate) * env * Short.MAX_VALUE * 0.9).toInt().toShort()
    }
}

/**
 * Two prebuilt static tracks, replayed for every beep (no per-beep allocation, lower latency).
 * Audio failures are swallowed: a missed beep must never crash a running workout.
 */
class Beeper(attrs: AudioAttributes) {
    private val rate = 44_100
    private val short = runCatching { track(attrs, beepPcm(880.0, 0.15, rate)) }.getOrNull()
    private val long = runCatching { track(attrs, beepPcm(1_320.0, 0.6, rate)) }.getOrNull()

    /** 0f..1f */
    var volume = 0.8f

    fun play(isLong: Boolean) {
        val t = (if (isLong) long else short) ?: return
        runCatching {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.setVolume(volume)
            t.play()
        }
    }

    private fun track(attrs: AudioAttributes, pcm: ShortArray): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        t.write(pcm, 0, pcm.size)
        return t
    }
}
