package ir.khanoomi.geofencealert.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    error = AlertRed,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = AlertRedLight,
    onErrorContainer = AlertRed
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF81C784),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF0B2E0E),
    error = androidx.compose.ui.graphics.Color(0xFFFF8A80),
    onError = androidx.compose.ui.graphics.Color(0xFF4A0000)
)

@Composable
fun GeofenceAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
