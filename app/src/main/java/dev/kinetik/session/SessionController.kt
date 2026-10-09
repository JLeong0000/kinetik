package dev.kinetik.session

import android.content.Context
import android.os.SystemClock
import dev.kinetik.cues.CuePlayer
import dev.kinetik.cues.cuesFor
import dev.kinetik.model.workout
import dev.kinetik.service.WorkoutService
import dev.kinetik.store.WorkoutStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Owns the running session. Everything runs on the main thread, so no locking is needed. */
class SessionController(
    private val context: Context,
    private val store: WorkoutStore,
    private val cues: CuePlayer,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = _state.asStateFlow()
    private var loop: Job? = null
    private val presses = PressGate()

    fun start(workoutId: String) {
        if (!canStart(_state.value)) return
        val w = store.library.value.workout(workoutId) ?: return
        loop?.cancel()
        apply(null, null, SessionState.start(w))
        WorkoutService.start(context)
        loop = scope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(100)
                val now = SystemClock.elapsedRealtime()
                send(SessionEvent.Tick(now - last))
                last = now
            }
        }
    }

    /** Ticks always apply; button presses (UI, notification, headphones) pass through [PressGate]. */
    fun send(e: SessionEvent) {
        val prev = _state.value ?: return
        if (e !is SessionEvent.Tick) {
            val hold = if (e == SessionEvent.Done) AFTER_DONE_HOLD_MS else PRESS_WINDOW_MS
            if (!presses.allow(SystemClock.elapsedRealtime(), hold)) return
        }
        val next = reduce(prev, e)
        if (next != prev) apply(prev, e, next)
    }

    fun primaryAction() {
        _state.value?.let(::primaryEvent)?.let(::send)
    }

    fun stop() {
        loop?.cancel()
        _state.value = null
        WorkoutService.stop(context)
    }

    private fun apply(prev: SessionState?, e: SessionEvent?, next: SessionState) {
        _state.value = next
        cues.play(cuesFor(prev, e, next))
        if (next.phase == Phase.FINISHED && prev?.phase != Phase.FINISHED) {
            loop?.cancel()
            store.update { it.copy(lastCompletedWorkoutId = next.workoutId) }
        }
    }
}
