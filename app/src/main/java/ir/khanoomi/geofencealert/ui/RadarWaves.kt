package ir.khanoomi.geofencealert.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * سیستم موج‌های راداری برنامه — زبان بصری مشترک کارت قهرمان (پایش کلی) و
 * کارت‌های محدوده:
 *
 * - **سونارِ زنده** (وقتی فعال است): هسته‌ای نورانی با هالهٔ شعاعیِ تنفسی و
 *   ۳+ حلقهٔ هم‌مرکز که با فازهای پلکانی باز می‌شوند؛ هر حلقه هنگام تولد
 *   روشن و پُر است و هرچه به لبه نزدیک‌تر می‌شود کم‌رنگ‌تر و باریک‌تر
 *   می‌شود — مثل موج واقعی سونار روی آب، نه حلقهٔ مکانیکی.
 * - **انفجار جشن** (روشن شدن): یک موج شوک سریع + حلقه‌ای از ذرات ریز که
 *   به بیرون پرتاب و محو می‌شوند + فلش هسته؛ بعد همه‌چیز آرام در سونار
 *   می‌نشیند.
 * - **فروریزش** (خاموش شدن): حلقه‌ها به‌سمت هسته جمع و محو می‌شوند و هسته
 *   با گذار رنگی نرم کم‌نور می‌شود (انفجارِ برعکس).
 *
 * همه‌چیز با یک ساعتِ انیمیشن واحد در هر نمونه رانده می‌شود
 * (rememberInfiniteTransition) و دو Animatable برای انفجار/فروریزش؛
 * تایمر جدا برای هر حلقه نداریم و در محدودهٔ رسم تخصیص اضافی انجام
 * نمی‌شود. وقتی کارت از ترکیب خارج شود، انیمیشن‌ها خودکار می‌ایستند.
 */

/** وضعیت گذارهای رادار برای یک کارت؛ با [rememberRadarBurstState] ساخته می‌شود. */
class RadarBurstState internal constructor(initialActive: Boolean) {
    /** پیشرفت موج شوک + ذرات هنگام روشن شدن (۱ یعنی تمام‌شده). */
    val burst = Animatable(1f)

    /** پیشرفت فروریزش حلقه‌ها هنگام خاموش شدن (۱ یعنی تمام‌شده). */
    val implode = Animatable(1f)

    /** آخرین وضعیت دیده‌شده؛ برای نادیده گرفتن اجرای نخست (بدون جشنِ ورود). */
    internal var lastActive: Boolean = initialActive
}

/**
 * ساخت و نگهداری وضعیت رادار برای یک کارت: روی هر تغییر [active]، انفجار
 * (روشن) یا فروریزش (خاموش) اجرا می‌شود. ساخت اولیهٔ کارت هیچ انیمیشنی
 * پخش نمی‌کند تا ورود صفحه تمیز بماند.
 */
@Composable
fun rememberRadarBurstState(active: Boolean): RadarBurstState {
    val state = remember { RadarBurstState(initialActive = active) }
    LaunchedEffect(active) {
        if (state.lastActive == active) return@LaunchedEffect
        state.lastActive = active
        if (active) {
            state.implode.snapTo(1f)
            state.burst.snapTo(0f)
            state.burst.animateTo(1f, tween(durationMillis = 750, easing = FastOutSlowInEasing))
        } else {
            state.burst.snapTo(1f)
            state.implode.snapTo(0f)
            state.implode.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing))
        }
    }
    return state
}

/** زاویه‌های ثابت ذرات انفجار (۱۲ ذره، هر ۳۰ درجه) — از پیش محاسبه‌شده. */
private val PARTICLE_ANGLES: List<Float> =
    List(12) { i -> (i * 30f) * (Math.PI.toFloat() / 180f) }

private fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)

/**
 * بوم موج‌های راداری.
 *
 * @param active وضعیت روشن/خاموش کارت (سونار و رنگ هسته از آن می‌آید).
 * @param waveColor رنگ اصلی حلقه‌ها و هسته در حالت فعال.
 * @param idleColor رنگ حلقه‌ها و هسته در حالت غیرفعال.
 * @param ringScale کسر شعاع بیشینه‌ای که حلقه‌ها می‌پوشانند (برای کارت
 *   محدوده با لگاریتم شعاع تنظیم می‌شود).
 * @param coreRadiusDp شعاع نقطهٔ مرکزی.
 * @param burstState وضعیت گذار؛ اگر داده نشود، داخلی ساخته می‌شود.
 */
@Composable
fun RadarWaves(
    active: Boolean,
    waveColor: Color,
    idleColor: Color,
    modifier: Modifier = Modifier,
    ringScale: Float = 1f,
    coreRadiusDp: Dp = 7.dp,
    burstState: RadarBurstState = rememberRadarBurstState(active)
) {
    // رنگ و اندازه هسته با گذار نرم (فنر ملایم) عوض می‌شود، هرگز برش سخت.
    val coreColor by animateColorAsState(
        targetValue = if (active) waveColor else idleColor,
        animationSpec = tween(450),
        label = "radarCoreColor"
    )
    val coreScale by animateFloatAsState(
        targetValue = if (active) 1f else 0.78f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "radarCoreScale"
    )
    val ringColorAnim by animateColorAsState(
        targetValue = if (active) waveColor else idleColor,
        animationSpec = tween(450),
        label = "radarRingColor"
    )

    // ساعت واحد سونار؛ فقط وقتی فعال است خوانده می‌شود تا در حالت خاموش
    // هیچ بازترکیبی رخ ندهد.
    val infinite = rememberInfiniteTransition(label = "radarSonar")
    val sonarProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonarProgress"
    )
    val burstProgress = burstState.burst.value
    val implodeProgress = burstState.implode.value

    Canvas(modifier = modifier) {
        val maxR = (size.minDimension / 2f) * ringScale.coerceIn(0.2f, 1f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val coreR = coreRadiusDp.toPx()

        if (active) {
            // هالهٔ نور نرم پشت هسته (تنفسی، هم‌فاز سونار)
            val glowPulse = 0.75f + 0.25f * sin(sonarProgress * 2f * Math.PI.toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.38f * glowPulse),
                        coreColor.copy(alpha = 0f)
                    ),
                    center = center,
                    radius = maxR * 0.95f
                ),
                radius = maxR * 0.95f,
                center = center
            )
            // حلقه‌های سونار: تولد روشن و ضخیم ← لبه محو و باریک
            for (i in 0 until 3) {
                val f = (sonarProgress + i / 3f) % 1f
                val eased = easeOutCubic(f)
                val radius = maxR * (0.16f + 0.84f * eased)
                val alpha = 0.55f * (1f - f).pow(1.6f)
                val width = (3.2f - 2.3f * f).dp.toPx().coerceAtLeast(0.7f)
                drawCircle(
                    color = ringColorAnim.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = width)
                )
            }
        } else {
            // حالت خاموش: سه حلقهٔ ایستای کم‌رنگ (همان زبان، بی‌حرکت)
            listOf(0.95f, 0.68f, 0.42f).forEachIndexed { idx, frac ->
                drawCircle(
                    color = ringColorAnim.copy(alpha = 0.30f - idx * 0.07f),
                    radius = maxR * frac,
                    center = center,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }

        // انفجار جشنِ روشن شدن: موج شوک + ذرات + فلش هسته
        if (burstProgress < 1f) {
            val p = burstProgress
            val eased = easeOutCubic(p)
            drawCircle(
                color = waveColor.copy(alpha = 0.75f * (1f - p)),
                radius = maxR * 1.05f * eased,
                center = center,
                style = Stroke(width = (1f + 3.5f * (1f - p)).dp.toPx())
            )
            PARTICLE_ANGLES.forEachIndexed { i, angle ->
                val dist = maxR * (0.22f + 0.88f * eased) * (0.85f + 0.15f * ((i % 3) / 2f))
                val px = center.x + cos(angle) * dist
                val py = center.y + sin(angle) * dist
                drawCircle(
                    color = waveColor.copy(alpha = 0.85f * (1f - p)),
                    radius = (0.8f + 2.2f * (1f - p)).dp.toPx(),
                    center = Offset(px, py)
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.65f * (1f - p)),
                radius = coreR * (1f + 1.1f * (1f - p)),
                center = center
            )
        }

        // فروریزش خاموش شدن: حلقه‌ها به هسته برمی‌گردند و محو می‌شوند
        if (implodeProgress < 1f) {
            val p = implodeProgress
            val shrink = 1f - easeOutCubic(p)
            for (i in 0 until 2) {
                val frac = 0.9f - i * 0.28f
                drawCircle(
                    color = idleColor.copy(alpha = 0.5f * (1f - p)),
                    radius = maxR * frac * shrink.coerceAtLeast(0.02f),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // هستهٔ مرکزی نورانی
        drawCircle(
            color = coreColor.copy(alpha = 0.35f),
            radius = coreR * 1.9f * coreScale,
            center = center
        )
        drawCircle(
            color = coreColor,
            radius = coreR * coreScale,
            center = center
        )
    }
}
