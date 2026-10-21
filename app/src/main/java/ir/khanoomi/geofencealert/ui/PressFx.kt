package ir.khanoomi.geofencealert.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * مقیاس فشردن فنری برای عناصر تعاملی: هنگام لمس، عنصر کمی جمع می‌شود و
 * با فیزیک فنر (جهش ملایم) برمی‌گردد — حس «زنده بودن» دکمه‌ها و کارت‌ها.
 * مقدار برگشتی را با graphicsLayer روی عنصر اعمال کنید.
 */
@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.93f
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return scale
}
