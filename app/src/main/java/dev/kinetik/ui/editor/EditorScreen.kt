package dev.kinetik.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kinetik.app
import dev.kinetik.model.CircuitOverride
import dev.kinetik.model.Exercise
import dev.kinetik.model.ExerciseKind
import dev.kinetik.model.OverrideScope
import dev.kinetik.model.Workout
import dev.kinetik.model.WorkoutType
import dev.kinetik.model.addBlock
import dev.kinetik.model.deleteBlock
import dev.kinetik.model.deleteBlockExercise
import dev.kinetik.model.moveBlock
import dev.kinetik.model.moveBlockExercise
import dev.kinetik.model.renameBlock
import dev.kinetik.model.upsertBlockExercise
import dev.kinetik.ui.components.RegularSummary
import dev.kinetik.model.deleteExercise
import dev.kinetik.model.deleteOverride
import dev.kinetik.model.moveExercise
import dev.kinetik.model.setCircuits
import dev.kinetik.model.updateWorkout
import dev.kinetik.model.upsertExercise
import dev.kinetik.model.upsertOverride
import dev.kinetik.model.validate
import dev.kinetik.model.workout
import dev.kinetik.plan.shortLabel
import dev.kinetik.plan.wholeLabel
import dev.kinetik.session.countdown
import dev.kinetik.ui.LayoutMode
import dev.kinetik.ui.components.Label
import dev.kinetik.ui.components.PlanGridView
import dev.kinetik.ui.components.ReorderableColumn
import dev.kinetik.ui.components.StatTile
import dev.kinetik.ui.components.Tag
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KShape
import dev.kinetik.ui.theme.KText

private val RowH = 64.dp

@Composable
fun EditorScreen(workoutId: String, mode: LayoutMode, onBack: () -> Unit) {
    val app = LocalContext.current.app
    val saved = remember(workoutId) { app.store.library.value.workout(workoutId) }
    if (saved == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var draft by remember(workoutId) { mutableStateOf(saved) }
    var exerciseEdit by remember { mutableStateOf<Pair<Exercise, Boolean>?>(null) }
    /** Regular workouts: (block id, exercise, isNew). */
    var blockExerciseEdit by remember { mutableStateOf<Triple<String, Exercise, Boolean>?>(null) }
    var overrideEdit by remember { mutableStateOf<Pair<CircuitOverride, Boolean>?>(null) }
    var stepper by remember { mutableStateOf<StepperSpec?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val errors = remember(draft) { draft.validate() }
    val dirty = draft != saved

    fun save() {
        if (errors.isNotEmpty()) return
        app.store.update { it.updateWorkout(draft) }
        onBack()
    }
    val back: () -> Unit = { if (dirty) { confirmDiscard = true } else { onBack() } }
    BackHandler(enabled = dirty) { confirmDiscard = true }

    val header: @Composable () -> Unit = { EditorHeader(draft, dirty && errors.isEmpty(), { draft = draft.copy(name = it) }, back, ::save) }
    val rules: @Composable () -> Unit = { RuleTiles(draft, { stepper = it }) { draft = it } }
    val list: @Composable () -> Unit = {
        ExerciseList(draft, onMove = { f, t -> draft = draft.moveExercise(f, t) }, onTap = { exerciseEdit = it to false }) {
            exerciseEdit = Exercise(name = "") to true
        }
        errors.forEach { Text(it, Modifier.padding(top = 6.dp), style = KText.body.copy(color = K.Rest, fontSize = 13.sp)) }
    }
    val plan: @Composable (Boolean) -> Unit = { labels ->
        PlanSection(draft, labels, onCircuits = { draft = draft.setCircuits(it) }, onAdd = {
            overrideEdit = CircuitOverride(
                circuit = draft.circuits - 1, scope = OverrideScope.WHOLE,
                exercises = listOf(Exercise(name = "", kind = ExerciseKind.MAX_REPS, maxBreakSec = 10)),
            ) to true
        }, onTap = { overrideEdit = it to false })
    }

    val errorText: @Composable () -> Unit = {
        errors.forEach { Text(it, Modifier.padding(top = 6.dp), style = KText.body.copy(color = K.Rest, fontSize = 13.sp)) }
    }
    val blocks: @Composable () -> Unit = {
        RegularRuleTiles(draft, { stepper = it }) { draft = it }
        draft.blocks.forEach { b ->
            BlockCard(
                draft, b,
                onRename = { draft = draft.renameBlock(b.id, it) },
                onMoveBlock = { draft = draft.moveBlock(b.id, it) },
                onDeleteBlock = { draft = draft.deleteBlock(b.id) },
                onMoveExercise = { f, t -> draft = draft.moveBlockExercise(b.id, f, t) },
                onTapExercise = { blockExerciseEdit = Triple(b.id, it, false) },
                onAddExercise = { blockExerciseEdit = Triple(b.id, Exercise(name = "", startReps = 10, sets = 3), true) },
            )
        }
        AddBlockButton { draft = draft.addBlock() }
        errorText()
    }

    if (draft.type == WorkoutType.REGULAR) {
        if (mode == LayoutMode.COVER) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
                header(); blocks()
            }
        } else {
            Row(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(18.dp, 18.dp)) {
                    header(); blocks()
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(18.dp, 18.dp)) {
                    Text("Run order", Modifier.padding(top = 8.dp, bottom = 10.dp), style = KText.display(20.sp))
                    RegularSummary(draft)
                }
            }
        }
    } else if (mode == LayoutMode.COVER) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            header(); rules(); list(); plan(false)
        }
    } else {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(18.dp, 18.dp)) {
                header(); rules(); list()
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(18.dp, 18.dp)) {
                plan(true)
            }
        }
    }

    exerciseEdit?.let { (e, isNew) ->
        val index = draft.exercises.indexOfFirst { it.id == e.id }
        ExerciseSheet(
            e, isNew,
            onSave = { draft = draft.upsertExercise(it); exerciseEdit = null },
            onDelete = if (isNew || draft.exercises.size <= 1) null else ({ draft = draft.deleteExercise(index); exerciseEdit = null }),
            onDismiss = { exerciseEdit = null },
        )
    }
    blockExerciseEdit?.let { (blockId, e, isNew) ->
        ExerciseSheet(
            e, isNew,
            onSave = { draft = draft.upsertBlockExercise(blockId, it); blockExerciseEdit = null },
            onDelete = if (isNew) null else ({ draft = draft.deleteBlockExercise(blockId, e.id); blockExerciseEdit = null }),
            onDismiss = { blockExerciseEdit = null },
            regular = true,
            globalSetRestSec = draft.setRestSec,
        )
    }
    overrideEdit?.let { (o, isNew) ->
        OverrideSheet(
            draft, o, isNew,
            onSave = { draft = draft.upsertOverride(it); overrideEdit = null },
            onDelete = if (isNew) null else ({ draft = draft.deleteOverride(o.id); overrideEdit = null }),
            onDismiss = { overrideEdit = null },
        )
    }
    stepper?.let { StepperDialog(it) { stepper = null } }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            confirmButton = { TextButton({ confirmDiscard = false; onBack() }) { Text("Discard") } },
            dismissButton = { TextButton({ confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun EditorHeader(w: Workout, canSave: Boolean, onName: (String) -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", Modifier.clickable(onClick = onBack).padding(end = 12.dp), style = KText.body.copy(fontSize = 26.sp, color = K.Muted))
        BasicTextField(
            w.name, onName, Modifier.weight(1f),
            textStyle = KText.display(30.sp), singleLine = true, cursorBrush = SolidColor(K.Teal),
        )
        Text(
            "Save",
            Modifier.clip(KShape.Small).background(if (canSave) K.Teal.copy(alpha = 0.14f) else K.Card2)
                .clickable(enabled = canSave, onClick = onSave).padding(horizontal = 12.dp, vertical = 6.dp),
            style = KText.body.copy(color = if (canSave) K.TealHi else K.Muted, fontSize = 13.sp),
        )
    }
}

@Composable
private fun RuleTiles(w: Workout, onStepper: (StepperSpec) -> Unit, onChange: (Workout) -> Unit) {
    Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("−${w.repDrop}", "reps / circuit", Modifier.weight(1f)) {
            onStepper(StepperSpec("Reps drop per circuit", w.repDrop, 0, 10, 1, { "−$it" }) { onChange(w.copy(repDrop = it)) })
        }
        StatTile(countdown(w.restExerciseSec * 1000L), "rest · exercise", Modifier.weight(1f)) {
            onStepper(StepperSpec("Rest between exercises", w.restExerciseSec, 0, 900, 15, { countdown(it * 1000L) }) { onChange(w.copy(restExerciseSec = it)) })
        }
        StatTile(countdown(w.restCircuitSec * 1000L), "rest · circuit", Modifier.weight(1f)) {
            onStepper(StepperSpec("Rest between circuits", w.restCircuitSec, 0, 900, 15, { countdown(it * 1000L) }) { onChange(w.copy(restCircuitSec = it)) })
        }
    }
}

@Composable
private fun ExerciseList(w: Workout, onMove: (Int, Int) -> Unit, onTap: (Exercise) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(26.dp)).background(K.Card).padding(8.dp)) {
        ReorderableColumn(w.exercises, key = { it.id }, rowHeight = RowH, onMove = onMove) { e, _, lifted ->
            ExerciseRow(e, lifted, Modifier.clickable { onTap(e) })
        }
    }
    Text(
        "+ Add exercise",
        Modifier.padding(top = 12.dp).clip(KShape.Small).background(K.Card2).clickable(onClick = onAdd).padding(horizontal = 14.dp, vertical = 10.dp),
        style = KText.body.copy(color = K.Muted, fontSize = 13.sp),
    )
}

@Composable
private fun ExerciseRow(e: Exercise, lifted: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxSize()
            .then(if (lifted) Modifier.shadow(14.dp, shape).background(K.Card2, shape).border(1.dp, K.TealHi.copy(alpha = 0.35f), shape) else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("⋮⋮", style = KText.body.copy(color = if (lifted) K.TealHi else K.Line))
        Text(shortLabel(e, e.startReps), Modifier.width(44.dp), style = KText.mono(20.sp).copy(color = K.TealHi))
        Text(e.name, Modifier.weight(1f), style = KText.body.copy(fontSize = 15.sp), maxLines = 1)
        e.weightKg?.let { Tag("${if (it % 1.0 == 0.0) it.toInt() else it} kg") }
    }
}

@Composable
private fun PlanSection(w: Workout, labels: Boolean, onCircuits: (Int) -> Unit, onAdd: () -> Unit, onTap: (CircuitOverride) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Circuit plan", Modifier.weight(1f), style = KText.display(16.sp))
        StepperRowCompact(w.circuits) { onCircuits(it) }
    }
    PlanGridView(w, showLabels = labels)
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Label("Overrides", Modifier.weight(1f))
        Tag("+ Override", modifier = Modifier.clickable(onClick = onAdd))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        w.overrides.forEach { o ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.Card).clickable { onTap(o) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val where = if (o.scope == OverrideScope.WHOLE) "whole" else w.exercises.getOrNull(o.slot ?: -1)?.name ?: "?"
                val sets = o.exercises.firstOrNull()?.sets ?: 1
                Tag("C${o.circuit + 1} · $where", accent = true)
                Text(wholeLabel(o.exercises) + (if (sets > 1) " ×$sets" else ""), style = KText.body, maxLines = 2)
            }
        }
    }
}

@Composable
private fun StepperRowCompact(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tag("−", modifier = Modifier.clickable { onChange((value - 1).coerceAtLeast(1)) })
        Text("$value circuits", style = KText.body.copy(color = K.Muted, fontSize = 12.sp))
        Tag("+", modifier = Modifier.clickable { onChange((value + 1).coerceAtMost(12)) })
    }
}
