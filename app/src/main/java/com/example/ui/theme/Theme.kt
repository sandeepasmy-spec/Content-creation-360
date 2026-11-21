package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueSoft,
    onPrimaryContainer = NavyDeep,
    secondary = StatusInfo,
    onSecondary = Color.White,
    secondaryContainer = StatusInfoSoft,
    onSecondaryContainer = NavyDeep,
    tertiary = StatusCompleted,
    onTertiary = Color.White,
    tertiaryContainer = StatusCompletedSoft,
    onTertiaryContainer = Color(0xFF064E3B),
    background = CanvasLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = StatusOverdue,
    onError = Color.White,
    errorContainer = StatusOverdueSoft,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricBlueBright,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = StatusInfo,
    onSecondary = Color.White,
    background = NavyDeep,
    onBackground = Color(0xFFF8FAFC),
    surface = NavySlate,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF283548),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = NavyBorder,
    error = StatusOverdue,
    onError = Color.White
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun StudioFlowTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudioFlowTypography,
        shapes = StudioShapes,
        content = content
    )
}
