package dev.kinetik.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import dev.kinetik.ui.theme.K

/** Arc centred on a circle inset by [inset] from the edge; a wider [stroke] at the same inset makes a halo. */
private fun DrawScope.ring(color: Color, start: Float, sweep: Float, stroke: Float, inset: Float = stroke) {
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
    drawArc(color, start, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
}

/** One segment per item; [active] glows teal, segments before [done] are dim teal. Pass active = -1 for none. */
@Composable
fun SegmentedRing(segments: Int, active: Int, done: Int, modifier: Modifier, stroke: Dp, gapDeg: Float = 9f) {
    Canvas(modifier) {
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
