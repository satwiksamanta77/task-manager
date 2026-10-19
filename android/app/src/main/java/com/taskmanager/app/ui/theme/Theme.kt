package com.taskmanager.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Iris,
    onPrimary = Color.White,
    primaryContainer = IrisDeep,
    onPrimaryContainer = Color.White,
    secondary = Cyan,
    onSecondary = Bg,
    tertiary = Warning,
    onTertiary = Bg,
    error = Danger,
    onError = Color.White,
    background = Bg,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = Surface2,
    onSurfaceVariant = Dim,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface2,
    outline = Border,
    outlineVariant = Border
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TaskManagerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Premium dark-first experience; the light scheme is intentionally not
    // offered so the brand stays consistent everywhere.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
