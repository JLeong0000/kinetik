package dev.kinetik.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/** Fixed-height rows; long-press and drag a row to move it. The dragged row lifts (no tilt). */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    rowHeight: Dp,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, index: Int, lifted: Boolean) -> Unit,
) {
    var dragging by remember { mutableIntStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    Column(modifier) {
        items.forEachIndexed { index, item ->
            key(key(item)) {
                val lifted = index == dragging
                Box(
                    Modifier
                        .height(rowHeight)
                        .zIndex(if (lifted) 1f else 0f)
                        .offset { IntOffset(0, if (lifted) offset.roundToInt() else 0) }
                        .pointerInput(index, items.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragging = index; offset = 0f },
                                onDrag = { change, amount -> change.consume(); offset += amount.y },
                                onDragEnd = {
                                    val target = (index + (offset / rowPx).roundToInt()).coerceIn(0, items.lastIndex)
                                    dragging = -1; offset = 0f
                                    if (target != index) onMove(index, target)
                                },
                                onDragCancel = { dragging = -1; offset = 0f },
                            )
                        },
                ) { row(item, index, lifted) }
            }
        }
    }
}
