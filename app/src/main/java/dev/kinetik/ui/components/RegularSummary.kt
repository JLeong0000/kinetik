package dev.kinetik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kinetik.model.Exercise
import dev.kinetik.model.Workout
import dev.kinetik.plan.shortLabel
import dev.kinetik.session.countdown
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KShape
import dev.kinetik.ui.theme.KText

/** "3 × 8 · 2:00" — sets × reps and the rest between sets (the workout-wide one if it's switched on). */
fun setsLabel(e: Exercise, globalRestSec: Int?): String {
    val reps = shortLabel(e.copy(sets = 1), e.startReps)
    return "${e.sets} × $reps · ${countdown((globalRestSec ?: e.restSec) * 1000L)}"
}

/** A regular workout's blocks and exercises, used where circuit workouts show the plan grid. */
@Composable
fun RegularSummary(w: Workout, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        w.blocks.filter { it.exercises.isNotEmpty() }.forEach { b ->
            Column(Modifier.fillMaxWidth().clip(KShape.Big).background(K.Card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(b.name)
                b.exercises.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.name, Modifier.weight(1f), style = KText.body.copy(fontSize = 14.sp), maxLines = 1)
                        Text(setsLabel(e, w.setRestSec), Modifier.padding(start = 8.dp), style = KText.mono(13.sp).copy(color = K.TealHi))
                    }
                }
            }
        }
    }
}
