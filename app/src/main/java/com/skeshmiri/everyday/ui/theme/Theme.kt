package com.skeshmiri.everyday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Shamrock,
    onPrimary = Snow,
    primaryContainer = BlushPop,
    onPrimaryContainer = JetBlack,
    secondary = DustyGrape,
    onSecondary = Snow,
    secondaryContainer = BlushPop,
    onSecondaryContainer = JetBlack,
    tertiary = BlushPop,
    onTertiary = JetBlack,
    background = Snow,
    onBackground = JetBlack,
    surface = Snow,
    onSurface = JetBlack,
    surfaceVariant = BlushPop,
    onSurfaceVariant = JetBlack,
    outline = DustyGrape,
    error = DustyGrape,
    onError = Snow,
)

private val DarkColors = darkColorScheme(
    primary = Shamrock,
    onPrimary = JetBlack,
    primaryContainer = DustyGrape,
    onPrimaryContainer = Snow,
    secondary = BlushPop,
    onSecondary = JetBlack,
    secondaryContainer = Shamrock,
    onSecondaryContainer = Snow,
    tertiary = DustyGrape,
    onTertiary = Snow,
    background = JetBlack,
    onBackground = Snow,
    surface = JetBlack,
    onSurface = Snow,
    surfaceVariant = DustyGrape,
    onSurfaceVariant = Snow,
    outline = BlushPop,
    error = BlushPop,
    onError = JetBlack,
)

@Composable
fun EverydayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
