package ir.khanoomi.geofencealert.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** عمر هر موج لمسی به میلی‌ثانیه؛ بعد از آن موج مرده و حذف می‌شود. */
private const val RIPPLE_LIFE_MS = 950L

/** سقف موج‌های هم‌زمان روی صفحه؛ تازه‌ترین‌ها می‌مانند و قدیمی‌ترین می‌افتد. */
private const val MAX_RIPPLES = 14

/** یک موج قطره آب: مرکز لمس و لحظه تولدش روی ساعت نانو (هم‌مبنا با ساعت فریم). */
private class TouchRippleDrop(
    val center: Offset,
    val startNanos: Long
)

/**
 * میزبان موج لمسی سراسری (قطره در آب): روی تمام محتوای برنامه می‌نشیند
 * و «فقط نگاه می‌کند» — رویدادها را در گذر Initial می‌خواند و هرگز
 * هیچ‌کدام را مصرف (consume) نمی‌کند، پس دکمه‌ها، اسکرول، درگ کارت‌ها
 * و همه ژست‌ها دقیقاً مثل قبل کار می‌کنند.
 *
 * هر فشردن تازه (تک‌لمس یا چندلمس) یک موج در نقطه لمس می‌سازد که در
 * ~۹۵۰ms پخش می‌شود: فرورفتگی لحظه برخورد، حلقه اصلی گسترش‌یابنده،
 * حلقه پژواک، یک کمان نور روی لبه حلقه و سایه ملایمش — فقط با دایره
 * و کمان ساده Canvas و آلفا. بومِ موج‌ها آخرین فرزند است (روی محتوا)
 * اما هیچ ورودی لمسی ندارد و هرگز جلوی لمس را نمی‌گیرد. ساعت فریم
 * فقط وقتی دست‌کم یک موج زنده هست می‌چرخد و با مردن آخرین موج
 * می‌ایستد.
 */
@Composable
fun TouchRippleHost(content: @Composable () -> Unit) {
    val rippleColor = MaterialTheme.colorScheme.primary
    val drops = remember { mutableStateListOf<TouchRippleDrop>() }
    var frameNanos by remember { mutableLongStateOf(System.nanoTime()) }
    // هر تولد موج، دوره ساعت را یکی بالا می‌برد تا اگر ساعت خوابیده
    // بود (هیچ موجی زنده نبود) با موج تازه دوباره بیدار شود؛ اگر هم
    // بیدار بود، راه‌اندازی دوباره‌اش بی‌ضرر است.
    var clockEpoch by remember { mutableIntStateOf(0) }

    LaunchedEffect(clockEpoch) {
        while (drops.isNotEmpty()) {
            withFrameNanos { frameNanos = it }
            val now = frameNanos
            drops.removeAll { (now - it.startNanos) / 1_000_000L >= RIPPLE_LIFE_MS }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        for (change in event.changes) {
                            if (change.pressed && !change.previousPressed) {
                                drops.add(
                                    TouchRippleDrop(
                                        center = change.position,
                                        startNanos = System.nanoTime()
                                    )
                                )
                                while (drops.size > MAX_RIPPLES) {
                                    drops.removeAt(0)
                                }
                                clockEpoch++
                            }
                        }
                    }
                }
            }
    ) {
        content()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val now = frameNanos
            for (drop in drops) {
                val ageMs = (now - drop.startNanos) / 1_000_000f
                val t = (ageMs / RIPPLE_LIFE_MS.toFloat()).coerceIn(0f, 1f)
                drawWaterDrop(drop.center, t, rippleColor)
            }
        }
    }
}

/** مکعب وارونه نرم برای گسترش حلقه‌ها: شتاب اولیه زیاد و نشستن آرام دم لبه. */
private fun easeOutCubic(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u
}

/** درون‌یابی خطی ساده بین دو مقدار. */
private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * رسم یک موج قطره آب در سن [t] (۰ تا ۱) دور [center]:
 * اول سایه حلقه اصلی، بعد فرورفتگی برخورد (فقط آغاز)، حلقه اصلی،
 * حلقه پژواک (از ۱۵٪ عمر) و کمان نور سفید روی لبه بالا-چپ حلقه اصلی
 * (نشانه عمق و نور روی آب). همه فقط دایره/کمان با آلفا.
 */
private fun DrawScope.drawWaterDrop(center: Offset, t: Float, color: Color) {
    val maxMain = 64.dp.toPx()
    val fade = 1f - t
    val mainRadius = easeOutCubic(t) * maxMain
    val mainStroke = lerp(3.5f, 0.8f, t).dp.toPx()

    // سایه حلقه اصلی: کمی پایین و راست، پیش از خود حلقه کشیده می‌شود.
    if (mainRadius > 0.5f) {
        drawCircle(
            color = Color.Black.copy(alpha = fade * 0.10f),
            radius = mainRadius,
            center = center + Offset(1.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = mainStroke)
        )
    }

    // فرورفتگی لحظه برخورد: حلقه کوچکی که جمع می‌شود و پررنگ می‌شود.
    if (t < 0.18f) {
        val local = t / 0.18f
        drawCircle(
            color = color.copy(alpha = 0.5f * local),
            radius = lerp(10f, 4f, local).dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // حلقه اصلی.
    if (mainRadius > 0.5f) {
        drawCircle(
            color = color.copy(alpha = fade * 0.50f),
            radius = mainRadius,
            center = center,
            style = Stroke(width = mainStroke)
        )
        // کمان نور روی حلقه اصلی (بالا-چپ): کمی کلفت‌تر از خود حلقه.
        drawArc(
            color = Color.White.copy(alpha = fade * 0.45f),
            startAngle = 195f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - mainRadius, center.y - mainRadius),
            size = Size(mainRadius * 2f, mainRadius * 2f),
            style = Stroke(width = mainStroke + 1.dp.toPx())
        )
    }

    // حلقه پژواک: دیرتر شروع می‌شود و کوچک‌تر و نازک‌تر است.
    if (t > 0.15f) {
        val te = ((t - 0.15f) / 0.85f).coerceIn(0f, 1f)
        val echoRadius = easeOutCubic(te) * 44.dp.toPx()
        if (echoRadius > 0.5f) {
            drawCircle(
                color = color.copy(alpha = fade * 0.32f),
                radius = echoRadius,
                center = center,
                style = Stroke(width = lerp(2f, 0.6f, te).dp.toPx())
            )
        }
    }
}
