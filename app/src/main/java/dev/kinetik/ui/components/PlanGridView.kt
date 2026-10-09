package dev.kinetik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kinetik.model.Workout
import dev.kinetik.plan.GridCell
import dev.kinetik.plan.planGrid
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KText

private val CellH = 34.dp
private val Gap = 6.dp
private val CellShape = RoundedCornerShape(10.dp)

/** Circuits as columns, exercises as rows; overrides are striped and a whole-circuit override spans the column. */
@Composable
fun PlanGridView(w: Workout, showLabels: Boolean, modifier: Modifier = Modifier) {
    val cols = remember(w) { planGrid(w) }
    val rows = w.exercises.size.coerceAtLeast(1)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Gap)) {
        if (showLabels) {
            Column(Modifier.width(92.dp), verticalArrangement = Arrangement.spacedBy(Gap)) {
                Box(Modifier.height(14.dp))
                w.exercises.forEach {
                    Box(Modifier.height(CellH), contentAlignment = Alignment.CenterStart) {
                        Text(it.name, style = KText.body.copy(fontSize = 11.sp, color = K.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        cols.forEach { col ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Gap)) {
                Text(
                    "C${col.circuit + 1}", Modifier.fillMaxWidth().height(14.dp),
                    style = KText.label, textAlign = TextAlign.Center,
                )
                if (col.whole != null) {
                    Box(
                        Modifier.fillMaxWidth().height(CellH * rows + Gap * (rows - 1))
                            .clip(CellShape).striped().border(1.dp, K.TealHi.copy(alpha = 0.32f), CellShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            col.whole, Modifier.verticalText(),
                            style = KText.body.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = K.TealHi),
                            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    col.cells.forEach { GridCellView(it) }
                }
            }
        }
    }
}

@Composable
private fun GridCellView(cell: GridCell) {
    val base = Modifier.fillMaxWidth().height(CellH).clip(CellShape)
    when (cell) {
        is GridCell.Reps -> Box(base.background(K.Card), contentAlignment = Alignment.Center) {
            Text("${cell.reps}", style = KText.mono(14.sp))
        }
        is GridCell.Max -> Box(base.background(K.Card), contentAlignment = Alignment.Center) {
            Text(cell.label, style = KText.mono(11.sp).copy(color = K.TealHi))
        }
        is GridCell.Swapped -> Box(
            base.striped().border(1.dp, K.TealHi.copy(alpha = 0.32f), CellShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(cell.label, style = KText.body.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = K.TealHi), maxLines = 1)
        }
    }
}
