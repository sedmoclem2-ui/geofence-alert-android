package ir.khanoomi.geofencealert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.MainActivity
import ir.khanoomi.geofencealert.R
import ir.khanoomi.geofencealert.receiver.WidgetActionReceiver
import ir.khanoomi.geofencealert.ui.appLangForContext
import ir.khanoomi.geofencealert.ui.localizedDigits
import ir.khanoomi.geofencealert.ui.stringsForContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * ویجت رادار: یک صفحه رادار تیره با حلقه‌های هم‌مرکز، یک گوه جاروب
 * و یک نقطه برای هر محدوده فعال، به‌همراه تعداد محدوده‌های فعال،
 * وضعیت پایش و دکمه معکوس‌کردن پایش. ساختار ارائه دقیقاً از
 * [ZoneWidgetProvider] پیروی می‌کند (همان onUpdate، همان updateAll
 * با CoroutineScope روی IO، همان PendingIntent باز کردن برنامه و
 * همان اکشن معکوس پایش از [WidgetActionReceiver]).
 *
 * رندر رادار با android.graphics.Canvas انجام می‌شود
 * و فقط از خط (stroke) و رنگ یکدست استفاده می‌کند: بدون گرادیان،
 * بدون تاری و بدون ذره.
 */
class RadarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context)
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** به‌روزرسانی همه نمونه‌های ویجت رادار؛ امن برای فراخوانی از هر نخی. */
        fun updateAll(context: Context) {
            scope.launch {
                try {
                    val app = context.applicationContext as GeofenceAlertApp
                    val monitoringOn = app.prefs.monitoringEnabled.first()
                    val activeZones = app.zoneRepo.getActiveZones()
                    val mgr = AppWidgetManager.getInstance(context)
                    val ids = mgr.getAppWidgetIds(
                        ComponentName(context, RadarWidgetProvider::class.java)
                    )
                    ids.forEach { id ->
                        updateOne(context, mgr, id, activeZones.map { it.id }, monitoringOn)
                    }
                } catch (_: Exception) {
                }
            }
        }

        private fun updateOne(
            context: Context,
            mgr: AppWidgetManager,
            widgetId: Int,
            activeZoneIds: List<Long>,
            monitoringOn: Boolean
        ) {
            val strings = stringsForContext(context)
            val lang = appLangForContext(context)
            val views = RemoteViews(context.packageName, R.layout.radar_widget)
            views.setImageViewBitmap(R.id.iv_radar, buildRadarBitmap(activeZoneIds))
            views.setTextViewText(
                R.id.tv_radar_count,
                String.format(
                    strings.widgetActiveZones,
                    localizedDigits(activeZoneIds.size.toString(), lang)
                )
            )
            // وضعیت پایش؛ رنگ‌ها روی پس‌زمینه تیره رادار خوانا انتخاب شده‌اند
            // (نسخه روشن ZoneWidgetProvider روی پس‌زمینه سفید بود).
            views.setTextViewText(
                R.id.tv_radar_monitor,
                if (monitoringOn) strings.widgetMonitoringOn else strings.widgetMonitoringOff
            )
            views.setTextColor(
                R.id.tv_radar_monitor,
                if (monitoringOn) 0xFF22D3EE.toInt() else 0xFF8A9BA8.toInt()
            )
            // دکمه معکوس‌کردن پایش: همان اکشن موجود ویجت اصلی.
            views.setTextViewText(
                R.id.btn_radar_toggle,
                if (monitoringOn) strings.widgetStopMonitoring else strings.widgetStartMonitoring
            )
            val toggleIntent = PendingIntent.getBroadcast(
                context,
                3,
                Intent(context, WidgetActionReceiver::class.java)
                    .setAction(WidgetActionReceiver.ACTION_TOGGLE_MONITORING),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_radar_toggle, toggleIntent)
            // لمس بدنه ویجت → باز کردن برنامه (همان الگوی ویجت اصلی).
            val openIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.radar_root, openIntent)
            mgr.updateAppWidget(widgetId, views)
        }

        // پالت رادار تیره؛ مستقل از تم برنامه تا روی هر صفحه اصلی خوانا باشد.
        private const val COLOR_DISC = 0xFF0B1E33
        private const val COLOR_RING = 0xFF6FA8C9
        private const val COLOR_RING_DIM = 0xFF3D6B8A
        private const val COLOR_SWEEP = 0xFF22D3EE
        private const val COLOR_DOT = 0xFF22D3EE
        private const val COLOR_DOT_CORE = 0xFFFFFFFF

        /**
         * ساخت بیت‌مپ رادار: صفحه تیره، حلقه‌های هم‌مرکز (stroke)،
         * خطوط متقاطع، یک گوه جاروب با رنگ یکدست نیمه‌شفاف و یک نقطه
         * برای هر محدوده فعال.
         *
         * نکته مهم: موقعیت نقطه‌ها صرفاً تزئینی است (decorative only —
         * not real geography). هر نقطه با یک مولد شبه‌تصادفی که با هش
         * شناسه محدوده مقداردهی اولیه می‌شود (seeded by the zone id hash)
         * در یک زاویه و فاصله تصادفی ولی پایدار داخل صفحه قرار می‌گیرد؛
         * این موقعیت‌ها موقعیت جغرافیایی واقعی محدوده‌ها نیستند.
         */
        fun buildRadarBitmap(activeZoneIds: List<Long>, size: Int = 320): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val cx = size / 2f
            val cy = size / 2f
            val maxRadius = min(cx, cy) - 8f

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

            // صفحه تیره رادار.
            fillPaint.color = COLOR_DISC.toInt()
            canvas.drawCircle(cx, cy, maxRadius, fillPaint)

            // حلقه‌های هم‌مرکز (فقط stroke، بدون گرادیان).
            strokePaint.color = COLOR_RING_DIM.toInt()
            for (fraction in floatArrayOf(0.25f, 0.5f, 0.75f)) {
                canvas.drawCircle(cx, cy, maxRadius * fraction, strokePaint)
            }
            strokePaint.color = COLOR_RING.toInt()
            strokePaint.strokeWidth = 4f
            canvas.drawCircle(cx, cy, maxRadius, strokePaint)

            // خطوط متقاطع مرکز.
            strokePaint.color = COLOR_RING_DIM.toInt()
            strokePaint.strokeWidth = 2f
            canvas.drawLine(cx - maxRadius, cy, cx + maxRadius, cy, strokePaint)
            canvas.drawLine(cx, cy - maxRadius, cx, cy + maxRadius, strokePaint)

            // گوه جاروب: رنگ یکدست نیمه‌شفاف + لبه پیشرو روشن.
            // زاویه از دقیقه جاری گرفته می‌شود تا در هر به‌روزرسانی جلو رفته باشد.
            val sweepStart = ((System.currentTimeMillis() / 60_000L) % 360L).toFloat() - 50f
            val oval = RectF(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius)
            fillPaint.color = COLOR_SWEEP.toInt()
            fillPaint.alpha = 70
            canvas.drawArc(oval, sweepStart, 50f, true, fillPaint)
            fillPaint.alpha = 255
            val edgeAngle = Math.toRadians((sweepStart + 50f).toDouble())
            strokePaint.color = COLOR_SWEEP.toInt()
            strokePaint.strokeWidth = 3f
            canvas.drawLine(
                cx,
                cy,
                (cx + cos(edgeAngle) * maxRadius).toFloat(),
                (cy + sin(edgeAngle) * maxRadius).toFloat(),
                strokePaint
            )

            // یک نقطه برای هر محدوده فعال، در موقعیت شبه‌تصادفی پایدار.
            for (zoneId in activeZoneIds) {
                val random = Random(zoneId.hashCode().toLong())
                val angle = random.nextDouble() * 2 * PI
                val radiusFraction = 0.18 + random.nextDouble() * 0.72
                val r = (maxRadius * radiusFraction).toFloat()
                val x = (cx + cos(angle) * r).toFloat()
                val y = (cy + sin(angle) * r).toFloat()
                fillPaint.color = COLOR_DOT.toInt()
                fillPaint.alpha = 255
                canvas.drawCircle(x, y, 10f, fillPaint)
                fillPaint.color = COLOR_DOT_CORE.toInt()
                canvas.drawCircle(x, y, 4f, fillPaint)
            }

            // نقطه مرکزی.
            fillPaint.color = COLOR_SWEEP.toInt()
            canvas.drawCircle(cx, cy, 6f, fillPaint)
            return bitmap
        }
    }
}
