package ir.khanoomi.geofencealert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.MainActivity
import ir.khanoomi.geofencealert.R
import ir.khanoomi.geofencealert.domain.TimeFormat
import ir.khanoomi.geofencealert.receiver.StopAlertReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ویجت صفحه اصلی: نمایش تعداد محدوده‌های فعال و دکمه توقف سریع همه هشدارها.
 *
 * تعداد محدوده‌های فعال در DataStore ([PrefsRepository.widgetActiveCount]) نگه
 * داشته می‌شود و با هر تغییر در صفحه اصلی به‌روز می‌گردد.
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
                    val mgr = AppWidgetManager.getInstance(context)
                    val ids = mgr.getAppWidgetIds(
                        ComponentName(context, ZoneWidgetProvider::class.java)
                    )
                    ids.forEach { id -> updateOne(context, mgr, id, activeCount) }
                } catch (_: Exception) {
                }
            }
        }

        private fun updateOne(
            context: Context,
            mgr: AppWidgetManager,
            widgetId: Int,
            activeCount: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_geofence)
            views.setTextViewText(
                R.id.tv_status,
                "${TimeFormat.toFaDigits(activeCount.toString())} محدوده فعال"
            )
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
