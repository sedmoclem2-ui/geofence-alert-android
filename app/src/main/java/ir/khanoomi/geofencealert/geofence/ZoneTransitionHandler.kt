package ir.khanoomi.geofencealert.geofence

import android.util.Log
import android.content.Context
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.audio.AlertVibrator
import ir.khanoomi.geofencealert.data.EventLog
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.AlertDecider
import ir.khanoomi.geofencealert.domain.ScheduleChecker
import ir.khanoomi.geofencealert.notify.Notifications
import ir.khanoomi.geofencealert.receiver.StayAlarm
import ir.khanoomi.geofencealert.sms.SmsSender
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

/**
 * منطق مشترک ورود/خروج/نزدیک‌شدن به محدوده، مستقل از منبع رویداد
 * (ژئوفنس سیستمی یا پایش دقیق ۲۰ ثانیه‌ای).
 *
 * باید از داخل کانتکست IO (کوروتین) فراخوانی شود.
 */
object ZoneTransitionHandler {
    private const val TAG = "ZoneTransitionHandler"

    /**
     * پرچم «هشدار نزدیک شدنِ این سفر» برای هر محدوده: هشدار نزدیک شدن حداکثر
     * یک بار در هر سفر پخش می‌شود؛ در ورود و خروج تازه پاک می‌شود.
     */
    private val approachedTrips = ConcurrentHashMap.newKeySet<Long>()

    /**
     * منطق ورود: سفر تازه — تأیید دستی قبلی پاک می‌شود تا هشدار مثل اولین بار
     * پخش شود؛ سپس به‌ترتیب: ثبت رویداد، زمان‌بندی، پیامک، زمان‌بندی یادآور
     * ماندن، بازه اعلان بی‌صدا، ساعات سکوت عمومی، و شروع هشدار + اعلام صوتی.
     */
    suspend fun onEnter(context: Context, zone: Zone) {
        val app = context.applicationContext as GeofenceAlertApp
        val id = zone.id
        // محافظ «همان سفر»: ثبت دوباره ژئوفنس‌ها (مثلاً با هر ویرایش و ذخیره
        // محدوده، refreshRegistrations صدا زده می‌شود) با
        // INITIAL_TRIGGER_ENTER برای محدوده‌ای که گوشی هنوز داخلش است، یک
        // ENTER تازه می‌سازد. اگر کاربر هشدار این سفر را قبلاً دستی متوقف
        // کرده (تأییدشده) و هنوز داخل محدوده است (insideSince پاک نشده)،
        // این رویداد همان سفر قبلی است، نه ورود تازه: کاملاً نادیده گرفته
        // می‌شود تا هشداری که کاربر ساکت کرده دوباره شروع نشود. ورود واقعی
        // همیشه بعد از یک خروج است و خروج insideSince را صفر می‌کند، پس
        // ورود تازه هرگز زیر این محافظ نمی‌ماند.
        val freshRow = try {
            app.zoneRepo.getById(id)
        } catch (e: Exception) {
            Log.e(TAG, "getById failed for zone $id", e)
            null
        }
        if (freshRow != null && freshRow.alertAcknowledged && freshRow.insideSince > 0) {
            Log.i(TAG, "ENTER ignored for zone $id: same trip, already acknowledged by user")
            return
        }
        // سفر جدید: هشدارِ دستیِ متوقف‌شده قبلی فراموش می‌شود.
        try {
            app.zoneRepo.setAcknowledged(id, false)
        } catch (e: Exception) {
            Log.e(TAG, "setAcknowledged(false) failed for zone $id", e)
        }
        // ثبت رویداد در تاریخچه — حتی اگر هشدار به‌خاطر تکراری بودن شروع نشود.
        if (zone.isActive) {
            try {
                app.db.eventDao().insert(
                    EventLog(zoneId = id, zoneName = zone.name, type = EventLog.TYPE_ENTER)
                )
                app.db.eventDao().trim()
                app.zoneRepo.updateLastTriggered(id, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Event log failed for zone $id", e)
            }
        }
        // اقامت تازه: زمان ورود و پرچم یادآور ماندن ثبت می‌شود.
        val now = System.currentTimeMillis()
        try {
            app.zoneRepo.setInsideSince(id, now)
            app.zoneRepo.setStayReminded(id, false)
        } catch (e: Exception) {
            Log.e(TAG, "insideSince/stayReminded reset failed for zone $id", e)
        }
        approachedTrips.remove(id)

        if (!AlertDecider.shouldStartAlert(zone, zone.isAlerting)) {
            Log.i(
                TAG,
                "ENTER ignored for zone $id (active=${zone.isActive}, alerting=${zone.isAlerting})"
            )
            return
        }
        // زمان‌بندی محدوده: خارج از بازه فعال، هشدار شروع نمی‌شود
        // (رویداد ورود بالاتر در تاریخچه ثبت شده است).
        val inSchedule = ScheduleChecker.isInSchedule(
            zone.scheduleEnabled,
            zone.scheduleDays,
            zone.scheduleStartMin,
            zone.scheduleEndMin
        )
        if (!inSchedule) {
            Log.i(TAG, "ENTER ignored for zone $id: outside schedule")
            return
        }
        try {
            app.zoneRepo.setAlerting(id, true)
        } catch (e: Exception) {
            Log.e(TAG, "setAlerting failed", e)
        }
        val fresh = try {
            app.zoneRepo.getById(id)
        } catch (e: Exception) {
            null
        } ?: zone

        // پیامک اطلاع‌رسانی (اگر شماره و متن تنظیم شده باشد).
        if (fresh.smsNumbers.isNotBlank() && fresh.smsMessage.isNotBlank()) {
            SmsSender.send(context, fresh.smsNumbers, fresh.smsMessage)
        }

        // یادآور ماندن: زمان‌بندی آلارم برای پایان مدت تنظیم‌شده.
        if (fresh.stayReminderMin > 0) {
            StayAlarm.schedule(
                context,
                id,
                now + fresh.stayReminderMin.coerceIn(1, 10080) * 60_000L
            )
        }

        // بازه «اعلان بی‌صدا» این محدوده: فقط اعلان، بدون صدا/لرزش/گفتار.
        val inSilentWindow = ScheduleChecker.isQuietNow(
            enabled = fresh.silentEnabled,
            startMin = fresh.silentStartMin,
            endMin = fresh.silentEndMin,
            nowMillis = now
        )
        if (inSilentWindow) {
            Log.i(TAG, "ENTER for zone $id: inside per-zone silent window — notification only")
            Notifications.notifyAlert(context, id, fresh.name)
            return
        }

        val quiet = try {
            app.prefs.isQuietNow()
        } catch (e: Exception) {
            Log.e(TAG, "isQuietNow failed", e)
            false
        }
        // اعلام صوتی نام محدوده (TTS خود گوشی) حالا داخل خود AlertController
        // و «پیش از» صدای هشدار انجام می‌شود (دو بار خواندن، بعد صدا) تا
        // ترتیب پخش تضمین شود؛ اینجا دیگر جداگانه صدا زده نمی‌شود.
        AlertController.startAlert(context, fresh, silent = quiet)
    }

    /**
     * منطق خروج: ثبت رویداد در تاریخچه، پاک‌سازی وضعیت اقامت و یادآور ماندن،
     * سپس رفتارِ پس از خروجِ همان محدوده:
     * - اگر کاربر هشدارِ این سفر را دستی متوقف کرده بود (تأییدشده)، فقط صدا
     *   قطع می‌شود و هیچ آلارم پس از خروجی پخش نمی‌گردد.
     * - توقف فوری: قطع هشدار.
     * - الگوی تکراری: قطع صدای داخل محدوده و شروع الگوی پس از خروج.
     * - ادامه تا توقف دستی: هیچ کاری (هشدار حلقه‌ای ادامه می‌یابد).
     */
    suspend fun onExit(context: Context, zone: Zone) {
        val app = context.applicationContext as GeofenceAlertApp
        val id = zone.id
        if (zone.isActive) {
            try {
                app.db.eventDao().insert(
                    EventLog(zoneId = id, zoneName = zone.name, type = EventLog.TYPE_EXIT)
                )
                app.db.eventDao().trim()
            } catch (e: Exception) {
                Log.e(TAG, "Event log failed for zone $id", e)
            }
        }
        // پایان اقامت: زمان ورود و یادآور ماندن پاک می‌شود؛ آلارم لغو می‌گردد.
        try {
            app.zoneRepo.setInsideSince(id, 0L)
            app.zoneRepo.setStayReminded(id, false)
        } catch (e: Exception) {
            Log.e(TAG, "insideSince cleanup failed for zone $id", e)
        }
        StayAlarm.cancel(context, id)
        approachedTrips.remove(id)
        // وضعیت تازه از دیتابیس؛ ممکن است پس از بارگذاری شیء zone تغییر کرده باشد.
        val fresh = try {
            app.zoneRepo.getById(id)
        } catch (e: Exception) {
            Log.e(TAG, "getById failed for $id", e)
            null
        } ?: zone
        if (fresh.alertAcknowledged) {
            Log.i(TAG, "EXIT for zone $id: acknowledged by user, stopping silently")
            AlertController.stopAlert(context, id, manual = false)
            return
        }
        when (fresh.exitBehavior) {
            Zone.EXIT_SEQUENCE -> {
                Log.i(TAG, "EXIT for zone $id: starting post-exit sequence")
                AlertController.stopAlert(context, id, manual = false)
                AlertController.startPostExitSequence(context, fresh)
            }
            Zone.EXIT_CONTINUE -> {
                Log.i(TAG, "EXIT for zone $id: continuing until manual stop")
                // عمداً هیچ کاری نمی‌شود؛ هشدار حلقه‌ای ادامه می‌یابد.
            }
            else -> { // Zone.EXIT_STOP
                AlertController.stopAlert(context, id, manual = false)
            }
        }
    }

    /**
     * منطق «نزدیک شدن» به محدوده (ژئوفنس حلقه بیرونی): حداکثر یک بار در هر
     * سفر. یک اعلان اطلاع‌رسانی به‌همراه لرزش کوتاه (اگر نوع هشدار اجازه دهد
     * و در ساعات سکوت عمومی نباشیم) ارسال می‌کند؛ رویداد «APPROACH» هم در
     * تاریخچه ثبت می‌شود (در آمار ورود/خروج شمرده نمی‌شود).
     */
    suspend fun onApproach(context: Context, zone: Zone) {
        val app = context.applicationContext as GeofenceAlertApp
        val id = zone.id
        if (zone.alertAcknowledged) {
            Log.i(TAG, "APPROACH ignored for zone $id: acknowledged by user")
            return
        }
        if (!approachedTrips.add(id)) {
            Log.i(TAG, "APPROACH ignored for zone $id: already approached this trip")
            return
        }
        if (zone.isActive) {
            try {
                app.db.eventDao().insert(
                    EventLog(zoneId = id, zoneName = zone.name, type = EventLog.TYPE_APPROACH)
                )
                app.db.eventDao().trim()
            } catch (e: Exception) {
                Log.e(TAG, "Approach event log failed for zone $id", e)
            }
        }
        val quiet = try {
            app.prefs.isQuietNow()
        } catch (e: Exception) {
            Log.e(TAG, "isQuietNow failed", e)
            false
        }
        Notifications.approachNotification(context, id, zone.name, silent = quiet)
        if (!quiet && AlertController.vibrationAllowed(zone.alertMode)) {
            val intensity = try {
                app.prefs.vibrationIntensity.first() / 100f
            } catch (e: Exception) {
                0.7f
            }
            AlertVibrator.pulse(context, intensity)
        }
    }
}
