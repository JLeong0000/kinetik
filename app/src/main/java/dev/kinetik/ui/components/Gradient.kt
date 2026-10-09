package dev.kinetik.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Start and end points matching CSS `linear-gradient(<angle>deg, …)` over a width × height box. */
fun cssGradientEndpoints(angleDeg: Float, width: Float, height: Float): Pair<Offset, Offset> {
    val rad = angleDeg * PI / 180
    val dx = sin(rad).toFloat()
    val dy = -cos(rad).toFloat()
    val half = (abs(width * dx) + abs(height * dy)) / 2
    val c = Offset(width / 2, height / 2)
    return Offset(c.x - dx * half, c.y - dy * half) to Offset(c.x + dx * half, c.y + dy * half)
}
