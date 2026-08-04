package com.majorgym.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Matches the Flutter client's dark theme: orange accent (0xFFFF6B35) on a
 * near-black background, with cards a shade lighter than the background —
 * see app.dart's `ColorScheme.fromSeed(seedColor: 0xFFFF6B35, dark)`.
 */
object ClientColors {
    val Accent = Color(0xFFFF6B35)
    val Background = Color(0xFF0F1115)
    val Surface = Color(0xFF1A1D24)
    val OnSurface = Color(0xFFECEDEE)
    val Hint = Color(0xFF9AA0A8)
    val Success = Color(0xFF34D399)
    val Danger = Color(0xFFEF5350)
}

private val DarkColors = darkColorScheme(
    primary = ClientColors.Accent,
    background = ClientColors.Background,
    surface = ClientColors.Surface,
    onBackground = ClientColors.OnSurface,
    onSurface = ClientColors.OnSurface,
)

@Composable
fun MajorGymClientTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
