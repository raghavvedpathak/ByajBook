package com.byajbook.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Spec Requirement: Light-only Premium Theme. Dark theme explicitly out of scope.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005AC1), // Modern Deep Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF575E71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBE2F9),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Color(0xFF715573),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBD7FA),
    onTertiaryContainer = Color(0xFF29132D),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFEFBFF), 
    surface = Color(0xFFFEFBFF),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474E)
)

@Composable
fun ByajBookTheme(
    content: @Composable () -> Unit
) {
    // [darkTheme = false hardcoded]
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}