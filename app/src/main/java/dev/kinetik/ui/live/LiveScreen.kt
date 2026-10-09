package dev.kinetik.ui.live

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kinetik.app
import dev.kinetik.session.Phase
import dev.kinetik.session.SessionEvent
import dev.kinetik.session.SessionState
import dev.kinetik.session.stopwatch
import dev.kinetik.ui.LayoutMode
import dev.kinetik.ui.components.BigButton
import dev.kinetik.ui.components.Label
import dev.kinetik.ui.components.ProgressBars
import dev.kinetik.ui.components.RestRing
import dev.kinetik.ui.components.SegmentedRing
import dev.kinetik.ui.components.ringBox
import dev.kinetik.ui.components.Tag
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KText

@Composable
fun LiveScreen(mode: LayoutMode, onExit: () -> Unit) {
    val app = LocalContext.current.app
    // Deduped: the screen only recomposes when something visible changes (about once a second), not every tick.
    val uiFlow = remember { liveUi(app.session.state) }
    val ui by uiFlow.collectAsStateWithLifecycle(initialValue = liveUi0(app.session.state.value))
    val lib by app.store.library.collectAsStateWithLifecycle()
    val voiceReady by app.cues.voiceReady.collectAsStateWithLifecycle()
    var confirmEnd by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf<Jump?>(null) }

    // Leaving always goes through stop() → state becomes null → we exit once.
    val u = ui ?: run {
        LaunchedEffect(Unit) { onExit() }
        return
    }
    KeepScreenOn(lib.settings.keepScreenOn && !u.finished)
    BackHandler { if (u.finished) { app.session.stop() } else { confirmEnd = true } }

    val m = u.model
    if (u.finished || m == null) {
        Finished(u) { app.session.stop() }
        return
    }
    val send: (SessionEvent) -> Unit = app.session::send
    val voiceMissing = lib.settings.voiceOn && !voiceReady
    val onEnd = { confirmEnd = true }
    val section = m.section.lowercase()
    val onJump = { c: Int ->
        if (c != m.circuitNumber - 1) {
            jump = Jump("Jump to $section ${c + 1}?", "You'll start at its first exercise. You're on $section ${m.circuitNumber} of ${m.circuits}.", SessionEvent.JumpTo(c))
        }
    }
    // The ring only has segments during a set, one per exercise in this circuit, in queue order.
    val onExercise = { i: Int ->
        if (i != m.activeSegment) {
            jump = Jump("Jump to ${m.queue[i].name}?", "Exercise ${i + 1} of ${m.segments} in this $section.", SessionEvent.JumpTo(m.circuitNumber - 1, i))
        }
    }
    when (mode) {
        LayoutMode.COVER -> LiveCover(m, send, voiceMissing, onEnd, onJump, onExercise)
        LayoutMode.MAIN -> LiveMain(m, send, voiceMissing, onEnd, onJump, onExercise)
        LayoutMode.TABLETOP -> LiveTabletop(m, send, onEnd, onJump, onExercise)
    }
    jump?.let { j ->
        AlertDialog(
            onDismissRequest = { jump = null },
            title = { Text(j.title) },
            text = { Text(j.detail) },
            confirmButton = { TextButton({ jump = null; send(j.event) }) { Text("Jump") } },
            dismissButton = { TextButton({ jump = null }) { Text("Cancel") } },
        )
    }
    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End workout?") },
            confirmButton = { TextButton({ confirmEnd = false; app.session.stop() }) { Text("End") } },
            dismissButton = { TextButton({ confirmEnd = false }) { Text("Keep going") } },
        )
    }
}

private data class Jump(val title: String, val detail: String, val event: SessionEvent.JumpTo)

@Composable
private fun KeepScreenOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun rememberPulse(active: Boolean, key: Any): Float {
    val anim = remember { Animatable(1f) }
    LaunchedEffect(key, active) {
        if (active) {
            anim.snapTo(1.08f)
            anim.animateTo(1f, tween(400))
        } else {
            anim.snapTo(1f)
        }
    }
    return anim.value
}

/** Everything sits in a tight group around the vertical centre (cover screen). */
@Composable
private fun LiveCover(m: LiveModel, send: (SessionEvent) -> Unit, voiceMissing: Boolean, onEnd: () -> Unit, onJump: (Int) -> Unit, onExercise: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp).padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LiveHeader(m, onEnd)
        ProgressBars(m.circuits, m.circuitNumber - 1, onTap = onJump)
        RingBlock(m, Modifier.ringBox(340.dp), stroke = 18.dp, bigSize = 116.sp, nameSize = 24.sp, onExercise)
        if (voiceMissing) Tag("Voice unavailable")
        if (m.isRest) {
            NextStrip(m, "Up next", dim = false)
            RestControls(m, send, 58.dp)
        } else {
            NextStrip(m, "After rest", dim = true)
            WorkControls(m, send, 84.dp)
        }
    }
}

@Composable
private fun LiveMain(m: LiveModel, send: (SessionEvent) -> Unit, voiceMissing: Boolean, onEnd: () -> Unit, onJump: (Int) -> Unit, onExercise: (Int) -> Unit) {
    Row(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(m.workoutName)
                    Text("${m.section} ${m.circuitNumber}/${m.circuits}", style = KText.mono(24.sp))
                }
                Text("End", Modifier.clickable(onClick = onEnd).padding(8.dp), style = KText.body.copy(color = K.Muted))
            }
            RingBlock(m, Modifier.ringBox(400.dp), stroke = 20.dp, bigSize = 150.sp, nameSize = 28.sp, onExercise)
            if (voiceMissing) Tag("Voice unavailable")
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(22.dp)) {
            Column(Modifier.align(Alignment.End), horizontalAlignment = Alignment.End) {
                Label("Left")
                Text(m.timeLeft, style = KText.mono(24.sp))
            }
            Label("This ${m.section.lowercase()}", Modifier.padding(top = 10.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { m.queue.forEach { QueueRowView(it) } }
            Label("${m.section}s", Modifier.padding(top = 20.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(m.circuits) { i ->
                    val (bg, fg, text) = when {
                        i < m.circuitNumber - 1 -> Triple(K.TealDim, K.Text, "done")
                        i == m.circuitNumber - 1 -> Triple(K.Teal, K.OnTeal, "now")
                        else -> Triple(K.Card, K.Muted, "")
                    }
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(bg).clickable { onJump(i) }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("${i + 1}", style = KText.mono(18.sp).copy(color = fg))
                        Text(text, style = KText.label.copy(color = fg))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (m.isRest) {
                NextStrip(m, "Up next", dim = false)
                Spacer(Modifier.height(12.dp))
                RestControls(m, send, 58.dp)
            } else {
                WorkControls(m, send, 84.dp)
            }
        }
    }
}

/** Half-folded on the floor: timer on the top half, big buttons on the bottom half. */
@Composable
private fun LiveTabletop(m: LiveModel, send: (SessionEvent) -> Unit, onEnd: () -> Unit, onJump: (Int) -> Unit, onExercise: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.weight(1f).fillMaxWidth().safeDrawingPadding().padding(horizontal = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            RingBlock(m, Modifier.ringBox(300.dp), stroke = 16.dp, bigSize = 100.sp, nameSize = 20.sp, onExercise)
            Column(Modifier.weight(1f)) {
                Label("${m.workoutName} · ${m.section} ${m.circuitNumber}/${m.circuits} · ${m.sub}")
                if (m.nextName != null) {
                    Text("Next up", Modifier.padding(top = 10.dp), style = KText.display(40.sp))
                    Text("${m.nextReps} ${m.nextName}", style = KText.display(40.sp).copy(color = K.TealHi))
                }
                Text("End", Modifier.padding(top = 12.dp).clickable(onClick = onEnd), style = KText.body.copy(color = K.Muted))
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().background(K.FlexBottom).padding(horizontal = 34.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            ProgressBars(m.circuits, m.circuitNumber - 1, onTap = onJump)
            if (m.isRest) RestControls(m, send, 80.dp) else WorkControls(m, send, 80.dp)
        }
    }
}

@Composable
private fun LiveHeader(m: LiveModel, onEnd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Label(m.section)
            Text("${m.circuitNumber}/${m.circuits}", style = KText.mono(24.sp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Label(m.workoutName)
            Text("End", Modifier.clickable(onClick = onEnd).padding(4.dp), style = KText.body.copy(color = K.Muted, fontSize = 12.sp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Label("Left")
            Text(m.timeLeft, style = KText.mono(24.sp))
        }
    }
}

@Composable
internal fun RingBlock(m: LiveModel, modifier: Modifier, stroke: Dp, bigSize: TextUnit, nameSize: TextUnit, onExercise: (Int) -> Unit = {}) {
    val pulse = rememberPulse(m.beepZone, m.big)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        if (m.isRest) RestRing(m.restFraction, Modifier.fillMaxSize(), stroke)
        else SegmentedRing(m.segments, m.activeSegment, m.activeSegment, Modifier.fillMaxSize(), stroke, gapDeg = 5f, onTap = onExercise)
        // Largest square inside the ring's inner edge (side = inner diameter / √2), so text never reaches the ring.
        val inside = (minOf(maxWidth, maxHeight) - stroke * 4) * 0.707f
        Column(
            Modifier.size(inside),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Label(if (m.paused) "Paused" else m.sub, color = if (m.isRest) K.Rest else K.Muted)
            if (m.bigIsClock) {
                FitText(
                    m.big,
                    Modifier.weight(1f).graphicsLayer { scaleX = pulse; scaleY = pulse },
                    KText.mono(if (m.isRest) 76.sp else 64.sp).copy(color = if (m.beepZone) K.Rest else K.Text, letterSpacing = (-2).sp),
                )
            } else {
                FitText(
                    m.big,
                    Modifier.weight(1.2f),
                    KText.display(if (m.bigIsWord) bigSize * 0.5f else bigSize, 900)
                        .copy(color = K.TealHi, shadow = Shadow(K.Teal.copy(alpha = 0.5f), blurRadius = 34f)),
                )
            }
            if (!m.isRest) {
                FitText(m.name, Modifier.weight(0.8f), KText.display(nameSize), maxLines = 2)
                if (m.tags.isNotEmpty()) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { m.tags.forEach { Tag(it) } }
                m.breakLeft?.let { Text("Break $it", Modifier.padding(top = 4.dp), style = KText.mono(18.sp).copy(color = K.Rest)) }
                m.elapsed?.let { Text("⏱\uFE0E $it", Modifier.padding(top = 4.dp), style = KText.mono(18.sp).copy(color = K.Muted)) }
            }
        }
    }
}

/** Centred text that shrinks (down to 10 sp) until it fits its box. */
@Composable
private fun FitText(text: String, modifier: Modifier, style: TextStyle, maxLines: Int = 1) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BasicText(
            text,
            style = style.copy(textAlign = TextAlign.Center),
            maxLines = maxLines,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize, stepSize = 1.sp),
        )
    }
}

@Composable
private fun NextStrip(m: LiveModel, label: String, dim: Boolean) {
    if (m.nextName == null) {
        // Last set: keep the strip's space so DONE doesn't jump up.
        Row(
            Modifier.fillMaxWidth().alpha(0.7f).clip(RoundedCornerShape(22.dp)).background(K.Card).padding(12.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(K.Card2), contentAlignment = Alignment.Center) {
                Text("✓", style = KText.body.copy(color = K.TealHi, fontSize = 22.sp))
            }
            Column {
                Label("Last set")
                Text("Finish strong", style = KText.body.copy(fontSize = 17.sp))
            }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().alpha(if (dim) 0.7f else 1f).clip(RoundedCornerShape(22.dp)).background(K.Card).padding(12.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(K.Card2), contentAlignment = Alignment.Center) {
            Text(m.nextReps.orEmpty(), style = KText.mono(if ((m.nextReps?.length ?: 0) > 2) 13.sp else 26.sp).copy(color = K.TealHi))
        }
        Column {
            Label(label)
            Text(m.nextName, style = KText.body.copy(fontSize = 17.sp))
        }
    }
}

@Composable
private fun WorkControls(m: LiveModel, send: (SessionEvent) -> Unit, height: Dp) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (m.canBreak) {
            BigButton(if (m.breakLeft != null) "GO" else "BREAK", { send(SessionEvent.ToggleBreak) }, Modifier.weight(1f), primary = false, height = height, textSize = 18.sp)
        }
        BigButton("DONE", { send(SessionEvent.Done) }, Modifier.weight(2f).testTag("done"), height = height)
    }
}

@Composable
private fun RestControls(m: LiveModel, send: (SessionEvent) -> Unit, height: Dp) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigButton("−30s", { send(SessionEvent.MinusThirty) }, Modifier.weight(1f), primary = false, height = height, textSize = 13.sp)
        BigButton(if (m.paused) "▶ Resume" else "❚❚ Pause", { send(SessionEvent.TogglePause) }, Modifier.weight(1.3f), primary = false, height = height, textSize = 13.sp)
        BigButton("Skip ›", { send(SessionEvent.Skip) }, Modifier.weight(1f), primary = false, height = height, textSize = 13.sp)
    }
}

@Composable
private fun QueueRowView(r: QueueRow) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().alpha(if (r.status == RowStatus.DONE) 0.45f else 1f).clip(shape)
            .background(if (r.status == RowStatus.NOW) K.Teal.copy(alpha = 0.16f) else K.Card)
            .then(if (r.status == RowStatus.NOW) Modifier.border(1.dp, K.TealHi.copy(alpha = 0.4f), shape) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(r.reps, Modifier.width(52.dp), style = KText.mono(if (r.reps.length > 2) 14.sp else 24.sp).copy(color = if (r.status == RowStatus.NOW) K.TealHi else K.Muted), textAlign = TextAlign.End)
        Text(
            r.name, Modifier.weight(1f),
            style = KText.body.copy(fontSize = 16.sp, textDecoration = if (r.status == RowStatus.DONE) TextDecoration.LineThrough else null),
            maxLines = 1,
        )
        Text(
            if (r.status == RowStatus.NOW) "NOW" else r.restAfter?.let { "$it rest" }.orEmpty(),
            style = KText.label.copy(color = if (r.status == RowStatus.NOW) K.TealHi else K.Muted),
        )
    }
}

@Composable
private fun Finished(u: LiveUi, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label(u.workoutName)
        Text("Workout\ncomplete", style = KText.display(44.sp), textAlign = TextAlign.Center)
        Text(stopwatch(u.totalSeconds * 1000), style = KText.mono(40.sp).copy(color = K.TealHi))
        BigButton("CLOSE", onClose, Modifier.fillMaxWidth(), height = 72.dp, textSize = 20.sp)
    }
}

/** Same mapping as [liveUi], for the first frame before the flow emits. */
private fun liveUi0(s: SessionState?): LiveUi? = s?.let {
    if (it.phase == Phase.FINISHED) LiveUi(true, null, it.workoutName, it.totalElapsedMs / 1000)
    else LiveUi(false, liveModel(it), it.workoutName, 0)
}
