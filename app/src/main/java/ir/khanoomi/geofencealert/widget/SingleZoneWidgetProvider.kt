package ir.khanoomi.geofencealert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.MainActivity
import ir.khanoomi.geofencealert.R
import ir.khanoomi.geofencealert.receiver.StopAlertReceiver
import ir.khanoomi.geofencealert.receiver.WidgetActionReceiver
import ir.khanoomi.geofencealert.service.AlertService
import ir.khanoomi.geofencealert.ui.stringsForContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ویجت اختصاصی یک محدوده: هر نمونه ویجت به یک محدوده متصل می‌شود
 * (انتخاب در [ZoneWidgetConfigureActivity]) و نام، وضعیت و دکمه‌های
 * «توقف هشدار» و «فعال/غیرفعال» همان محدوده را نشان می‌دهد.
 */
class SingleZoneWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateOne(context, appWidgetManager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { clearZoneForWidget(context, it) }
    }

    companion object {
        private const val TAG = "SingleZoneWidget"
        private const val PREFS = "zone_widgets"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun zoneIdForWidget(context: Context, appWidgetId: Int): Long =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(widgetKey(appWidgetId), -1L)

        fun setZoneForWidget(context: Context, appWidgetId: Int, zoneId: Long) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(widgetKey(appWidgetId), zoneId)
                .apply()
        }

        fun clearZoneForWidget(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(widgetKey(appWidgetId))
                .apply()
        }

        private fun widgetKey(appWidgetId: Int) = "widget_zone_$appWidgetId"

        /** به‌روزرسانی همه نمونه‌های ویجت تک‌محدوده؛ امن برای فراخوانی از هر نخی. */
        fun updateAll(context: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(
                    ComponentName(context, SingleZoneWidgetProvider::class.java)
                )
                ids.forEach { updateOne(context, mgr, it) }
            } catch (e: Exception) {
                Log.w(TAG, "updateAll failed", e)
            }
        }

        fun updateOne(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val zoneId = zoneIdForWidget(context, appWidgetId)
            if (zoneId == -1L) {
                showUnconfigured(context, mgr, appWidgetId)
                return
            }
            scope.launch {
                try {
                    val app = context.applicationContext as GeofenceAlertApp
                    val zone = app.zoneRepo.getById(zoneId)
                    val strings = stringsForContext(context)
                    val views = RemoteViews(context.packageName, R.layout.widget_single_zone)
                    views.setTextViewText(R.id.btn_zone_stop, strings.stopAlert)
                    views.setTextViewText(R.id.btn_zone_toggle, strings.widgetToggle)
                    if (zone == null) {
                        views.setTextViewText(R.id.tv_zone_name, strings.widgetZoneDeleted)
                        views.setTextViewText(R.id.tv_zone_status, strings.widgetZoneDeletedSub)
                    } else {
                        views.setTextViewText(R.id.tv_zone_name, zone.name)
                        val status = buildString {
                            append(if (zone.isActive) strings.active else strings.inactive)
                            if (zone.isAlerting) append(" • ${strings.alertActive}")
                        }
                        views.setTextViewText(R.id.tv_zone_status, status)
                        // توقف دستی هشدار همین محدوده
                        val stopPi = PendingIntent.getBroadcast(
                            context,
                            appWidgetId,
                            Intent(context, StopAlertReceiver::class.java)
                                .putExtra(AlertService.EXTRA_ZONE_ID, zoneId),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.btn_zone_stop, stopPi)
                        // فعال/غیرفعال کردن همین محدوده
                        val togglePi = PendingIntent.getBroadcast(
                            context,
                            appWidgetId + REQUEST_CODE_OFFSET,
                            Intent(context, WidgetActionReceiver::class.java)
                                .setAction(WidgetActionReceiver.ACTION_TOGGLE_ZONE)
                                .putExtra(AlertService.EXTRA_ZONE_ID, zoneId),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.btn_zone_toggle, togglePi)
                    }
                    // لمس بدنه → باز کردن برنامه
                    val openPi = PendingIntent.getActivity(
                        context,
                        appWidgetId + REQUEST_CODE_OFFSET * 2,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.single_zone_root, openPi)
                    mgr.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.w(TAG, "updateOne failed for widget $appWidgetId", e)
                }
            }
        }

        private fun showUnconfigured(
            context: Context,
            mgr: AppWidgetManager,
            appWidgetId: Int
        ) {
            val strings = stringsForContext(context)
            val views = RemoteViews(context.packageName, R.layout.widget_single_zone)
            views.setTextViewText(R.id.tv_zone_name, strings.widgetZoneNotSelected)
            views.setTextViewText(R.id.tv_zone_status, strings.widgetZoneNotSelectedSub)
            views.setTextViewText(R.id.btn_zone_stop, strings.stopAlert)
            views.setTextViewText(R.id.btn_zone_toggle, strings.widgetToggle)
            val pi = PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, ZoneWidgetConfigureActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.single_zone_root, pi)
            try {
                mgr.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.w(TAG, "showUnconfigured failed", e)
            }
        }

        private const val REQUEST_CODE_OFFSET = 100_000
    }
}
