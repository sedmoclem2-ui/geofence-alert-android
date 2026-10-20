package ir.khanoomi.geofencealert.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import ir.khanoomi.geofencealert.MainActivity
import ir.khanoomi.geofencealert.R
import ir.khanoomi.geofencealert.domain.TimeFormat
import ir.khanoomi.geofencealert.receiver.GpsActionReceiver
import ir.khanoomi.geofencealert.receiver.StopAlertReceiver
import ir.khanoomi.geofencealert.service.AlertService
import ir.khanoomi.geofencealert.util.Permissions

/**
 * ساخت کانال‌ها و اعلان‌های هشدار.
 *
 * اعلان هشدار «در حال انجام» (ongoing) است و دکمه «توقف هشدار» دارد تا کاربر
 * حتی با بسته بودن برنامه بتواند صدا را قطع کند.
 */
object Notifications {
    const val ALERT_CHANNEL_ID = "alert_channel"
    const val STATUS_CHANNEL_ID = "status_channel"
    const val PROXIMITY_CHANNEL_ID = "proximity_channel"
    const val GPS_CHANNEL_ID = "gps_channel"

    private const val STATUS_NOTIFICATION_ID = 9002

    /** شناسه ثابت اعلان «جی‌پی‌اس خاموش است». */
    const val GPS_NOTIFICATION_ID = 9004

    /** شناسه یکتای اعلان هر محدوده. */
    fun alertNotificationId(zoneId: Long): Int = (1000 + (zoneId % 100000)).toInt()

    /** شناسه اعلان نزدیک شدن هر محدوده. */
    fun approachNotificationId(zoneId: Long): Int = (3000 + (zoneId % 100000)).toInt()

    /** شناسه اعلان یادآور ماندن هر محدوده. */
    fun stayNotificationId(zoneId: Long): Int = (5000 + (zoneId % 100000)).toInt()

    private fun manager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = manager(context)
        val alert = NotificationChannel(
            ALERT_CHANNEL_ID,
            context.getString(R.string.channel_alerts),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            description = "اعلان هشدار ورود به محدوده"
        }
        val status = NotificationChannel(
            STATUS_CHANNEL_ID,
            context.getString(R.string.channel_status),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "وضعیت کلی پایش محدوده‌ها"
        }
        val proximity = NotificationChannel(
            PROXIMITY_CHANNEL_ID,
            "پایش دقیق محدوده",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "پایش دقیق محدوده‌های کوچک هنگام نزدیک شدن"
        }
        val gps = NotificationChannel(
            GPS_CHANNEL_ID,
            "هشدار مکان",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "هشدار خاموش بودن مکان‌یابی گوشی هنگام فعال بودن پایش"
        }
        nm.createNotificationChannel(alert)
        nm.createNotificationChannel(status)
        nm.createNotificationChannel(proximity)
        nm.createNotificationChannel(gps)
    }

    /** اعلان هشدار یک محدوده (برای سرویس پیش‌زمینه و مسیر جایگزین). */
    fun alertNotification(context: Context, zoneId: Long, zoneName: String): Notification {
        val stopIntent = PendingIntent.getBroadcast(
            context,
            alertNotificationId(zoneId),
            Intent(context, StopAlertReceiver::class.java)
                .putExtra(AlertService.EXTRA_ZONE_ID, zoneId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("هشدار ورود به محدوده")
            .setContentText("$zoneName — برای توقف هشدار، دکمه زیر را بزنید")
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "توقف هشدار",
                stopIntent
            )
            .build()
    }

    /** نمایش اعلان هشدار (مسیر جایگزین وقتی سرویس پیش‌زمینه ممکن نیست). */
    fun notifyAlert(context: Context, zoneId: Long, zoneName: String) {
        if (!Permissions.hasNotifications(context)) return
        manager(context).notify(
            alertNotificationId(zoneId),
            alertNotification(context, zoneId, zoneName)
        )
    }

    fun cancelAlert(context: Context, zoneId: Long) {
        manager(context).cancel(alertNotificationId(zoneId))
    }

    /**
     * اعلان «نزدیک شدن به محدوده»؛ در ساعات سکوت عمومی، بی‌صدا نمایش داده
     * می‌شود ([silent]). این اعلان اطلاعات‌محور است و دکمه توقف ندارد.
     */
    fun approachNotification(context: Context, zoneId: Long, zoneName: String, silent: Boolean) {
        if (!Permissions.hasNotifications(context)) return
        val contentIntent = PendingIntent.getActivity(
            context,
            approachNotificationId(zoneId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("نزدیک شدن به محدوده")
            .setContentText(zoneName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
        if (silent) {
            builder.setSilent(true)
        }
        manager(context).notify(approachNotificationId(zoneId), builder.build())
    }

    /**
     * اعلان «یادآور ماندن» برای اقامت طولانی داخل یک محدوده.
     * [minutes] مدت ماندن تنظیم‌شده توسط کاربر (به دقیقه) است.
     */
    fun stayReminderNotification(context: Context, zoneId: Long, zoneName: String, minutes: Int) {
        if (!Permissions.hasNotifications(context)) return
        val contentIntent = PendingIntent.getActivity(
            context,
            stayNotificationId(zoneId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("یادآور ماندن")
            .setContentText(
                "بیش از ${TimeFormat.toFaDigits(minutes.toString())} دقیقه است که در محدودهٔ $zoneName هستید."
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .build()
        manager(context).notify(stayNotificationId(zoneId), notification)
    }

    /** اعلان کم‌اهمیت وضعیت پایش (اختیاری). */
    fun notifyStatus(context: Context, text: String) {
        if (!Permissions.hasNotifications(context)) return
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("پایش محدوده‌ها")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()
        manager(context).notify(STATUS_NOTIFICATION_ID, notification)
    }

    fun cancelStatus(context: Context) {
        manager(context).cancel(STATUS_NOTIFICATION_ID)
    }

    /**
     * اعلان «جی‌پی‌اس خاموش است» — وقتی پایش فعال است اما مکان‌یابی گوشی
     * خاموش شده و هشدارها عملاً کار نمی‌کنند.
     *
     * دو کنش دارد:
     * - «فعال کردن جی پی اس»: صفحه تنظیمات مکان سیستم را باز می‌کند (برنامه‌ها
     *   نمی‌توانند مستقیم GPS را روشن کنند؛ کاربر آنجا با یک لمس فعالش می‌کند).
     * - «غیر فعال کردن پایش»: پایش را از طریق [GpsActionReceiver] خاموش می‌کند.
     */
    fun gpsOffNotification(context: Context): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            GPS_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val enableGpsIntent = PendingIntent.getActivity(
            context,
            GPS_NOTIFICATION_ID + 1,
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disableIntent = PendingIntent.getBroadcast(
            context,
            GPS_NOTIFICATION_ID + 2,
            Intent(context, GpsActionReceiver::class.java)
                .setAction(GpsActionReceiver.ACTION_DISABLE_MONITORING),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = "پایش فعال است اما مکان‌یابی گوشی خاموش است؛ هشدارها کار نمی‌کنند."
        return NotificationCompat.Builder(context, GPS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("جی‌پی‌اس خاموش است")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_mylocation,
                "فعال کردن جی پی اس",
                enableGpsIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "غیر فعال کردن پایش",
                disableIntent
            )
            .build()
    }

    /** نمایش اعلان «جی‌پی‌اس خاموش است» (اگر مجوز اعلان داده شده باشد). */
    fun notifyGpsOff(context: Context) {
        if (!Permissions.hasNotifications(context)) return
        manager(context).notify(GPS_NOTIFICATION_ID, gpsOffNotification(context))
    }

    /** حذف اعلان «جی‌پی‌اس خاموش است» (روشن شدن مکان یا خاموش شدن پایش). */
    fun cancelGpsOff(context: Context) {
        manager(context).cancel(GPS_NOTIFICATION_ID)
    }

    /**
     * اعلان دائمی سرویس پیش‌زمینه پایش محدوده‌ها.
     * - اگر [zoneName] داده شود، نمای سفارشی با نام محدوده و ثانیه‌شمار
     *   دایره‌ای قرمزِ شمارش معکوس تا بررسی بعدی نمایش داده می‌شود.
     * - اگر [monitoringIdle] درست باشد (پایش روشن است اما هیچ محدوده کوچکی
     *   در نزدیکی نیست)، عنوان «پایش فعال است» و متن «در حال پایش
     *   محدوده‌ها» بدون ثانیه‌شمار نمایش داده می‌شود.
     * - در غیر این صورت نمای ساده بدون ثانیه‌شمار.
     */
    fun proximityNotification(
        context: Context,
        zoneName: String? = null,
        countdownSec: Int? = null,
        monitoringIdle: Boolean = false
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val views = RemoteViews(context.packageName, R.layout.notification_proximity)
        when {
            monitoringIdle -> {
                views.setTextViewText(R.id.tv_prox_title, "پایش فعال است")
                views.setTextViewText(R.id.tv_prox_zone, "در حال پایش محدوده‌ها")
                views.setViewVisibility(R.id.tv_prox_countdown, View.GONE)
            }
            zoneName != null -> {
                views.setTextViewText(R.id.tv_prox_title, "پایش دقیق فعال است")
                views.setTextViewText(R.id.tv_prox_zone, zoneName)
                views.setViewVisibility(R.id.tv_prox_countdown, View.VISIBLE)
                views.setTextViewText(
                    R.id.tv_prox_countdown,
                    TimeFormat.toFaDigits((countdownSec ?: 0).toString())
                )
            }
            else -> {
                views.setTextViewText(R.id.tv_prox_title, "پایش دقیق فعال است")
                views.setTextViewText(R.id.tv_prox_zone, "نزدیک شدن به محدوده‌های کوچک")
                views.setViewVisibility(R.id.tv_prox_countdown, View.GONE)
            }
        }
        return NotificationCompat.Builder(context, PROXIMITY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCustomContentView(views)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()
    }
}
