package dev.kinetik.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import dev.kinetik.ui.theme.K
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** Arc centred on a circle inset by [inset] from the edge; a wider [stroke] at the same inset makes a halo. */
private fun DrawScope.ring(color: Color, start: Float, sweep: Float, stroke: Float, inset: Float = stroke) {
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
    drawArc(color, start, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
}

/** One segment per item; [active] glows teal, segments before [done] are dim teal. Pass active = -1 for none. */
@Composable
fun SegmentedRing(segments: Int, active: Int, done: Int, modifier: Modifier, stroke: Dp, gapDeg: Float = 9f, onTap: ((Int) -> Unit)? = null) {
    // The live screen recomposes every second with a new lambda; reading the latest one keeps the detector from restarting mid-tap.
    val latest by rememberUpdatedState(onTap)
    val tap = if (onTap == null) Modifier else Modifier.pointerInput(segments) {
        detectTapGestures { o -> ringSegmentAt(o.x, o.y, size.width.toFloat(), stroke.toPx(), segments)?.let { latest?.invoke(it) } }
    }
    Canvas(modifier.then(tap)) {
        val w = stroke.toPx()
        val n = segments.coerceAtLeast(1)
        val span = 360f / n
        for (i in 0 until n) {
            val start = -90f + i * span + gapDeg
            val sweep = (span - gapDeg * 2).coerceAtLeast(1f)
            val color = when {
                i == active -> K.Teal
                i < done -> K.TealDim
                else -> K.Track
            }
            ring(color, start, sweep, w)
        }
    }
}

/**
 * The segment of an [n]-segment ring (numbered clockwise from 12 o'clock) under a tap at ([x], [y]) in its [side] px box,
 * or null when the tap is off the ring. The hit band is wider than the stroke so the thin arc is easy to hit.
 */
fun ringSegmentAt(x: Float, y: Float, side: Float, stroke: Float, n: Int): Int? {
    val c = side / 2
    if (n < 1 || abs(hypot(x - c, y - c) - (c - stroke)) > stroke * 2) return null
    val deg = (Math.toDegrees(atan2((y - c).toDouble(), (x - c).toDouble())) + 450) % 360
    return (deg / (360.0 / n)).toInt().coerceAtMost(n - 1)
}

/** Amber ring that drains clockwise from the top as [fraction] goes 1 → 0. */
@Composable
fun RestRing(fraction: Float, modifier: Modifier, stroke: Dp) {
    Canvas(modifier) {
        val w = stroke.toPx()
        ring(K.Track, 0f, 360f, w)
        val sweep = 360f * fraction.coerceIn(0f, 1f)
        if (sweep > 0f) {
            ring(K.Rest, -90f, sweep, w)
        }
    }
}

/** A square of side min([max], available width, available height), so a ring always stays a circle. */
fun Modifier.ringBox(max: Dp): Modifier = layout { measurable, constraints ->
    val side = minOf(max.roundToPx(), constraints.maxWidth, constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(side, side))
    layout(side, side) { placeable.place(0, 0) }
}
