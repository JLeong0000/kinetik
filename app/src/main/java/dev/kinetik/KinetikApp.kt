package dev.kinetik

import android.app.Application
import android.content.Context
import dev.kinetik.cues.CuePlayer
import dev.kinetik.session.SessionController
import dev.kinetik.store.WorkoutStore
import dev.kinetik.widget.KinetikWidget
import dev.kinetik.widget.widgetModel
import dev.kinetik.widget.widgetModels
import dev.kinetik.widget.WidgetModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class KinetikApp : Application() {
    lateinit var store: WorkoutStore
        private set
    lateinit var cues: CuePlayer
        private set
    lateinit var session: SessionController
        private set
    lateinit var widget: StateFlow<WidgetModel>
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
        // Widgets read this deduped model, and are poked only when it changes (about once a second during a rest).
        widget = widgetModels(session.state, store.library)
            .stateIn(scope, SharingStarted.Eagerly, widgetModel(session.state.value, store.library.value))
        scope.launch {
            widget.collect { runCatching { KinetikWidget().updateAll(this@KinetikApp) } }
        }
    }
}

val Context.app: KinetikApp get() = applicationContext as KinetikApp
