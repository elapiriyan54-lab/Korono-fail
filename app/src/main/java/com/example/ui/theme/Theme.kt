package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val KronoColorScheme =
  darkColorScheme(
    primary = IceCyan,
    onPrimary = ObsidianDeep,
    primaryContainer = Color(0xFF0B4666),
    onPrimaryContainer = NeonCyanSoft,
    secondary = PrismaticViolet,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFF331B5E),
    onSecondaryContainer = NeonVioletSoft,
    tertiary = WarmAmber,
    onTertiary = ObsidianDeep,
    tertiaryContainer = Color(0xFF4A2E08),
    onTertiaryContainer = WarmGold,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderSubtle,
    error = NeonRed,
    onError = TextPrimary,
  )

private val KronoShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun KronoFilesTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = KronoColorScheme,
    typography = KronoTypography,
    shapes = KronoShapes,
    content = content,
  )
}
