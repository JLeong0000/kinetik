package dev.kinetik.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo

enum class LayoutMode { COVER, MAIN, TABLETOP }

data class Fold(val halfOpened: Boolean, val horizontal: Boolean)

/** Cover screen below 600 dp; half-folded with a horizontal hinge is flex (tabletop) mode. */
fun layoutModeFor(widthDp: Int, fold: Fold?): LayoutMode = when {
    fold != null && fold.halfOpened && fold.horizontal -> LayoutMode.TABLETOP
    widthDp >= 600 -> LayoutMode.MAIN
    else -> LayoutMode.COVER
}

fun WindowLayoutInfo.toFold(): Fold? =
    displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()?.let {
        Fold(
            halfOpened = it.state == FoldingFeature.State.HALF_OPENED,
            horizontal = it.orientation == FoldingFeature.Orientation.HORIZONTAL,
        )
    }

val LocalLayoutMode = staticCompositionLocalOf { LayoutMode.COVER }
