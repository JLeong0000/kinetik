package dev.kinetik.widget

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import dev.kinetik.session.Control
import dev.kinetik.session.Phase
import dev.kinetik.session.SessionState
import dev.kinetik.session.controlsFor
import dev.kinetik.ui.live.liveModel

/** What the home-screen widget shows: the live workout, or "Start a workout" (which opens the app) when idle. */
data class WidgetModel(
    val headline: String,
    val title: String,
    val big: String,
    val sub: String,
    val isRest: Boolean,
    val controls: List<Control>,
    /** Idle: a Start button that opens the app to pick a workout. */
    val showStart: Boolean,
)

fun widgetModel(s: SessionState?): WidgetModel {
    if (s == null) return WidgetModel("Kinetik", "Start a workout", "", "", false, emptyList(), showStart = true)
    if (s.phase == Phase.FINISHED) return WidgetModel(s.workoutName, "Workout complete", "", "", false, emptyList(), showStart = false)
    val m = liveModel(s)
    return WidgetModel(
        headline = "${m.workoutName} · ${m.section} ${m.circuitNumber}/${m.circuits}",
        title = m.name,
        // A max-time stopwatch would redraw the widget every second; the rest countdown is worth it.
        big = if (m.bigIsClock && !m.isRest) "MAX" else m.big,
        sub = if (m.isRest) m.nextName?.let { "Next: $it · ${m.nextReps}" }.orEmpty() else m.sub,
        isRest = m.isRest,
        controls = controlsFor(s),
        showStart = false,
    )
}

/** The widget's content over time: a new value only when what it shows changes (about once a second in a rest). */
fun widgetModels(session: Flow<SessionState?>): Flow<WidgetModel> =
    session.map(::widgetModel).distinctUntilChanged()
