package dev.kinetik.ui.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kinetik.app
import dev.kinetik.session.SessionEvent
import dev.kinetik.session.notificationText
import dev.kinetik.ui.LayoutMode
import dev.kinetik.ui.components.BigButton

@Composable
fun LiveScreen(mode: LayoutMode, onExit: () -> Unit) {
    val app = LocalContext.current.app
    val s by app.session.state.collectAsStateWithLifecycle()
    val state = s ?: run { LaunchedEffect(Unit) { onExit() }; return }
    Column(Modifier.safeDrawingPadding()) {
        Text(notificationText(state).toString())
        BigButton("DONE", { app.session.send(SessionEvent.Done) })
        BigButton("END", { app.session.stop() }, primary = false)
    }
}
