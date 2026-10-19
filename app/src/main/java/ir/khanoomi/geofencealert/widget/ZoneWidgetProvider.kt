package ir.khanoomi.geofencealert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.MainActivity
import ir.khanoomi.geofencealert.R
import ir.khanoomi.geofencealert.domain.TimeFormat
import ir.khanoomi.geofencealert.receiver.StopAlertReceiver
import ir.khanoomi.geofencealert.receiver.WidgetActionReceiver
import ir.khanoomi.geofencealert.service.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ویجت صفحه اصلی: تعداد محدوده‌های فعال، وضعیت پایش، وضعیت هشدار،
 * دکمه توقف سریع هشدارها و دکمه معکوس‌کردن پایش.
 */
class ZoneWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context)
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** به‌روزرسانی همه نمونه‌های ویجت؛ امن برای فراخوانی از هر نخی. */
        fun updateAll(context: Context) {
            scope.launch {
                try {
                    val app = context.applicationContext as GeofenceAlertApp
                    val activeCount = app.prefs.widgetActiveCount.first()
                    val monitoringOn = app.prefs.monitoringEnabled.first()
                    val alerting = AlertService.isRunning
                    val mgr = AppWidgetManager.getInstance(context)
                    val ids = mgr.getAppWidgetIds(
                        ComponentName(context, ZoneWidgetProvider::class.java)
                    )
                    ids.forEach { id ->
                        updateOne(context, mgr, id, activeCount, monitoringOn, alerting)
                    }
                } catch (_: Exception) {
                }
            }
        }

        private fun updateOne(
            context: Context,
            mgr: AppWidgetManager,
            widgetId: Int,
            activeCount: Int,
            monitoringOn: Boolean,
            alerting: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_geofence)
            views.setTextViewText(
                R.id.tv_status,
                "${TimeFormat.toFaDigits(activeCount.toString())} محدوده فعال"
            )
            // وضعیت پایش
            views.setTextViewText(
                R.id.tv_monitor,
                if (monitoringOn) "پایش: فعال" else "پایش: غیرفعال"
            )
            views.setTextColor(
                R.id.tv_monitor,
                if (monitoringOn) 0xFF2E7D32.toInt() else 0xFF9E9E9E.toInt()
            )
            // وضعیت هشدار: فقط هنگام پخش صدا نمایش داده می‌شود
            if (alerting) {
                views.setViewVisibility(R.id.tv_alerting, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.tv_alerting, View.GONE)
            }
            // دکمه معکوس‌کردن پایش
            views.setTextViewText(
                R.id.btn_toggle,
                if (monitoringOn) "توقف پایش" else "شروع پایش"
            )
            val toggleIntent = PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, WidgetActionReceiver::class.java)
                    .setAction(WidgetActionReceiver.ACTION_TOGGLE_MONITORING),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_toggle, toggleIntent)
            // لمس بدنه ویجت → باز کردن برنامه
            val openIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openIntent)
            // دکمه توقف → توقف همه هشدارهای فعال
            val stopIntent = PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, StopAlertReceiver::class.java)
                    .setAction(StopAlertReceiver.ACTION_STOP_ALL),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_stop, stopIntent)
            mgr.updateAppWidget(widgetId, views)
        }
    }
}
