package dev.kinetik.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kinetik.model.Block
import dev.kinetik.model.Exercise
import dev.kinetik.model.Workout
import dev.kinetik.plan.shortLabel
import dev.kinetik.session.countdown
import dev.kinetik.ui.components.ReorderableColumn
import dev.kinetik.ui.components.StatTile
import dev.kinetik.ui.components.setsLabel
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KShape
import dev.kinetik.ui.theme.KText

private val RowH = 64.dp

/** Rest between exercises, and the optional workout-wide rest between sets (0 = off, each exercise uses its own). */
@Composable
fun RegularRuleTiles(w: Workout, onStepper: (StepperSpec) -> Unit, onChange: (Workout) -> Unit) {
    Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(countdown(w.restExerciseSec * 1000L), "rest · exercise", Modifier.weight(1f)) {
            onStepper(StepperSpec("Rest between exercises", w.restExerciseSec, 0, 900, 15, { countdown(it * 1000L) }) { onChange(w.copy(restExerciseSec = it)) })
        }
        StatTile(w.setRestSec?.let { countdown(it * 1000L) } ?: "Off", "rest · sets (all)", Modifier.weight(1f)) {
            onStepper(
                StepperSpec("Rest between sets, all exercises (0:00 = off)", w.setRestSec ?: 0, 0, 900, 15, { if (it == 0) "Off" else countdown(it * 1000L) }) {
                    onChange(w.copy(setRestSec = it.takeIf { s -> s > 0 }))
                },
            )
        }
    }
}

@Composable
fun BlockCard(
    w: Workout,
    block: Block,
    onRename: (String) -> Unit,
    onMoveBlock: (Int) -> Unit,
    onDeleteBlock: () -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
    onTapExercise: (Exercise) -> Unit,
    onAddExercise: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(KShape.Big).background(K.Card).padding(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp, 6.dp, 0.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                block.name, onRename, Modifier.weight(1f),
                textStyle = KText.display(17.sp), singleLine = true, cursorBrush = SolidColor(K.Teal),
            )
            Box {
                Text("⋮", Modifier.clip(KShape.Small).clickable { menu = true }.padding(horizontal = 12.dp, vertical = 4.dp), style = KText.body.copy(color = K.Muted))
                DropdownMenu(menu, { menu = false }) {
                    if (w.blocks.first() != block) DropdownMenuItem({ Text("Move block up") }, { menu = false; onMoveBlock(-1) })
                    if (w.blocks.last() != block) DropdownMenuItem({ Text("Move block down") }, { menu = false; onMoveBlock(1) })
                    if (w.blocks.size > 1) DropdownMenuItem({ Text("Delete block") }, { menu = false; onDeleteBlock() })
                }
            }
        }
        ReorderableColumn(block.exercises, key = { it.id }, rowHeight = RowH, onMove = onMoveExercise) { e, _, lifted ->
            val shape = KShape.Small
            Row(
                Modifier.fillMaxSize()
                    .then(if (lifted) Modifier.shadow(14.dp, shape).background(K.Card2, shape).border(1.dp, K.TealHi.copy(alpha = 0.35f), shape) else Modifier)
                    .clickable { onTapExercise(e) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("⋮⋮", style = KText.body.copy(color = if (lifted) K.TealHi else K.Line))
                Text(shortLabel(e.copy(sets = 1), e.startReps), Modifier.width(44.dp), style = KText.mono(20.sp).copy(color = K.TealHi))
                Text(e.name, Modifier.weight(1f), style = KText.body.copy(fontSize = 15.sp), maxLines = 1)
                Text(setsLabel(e, w.setRestSec), style = KText.mono(12.sp).copy(color = K.Muted))
            }
        }
        Text(
            "+ Add exercise",
            Modifier.padding(8.dp).clip(KShape.Small).background(K.Card2).clickable(onClick = onAddExercise).padding(horizontal = 14.dp, vertical = 10.dp),
            style = KText.body.copy(color = K.Muted, fontSize = 13.sp),
        )
    }
}

@Composable
fun AddBlockButton(onClick: () -> Unit) {
    Text(
        "+ Add block",
        Modifier.fillMaxWidth().clip(KShape.Big).border(2.dp, K.Line, KShape.Big).clickable(onClick = onClick).padding(16.dp),
        style = KText.body.copy(color = K.Muted, fontSize = 14.sp),
    )
}
