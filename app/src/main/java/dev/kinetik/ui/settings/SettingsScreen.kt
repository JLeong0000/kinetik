package dev.kinetik.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kinetik.app
import dev.kinetik.cues.Cue
import dev.kinetik.model.Settings
import dev.kinetik.ui.components.BigButton
import dev.kinetik.ui.components.SwitchRow
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KText
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.app
    val lib by app.store.library.collectAsStateWithLifecycle()
    val s = lib.settings
    fun set(t: (Settings) -> Settings) = app.store.update { it.copy(settings = t(it.settings)) }
    var volume by remember { mutableFloatStateOf(s.beepVolume / 100f) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clickable(onClick = onBack).padding(end = 12.dp), style = KText.body.copy(fontSize = 26.sp, color = K.Muted))
            Text("Settings", style = KText.display(26.sp))
        }
        SettingCard { SwitchRow("Voice announcements", s.voiceOn) { v -> set { it.copy(voiceOn = v) } } }
        SettingCard {
            Text("Beep volume", style = KText.body.copy(fontSize = 15.sp))
            Slider(
                value = volume,
                onValueChange = { volume = it },
                onValueChangeFinished = { set { it.copy(beepVolume = (volume * 100).roundToInt()) } },
                colors = SliderDefaults.colors(thumbColor = K.Teal, activeTrackColor = K.Teal, inactiveTrackColor = K.Card2),
            )
            TextButton({ app.cues.play(listOf(Cue.ShortBeep, Cue.LongBeep)) }) { Text("Test beep") }
        }
        SettingCard { SwitchRow("Keep screen on during workouts", s.keepScreenOn) { v -> set { it.copy(keepScreenOn = v) } } }
        SettingCard {
            Text("Reset workouts", style = KText.body.copy(fontSize = 15.sp))
            Text("Replaces every workout with the original Pull, Push, Legs and Abs plan.", style = KText.body.copy(color = K.Muted, fontSize = 13.sp))
            BigButton("Reset", { confirmReset = true }, Modifier.fillMaxWidth().padding(top = 8.dp), primary = false, height = 52.dp, textSize = 15.sp)
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all workouts?") },
            text = { Text("Your edits will be lost.") },
            confirmButton = { TextButton({ confirmReset = false; app.store.resetToSeed() }) { Text("Reset") } },
            dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Card).padding(16.dp), content = content)
}
