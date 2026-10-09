package dev.kinetik.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kinetik.model.Exercise
import dev.kinetik.model.ExerciseKind
import dev.kinetik.ui.components.Label
import dev.kinetik.ui.components.SwitchRow
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KText

@Composable
fun <T> ChoiceRow(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            val on = value == selected
            Text(
                label,
                Modifier.clip(CircleShape).background(if (on) K.Teal else K.Card2).clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                style = KText.body.copy(color = if (on) K.OnTeal else K.Text),
            )
        }
    }
}

@Composable
fun StepperRow(label: String, value: Int, min: Int, max: Int, format: (Int) -> String = { "$it" }, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = KText.body.copy(fontSize = 15.sp))
        RoundButton("−") { onChange((value - 1).coerceAtLeast(min)) }
        Text(format(value), Modifier.width(64.dp), style = KText.mono(18.sp).copy(color = K.TealHi), textAlign = TextAlign.Center)
        RoundButton("+") { onChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun RoundButton(text: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(K.Card2).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = KText.body.copy(fontSize = 20.sp))
    }
}

data class StepperSpec(
    val title: String, val value: Int, val min: Int, val max: Int, val step: Int,
    val format: (Int) -> String, val onSet: (Int) -> Unit,
)

@Composable
fun StepperDialog(spec: StepperSpec, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf(spec.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.title) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundButton("−") { v = (v - spec.step).coerceAtLeast(spec.min) }
                Text(spec.format(v), Modifier.weight(1f), style = KText.mono(28.sp).copy(color = K.TealHi), textAlign = TextAlign.Center)
                RoundButton("+") { v = (v + spec.step).coerceAtMost(spec.max) }
            }
        },
        confirmButton = { TextButton({ spec.onSet(v); onDismiss() }) { Text("Set") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

/** Every editable field of one exercise. Used by the exercise sheet and the override sheet. */
@Composable
fun ExerciseFields(e: Exercise, onChange: (Exercise) -> Unit) {
    var weightText by remember(e.id) { mutableStateOf(e.weightKg?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" } ?: "") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(e.name, { onChange(e.copy(name = it)) }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
        Label("Type")
        ChoiceRow(listOf("Reps" to ExerciseKind.REPS, "Max reps" to ExerciseKind.MAX_REPS, "Max time" to ExerciseKind.MAX_TIME), e.kind) {
            onChange(e.copy(kind = it, maxBreakSec = if (it == ExerciseKind.MAX_REPS) e.maxBreakSec else null))
        }
        if (e.kind == ExerciseKind.REPS) StepperRow("Start reps", e.startReps, 1, 200) { onChange(e.copy(startReps = it)) }
        StepperRow("Sets in a row", e.sets, 1, 10) { onChange(e.copy(sets = it)) }
        if (e.kind == ExerciseKind.MAX_REPS) {
            Label("Max break")
            ChoiceRow(listOf("Off" to null, "5s" to 5, "10s" to 10, "15s" to 15, "30s" to 30), e.maxBreakSec) {
                onChange(e.copy(maxBreakSec = it))
            }
        }
        SwitchRow("Each side", e.eachSide) { onChange(e.copy(eachSide = it)) }
        OutlinedTextField(
            weightText,
            { weightText = it; onChange(e.copy(weightKg = it.toDoubleOrNull())) },
            Modifier.fillMaxWidth(),
            label = { Text("Weight (kg, optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}
