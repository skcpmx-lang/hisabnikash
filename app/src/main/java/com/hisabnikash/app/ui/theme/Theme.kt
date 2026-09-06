package com.hisabnikash.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Light-only theme. The application deliberately ignores system dark mode so
 * documents and financial data keep a consistent, trustworthy appearance.
 */
private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Surface,
    primaryContainer = BrandGreenSoft,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGold,
    onSecondary = Ink,
    secondaryContainer = BrandGoldSoft,
    onSecondaryContainer = Ink,
    tertiary = Info,
    onTertiary = Surface,
    background = Background,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = InkSoft,
    outline = OutlineSoft,
    outlineVariant = SurfaceTint,
    error = Error,
    onError = Surface,
    errorContainer = Color(0xFFF8E5E1),
    onErrorContainer = Color(0xFF7A2A1C),
    surfaceTint = BrandGreen
)

private val HisabShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
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
