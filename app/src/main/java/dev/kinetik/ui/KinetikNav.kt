package dev.kinetik.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.kinetik.app
import dev.kinetik.ui.editor.EditorScreen
import dev.kinetik.ui.home.HomeScreen
import dev.kinetik.ui.live.LiveScreen
import dev.kinetik.ui.settings.SettingsScreen

@Composable
fun KinetikNav(mode: LayoutMode) {
    val app = LocalContext.current.app
    val nav = rememberNavController()
    val session by app.session.state.collectAsStateWithLifecycle()
    val start = remember { if (session != null) "live" else "home" }

    NavHost(nav, startDestination = start) {
        composable("home") {
            HomeScreen(
                mode = mode,
                onStart = { id -> app.session.start(id); nav.navigate("live") },
                onEdit = { id -> nav.navigate("edit/$id") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("edit/{id}") { entry ->
            EditorScreen(entry.arguments?.getString("id").orEmpty(), mode, onBack = { nav.popBackStack() })
        }
        composable("live") {
            LiveScreen(mode, onExit = {
                if (!nav.popBackStack("home", inclusive = false)) {
                    nav.navigate("home") { popUpTo("live") { inclusive = true } }
                }
            })
        }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
