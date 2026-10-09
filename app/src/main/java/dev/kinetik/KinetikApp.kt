package dev.kinetik

import android.app.Application
import android.content.Context
import dev.kinetik.cues.CuePlayer
import dev.kinetik.session.SessionController
import dev.kinetik.store.WorkoutStore
import dev.kinetik.widget.KinetikWidget
import dev.kinetik.widget.widgetModel
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class KinetikApp : Application() {
    lateinit var store: WorkoutStore
        private set
    lateinit var cues: CuePlayer
        private set
    lateinit var session: SessionController
        private set

    /** Expanded home-screen groups; null until Home first shows. Not saved, so a restart starts collapsed. */
    val expandedGroups = MutableStateFlow<Set<String>?>(null)

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
        // Redraw home-screen widgets only when what they show changes (about once a second during a rest).
        scope.launch {
            combine(session.state, store.library, ::widgetModel)
                .distinctUntilChanged()
                .collect { KinetikWidget().updateAll(this@KinetikApp) }
        }
    }
}

val Context.app: KinetikApp get() = applicationContext as KinetikApp
