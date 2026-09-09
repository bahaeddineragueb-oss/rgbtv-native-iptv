package com.rgbtv.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* -------------------------------------------------------------------------- */
/* Neon Cinema palette                                                        */
/*                                                                            */
/* Two pinks on purpose:                                                      */
/*  - NeonPink      is for text/icons on the dark canvas (5.8:1 on midnight).  */
/*  - NeonPinkDeep  is for filled surfaces that carry white labels (4.95:1).   */
/* Using the vivid pink behind white text would sit at 3.41:1 and fail WCAG AA.*/
/* -------------------------------------------------------------------------- */
val NeonPink: Color = Color(0xFFFF3D71)
val NeonPinkDeep: Color = Color(0xFFD81B60)
val NeonViolet: Color = Color(0xFF8B5CF6)
val NeonCyan: Color = Color(0xFF20D9D2)
val Midnight: Color = Color(0xFF080A12)
val Graphite: Color = Color(0xFF101421)
val Slate: Color = Color(0xFF1A2030)
val Ash: Color = Color(0xFFA5ADC0)

private val NeonDark = darkColorScheme(
    primary = NeonPink,
    onPrimary = Color.White,
    primaryContainer = NeonPinkDeep,
    onPrimaryContainer = Color.White,
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A1E4A),
    onSecondaryContainer = Color(0xFFE4D9FF),
    tertiary = NeonCyan,
    onTertiary = Midnight,
    tertiaryContainer = Color(0xFF0E3B3A),
    onTertiaryContainer = Color(0xFF8DF3EE),
    background = Midnight,
    onBackground = Color(0xFFF6F7FB),
    surface = Graphite,
    onSurface = Color(0xFFF6F7FB),
    surfaceVariant = Slate,
    onSurfaceVariant = Ash,
    surfaceContainerLowest = Color(0xFF05070D),
    surfaceContainerLow = Graphite,
    surfaceContainer = Color(0xFF131A29),
    surfaceContainerHigh = Color(0xFF161C2C),
    surfaceContainerHighest = Color(0xFF222A3D),
    outline = Color(0xFF2C3448),
    outlineVariant = Color(0xFF202838),
    scrim = Color(0xFF03050A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val NeonTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.5.sp)
)

private val NeonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * RGBTv is a deliberately dark, cinematic player: the brand identity is a dark-only canvas,
 * so the theme does not follow the system light/dark setting.
 */
@Composable
fun RgbTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonDark,
        typography = NeonTypography,
        shapes = NeonShapes,
        content = content
    )
}
