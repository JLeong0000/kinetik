package dev.kinetik.session

/** How long after an accepted button press further presses are ignored. */
const val PRESS_WINDOW_MS = 300L

/** Drops a button press that comes within [windowMs] of the last accepted one (accidental double taps). */
class PressGate(private val windowMs: Long = PRESS_WINDOW_MS) {
    private var lastAccepted: Long? = null

    fun allow(nowMs: Long): Boolean {
        val last = lastAccepted
        if (last != null && nowMs - last < windowMs) return false
        lastAccepted = nowMs
        return true
    }
}
