package com.example.matchering.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MatcheringGold,
    onPrimary = Color(0xFF1C1A00),
    primaryContainer = Color(0xFF383000),
    onPrimaryContainer = MatcheringGoldLight,
    secondary = ReferenceCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = ReferenceCyanDim,
    onSecondaryContainer = Color(0xFF80F0FF),
    tertiary = TargetOrange,
    onTertiary = Color(0xFF381500),
    tertiaryContainer = TargetOrangeDim,
    onTertiaryContainer = Color(0xFFFFB59D),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderAccent
)

@Composable
fun MatcheringTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
