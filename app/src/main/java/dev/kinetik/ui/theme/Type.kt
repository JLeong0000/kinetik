package dev.kinetik.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.kinetik.R

@OptIn(ExperimentalTextApi::class)
private fun archivo(w: Int) = Font(
    R.font.archivo, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(125f)),
)

@OptIn(ExperimentalTextApi::class)
private fun manrope(w: Int) = Font(R.font.manrope, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

@OptIn(ExperimentalTextApi::class)
private fun mono(w: Int) = Font(R.font.jetbrains_mono, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

/** Archivo at width 125 = "Archivo Expanded". */
val Archivo = FontFamily(archivo(700), archivo(800), archivo(900))
val Manrope = FontFamily(manrope(500), manrope(600), manrope(700), manrope(800))
val Mono = FontFamily(mono(500), mono(700))

object KText {
    fun display(size: TextUnit, weight: Int = 800) =
        TextStyle(fontFamily = Archivo, fontWeight = FontWeight(weight), fontSize = size, color = K.Text, letterSpacing = (-0.5).sp)

    fun mono(size: TextUnit) = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = size, color = K.Text)

    val body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = K.Text)
    val label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.7.sp, color = K.Muted)
}
