package dev.kinetik.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import dev.kinetik.app
import dev.kinetik.model.Exercise
import dev.kinetik.model.Group
import dev.kinetik.model.Library
import dev.kinetik.model.Workout
import dev.kinetik.model.WorkoutType
import dev.kinetik.model.newCircuitWorkout
import dev.kinetik.model.newRegularWorkout
import dev.kinetik.model.sections
import dev.kinetik.model.summary
import dev.kinetik.ui.components.RegularSummary
import dev.kinetik.model.addGroup
import dev.kinetik.model.addWorkout
import dev.kinetik.model.deleteGroup
import dev.kinetik.model.deleteWorkout
import dev.kinetik.model.groupOf
import dev.kinetik.model.moveGroup
import dev.kinetik.model.moveWorkout
import dev.kinetik.model.moveWorkoutToGroup
import dev.kinetik.model.renameGroup
import dev.kinetik.model.upNext
import dev.kinetik.model.updateWorkout
import dev.kinetik.model.workout
import dev.kinetik.plan.buildPlan
import dev.kinetik.plan.estimateSeconds
import dev.kinetik.store.WorkoutStore
import dev.kinetik.ui.LayoutMode
import dev.kinetik.ui.components.BigButton
import dev.kinetik.ui.components.Label
import dev.kinetik.ui.components.PlanGridView
import dev.kinetik.ui.components.SegmentedRing
import dev.kinetik.ui.components.StatTile
import dev.kinetik.ui.components.Tag
import dev.kinetik.ui.components.cssGradientEndpoints
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KShape
import dev.kinetik.ui.theme.KText
import dev.kinetik.ui.components.kFieldColors

private class HomeActions(
    private val store: WorkoutStore,
    private val expanded: MutableStateFlow<Set<String>?>,
    private val onEdit: (String) -> Unit,
) {
    fun toggle(groupId: String, current: Set<String>) {
        expanded.value = if (groupId in current) current - groupId else current + groupId
    }

    /** New groups start expanded. */
    private fun expandNewest() {
        val id = store.library.value.groups.lastOrNull()?.id ?: return
        expanded.value = (expanded.value ?: expandedOnStart(store.library.value)) + id
    }

    /** Set while the "Circuit or Regular?" choice is showing: the target group ("" = first group). */
    val choosingFor = mutableStateOf<String?>(null)

    fun newWorkout(groupId: String?) {
        choosingFor.value = groupId.orEmpty()
    }

    fun create(type: WorkoutType) {
        val groupId = choosingFor.value?.ifEmpty { null }
        choosingFor.value = null
        val gid = groupId ?: store.library.value.groups.firstOrNull()?.id ?: run {
            store.update { it.addGroup("Workouts") }
            expandNewest()
            store.library.value.groups.first().id
        }
        val w = if (type == WorkoutType.REGULAR) newRegularWorkout() else newCircuitWorkout()
        store.update { it.addWorkout(gid, w) }
        onEdit(w.id)
    }
    fun rename(w: Workout, name: String) = store.update { it.updateWorkout(w.copy(name = name)) }
    fun move(id: String, delta: Int) = store.update { it.moveWorkout(id, delta) }
    fun moveToGroup(id: String, groupId: String) = store.update { it.moveWorkoutToGroup(id, groupId) }
    fun delete(id: String) = store.update { it.deleteWorkout(id) }
    fun addGroup() {
        store.update { it.addGroup("New group") }
        expandNewest()
    }
    fun renameGroup(id: String, name: String) = store.update { it.renameGroup(id, name) }
    fun moveGroup(id: String, delta: Int) = store.update { it.moveGroup(id, delta) }
    fun deleteGroup(id: String) = store.update { it.deleteGroup(id) }
}

@Composable
fun HomeScreen(mode: LayoutMode, onStart: (String) -> Unit, onEdit: (String) -> Unit, onSettings: () -> Unit) {
    val app = LocalContext.current.app
    val lib by app.store.library.collectAsStateWithLifecycle()
    val actions = remember(onEdit) { HomeActions(app.store, app.expandedGroups, onEdit) }
    // Collapse state lives for the app process only: a restart opens with just the up-next group expanded.
    val expandedState by app.expandedGroups.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (app.expandedGroups.value == null) app.expandedGroups.value = expandedOnStart(lib) }
    val expanded = expandedState ?: expandedOnStart(lib)
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val featured = selectedId?.let(lib::workout) ?: lib.upNext()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (app.store.recoveredFromCorruption) {
            app.store.clearRecoveredFlag()
            snackbar.showSnackbar("Saved workouts couldn't be read. They were kept as workouts.bad.json and the defaults were restored.")
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (mode == LayoutMode.COVER) {
            HomeCover(lib, expanded, featured, { selectedId = it }, actions, onStart, onEdit, onSettings)
        } else {
            HomeMain(lib, expanded, featured, { selectedId = it }, actions, onStart, onEdit, onSettings)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
    if (actions.choosingFor.value != null) {
        AlertDialog(
            onDismissRequest = { actions.choosingFor.value = null },
            title = { Text("New workout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Circuit", { actions.create(WorkoutType.CIRCUIT) }, Modifier.fillMaxWidth(), primary = false, height = 56.dp, textSize = 16.sp)
                    Text("Rounds of the same exercises, reps dropping each round.", style = KText.body.copy(color = K.Muted, fontSize = 12.sp))
                    BigButton("Regular", { actions.create(WorkoutType.REGULAR) }, Modifier.fillMaxWidth(), primary = false, height = 56.dp, textSize = 16.sp)
                    Text("Blocks of exercises, each with its own sets and rest.", style = KText.body.copy(color = K.Muted, fontSize = 12.sp))
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ actions.choosingFor.value = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HomeCover(
    lib: Library, expanded: Set<String>, featured: Workout?, onSelect: (String) -> Unit, actions: HomeActions,
    onStart: (String) -> Unit, onEdit: (String) -> Unit, onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            HomeHeader(onAdd = { actions.newWorkout(null) }, onSettings = null)
            lib.groups.forEach { g ->
                GroupHeader(g, lib, g.id in expanded, actions) { actions.toggle(g.id, expanded) }
                if (g.id in expanded) Bento(g, lib, featured?.id, bigFeatured = true, actions, onSelect, onStart, onEdit)
                Spacer(Modifier.size(18.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().background(K.Bg).padding(top = 14.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Text("● Workouts", style = KText.body.copy(color = K.TealHi, fontSize = 12.sp))
            Text("Settings", Modifier.clickable(onClick = onSettings), style = KText.body.copy(color = K.Muted, fontSize = 12.sp))
        }
    }
}

@Composable
private fun HomeMain(
    lib: Library, expanded: Set<String>, featured: Workout?, onSelect: (String) -> Unit, actions: HomeActions,
    onStart: (String) -> Unit, onEdit: (String) -> Unit, onSettings: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        HingeGradient(Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp)) {
                HomeHeader(onAdd = { actions.newWorkout(null) }, onSettings = onSettings)
                lib.groups.forEach { g ->
                    GroupHeader(g, lib, g.id in expanded, actions) { actions.toggle(g.id, expanded) }
                    if (g.id in expanded) Bento(g, lib, featured?.id, bigFeatured = false, actions, onSelect, onStart, onEdit)
                    Spacer(Modifier.size(18.dp))
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 22.dp, vertical = 18.dp)) {
                featured?.let { WorkoutDetail(it, onStart, onEdit) }
            }
        }
    }
}

/**
 * Teal at the top of the right pane, fading down at 190° (tilted 10°), feathered into the
 * left pane over ~58 dp centred on the hinge — the mockup's `.dev.blend::before`.
 */
@Composable
private fun HingeGradient(modifier: Modifier) {
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val left = size.width / 2 - 29.dp.toPx()
        val w = size.width - left
        val (s, e) = cssGradientEndpoints(190f, w, size.height)
        drawRect(
            Brush.linearGradient(0f to K.GradientTeal, 0.55f to K.Bg, start = s + Offset(left, 0f), end = e + Offset(left, 0f)),
            topLeft = Offset(left, 0f), size = Size(w, size.height),
        )
        drawRect(
            Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, startX = left, endX = left + 58.dp.toPx()),
            topLeft = Offset(left, 0f), size = Size(w, size.height), blendMode = BlendMode.DstIn,
        )
    }
}

@Composable
private fun HomeHeader(onAdd: () -> Unit, onSettings: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(4.dp, 6.dp, 4.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Workouts", Modifier.weight(1f), style = KText.display(26.sp))
        if (onSettings != null) {
            Text("Settings", Modifier.padding(end = 14.dp).clickable(onClick = onSettings), style = KText.body.copy(color = K.Muted))
        }
        Box(Modifier.size(44.dp).clip(KShape.Small).background(K.Card).clickable(onClick = onAdd), contentAlignment = Alignment.Center) {
            Text("+", style = KText.body.copy(fontSize = 22.sp))
        }
    }
}

@Composable
private fun GroupHeader(g: Group, lib: Library, open: Boolean, actions: HomeActions, onToggle: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(KShape.Small).clickable(onClick = onToggle).padding(6.dp, 8.dp, 6.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (open) "▾" else "▸", Modifier.padding(end = 8.dp), style = KText.body.copy(color = K.Muted))
        Label(g.name + if (open) "" else " · ${g.workouts.size}", Modifier.weight(1f))
        Box {
            Text("⋮", Modifier.clickable { menu = true }.padding(horizontal = 8.dp), style = KText.body.copy(color = K.Muted))
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Rename group") }, { menu = false; renaming = true })
                DropdownMenuItem({ Text("New workout here") }, { menu = false; actions.newWorkout(g.id) })
                if (lib.groups.first() != g) DropdownMenuItem({ Text("Move group up") }, { menu = false; actions.moveGroup(g.id, -1) })
                if (lib.groups.last() != g) DropdownMenuItem({ Text("Move group down") }, { menu = false; actions.moveGroup(g.id, 1) })
                DropdownMenuItem({ Text("New group") }, { menu = false; actions.addGroup() })
                if (g.workouts.isEmpty()) DropdownMenuItem({ Text("Delete group") }, { menu = false; actions.deleteGroup(g.id) })
            }
        }
    }
    if (renaming) TextDialog("Rename group", g.name, { renaming = false }) { actions.renameGroup(g.id, it) }
}

@Composable
private fun Bento(
    g: Group, lib: Library, featuredId: String?, bigFeatured: Boolean, actions: HomeActions,
    onSelect: (String) -> Unit, onStart: (String) -> Unit, onEdit: (String) -> Unit,
) {
    val big = if (bigFeatured) g.workouts.firstOrNull { it.id == featuredId } else null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        big?.let { b ->
            WorkoutCard(
                b, lib, big = true, selected = false, upNext = b.id == lib.upNext()?.id, actions = actions,
                onClick = {}, onStart = { onStart(b.id) }, onEdit = { onEdit(b.id) },
            )
        }
        val small: List<Workout?> = g.workouts.filter { it.id != big?.id } + listOf(null) // null = "+" card
        small.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { w ->
                    Box(Modifier.weight(1f)) {
                        if (w == null) AddCard { actions.newWorkout(g.id) }
                        else WorkoutCard(
                            w, lib, big = false, selected = !bigFeatured && w.id == featuredId,
                            upNext = w.id == lib.upNext()?.id, actions = actions,
                            onClick = { onSelect(w.id) }, onStart = null, onEdit = { onEdit(w.id) },
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutCard(
    w: Workout, lib: Library, big: Boolean, selected: Boolean, upNext: Boolean, actions: HomeActions,
    onClick: () -> Unit, onStart: (() -> Unit)?, onEdit: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val minutes = remember(w) { (estimateSeconds(buildPlan(w)) + 59) / 60 }
    val shape = RoundedCornerShape(26.dp)
    val bg = if (big) Brush.linearGradient(0f to Color(0xFF134E48), 0.62f to K.Card) else Brush.linearGradient(listOf(K.Card, K.Card))
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (big) 196.dp else 140.dp)
            .clip(shape)
            .background(bg)
            .then(
                when {
                    selected -> Modifier.border(2.dp, K.Teal, shape)
                    big -> Modifier.border(1.dp, K.TealHi.copy(alpha = 0.18f), shape)
                    else -> Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = { menu = true })
            .padding(16.dp),
    ) {
        SegmentedRing(
            w.sections, active = if (big || selected) 0 else -1, done = 0,
            modifier = Modifier.size(if (big) 66.dp else 40.dp).align(Alignment.TopEnd),
            stroke = if (big) 6.dp else 4.dp,
        )
        if (big) {
            Column {
                Tag(if (upNext) "Up next" else "Selected", accent = true)
                Text(w.name, Modifier.padding(top = 10.dp), style = KText.display(40.sp).copy(letterSpacing = (-1).sp))
                Text("${w.summary()} · ~$minutes min", style = KText.body.copy(fontSize = 12.sp, color = K.Muted))
                Spacer(Modifier.size(16.dp))
                Box(
                    Modifier.clip(KShape.Small).background(K.Teal).clickable { onStart?.invoke() }
                        .testTag("start").padding(horizontal = 22.dp, vertical = 12.dp),
                ) { Text("▶ Start", style = KText.body.copy(color = K.OnTeal, fontSize = 15.sp)) }
            }
        } else {
            Column(Modifier.align(Alignment.BottomStart)) {
                if (upNext && selected) Tag("Up next", accent = true)
                Text(w.name, style = KText.display(22.sp))
                Text("${w.summary().substringBefore(" ·")} · ~$minutes min", style = KText.body.copy(fontSize = 12.sp, color = K.Muted))
            }
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem({ Text("Edit") }, { menu = false; onEdit() })
            DropdownMenuItem({ Text("Rename") }, { menu = false; renaming = true })
            DropdownMenuItem({ Text("Move earlier") }, { menu = false; actions.move(w.id, -1) })
            DropdownMenuItem({ Text("Move later") }, { menu = false; actions.move(w.id, 1) })
            lib.groups.filter { it.id != lib.groupOf(w.id)?.id }.forEach { g ->
                DropdownMenuItem({ Text("Move to ${g.name}") }, { menu = false; actions.moveToGroup(w.id, g.id) })
            }
            DropdownMenuItem({ Text("Delete") }, { menu = false; deleting = true })
        }
    }
    if (renaming) TextDialog("Rename workout", w.name, { renaming = false }) { actions.rename(w, it) }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Delete ${w.name}?") },
            confirmButton = { TextButton({ deleting = false; actions.delete(w.id) }) { Text("Delete") } },
            dismissButton = { TextButton({ deleting = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddCard(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 140.dp).clip(RoundedCornerShape(26.dp))
            .border(2.dp, K.Line, RoundedCornerShape(26.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("+", style = KText.body.copy(fontSize = 34.sp, color = K.Muted)) }
}

@Composable
private fun WorkoutDetail(w: Workout, onStart: (String) -> Unit, onEdit: (String) -> Unit) {
    val minutes = remember(w) { (estimateSeconds(buildPlan(w)) + 59) / 60 }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Label("Workout")
                Text(w.name, style = KText.display(64.sp).copy(letterSpacing = (-2).sp))
            }
            SegmentedRing(w.sections, active = 0, done = 0, modifier = Modifier.size(96.dp), stroke = 9.dp)
        }
        Row(Modifier.padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (w.type == WorkoutType.REGULAR) {
                StatTile("${w.sections}", "blocks", Modifier.weight(1f))
                StatTile("${w.blocks.sumOf { it.exercises.size }}", "exercises", Modifier.weight(1f))
            } else {
                StatTile("${w.circuits}", "circuits", Modifier.weight(1f))
                StatTile("−${w.repDrop}", "reps / circuit", Modifier.weight(1f))
            }
            StatTile("~${minutes}m", "total", Modifier.weight(1f))
        }
        if (w.type == WorkoutType.REGULAR) {
            RegularSummary(w, Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()))
        } else {
            PlanGridView(w, showLabels = true)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("✎ Edit", { onEdit(w.id) }, Modifier.width(120.dp), primary = false, height = 64.dp, textSize = 15.sp)
            BigButton("▶ START", { onStart(w.id) }, Modifier.weight(1f).testTag("start"), height = 64.dp, textSize = 20.sp)
        }
    }
}

@Composable
internal fun TextDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, colors = kFieldColors()) },
        confirmButton = {
            TextButton({ if (text.isNotBlank()) { onConfirm(text.trim()); onDismiss() } }) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
