package com.skeshmiri.aphotoaday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

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

private data class AppFontResource(
    val resourceName: String,
    val weight: FontWeight,
)

private val AppFontResources = listOf(
    AppFontResource("app_font_regular", FontWeight.Normal),
    AppFontResource("app_font_medium", FontWeight.Medium),
    AppFontResource("app_font_semibold", FontWeight.SemiBold),
    AppFontResource("app_font_bold", FontWeight.Bold),
)

@Composable
fun EverydayTheme(content: @Composable () -> Unit) {
    val typography = Typography().withFontFamily(appFontFamily())

    MaterialTheme(
        colorScheme = DarkColors,
        typography = typography,
        content = content,
    )
}

@Composable
private fun appFontFamily(): FontFamily {
    val context = LocalContext.current
    return remember(context) {
        val fonts = AppFontResources.mapNotNull { appFontResource ->
            val fontResId = context.resources.getIdentifier(
                appFontResource.resourceName,
                "font",
                context.packageName,
            )

            if (fontResId == 0) {
                null
            } else {
                Font(
                    resId = fontResId,
                    weight = appFontResource.weight,
                )
            }
        }

        if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
    }
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)
