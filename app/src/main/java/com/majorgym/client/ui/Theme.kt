package com.majorgym.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Premium blue color system for the MajorGym client. Replaces the previous
 * orange accent theme end-to-end — every screen pulls from this single
 * palette so the whole app reads as one consistent, premium design system.
 */
object ClientColors {
    val Background = Color(0xFF090E18)
    val Surface = Color(0xFF111827)
    val Primary = Color(0xFF2563EB)
    val Accent = Color(0xFF3B82F6)
    val LightBlue = Color(0xFF60A5FA)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)
    val OnSurface = Color(0xFFFFFFFF)
    val Hint = Color(0xFF94A3B8)
    val Divider = Color(0xFF1E293B)
}

private val DarkColors = darkColorScheme(
    primary = ClientColors.Primary,
    onPrimary = ClientColors.OnSurface,
    secondary = ClientColors.Accent,
    onSecondary = ClientColors.OnSurface,
    tertiary = ClientColors.LightBlue,
    background = ClientColors.Background,
    onBackground = ClientColors.OnSurface,
    surface = ClientColors.Surface,
    onSurface = ClientColors.OnSurface,
    surfaceVariant = ClientColors.Surface,
    onSurfaceVariant = ClientColors.Hint,
    error = ClientColors.Danger,
    onError = ClientColors.OnSurface,
    outline = ClientColors.Divider,
    outlineVariant = ClientColors.Divider,
)

@Composable
fun MajorGymClientTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
