package dev.kinetik.ui.editor

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.kinetik.ui.LayoutMode

@Composable
fun EditorScreen(workoutId: String, mode: LayoutMode, onBack: () -> Unit) = Text("Editor $workoutId")
