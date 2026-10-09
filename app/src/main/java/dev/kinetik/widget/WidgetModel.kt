package dev.kinetik.widget

import dev.kinetik.model.Library
import dev.kinetik.model.upNext
import dev.kinetik.model.summary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import dev.kinetik.plan.buildPlan
import dev.kinetik.plan.estimateSeconds
import dev.kinetik.session.Control
import dev.kinetik.session.Phase
import dev.kinetik.session.SessionState
import dev.kinetik.session.controlsFor
import dev.kinetik.ui.live.liveModel

/** What the home-screen widget shows: the live workout, or the up-next workout with Start. */
data class WidgetModel(
    val headline: String,
    val title: String,
    val big: String,
    val sub: String,
    val isRest: Boolean,
    val controls: List<Control>,
    /** Set when idle: the workout the Start button opens. */
    val startWorkoutId: String?,
)

fun widgetModel(s: SessionState?, lib: Library): WidgetModel {
    if (s == null) {
        val w = lib.upNext() ?: return WidgetModel("Kinetik", "No workouts", "", "", false, emptyList(), null)
        val minutes = (estimateSeconds(buildPlan(w)) + 59) / 60
        return WidgetModel("Up next", w.name, "", "${w.summary().substringBefore(" ·")} · ~$minutes min", false, emptyList(), w.id)
    }
    if (s.phase == Phase.FINISHED) {
        val next = lib.upNext()
        return WidgetModel(s.workoutName, "Workout complete", "", next?.let { "Up next: ${it.name}" }.orEmpty(), false, emptyList(), next?.id)
    }
    val m = liveModel(s)
    return WidgetModel(
        headline = "${m.workoutName} · ${m.section} ${m.circuitNumber}/${m.circuits}",
        title = m.name,
        // A max-time stopwatch would redraw the widget every second; the rest countdown is worth it.
        big = if (m.bigIsClock && !m.isRest) "MAX" else m.big,
        sub = if (m.isRest) m.nextName?.let { "Next: $it · ${m.nextReps}" }.orEmpty() else m.sub,
        isRest = m.isRest,
        controls = controlsFor(s),
        startWorkoutId = null,
    )
}

/** The widget's content over time: a new value only when what it shows changes (about once a second in a rest). */
fun widgetModels(session: Flow<SessionState?>, library: Flow<Library>): Flow<WidgetModel> =
    combine(session, library, ::widgetModel).distinctUntilChanged()
