package dev.kinetik.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kinetik.model.CircuitOverride
import dev.kinetik.model.Exercise
import dev.kinetik.model.ExerciseKind
import dev.kinetik.model.OverrideScope
import dev.kinetik.model.Workout
import dev.kinetik.ui.components.BigButton
import dev.kinetik.ui.components.Label
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSheet(initial: Exercise, isNew: Boolean, onSave: (Exercise) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var e by remember(initial.id) { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = K.Card) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (isNew) "New exercise" else "Edit exercise", style = KText.display(20.sp))
            ExerciseFields(e) { e = it }
            SheetButtons(onDelete, onSave = { if (e.name.isNotBlank()) onSave(e.copy(name = e.name.trim())) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverrideSheet(workout: Workout, initial: CircuitOverride, isNew: Boolean, onSave: (CircuitOverride) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var o by remember(initial.id) { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = K.Card) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (isNew) "New override" else "Edit override", style = KText.display(20.sp))
            Label("Circuit")
            ChoiceRow((0 until workout.circuits).map { "C${it + 1}" to it }, o.circuit) { o = o.copy(circuit = it) }
            Label("Replaces")
            ChoiceRow(listOf("One exercise" to OverrideScope.SLOT, "Whole circuit" to OverrideScope.WHOLE), o.scope) { s ->
                o = o.copy(
                    scope = s,
                    slot = if (s == OverrideScope.SLOT) (o.slot ?: 0) else null,
                    exercises = if (s == OverrideScope.SLOT) o.exercises.take(1) else o.exercises,
                )
            }
            if (o.scope == OverrideScope.SLOT) {
                Label("Exercise to replace")
                ChoiceRow(workout.exercises.mapIndexed { i, e -> e.name to i }, o.slot) { o = o.copy(slot = it) }
            }
            o.exercises.forEachIndexed { i, e ->
                if (o.scope == OverrideScope.WHOLE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label("Exercise ${i + 1}", Modifier.weight(1f))
                        if (o.exercises.size > 1) {
                            TextButton({ o = o.copy(exercises = o.exercises.filterIndexed { j, _ -> j != i }) }) { Text("Remove") }
                        }
                    }
                }
                ExerciseFields(e) { ne -> o = o.copy(exercises = o.exercises.toMutableList().also { it[i] = ne }) }
            }
            if (o.scope == OverrideScope.WHOLE) {
                TextButton({ o = o.copy(exercises = o.exercises + Exercise(name = "", kind = ExerciseKind.MAX_REPS)) }) { Text("+ Add exercise") }
            }
            SheetButtons(onDelete, onSave = { onSave(o) })
        }
    }
}

@Composable
private fun SheetButtons(onDelete: (() -> Unit)?, onSave: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onDelete != null) BigButton("Delete", onDelete, Modifier.weight(1f), primary = false, height = 56.dp, textSize = 15.sp)
        BigButton("Save", onSave, Modifier.weight(1f), height = 56.dp, textSize = 15.sp)
    }
}
