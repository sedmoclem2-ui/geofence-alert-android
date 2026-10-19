package ir.khanoomi.geofencealert.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = AmberOnSecondary,
    background = PaperBackground,
    onBackground = InkOnBackground,
    surface = Color.White,
    onSurface = InkOnBackground,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = SandOnSurfaceVariant,
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralErrorContainer,
    onErrorContainer = CoralOnErrorContainer,
    outline = SandOutline,
    outlineVariant = Color(0xFFE7E1D3)
)

private val DarkColors = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberOnContainerDark,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    error = SalmonErrorDark,
    onError = NightOnError,
    errorContainer = NightErrorContainer,
    onErrorContainer = NightOnErrorContainer,
    outline = NightOutline,
    outlineVariant = NightOutline
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun GeofenceAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
