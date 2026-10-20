package ir.khanoomi.geofencealert.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * لرزاننده هشدار.
 *
 * الگوی تکرارشونده [0, 700, 400] میلی‌ثانیه با شدت قابل‌تنظیم (شدت صفر یعنی
 * بدون لرزش). برای هر محدوده یک ورودی جدا نگه می‌دارد تا توقف یک هشدار،
 * لرزش هشدار دیگر را قطع نکند؛ فقط وقتی هیچ محدوده‌ای لرزش نمی‌خواهد،
 * ویبراتور لغو می‌شود.
 */
object AlertVibrator {
    private const val TAG = "AlertVibrator"

    private val activeZones = ConcurrentHashMap.newKeySet<Long>()

    private fun vibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot get vibrator", e)
            null
        }
    }

    private fun amplitude01(intensity01: Float): Int =
        (intensity01.coerceIn(0f, 1f) * 255f).toInt().coerceIn(1, 255)

    /**
     * شروع لرزش تکرارشونده برای یک محدوده؛ [intensity01] شدت بین ۰ و ۱.
     * شدت صفر یعنی بدون لرزش.
     */
    fun start(context: Context, zoneId: Long, intensity01: Float) {
        if (intensity01 <= 0f) return
        activeZones.add(zoneId)
        try {
            val vib = vibrator(context) ?: return
            val amp = amplitude01(intensity01)
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 700, 400),
                intArrayOf(0, amp, 0),
                0
            )
            vib.vibrate(effect)
            Log.i(TAG, "Started vibration for zone $zoneId (intensity=$intensity01)")
        } catch (e: Exception) {
            Log.e(TAG, "vibrate failed for zone $zoneId", e)
        }
    }

    /** توقف لرزش یک محدوده؛ اگر محدوده لرزشی نماند، ویبراتور لغو می‌شود. */
    fun stop(context: Context, zoneId: Long) {
        activeZones.remove(zoneId)
        if (activeZones.isEmpty()) {
            try {
                vibrator(context)?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "cancel vibration failed", e)
            }
        }
    }

    /** توقف لرزش همه محدوده‌ها. */
    fun stopAll(context: Context) {
        activeZones.clear()
        try {
            vibrator(context)?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "cancel all vibrations failed", e)
        }
    }

    /**
     * یک لرزش تک‌نوبت (مثلاً برای هشدار نزدیک شدن یا یادآور ماندن).
     * [durationMs] مدت لرزش به میلی‌ثانیه.
     */
    fun pulse(context: Context, intensity01: Float, durationMs: Long = 600L) {
        if (intensity01 <= 0f) return
        try {
            val vib = vibrator(context) ?: return
            val effect = VibrationEffect.createOneShot(
                durationMs.coerceIn(100L, 5000L),
                amplitude01(intensity01)
            )
            vib.vibrate(effect)
        } catch (e: Exception) {
            Log.e(TAG, "pulse failed", e)
        }
    }
}
