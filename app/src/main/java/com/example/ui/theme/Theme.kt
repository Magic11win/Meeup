package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = MeetupTealDark,
    onPrimaryContainer = Color.White,
    secondary = MeetupEmerald,
    onSecondary = DarkBackground,
    secondaryContainer = DarkBubbleOutgoing,
    onSecondaryContainer = DarkOnSurface,
    tertiary = ReadReceiptBlue,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = MeetupTealDark,
    onPrimary = LightOnPrimary,
    primaryContainer = MeetupTealMedium,
    onPrimaryContainer = Color.White,
    secondary = MeetupEmerald,
    onSecondary = Color.White,
    secondaryContainer = LightBubbleOutgoing,
    onSecondaryContainer = LightOnSurface,
    tertiary = ReadReceiptBlue,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
