package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GameColorScheme = darkColorScheme(
    primary = DarkWoodButton,
    secondary = GoldYell,
    tertiary = BrightCrimson,
    background = DarkBg,
    surface = PapyrusBg,
    onPrimary = PapyrusBgLight,
    onSecondary = PapyrusText,
    onTertiary = PapyrusBgLight,
    onBackground = PapyrusBgLight,
    onSurface = PapyrusText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force gamified dark theme
    dynamicColor: Boolean = false, // Disable system dynamic color
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GameColorScheme,
        typography = Typography,
        content = content
    )
}
