package com.beehivehex.fill.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HiveColors = darkColorScheme(
    primary = PrimaryLeaf, onPrimary = Background,
    secondary = ActionBlue, onSecondary = TextPrimary,
    tertiary = Honey, onTertiary = Background,
    background = Background, onBackground = TextPrimary,
    surface = RaisedForest, onSurface = TextPrimary,
    surfaceVariant = SecondaryForest, onSurfaceVariant = TextSecondary,
    outline = TextSecondary.copy(alpha = 0.4f), error = GentleWarning, onError = Background
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HiveColors, typography = HiveTypography, content = content)
}
