package dev.kinetik.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import dev.kinetik.cues.VOICE_LABELS
import dev.kinetik.cues.chosenVoice
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
    var pickingVoice by remember { mutableStateOf(false) }
    val voices by app.cues.voices.collectAsStateWithLifecycle()
    val voice = chosenVoice(s.voiceName, voices.map { it.name })

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clickable(onClick = onBack).padding(end = 12.dp), style = KText.body.copy(fontSize = 26.sp, color = K.Muted))
            Text("Settings", style = KText.display(26.sp))
        }
        SettingCard {
            SwitchRow("Voice announcements", s.voiceOn) { v -> set { it.copy(voiceOn = v) } }
            Row(Modifier.fillMaxWidth().clickable { pickingVoice = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Voice", Modifier.weight(1f), style = KText.body.copy(fontSize = 15.sp))
                Text(VOICE_LABELS[voice] ?: "Unavailable", style = KText.body.copy(fontSize = 14.sp, color = K.Muted))
            }
        }
        SettingCard {
            Text("Beep volume", style = KText.body.copy(fontSize = 15.sp))
            Slider(
                value = volume,
                onValueChange = { volume = it },
                onValueChangeFinished = { set { it.copy(beepVolume = (volume * 100).roundToInt()) } },
                colors = SliderDefaults.colors(thumbColor = K.Teal, activeTrackColor = K.Teal, inactiveTrackColor = K.Card2),
            )
            BigButton("Test beep", { app.cues.play(listOf(Cue.ShortBeep, Cue.LongBeep)) }, Modifier.padding(top = 8.dp), primary = false, height = 48.dp, textSize = 14.sp)
        }
        SettingCard { SwitchRow("Keep screen on during workouts", s.keepScreenOn) { v -> set { it.copy(keepScreenOn = v) } } }
    }
    if (pickingVoice) {
        // Picking a voice plays a sample straight away, so you can compare them without leaving the list.
        fun pick(name: String) {
            set { it.copy(voiceName = name) }
            app.cues.voiceName = name
            app.cues.play(listOf(Cue.Speak("Eight Pull ups")))
        }
        AlertDialog(
            onDismissRequest = { pickingVoice = false },
            title = { Text("Voice") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (voices.isEmpty()) Text("None of the voices are installed.", style = KText.body.copy(color = K.Muted, fontSize = 13.sp))
                    voices.forEach { v -> VoiceRow(VOICE_LABELS.getValue(v.name), voice == v.name) { pick(v.name) } }
                }
            },
            confirmButton = { TextButton({ pickingVoice = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun VoiceRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) K.Teal.copy(alpha = 0.16f) else K.Card)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(name, style = KText.body.copy(fontSize = 15.sp, color = if (selected) K.TealHi else K.Text))
    }
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Card).padding(16.dp), content = content)
}
