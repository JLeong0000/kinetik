package dev.kinetik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kinetik.model.upNext
import dev.kinetik.session.SessionEvent
import dev.kinetik.session.notificationText

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val s by app.session.state.collectAsStateWithLifecycle()
            Column(
                Modifier.fillMaxSize().background(Color(0xFF121414)).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(s?.let { notificationText(it).toString() } ?: "No session", color = Color.White)
                Button(onClick = { app.store.library.value.upNext()?.let { app.session.start(it.id) } }) { Text("Start up next") }
                Button(onClick = { app.session.send(SessionEvent.Done) }) { Text("Done") }
                Button(onClick = { app.session.send(SessionEvent.Skip) }) { Text("Skip") }
                Button(onClick = { app.session.stop() }) { Text("Stop") }
            }
        }
    }
}
