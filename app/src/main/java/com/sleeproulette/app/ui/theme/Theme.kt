package com.sleeproulette.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Night-forward palette — deep slate, cool blue accent (not purple-default AI look).
private val NightBlue = Color(0xFF5B8DEF)
private val NightBg = Color(0xFF0B1220)
private val NightSurface = Color(0xFF152036)
private val NightOn = Color(0xFFE8EEF8)
private val DawnAmber = Color(0xFFE0A45C)

private val DarkColors = darkColorScheme(
    primary = NightBlue,
    onPrimary = Color.White,
    secondary = DawnAmber,
    onSecondary = Color(0xFF1A1208),
    background = NightBg,
    onBackground = NightOn,
    surface = NightSurface,
    onSurface = NightOn,
    surfaceVariant = Color(0xFF1E2A42),
    onSurfaceVariant = Color(0xFFB7C2D6),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5FB8),
    onPrimary = Color.White,
    secondary = Color(0xFF9A6A2F),
    background = Color(0xFFF3F6FB),
    onBackground = Color(0xFF121826),
    surface = Color.White,
    onSurface = Color(0xFF121826),
)

@Composable
fun SleepRouletteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
