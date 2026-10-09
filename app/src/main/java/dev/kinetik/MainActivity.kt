package dev.kinetik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.WindowInfoTracker
import dev.kinetik.ui.KinetikNav
import dev.kinetik.ui.LocalLayoutMode
import dev.kinetik.ui.layoutModeFor
import dev.kinetik.ui.theme.K
import dev.kinetik.ui.theme.KinetikTheme
import dev.kinetik.ui.toFold
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kinetik is always dark, so system bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        startFromIntent(intent)
        setContent {
            KinetikTheme {
                val foldFlow = remember { WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this).map { it.toFold() } }
                val fold by foldFlow.collectAsStateWithLifecycle(initialValue = null)
                val mode = layoutModeFor(LocalConfiguration.current.screenWidthDp, fold)
                CompositionLocalProvider(LocalLayoutMode provides mode) {
                    Box(Modifier.fillMaxSize().background(K.Bg)) { KinetikNav(mode) }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        startFromIntent(intent)
    }

    /** The widget's Start button opens the app with the workout to start. */
    private fun startFromIntent(intent: android.content.Intent?) {
        val id = intent?.getStringExtra(EXTRA_START) ?: return
        intent.removeExtra(EXTRA_START)
        app.session.start(id)
    }

    companion object {
        const val EXTRA_START = "dev.kinetik.START_WORKOUT"
    }
}
