package dev.kinetik.session

/** How long after an accepted button press further presses are ignored. */
const val PRESS_WINDOW_MS = 300L

/** After DONE the rest controls appear under the thumb, so the next press waits longer. */
const val AFTER_DONE_HOLD_MS = 700L

/** Drops a button press that comes too soon after the last accepted one (accidental double taps). */
class PressGate(private val windowMs: Long = PRESS_WINDOW_MS) {
    private var blockedUntil: Long? = null

    /** [holdMs] is how long this press, if accepted, blocks the next one. */
    fun allow(nowMs: Long, holdMs: Long = windowMs): Boolean {
        val until = blockedUntil
        if (until != null && nowMs < until) return false
        blockedUntil = nowMs + holdMs
        return true
    }
}
