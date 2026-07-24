package com.sleeproulette.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SleepRouletteColors.PowderBlueDeep,
    onPrimary = Color.White,
    primaryContainer = SleepRouletteColors.PowderBlue.copy(alpha = 0.35f),
    onPrimaryContainer = SleepRouletteColors.Ink,
    secondary = SleepRouletteColors.BlushDeep,
    onSecondary = Color.White,
    secondaryContainer = SleepRouletteColors.Blush.copy(alpha = 0.35f),
    onSecondaryContainer = SleepRouletteColors.Ink,
    tertiary = SleepRouletteColors.LavenderDeep,
    onTertiary = Color.White,
    tertiaryContainer = SleepRouletteColors.Lavender.copy(alpha = 0.35f),
    onTertiaryContainer = SleepRouletteColors.Ink,
    background = SleepRouletteColors.Cream,
    onBackground = SleepRouletteColors.Ink,
    surface = SleepRouletteColors.Cloud,
    onSurface = SleepRouletteColors.Ink,
    surfaceVariant = SleepRouletteColors.CreamSoft,
    onSurfaceVariant = SleepRouletteColors.Stone,
    outline = SleepRouletteColors.Mist,
    outlineVariant = SleepRouletteColors.Mist,
    error = SleepRouletteColors.Coral,
    onError = Color.White,
    errorContainer = SleepRouletteColors.Coral.copy(alpha = 0.2f),
    onErrorContainer = SleepRouletteColors.Ink,
    inverseSurface = SleepRouletteColors.Ink,
    inverseOnSurface = SleepRouletteColors.Cream,
    inversePrimary = SleepRouletteColors.PowderBlue,
    surfaceTint = SleepRouletteColors.PowderBlueDeep,
    scrim = Color.Black.copy(alpha = 0.4f),
)

private val DarkColors = darkColorScheme(
    primary = SleepRouletteColors.PowderBlue,
    onPrimary = SleepRouletteColors.Ink,
    primaryContainer = SleepRouletteColors.PowderBlueDeep.copy(alpha = 0.35f),
    onPrimaryContainer = SleepRouletteColors.NightOn,
    secondary = SleepRouletteColors.Blush,
    onSecondary = SleepRouletteColors.Ink,
    secondaryContainer = SleepRouletteColors.BlushDeep.copy(alpha = 0.3f),
    onSecondaryContainer = SleepRouletteColors.NightOn,
    tertiary = SleepRouletteColors.Lavender,
    onTertiary = SleepRouletteColors.Ink,
    tertiaryContainer = SleepRouletteColors.LavenderDeep.copy(alpha = 0.3f),
    onTertiaryContainer = SleepRouletteColors.NightOn,
    background = SleepRouletteColors.NightBg,
    onBackground = SleepRouletteColors.NightOn,
    surface = SleepRouletteColors.NightSurface,
    onSurface = SleepRouletteColors.NightOn,
    surfaceVariant = SleepRouletteColors.NightSurfaceVariant,
    onSurfaceVariant = SleepRouletteColors.NightMuted,
    outline = SleepRouletteColors.InkSoft,
    outlineVariant = SleepRouletteColors.NightSurfaceVariant,
    error = SleepRouletteColors.Coral,
    onError = SleepRouletteColors.Ink,
    errorContainer = SleepRouletteColors.Coral.copy(alpha = 0.25f),
    onErrorContainer = SleepRouletteColors.NightOn,
    inverseSurface = SleepRouletteColors.Cream,
    inverseOnSurface = SleepRouletteColors.Ink,
    inversePrimary = SleepRouletteColors.PowderBlueDeep,
    surfaceTint = SleepRouletteColors.PowderBlue,
    scrim = Color.Black.copy(alpha = 0.55f),
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
