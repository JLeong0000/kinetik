package dev.kinetik.cues

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Speaks and beeps. Each cue briefly takes audio focus with ducking, so music dips rather than stops.
 * If focus is refused (e.g. during a call) the cue is dropped, not queued.
 */
class CuePlayer(context: Context) : TextToSpeech.OnInitListener {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .build()
    private val abandon = Runnable { audio.abandonAudioFocusRequest(focus) }
    private val beeper = Beeper(attrs)
    private val tts = TextToSpeech(context.applicationContext, this)

    private val _voiceReady = MutableStateFlow(false)
    val voiceReady: StateFlow<Boolean> = _voiceReady.asStateFlow()

    var voiceOn = true

    /** 0..100 */
    var beepVolume = 80
        set(value) {
            field = value
            beeper.volume = value / 100f
        }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.setAudioAttributes(attrs)
        val lang = tts.setLanguage(Locale.getDefault())
        _voiceReady.value = lang >= TextToSpeech.LANG_AVAILABLE
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = releaseFocusSoon(300)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = releaseFocusSoon(0)
        })
    }

    /** Plays cues in order. Speech after a long beep waits for the beep to finish. */
    fun play(cues: List<Cue>) {
        var delay = 0L
        for (cue in cues) {
            main.postDelayed({ playNow(cue) }, delay)
            if (cue == Cue.LongBeep) delay += 650
        }
    }

    private fun playNow(cue: Cue) {
        if (cue is Cue.Speak && (!voiceOn || !_voiceReady.value)) return
        if (!takeFocus()) return
        when (cue) {
            is Cue.Speak -> tts.speak(cue.text, TextToSpeech.QUEUE_FLUSH, null, cue.text)
            Cue.ShortBeep -> { beeper.play(false); releaseFocusSoon(450) }
            Cue.LongBeep -> { beeper.play(true); releaseFocusSoon(900) }
        }
    }

    private fun takeFocus(): Boolean {
        main.removeCallbacks(abandon)
        return audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun releaseFocusSoon(ms: Long) {
        main.removeCallbacks(abandon)
        main.postDelayed(abandon, ms)
    }
}
