package dev.kinetik

import android.app.Application
import android.content.Context
import dev.kinetik.cues.CuePlayer
import dev.kinetik.session.SessionController
import dev.kinetik.store.WorkoutStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class KinetikApp : Application() {
    lateinit var store: WorkoutStore
        private set
    lateinit var cues: CuePlayer
        private set
    lateinit var session: SessionController
        private set

    override fun onCreate() {
        super.onCreate()
        val scope = MainScope()
        store = WorkoutStore(filesDir)
        cues = CuePlayer(this)
        session = SessionController(this, store, cues, scope)
        scope.launch {
            store.library.collect {
                cues.voiceOn = it.settings.voiceOn
                cues.beepVolume = it.settings.beepVolume
            }
        }
    }
}

val Context.app: KinetikApp get() = applicationContext as KinetikApp
