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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private fun lightScheme(p: PaletteColors): ColorScheme = lightColorScheme(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.secondary,
    onSecondary = if (p.secondary.luminance() > 0.55f) Color(0xFF1A1A1A) else Color.White,
    secondaryContainer = mixColor(p.secondary, Color.White, 0.78f),
    onSecondaryContainer = mixColor(p.secondary, Color.Black, 0.6f),
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
        AppPalette.DEEP_BLUE -> Color(0xFFE6F0FA)
        AppPalette.TEAL -> NightOnSurface
        AppPalette.INDIGO -> Color(0xFFE9EBFA)
        AppPalette.ROSE -> Color(0xFFFAE9EE)
        AppPalette.GALAXY -> Color(0xFFF0E9FB)
        AppPalette.SUNSET -> Color(0xFFFBEFE4)
        AppPalette.GOLD -> Color(0xFFF7F0DC)
        AppPalette.OCEAN -> Color(0xFFE4F1FA)
        AppPalette.FOREST -> Color(0xFFE8F3E8)
        AppPalette.CHERRY -> Color(0xFFFAE9F0)
        AppPalette.MIDNIGHT -> Color(0xFFE3EAF2)
    }
    val onDarkVariant = when (palette) {
        AppPalette.DEEP_BLUE -> Color(0xFFA9BFD6)
        AppPalette.TEAL -> NightOnSurfaceVariant
        AppPalette.INDIGO -> Color(0xFFB9BFD9)
        AppPalette.ROSE -> Color(0xFFD9B9C2)
        AppPalette.GALAXY -> Color(0xFFC4B3DB)
        AppPalette.SUNSET -> Color(0xFFD9BFA9)
        AppPalette.GOLD -> Color(0xFFD9CC9E)
        AppPalette.OCEAN -> Color(0xFFADC4D9)
        AppPalette.FOREST -> Color(0xFFB3CCB3)
        AppPalette.CHERRY -> Color(0xFFD9B3C2)
        AppPalette.MIDNIGHT -> Color(0xFFA9BACB)
    }
    val outline = when (palette) {
        AppPalette.DEEP_BLUE -> Color(0xFF24435C)
        AppPalette.TEAL -> NightOutline
        AppPalette.INDIGO -> Color(0xFF3A4270)
        AppPalette.ROSE -> Color(0xFF5A3540)
        AppPalette.GALAXY -> Color(0xFF4A3470)
        AppPalette.SUNSET -> Color(0xFF6B3A24)
        AppPalette.GOLD -> Color(0xFF5C4A1E)
        AppPalette.OCEAN -> Color(0xFF24435C)
        AppPalette.FOREST -> Color(0xFF2E4A30)
        AppPalette.CHERRY -> Color(0xFF5C2C3A)
        AppPalette.MIDNIGHT -> Color(0xFF2E4356)
    }
    return darkColorScheme(
        primary = p.primary,
        onPrimary = p.onPrimary,
        primaryContainer = p.primaryContainer,
        onPrimaryContainer = p.onPrimaryContainer,
        secondary = p.secondary,
        onSecondary = if (p.secondary.luminance() > 0.55f) Color(0xFF1A1A1A) else Color.White,
        secondaryContainer = mixColor(p.secondary, Color.Black, 0.55f),
        onSecondaryContainer = mixColor(p.secondary, Color.White, 0.8f),
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

/** طرح تیره برای پالت شخصی‌سازی‌شده (رنگ‌های رویی خنثی). */
private fun darkSchemeCustom(p: PaletteColors): ColorScheme = darkColorScheme(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.secondary,
    onSecondary = if (p.secondary.luminance() > 0.55f) Color(0xFF1A1A1A) else Color.White,
    secondaryContainer = mixColor(p.secondary, Color.Black, 0.55f),
    onSecondaryContainer = mixColor(p.secondary, Color.White, 0.8f),
    tertiary = p.tertiary,
    background = p.background,
    onBackground = Color(0xFFF5F5F5),
    surface = p.surface,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = SalmonErrorDark,
    onError = NightOnError,
    errorContainer = NightErrorContainer,
    onErrorContainer = NightOnErrorContainer,
    outline = Color(0xFF424242),
    outlineVariant = Color(0xFF424242)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

/** تم برنامه با پالت رنگی قابل انتخاب کاربر. */
@Composable
fun AppTheme(
    palette: AppPalette = AppPalette.DEEP_BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppTheme(
        paletteKey = palette.key,
        customColors = null,
        darkTheme = darkTheme,
        content = content
    )
}

/**
 * تم برنامه بر اساس کلید رشته‌ای + پالت شخصی‌سازی‌شده اختیاری.
 * اگر [paletteKey] برابر [CUSTOM_THEME_KEY] باشد و [customColors] داده شده باشد،
 * از پالت ساخته‌شده توسط کاربر استفاده می‌شود.
 */
@Composable
fun AppTheme(
    paletteKey: String,
    customColors: PaletteColors? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = when {
        paletteKey == CUSTOM_THEME_KEY && customColors != null ->
            if (darkTheme) darkSchemeCustom(customColors) else lightScheme(customColors)
        else -> {
            val palette = AppPalette.fromKey(paletteKey)
            if (darkTheme) darkScheme(palette) else lightScheme(palette.light)
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
