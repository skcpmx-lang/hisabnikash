package com.hisabnikash.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Light-only premium theme. The app deliberately ignores system dark mode so
 * documents and financial data keep a consistent, trustworthy appearance.
 *
 * Surface hierarchy (Level 1 -> Level 3):
 *  - app canvas          Background (warm ivory)
 *  - open sections live  directly on the canvas
 *  - soft containers     SurfaceTint (secondary, no elevation)
 *  - elevated cards      Surface (white, only for key actions/data)
 */
private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Surface,
    primaryContainer = BrandGreenSoft,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGold,
    onSecondary = Surface,
    secondaryContainer = BrandGoldSoft,
    onSecondaryContainer = Ink,
    tertiary = Info,
    onTertiary = Surface,
    background = Background,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = SurfaceTint,
    surfaceContainer = SurfaceAlt,
    surfaceContainerHigh = SurfaceAlt,
    outline = OutlineSoft,
    outlineVariant = SurfaceStroke,
    error = Error,
    onError = Surface,
    errorContainer = Color(0xFFF7E6E1),
    onErrorContainer = Color(0xFF7A2A1C),
    surfaceTint = BrandGreen
)

private val HisabShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun HisabNikashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = HisabTypography,
        shapes = HisabShapes,
        content = content
    )
}
