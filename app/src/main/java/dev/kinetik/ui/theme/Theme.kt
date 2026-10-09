package dev.kinetik.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun KinetikTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = K.Teal, onPrimary = K.OnTeal, secondary = K.TealHi, tertiary = K.Rest,
            background = K.Bg, onBackground = K.Text, surface = K.Card, onSurface = K.Text,
            surfaceVariant = K.Card2, onSurfaceVariant = K.Muted, outline = K.Line,
            surfaceContainerLow = K.Card, surfaceContainer = K.Card, surfaceContainerHigh = K.Card2,
        ),
        typography = Typography(
            bodyLarge = KText.body.copy(fontSize = 16.sp),
            bodyMedium = KText.body,
            labelLarge = KText.body.copy(fontWeight = FontWeight.Bold),
            titleLarge = KText.display(22.sp),
            headlineSmall = KText.display(24.sp),
        ),
        content = content,
    )
}
