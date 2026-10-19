package ir.khanoomi.geofencealert.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import ir.khanoomi.geofencealert.MainActivity
import ir.khanoomi.geofencealert.R
import ir.khanoomi.geofencealert.domain.TimeFormat
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

    private const val STATUS_NOTIFICATION_ID = 9002

    /** شناسه یکتای اعلان هر محدوده. */
    fun alertNotificationId(zoneId: Long): Int = (1000 + (zoneId % 100000)).toInt()

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
        nm.createNotificationChannel(alert)
        nm.createNotificationChannel(status)
        nm.createNotificationChannel(proximity)
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
     * اعلان دائمی سرویس پیش‌زمینه پایش دقیق محدوده‌های کوچک.
     * اگر [zoneName] داده شود، نمای سفارشی با نام محدوده و ثانیه‌شمار
     * دایره‌ای قرمزِ شمارش معکوس تا بررسی بعدی نمایش داده می‌شود؛
     * در غیر این صورت نمای ساده بدون ثانیه‌شمار.
     */
    fun proximityNotification(
        context: Context,
        zoneName: String? = null,
        countdownSec: Int? = null
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val views = RemoteViews(context.packageName, R.layout.notification_proximity)
        if (zoneName != null) {
            views.setTextViewText(R.id.tv_prox_zone, zoneName)
            views.setViewVisibility(R.id.tv_prox_countdown, View.VISIBLE)
            views.setTextViewText(
                R.id.tv_prox_countdown,
                TimeFormat.toFaDigits((countdownSec ?: 0).toString())
            )
        } else {
            views.setTextViewText(R.id.tv_prox_zone, "نزدیک شدن به محدوده‌های کوچک")
            views.setViewVisibility(R.id.tv_prox_countdown, View.GONE)
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
