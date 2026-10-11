package dev.kinetik.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    // Whenever a workout is running (say the app is reopened mid-workout), show the live screen.
    LaunchedEffect(session != null) {
        if (session != null && nav.currentDestination?.route != "live") {
            nav.navigate("live") { launchSingleTop = true }
        }
    }

    // Quick cross-fades between screens; the navigation default (700 ms) felt sluggish.
    NavHost(
        nav, startDestination = start,
        enterTransition = { fadeIn(tween(150)) }, exitTransition = { fadeOut(tween(150)) },
    ) {
        composable("home") {
            HomeScreen(
                mode = mode,
                onStart = { id -> app.session.start(id); nav.navigate("live") { launchSingleTop = true } },
                onEdit = { id -> nav.navigate("edit/$id") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("edit/{id}") { entry ->
            EditorScreen(entry.arguments?.getString("id").orEmpty(), mode, onBack = { nav.popBackStack() })
        }
        composable("live") {
            // Back to wherever the workout was started from (home, or an open editor with its unsaved draft).
            LiveScreen(mode, onExit = {
                if (!nav.popBackStack()) nav.navigate("home") { popUpTo("live") { inclusive = true } }
            })
        }
        composable("settings") { SettingsScreen(mode, onBack = { nav.popBackStack() }) }
    }
}
