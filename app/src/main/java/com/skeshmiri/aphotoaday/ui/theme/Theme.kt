package com.skeshmiri.aphotoaday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
        colorScheme = DarkColors,
        content = content,
    )
}
