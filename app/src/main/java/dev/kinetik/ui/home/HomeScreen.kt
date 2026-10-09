package dev.kinetik.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kinetik.app
import dev.kinetik.model.allWorkouts
import dev.kinetik.ui.LayoutMode
import dev.kinetik.ui.components.BigButton
import dev.kinetik.ui.theme.KText

@Composable
fun HomeScreen(mode: LayoutMode, onStart: (String) -> Unit, onEdit: (String) -> Unit, onSettings: () -> Unit) {
    val lib by LocalContext.current.app.store.library.collectAsStateWithLifecycle()
    Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Workouts · $mode", style = KText.display(26.sp))
        lib.allWorkouts().forEach { w ->
            BigButton("▶ ${w.name}", { onStart(w.id) }, Modifier.fillMaxWidth(), height = 56.dp, textSize = 16.sp)
        }
    }
}
