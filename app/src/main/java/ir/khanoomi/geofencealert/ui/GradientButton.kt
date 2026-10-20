package ir.khanoomi.geofencealert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * دکمه اصلی (CTA) با گرادیان افقی primary → secondary روی متن onPrimary —
 * ظاهر آینده‌نگر برای دکمه‌های «ذخیره»، «تأیید» و مانند آن.
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val gradient = Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary))
    Button(
        onClick = onClick,
        modifier = modifier
            .background(gradient, shape)
            .alpha(if (enabled) 1f else 0.45f),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = scheme.onPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = scheme.onPrimary
        ),
        shape = shape,
        content = content
    )
}
