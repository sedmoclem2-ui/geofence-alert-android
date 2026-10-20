package ir.khanoomi.geofencealert.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.GuardedBy
import android.media.RingtoneManager

/**
 * پخش‌کننده هشدار صوتی.
 *
 * - برای هر محدوده حداکثر یک MediaPlayer هم‌زمان (جلوگیری از پخش هم‌زمان چندگانه).
 * - پخش به‌صورت حلقه (loop) تا زمان توقف صریح.
 * - پیش‌نمایش صدا با پخش‌کننده جداگانه و توقف خودکار پس از ۳٫۵ ثانیه.
 */
object AlertPlayer {
    private const val TAG = "AlertPlayer"
    private const val PREVIEW_ID = -1L
    private const val PREVIEW_DURATION_MS = 3500L

    @GuardedBy("this")
    private val players = mutableMapOf<Long, MediaPlayer>()

    @GuardedBy("this")
    private var previewPlayer: MediaPlayer? = null
    private val previewHandler = Handler(Looper.getMainLooper())
    private val stopPreviewRunnable = Runnable { stopPreviewInternal() }

    /** URI صدای سیستمی متناظر با کلید انتخاب‌شده توسط کاربر. */
    fun soundUri(context: Context, key: String): Uri {
        val type = when (key) {
            "alarm" -> RingtoneManager.TYPE_ALARM
            "ringtone" -> RingtoneManager.TYPE_RINGTONE
            else -> RingtoneManager.TYPE_NOTIFICATION
        }
        return RingtoneManager.getDefaultUri(type)
            ?: Settings.System.DEFAULT_NOTIFICATION_URI
    }

    /**
     * شروع پخش حلقه‌ای هشدار برای یک محدوده.
     * اگر همین حالا در حال پخش باشد، هیچ پخش‌کننده جدیدی ساخته نمی‌شود.
     * اگر [customUri] داده شود، اول همان امتحان می‌شود و در صورت خطا،
     * صدای سیستمی [soundKey] پخش می‌گردد.
     * [volume01]: بلندی صدا بین ۰ و ۱ (پیش‌فرض: کامل).
     */
    @Synchronized
    fun play(
        context: Context,
        zoneId: Long,
        soundKey: String,
        customUri: String? = null,
        volume01: Float = 1f
    ) {
        // B2: پیش‌نمایش صدا هرگز نباید با هشدار واقعی هم‌پوشانی داشته باشد.
        stopPreviewInternal()
        if (players.containsKey(zoneId)) {
            Log.i(TAG, "Already playing for zone $zoneId; ignoring duplicate")
            return
        }
        val appContext = context.applicationContext
        val candidates = mutableListOf<Uri>()
        customUri?.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?.let { candidates += it }
        candidates += soundUri(appContext, soundKey)
        var lastError: Exception? = null
        val volume = volume01.coerceIn(0f, 1f)
        for (uri in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setDataSource(appContext, uri)
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                mp.isLooping = true
                mp.prepare()
                mp.setVolume(volume, volume)
                mp.start()
                players[zoneId] = mp
                Log.i(TAG, "Started alert loop for zone $zoneId (volume=$volume)")
                return
            } catch (e: Exception) {
                lastError = e
                try {
                    mp.release()
                } catch (_: Exception) {
                }
            }
        }
        Log.e(TAG, "Failed to play alert for zone $zoneId", lastError)
        players.remove(zoneId)
    }

    /** توقف و آزادسازی پخش‌کننده یک محدوده. */
    @Synchronized
    fun stop(zoneId: Long) {
        val mp = players.remove(zoneId) ?: return
        try {
            if (mp.isPlaying) mp.stop()
        } catch (_: Exception) {
        }
        try {
            mp.release()
        } catch (_: Exception) {
        }
        Log.i(TAG, "Stopped alert for zone $zoneId")
    }

    /** توقف همه هشدارها. */
    @Synchronized
    fun stopAll() {
        val ids = players.keys.toList()
        ids.forEach { stop(it) }
        stopPreviewInternal()
    }

    @Synchronized
    fun isPlaying(zoneId: Long): Boolean =
        players[zoneId]?.let {
            try {
                it.isPlaying
            } catch (_: Exception) {
                false
            }
        } ?: false

    /**
     * پیش‌نمایش صدا (غیر حلقه‌ای)؛ پیش‌نمایش قبلی متوقف و پس از ۳٫۵ ثانیه
     * به‌طور خودکار قطع می‌شود. اگر [customUri] داده شود، همان پیش‌نمایش
     * می‌شود وگرنه صدای سیستمی [soundKey].
     */
    @Synchronized
    fun preview(context: Context, soundKey: String, customUri: String? = null) {
        stopPreviewInternal()
        val appContext = context.applicationContext
        val candidates = mutableListOf<Uri>()
        customUri?.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?.let { candidates += it }
        candidates += soundUri(appContext, soundKey)
        var ok = false
        for (uri in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setDataSource(appContext, uri)
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.isLooping = false
                mp.prepare()
                mp.start()
                previewPlayer = mp
                previewHandler.postDelayed(stopPreviewRunnable, PREVIEW_DURATION_MS)
                mp.setOnCompletionListener { stopPreviewInternal() }
                ok = true
                break
            } catch (e: Exception) {
                Log.e(TAG, "Preview failed for $uri", e)
                try {
                    mp.release()
                } catch (_: Exception) {
                }
            }
        }
        if (!ok) previewPlayer = null
    }

    @Synchronized
    private fun stopPreviewInternal() {
        previewHandler.removeCallbacks(stopPreviewRunnable)
        val mp = previewPlayer ?: return
        previewPlayer = null
        try {
            if (mp.isPlaying) mp.stop()
        } catch (_: Exception) {
        }
        try {
            mp.release()
        } catch (_: Exception) {
        }
    }
}
