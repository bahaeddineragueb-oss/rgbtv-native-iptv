package com.rgbtv.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeonDark = darkColorScheme(
    primary = Color(0xFFFF3D71),
    onPrimary = Color.White,
    secondary = Color(0xFF8B5CF6),
    tertiary = Color(0xFF20D9D2),
    background = Color(0xFF080A12),
    surface = Color(0xFF101421),
    surfaceVariant = Color(0xFF1A2030),
    onBackground = Color(0xFFF6F7FB),
    onSurface = Color(0xFFF6F7FB),
    onSurfaceVariant = Color(0xFFA5ADC0)
)

@Composable
fun RgbTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NeonDark, content = content)
}
