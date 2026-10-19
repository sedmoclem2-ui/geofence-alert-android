package ir.khanoomi.geofencealert.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private fun lightScheme(p: PaletteColors): ColorScheme = lightColorScheme(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.secondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = AmberOnSecondary,
    tertiary = p.tertiary,
    background = p.background,
    onBackground = InkOnBackground,
    surface = p.surface,
    onSurface = InkOnBackground,
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = SandOnSurfaceVariant,
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralErrorContainer,
    onErrorContainer = CoralOnErrorContainer,
    outline = SandOutline,
    outlineVariant = Color(0xFFE7E1D3)
)

private fun darkScheme(palette: AppPalette): ColorScheme {
    val p = palette.dark
    val onDark = when (palette) {
        AppPalette.TEAL -> NightOnSurface
        AppPalette.INDIGO -> Color(0xFFE9EBFA)
        AppPalette.ROSE -> Color(0xFFFAE9EE)
    }
    val onDarkVariant = when (palette) {
        AppPalette.TEAL -> NightOnSurfaceVariant
        AppPalette.INDIGO -> Color(0xFFB9BFD9)
        AppPalette.ROSE -> Color(0xFFD9B9C2)
    }
    val outline = when (palette) {
        AppPalette.TEAL -> NightOutline
        AppPalette.INDIGO -> Color(0xFF3A4270)
        AppPalette.ROSE -> Color(0xFF5A3540)
    }
    return darkColorScheme(
        primary = p.primary,
        onPrimary = p.onPrimary,
        primaryContainer = p.primaryContainer,
        onPrimaryContainer = p.onPrimaryContainer,
        secondary = p.secondary,
        onSecondary = AmberOnSecondary,
        secondaryContainer = AmberContainerDark,
        onSecondaryContainer = AmberOnContainerDark,
        tertiary = p.tertiary,
        background = p.background,
        onBackground = onDark,
        surface = p.surface,
        onSurface = onDark,
        surfaceVariant = p.surfaceVariant,
        onSurfaceVariant = onDarkVariant,
        error = SalmonErrorDark,
        onError = NightOnError,
        errorContainer = NightErrorContainer,
        onErrorContainer = NightOnErrorContainer,
        outline = outline,
        outlineVariant = outline
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

/** تم برنامه با پالت رنگی قابل انتخاب کاربر. */
@Composable
fun AppTheme(
    palette: AppPalette = AppPalette.TEAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme(palette) else lightScheme(palette.light),
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
