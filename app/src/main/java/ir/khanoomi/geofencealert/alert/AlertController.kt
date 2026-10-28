package ir.khanoomi.geofencealert.alert

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.audio.AlertPlayer
import ir.khanoomi.geofencealert.audio.AlertVibrator
import ir.khanoomi.geofencealert.audio.VoiceAnnouncer
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.notify.Notifications
import ir.khanoomi.geofencealert.service.AlertService
import ir.khanoomi.geofencealert.widget.SingleZoneWidgetProvider
import ir.khanoomi.geofencealert.widget.ZoneWidgetProvider
import ir.khanoomi.geofencealert.widget.RadarWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * نقطه واحد شروع/توقف هشدارها.
 *
 * مسیر اصلی: [AlertService] (سرویس پیش‌زمینه).
 * مسیر جایگزین: اگر شروع سرویس پیش‌زمینه ممکن نباشد (مثلاً محدودیت شروع
 * سرویس در پس‌زمینه در اندروید ۱۲ به بالا یا نبود مجوز لازم)، مستقیم
 * [AlertPlayer] اجرا و اعلان هشدار نمایش داده می‌شود.
 *
 * توقف دستی هشدار، وضعیت «فعال بودن» محدوده را تغییر نمی‌دهد؛ این دو
 * مفهوم مستقل‌اند.
 */
object AlertController {
    private const val TAG = "AlertController"

    /** مهلت سخت انتظار برای پایان اعلام صوتی پیش از شروع صدای هشدار. */
    private const val VOICE_TIMEOUT_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** جاب‌های الگوی تکراری پس از خروج، یکی برای هر محدوده. */
    private val sequenceJobs = ConcurrentHashMap<Long, Job>()

    /** آیا نوع هشدار محدوده، پخش صدا را اجازه می‌دهد. */
    fun soundAllowed(mode: Int): Boolean =
        mode == Zone.ALERT_BOTH || mode == Zone.ALERT_SOUND

    /** آیا نوع هشدار محدوده، لرزش را اجازه می‌دهد. */
    fun vibrationAllowed(mode: Int): Boolean =
        mode == Zone.ALERT_BOTH || mode == Zone.ALERT_VIBRATE

    /**
     * آخرین بلندی صدا / شدت لرزش خوانده‌شده از تنظیمات (۰ تا ۱).
     *
     * [volume01] و [vibration01] هم‌زمان (غیرتعلیقی) صدا زده می‌شوند، پس
     * قبلاً با runBlocking مقدار را از DataStore می‌خواندند؛ runBlocking
     * روی نخ فراخواننده خطر گیر کردن کل برنامه را دارد. حالا دو گردآورنده
     * دائمی روی نخ IO این کش‌ها را تازه نگه می‌دارند و خواندن‌ها فوری و
     * بدون مسدودسازی‌اند. پیش‌فرض‌ها با پیش‌فرض تنظیمات یکی‌اند (۰٫۸ و ۰٫۷)
     * تا پیش از نخستین خواندن هم مقدار معقولی برگردد.
     */
    @Volatile
    private var cachedVolume01 = 0.8f

    @Volatile
    private var cachedVibration01 = 0.7f

    @Volatile
    private var settingsObserverStarted = false

    /** راه‌اندازی یک‌باره گردآورنده‌های تنظیمات صدا/لرزش (نخ IO). */
    private fun ensureSettingsObserver(context: Context) {
        if (settingsObserverStarted) return
        synchronized(this) {
            if (settingsObserverStarted) return
            settingsObserverStarted = true
        }
        val app = context.applicationContext as GeofenceAlertApp
        scope.launch {
            try {
                app.prefs.alertVolume.collect { cachedVolume01 = it / 100f }
            } catch (e: Exception) {
                Log.w(TAG, "Volume observer stopped", e)
            }
        }
        scope.launch {
            try {
                app.prefs.vibrationIntensity.collect { cachedVibration01 = it / 100f }
            } catch (e: Exception) {
                Log.w(TAG, "Vibration observer stopped", e)
            }
        }
    }

    /** بلندی صدای تنظیم‌شده در تنظیمات (۰ تا ۱)؛ از کش، بدون مسدودسازی. */
    fun volume01(context: Context): Float {
        ensureSettingsObserver(context)
        return cachedVolume01
    }

    /** شدت لرزش تنظیم‌شده در تنظیمات (۰ تا ۱)؛ از کش، بدون مسدودسازی. */
    fun vibration01(context: Context): Float {
        ensureSettingsObserver(context)
        return cachedVibration01
    }

    /**
     * شروع هشدار برای یک محدوده (پس از رویداد ورود معتبر).
     * اگر [silent] درست باشد (مثلاً در ساعات سکوت)، فقط اعلان نمایش داده
     * می‌شود و صدایی پخش نمی‌گردد؛ دکمه توقف همچنان در اعلان هست.
     *
     * ترتیب پخش از نسخه ۲.۳.۰: اگر اعلام صوتی نام فعال باشد و نوع هشدار
     * صدا را اجازه دهد، «اول» نام محدوده دو بار با صدای گوشی خوانده می‌شود
     * و «بعد» صدای هشدار (و لرزش) شروع می‌شود — مدیریت این ترتیب همین‌جا
     * در [startAlertAfterVoice] است، نه در فراخواننده.
     */
    fun startAlert(context: Context, zone: Zone, silent: Boolean = false) {
        if (AlertPlayer.isPlaying(zone.id)) {
            Log.i(TAG, "Alert already playing for zone ${zone.id}; not starting another")
            return
        }
        refreshWidget(context)
        if (silent) {
            Log.i(TAG, "Silent alert (quiet hours) for zone ${zone.id}")
            Notifications.notifyAlert(context, zone.id, zone.name)
            return
        }
        if (zone.voiceAnnounce && soundAllowed(zone.alertMode)) {
            startAlertAfterVoice(context, zone)
        } else {
            startAlertSound(context, zone)
        }
    }

    /**
     * شناسه محدوده‌هایی که شروع صدایشان در انتظار پایان اعلام صوتی است.
     * توقف هشدار در این فاصله، شناسه را حذف می‌کند تا صدا بعد از گفتار
     * هم شروع نشود.
     */
    private val pendingVoiceStarts = ConcurrentHashMap.newKeySet<Long>()

    /**
     * اول اعلام صوتی (دو بار خواندن نام به زبان فعلی برنامه)، بعد صدا.
     * شروع صدا دقیقاً یک بار انجام می‌شود: هرکدام زودتر برسد — پایان گفتار
     * یا مهلت سخت ~۵ ثانیه — و دومی بی‌اثر است. خطای گفتار هم صدا را
     * معطل نمی‌کند.
     */
    private fun startAlertAfterVoice(context: Context, zone: Zone) {
        val appContext = context.applicationContext
        pendingVoiceStarts.add(zone.id)
        val startOnce: () -> Unit = {
            // فقط اگر در این فاصله کاربر هشدار را متوقف نکرده باشد.
            if (pendingVoiceStarts.remove(zone.id)) {
                startAlertSound(appContext, zone)
            }
        }
        scope.launch {
            val lang = try {
                (appContext as GeofenceAlertApp).prefs.appLanguage.first()
            } catch (e: Exception) {
                Log.e(TAG, "Cannot read app language; defaulting to fa", e)
                "fa"
            }
            try {
                VoiceAnnouncer.announceNameTwice(appContext, zone.name, lang) {
                    startOnce()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Voice announce failed; starting sound directly", e)
                startOnce()
            }
            // مهلت سخت: صدا هیچ‌وقت بیش از ~۵ ثانیه منتظر گفتار نمی‌ماند.
            launch {
                delay(VOICE_TIMEOUT_MS)
                startOnce()
            }
        }
    }

    /** شروع واقعی صدای هشدار: سرویس پیش‌زمینه یا مسیر جایگزین پخش مستقیم. */
    private fun startAlertSound(context: Context, zone: Zone) {
        val intent = Intent(context, AlertService::class.java)
            .setAction(AlertService.ACTION_START)
            .putExtra(AlertService.EXTRA_ZONE_ID, zone.id)
            .putExtra(AlertService.EXTRA_ZONE_NAME, zone.name)
            .putExtra(AlertService.EXTRA_SOUND_KEY, zone.soundKey)
            .putExtra(AlertService.EXTRA_SOUND_URI, zone.customSoundUri)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.w(TAG, "Foreground service start failed; using fallback playback", e)
            // مسیر جایگزین هم نوع هشدار (صدا/لرزش) و بلندی صدا را رعایت می‌کند.
            if (soundAllowed(zone.alertMode)) {
                AlertPlayer.play(
                    context, zone.id, zone.soundKey, zone.customSoundUri, volume01(context)
                )
            }
            if (vibrationAllowed(zone.alertMode)) {
                AlertVibrator.start(context, zone.id, vibration01(context))
            }
            Notifications.notifyAlert(context, zone.id, zone.name)
        }
    }

    /**
     * توقف هشدار یک محدوده؛ محدوده فعال می‌ماند و برای ورود بعدی آماده است.
     * اگر [manual] درست باشد (توقف توسط کاربر)، هشدارِ «این سفر» تأییدشده
     * ثبت می‌شود تا تا ورود تازه بعدی تکرار نشود؛ همچنین هر الگوی تکراری
     * در حال اجرای همان محدوده لغو می‌گردد و آلارم‌های بعدی به صدا درنمی‌آید.
     */
    fun stopAlert(context: Context, zoneId: Long, manual: Boolean) {
        // اگر شروع صدا هنوز در انتظار پایان اعلام صوتی است، لغوش کن.
        pendingVoiceStarts.remove(zoneId)
        // لغو الگوی تکراری پس از خروج (اگر در حال اجراست)
        sequenceJobs.remove(zoneId)?.cancel()
        AlertPlayer.stop(zoneId)
        AlertVibrator.stop(context, zoneId)
        Notifications.cancelAlert(context, zoneId)
        refreshWidget(context)
        val app = context.applicationContext as GeofenceAlertApp
        scope.launch {
            try {
                if (manual) {
                    app.zoneRepo.setAcknowledged(zoneId, true)
                }
                app.zoneRepo.setAlerting(zoneId, false)
            } catch (e: Exception) {
                Log.e(TAG, "setAlerting(false) failed for $zoneId", e)
            }
        }
        if (AlertService.isRunning) {
            try {
                val intent = Intent(context, AlertService::class.java)
                    .setAction(AlertService.ACTION_STOP)
                    .putExtra(AlertService.EXTRA_ZONE_ID, zoneId)
                // شروع عادی کافی است: سرویس از قبل در حالت پیش‌زمینه است و
                // فقط فرمان توقف می‌گیرد. استفاده از startForegroundService
                // برای این فرمان اشتباه است، چون مسیر توقف در سرویس هرگز
                // startForeground صدا نمی‌زند و سیستم می‌تواند برنامه را با
                // ForegroundServiceDidNotStartInTimeException بکشد.
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not deliver STOP to service", e)
            }
        }
    }

    /** توقف همه هشدارهای فعال (دکمه ویجت). */
    fun stopAllAlerts(context: Context, manual: Boolean) {
        // لغو شروع‌های در انتظار اعلام صوتی
        pendingVoiceStarts.clear()
        // لغو همه الگوهای تکراری در حال اجرا
        sequenceJobs.keys.toList().forEach { sequenceJobs.remove(it)?.cancel() }
        AlertPlayer.stopAll()
        AlertVibrator.stopAll(context)
        val app = context.applicationContext as GeofenceAlertApp
        scope.launch {
            try {
                app.zoneRepo.getActiveZones()
                    .filter { it.isAlerting }
                    .forEach {
                        Notifications.cancelAlert(context, it.id)
                        if (manual) {
                            app.zoneRepo.setAcknowledged(it.id, true)
                        }
                        app.zoneRepo.setAlerting(it.id, false)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "stopAllAlerts failed", e)
            }
        }
        refreshWidget(context)
    }

    /**
     * شروع «الگوی تکراری پس از خروج» برای یک محدوده:
     * [Zone.postExitCount] بار، هر بار [Zone.postExitDurationSec] ثانیه پخش
     * حلقه‌ای و سپس [Zone.postExitGapSec] ثانیه مکث. توقف دستی در هر لحظه،
     * ادامه الگو را لغو می‌کند ([stopAlert] با manual=true).
     */
    fun startPostExitSequence(context: Context, zone: Zone) {
        sequenceJobs.remove(zone.id)?.cancel()
        val appContext = context.applicationContext
        sequenceJobs[zone.id] = scope.launch {
            try {
                val app = appContext as GeofenceAlertApp
                val count = zone.postExitCount.coerceIn(1, 20)
                val durationMs = zone.postExitDurationSec.coerceIn(3, 120) * 1000L
                val gapMs = zone.postExitGapSec.coerceIn(0, 600) * 1000L
                try {
                    app.zoneRepo.setAlerting(zone.id, true)
                } catch (e: Exception) {
                    Log.e(TAG, "setAlerting(true) failed for ${zone.id}", e)
                }
                refreshWidget(appContext)
                Notifications.notifyAlert(appContext, zone.id, zone.name)
                // نوع هشدار و شدت‌ها در الگوی تکراری هم رعایت می‌شوند:
                // صدا اگر مجاز باشد، لرزش به‌اندازه مدت هر نوبت.
                val volume = volume01(appContext)
                val vibIntensity = vibration01(appContext)
                val soundOk = soundAllowed(zone.alertMode)
                val vibOk = vibrationAllowed(zone.alertMode)
                repeat(count) { i ->
                    ensureActive()
                    if (soundOk) {
                        AlertPlayer.play(
                            appContext, zone.id, zone.soundKey, zone.customSoundUri, volume
                        )
                    }
                    if (vibOk) {
                        AlertVibrator.start(appContext, zone.id, vibIntensity)
                    }
                    delay(durationMs)
                    AlertPlayer.stop(zone.id)
                    AlertVibrator.stop(appContext, zone.id)
                    if (i < count - 1) {
                        ensureActive()
                        delay(gapMs)
                    }
                }
                Log.i(TAG, "Post-exit sequence completed for zone ${zone.id}")
            } catch (e: CancellationException) {
                Log.i(TAG, "Post-exit sequence cancelled for zone ${zone.id}")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Post-exit sequence failed for zone ${zone.id}", e)
            } finally {
                try {
                    AlertPlayer.stop(zone.id)
                    AlertVibrator.stop(appContext, zone.id)
                    Notifications.cancelAlert(appContext, zone.id)
                    val app = appContext as GeofenceAlertApp
                    app.zoneRepo.setAlerting(zone.id, false)
                } catch (_: Exception) {
                }
                sequenceJobs.remove(zone.id)
                refreshWidget(appContext)
            }
        }
    }

    /** به‌روزرسانی ویجت‌ها؛ خطا نادیده گرفته می‌شود تا هشدار مختل نشود. */
    private fun refreshWidget(context: Context) {
        try {
            ZoneWidgetProvider.updateAll(context)
            RadarWidgetProvider.updateAll(context)
        } catch (e: Exception) {
            Log.w(TAG, "Widget refresh failed", e)
        }
        try {
            SingleZoneWidgetProvider.updateAll(context)
            RadarWidgetProvider.updateAll(context)
        } catch (e: Exception) {
            Log.w(TAG, "Single-zone widget refresh failed", e)
        }
    }
}
