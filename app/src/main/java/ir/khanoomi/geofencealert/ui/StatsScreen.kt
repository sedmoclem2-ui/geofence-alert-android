package ir.khanoomi.geofencealert.ui

import android.app.Application
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.BackupManager
import ir.khanoomi.geofencealert.data.EventLog
import ir.khanoomi.geofencealert.domain.StatsMath
import ir.khanoomi.geofencealert.domain.ZoneEvent
import ir.khanoomi.geofencealert.ui.theme.ZoneColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** نام ماه میلادی در زبان فعال برنامه. */
private fun monthName(strings: UiStrings, month: Int): String = when (month) {
    1 -> strings.monthJan
    2 -> strings.monthFeb
    3 -> strings.monthMar
    4 -> strings.monthApr
    5 -> strings.monthMay
    6 -> strings.monthJun
    7 -> strings.monthJul
    8 -> strings.monthAug
    9 -> strings.monthSep
    10 -> strings.monthOct
    11 -> strings.monthNov
    else -> strings.monthDec
}

/** یک ردیف جدول گزارش ماهانه HTML (موج ۳). */
private data class MonthlyReportRow(
    val name: String,
    val colorHex: String,
    val enters: Int,
    val exits: Int,
    val stayLine: String?
)

/** escape حداقلی متن برای جایگذاری امن در HTML. */
private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

/**
 * سند HTML گزارش ماهانه: فقط CSS درون‌خطی، بدون هیچ دارایی خارجی،
 * جهت متن بر اساس زبان فعال. سطرها از پیش با همان منطق صفحه آمار
 * ساخته شده‌اند (ماندگاری از StatsMath.stayStatsByZone، نه جفت‌سازی
 * دوباره)؛ این تابع فقط قالب‌بندی می‌کند.
 */
private fun buildMonthlyReportHtml(
    title: String,
    monthLabel: String,
    totalEntriesLabel: String,
    totalExitsLabel: String,
    perZoneLabel: String,
    noEventsText: String,
    enters: Int,
    exits: Int,
    rows: List<MonthlyReportRow>,
    dir: String,
    langCode: String
): String {
    val sb = StringBuilder()
    sb.append("<!DOCTYPE html><html lang=\"").append(langCode)
        .append("\" dir=\"").append(dir).append("\"><head><meta charset=\"utf-8\">")
        .append("<title>").append(escapeHtml(title)).append(" — ")
        .append(escapeHtml(monthLabel)).append("</title></head>")
        .append("<body style=\"font-family:sans-serif;margin:24px;color:#222;\">")
        .append("<h1 style=\"font-size:22px;\">").append(escapeHtml(title)).append("</h1>")
        .append("<p style=\"color:#555;\">").append(escapeHtml(monthLabel)).append("</p>")
        .append("<p>").append(escapeHtml(totalEntriesLabel)).append(": ")
        .append(enters).append(" &nbsp;|&nbsp; ")
        .append(escapeHtml(totalExitsLabel)).append(": ").append(exits).append("</p>")
    if (rows.isEmpty()) {
        sb.append("<p style=\"color:#555;\">").append(escapeHtml(noEventsText)).append("</p>")
    } else {
        sb.append("<h2 style=\"font-size:17px;\">").append(escapeHtml(perZoneLabel))
            .append("</h2>")
        sb.append("<table style=\"border-collapse:collapse;width:100%;\">")
        for (row in rows) {
            sb.append("<tr style=\"border-top:1px solid #ddd;\"><td style=\"padding:8px 4px;\">")
                .append("<span style=\"display:inline-block;width:10px;height:10px;")
                .append("border-radius:50%;background:").append(row.colorHex)
                .append(";\"></span> ").append(escapeHtml(row.name))
                .append("</td><td style=\"padding:8px 4px;text-align:end;\">")
                .append(row.enters).append(" / ").append(row.exits)
            if (row.stayLine != null) {
                sb.append("<br><span style=\"color:#555;font-size:13px;\">")
                    .append(escapeHtml(row.stayLine)).append("</span>")
            }
            sb.append("</td></tr>")
        }
        sb.append("</table>")
    }
    sb.append("</body></html>")
    return sb.toString()
}

/** قالب تاریخ میلادی: «14 Oct 2026» — ارقام لاتین و نام انگلیسی ماه. */
private val GREGORIAN_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** آمار یک ماه تقویمی. */
private data class MonthBucket(
    val yearMonth: YearMonth,
    val enters: Int,
    val exits: Int
)

/** یک ردیف تفکیک ورودها بر اساس محدوده (شناسه برای یافتن رنگ اختصاصی). */
private data class ZoneEnterRow(
    val zoneId: Long,
    val zoneName: String,
    val count: Int
)

/** نام کوتاه روز هفته (یک حرف) در زبان فعال برنامه، از خود تقویم سیستم. */
private fun weekdayNarrow(day: LocalDate, lang: AppLang): String {
    val locale = when (lang) {
        AppLang.FA -> Locale("fa")
        AppLang.EN -> Locale.ENGLISH
        AppLang.AR -> Locale("ar")
    }
    return day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
}

/** درصد با ارقام بومی: در فارسی/عربی نشانه ٪ پیش از عدد، در انگلیسی پس از آن. */
private fun formatPercent(pct: Int, lang: AppLang): String {
    val digits = localizedDigits(pct.toString(), lang)
    return if (lang == AppLang.EN) "$digits%" else "٪$digits"
}

/**
 * قالب‌بندی مدت ماندگاری از روی ارقام بومی ([localizedDigits]) و واحدهای
 * موجود ([UiStrings.unitMinute] و [UiStrings.unitHour]): «۲ ساعت ۱۵ دقیقه»،
 * «۴۵ دقیقه» یا «۳ ساعت». زیر یک دقیقه، «۰ دقیقه» می‌شود. مفرد انگلیسی
 * (Hour/Minute) فقط برای مقدار ۱ به‌کار می‌رود چون رشته‌های واحد جمع‌اند.
 */
private fun formatStayDuration(millis: Long, strings: UiStrings, lang: AppLang): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val hourWord = if (lang == AppLang.EN && hours == 1L) "Hour" else strings.unitHour
    val minuteWord = if (lang == AppLang.EN && minutes == 1L) "Minute" else strings.unitMinute
    return when {
        hours == 0L -> "${localizedDigits(minutes.toString(), lang)} $minuteWord"
        minutes == 0L -> "${localizedDigits(hours.toString(), lang)} $hourWord"
        else -> "${localizedDigits(hours.toString(), lang)} $hourWord " +
            "${localizedDigits(minutes.toString(), lang)} $minuteWord"
    }
}

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val eventDao get() = (getApplication<Application>() as GeofenceAlertApp).db.eventDao()
    private val zoneDao get() = (getApplication<Application>() as GeofenceAlertApp).db.zoneDao()

    private val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000

    val stats = eventDao.statsSince(weekAgo)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** فهرست زنده محدوده‌ها؛ برای حل رنگ اختصاصی هر ردیف آمار. */
    val zones = zoneDao.observeZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** رویدادهای ۶ ماه تقویمی اخیر برای آمار ماهانه (یک بار خوانده می‌شود). */
    val monthlyEvents = flow {
        val since = YearMonth.now().minusMonths(5).atDay(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        emit(eventDao.getEventsSince(since))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** آمار ورود و خروج: شاخص‌های کلیدی + نمودار ۷ روز + تفکیک محدوده‌ها + آمار ماهانه. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
    val scheme = MaterialTheme.colorScheme
    val app = context.applicationContext as GeofenceAlertApp
    val vm: StatsViewModel = viewModel { StatsViewModel(app) }
    val stats by vm.stats.collectAsStateWithLifecycle()
    val monthlyEvents by vm.monthlyEvents.collectAsStateWithLifecycle()
    val zones by vm.zones.collectAsStateWithLifecycle()

    val totalEnters = stats.filter { it.type == EventLog.TYPE_ENTER }.sumOf { it.count }
    val totalExits = stats.filter { it.type == EventLog.TYPE_EXIT }.sumOf { it.count }

    // تفکیک ورودها بر اساس محدوده: گروه‌بندی با شناسه (تا رنگ اختصاصی
    // دقیق حل شود)؛ نام نمایشی از پرتکرارترین رکورد همان محدوده.
    val perZone = remember(stats) {
        stats.filter { it.type == EventLog.TYPE_ENTER }
            .groupBy { it.zoneId }
            .map { (zoneId, rows) ->
                ZoneEnterRow(
                    zoneId = zoneId,
                    zoneName = rows.maxByOrNull { it.count }?.zoneName ?: rows.first().zoneName,
                    count = rows.sumOf { it.count }
                )
            }
            .sortedByDescending { it.count }
            .take(12)
    }
    val maxCount = (perZone.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)

    // رنگ هر ردیف: رنگ اختصاصی محدوده متناظر (اول با شناسه، بعد با نام
    // برای محدوده‌های حذف‌شده)؛ اگر پیدا نشد رنگ اصلی تم.
    fun zoneRowColor(row: ZoneEnterRow): Color {
        val zone = zones.firstOrNull { it.id == row.zoneId }
            ?: zones.firstOrNull { it.name == row.zoneName }
        return zone?.let { ZoneColors.zoneColor(it.colorIndex, scheme.primary) }
            ?: scheme.primary
    }

    // بازه ۷ روز اخیر به میلادی
    val weekRange = remember(strings) {
        val end = LocalDate.now()
        val start = end.minusDays(6)
        "${start.format(GREGORIAN_FORMATTER)} ${strings.rangeTo} ${end.format(GREGORIAN_FORMATTER)}"
    }

    // سری ورودهای ۷ روز اخیر (قدیمی‌ترین تا امروز) برای نمودار میله‌ای؛
    // از همان رویدادهایی می‌آید که آمار ماهانه هم از آن‌ها ساخته می‌شود.
    val weekDays = remember(monthlyEvents) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        (6 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            val count = monthlyEvents.count { ev ->
                ev.type == EventLog.TYPE_ENTER &&
                    Instant.ofEpochMilli(ev.timestamp).atZone(zone).toLocalDate() == day
            }
            day to count
        }
    }

    // سبدهای ۶ ماه تقویمی اخیر؛ ماه‌های قبلی هرگز حذف نمی‌شوند
    val months = remember(monthlyEvents) {
        val zone = ZoneId.systemDefault()
        (0..5).map { YearMonth.now().minusMonths(it.toLong()) }.sorted().map { ym ->
            val events = monthlyEvents.filter {
                YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == ym
            }
            MonthBucket(
                yearMonth = ym,
                enters = events.count { it.type == EventLog.TYPE_ENTER },
                exits = events.count { it.type == EventLog.TYPE_EXIT }
            )
        }
    }
    val maxMonthTotal = (months.maxOfOrNull { it.enters + it.exits } ?: 0).coerceAtLeast(1)
    val monthsEmpty = months.all { it.enters == 0 && it.exits == 0 }

    // آمار هوشمند (موج ۲) از همان رویدادهای خامی که نمودار هفتگی و
    // ماهانه از آن ساخته می‌شوند (بازه ۶ ماه تقویمی اخیر): ماندگاری هر
    // محدوده از جفت‌های کامل ENTER→EXIT، سبدهای نقشه حرارتی، و پرتکرارترین
    // ساعت ورود هر محدوده. محاسبات خالص در StatsMath زندگی می‌کنند.
    val zoneEvents = remember(monthlyEvents) {
        monthlyEvents.map { ZoneEvent(it.zoneId, it.type, it.timestamp) }
    }
    val stayByZone = remember(zoneEvents) { StatsMath.stayStatsByZone(zoneEvents) }
    val heatmapBuckets = remember(zoneEvents) { StatsMath.entryHeatmap(zoneEvents) }
    val patternHourByZone = remember(zoneEvents, perZone) {
        perZone.mapNotNull { row ->
            val hour = StatsMath.mostFrequentEntryHour(
                StatsMath.entryHoursForZone(zoneEvents, row.zoneId)
            )
            if (hour != null) row.zoneId to hour else null
        }.toMap()
    }

    // گزارش ماهانه (موج ۳): همان monthlyEvents صفحه، فیلترشده به ماه
    // جاری تقویمی؛ ماندگاری از StatsMath.stayStatsByZone می‌آید، نه از
    // جفت‌سازی دوباره. فایل با نام ASCII-امن در Documents/محدوده هشدار
    // (خانواده پوشه backup.json) نوشته می‌شود و اگر Documents در
    // دسترس نباشد (مثلاً scoped storage اندروید ۱۰ به بالا که نوشتن
    // مستقیم با File را نمی‌دهد) به cacheDir می‌افتد؛ اشتراک در هر دو
    // حالت از FileProvider تازه‌اضافه‌شده با grant موقت خواندن است.
    // مسیر خروجی دستی قبلی (SAF/CreateDocument) برای این گزارش استفاده
    // نشده، چون گزارش باید بی‌درنگ ساخته و به اشتراک گذاشته شود، نه
    // اینکه کاربر اول مسیر انتخاب کند.
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    fun shareMonthlyReport() {
        scope.launch(Dispatchers.IO) {
            try {
                val zoneId = ZoneId.systemDefault()
                val currentMonth = YearMonth.now()
                val currentEvents = monthlyEvents.filter {
                    YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zoneId)) ==
                        currentMonth
                }
                val enters = currentEvents.count { it.type == EventLog.TYPE_ENTER }
                val exits = currentEvents.count { it.type == EventLog.TYPE_EXIT }
                val currentZoneEvents =
                    currentEvents.map { ZoneEvent(it.zoneId, it.type, it.timestamp) }
                val stayCurrent = StatsMath.stayStatsByZone(currentZoneEvents)
                val rows = currentEvents.groupBy { it.zoneId }
                    .map { (zoneIdKey, evs) ->
                        val zone = zones.firstOrNull { it.id == zoneIdKey }
                        val zoneEnters = evs.count { it.type == EventLog.TYPE_ENTER }
                        val zoneExits = evs.count { it.type == EventLog.TYPE_EXIT }
                        val stay = stayCurrent[zoneIdKey]
                        val color = zone?.let {
                            ZoneColors.zoneColor(it.colorIndex, scheme.primary)
                        } ?: scheme.primary
                        MonthlyReportRow(
                            name = zone?.name
                                ?: evs.maxByOrNull { it.id }?.zoneName
                                ?: evs.first().zoneName,
                            colorHex = String.format(
                                Locale.US, "#%06X", color.toArgb() and 0xFFFFFF
                            ),
                            enters = zoneEnters,
                            exits = zoneExits,
                            stayLine = stay?.let {
                                String.format(
                                    strings.stayDurationLine,
                                    formatStayDuration(it.averageMillis, strings, lang),
                                    formatStayDuration(it.totalMillis, strings, lang)
                                )
                            }
                        )
                    }
                    .sortedByDescending { it.enters + it.exits }
                val monthLabel = "${monthName(strings, currentMonth.monthValue)} " +
                    localizedDigits(currentMonth.year.toString(), lang)
                val html = buildMonthlyReportHtml(
                    title = strings.monthlyReport,
                    monthLabel = monthLabel,
                    totalEntriesLabel = strings.totalEntries,
                    totalExitsLabel = strings.totalExits,
                    perZoneLabel = strings.entriesPerZone,
                    noEventsText = strings.statsNoEvents,
                    enters = enters,
                    exits = exits,
                    rows = rows,
                    dir = if (lang == AppLang.EN) "ltr" else "rtl",
                    langCode = when (lang) {
                        AppLang.FA -> "fa"
                        AppLang.EN -> "en"
                        AppLang.AR -> "ar"
                    }
                )
                val fileName = "monthly-report-$currentMonth.html"
                val docsDir = try {
                    File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS
                        ),
                        BackupManager.FOLDER_NAME
                    ).apply { mkdirs() }
                } catch (_: Exception) {
                    null
                }
                val dir = if (docsDir != null && docsDir.exists()) docsDir else context.cacheDir
                val file = File(dir, fileName)
                file.writeText(html, Charsets.UTF_8)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                withContext(Dispatchers.Main) {
                    try {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/html"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(send, strings.monthlyReport)
                        )
                        Toast.makeText(context, strings.reportDone, Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, strings.reportFailed, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, strings.reportFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalStrings.current.statistics, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalStrings.current.back)
                    }
                }
            )
        }
    ) { padding ->
        if (stats.isEmpty() && monthsEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    LocalStrings.current.statsEmpty,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    LocalStrings.current.statsEmptySub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { shareMonthlyReport() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(LocalStrings.current.monthlyReport)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── بخش هفتگی: سربرگ + شاخص‌های کلیدی ───
                item {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            LocalStrings.current.weeklyStats,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            weekRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (stats.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiCard(
                                label = LocalStrings.current.totalEntries,
                                value = totalEnters,
                                dotColor = scheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                label = LocalStrings.current.totalExits,
                                value = totalExits,
                                dotColor = scheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                label = LocalStrings.current.totalEvents,
                                value = totalEnters + totalExits,
                                dotColor = scheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ─── نمودار ورودهای ۷ روز اخیر ───
                    item {
                        Text(
                            LocalStrings.current.chartLast7Days,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    item {
                        WeekChart(days = weekDays, lang = lang)
                    }

                    // ─── نقشه حرارتی ورودها: ۲۴ ستون ساعت × ۷ ردیف روز هفته ───
                    item {
                        Text(
                            LocalStrings.current.entryHeatmap,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    item {
                        EntryHeatmap(buckets = heatmapBuckets, lang = lang)
                    }

                    // ─── تفکیک بر اساس محدوده ───
                    item {
                        Text(
                            LocalStrings.current.entriesPerZone,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(perZone, key = { it.zoneId }) { row ->
                        val stay = stayByZone[row.zoneId]
                        val patternHour = patternHourByZone[row.zoneId]
                        ZoneStatRow(
                            name = row.zoneName,
                            count = row.count,
                            percent = if (totalEnters > 0) {
                                (row.count * 100f / totalEnters).toInt()
                            } else {
                                0
                            },
                            fraction = row.count.toFloat() / maxCount,
                            color = zoneRowColor(row),
                            durationLine = stay?.let {
                                String.format(
                                    LocalStrings.current.stayDurationLine,
                                    formatStayDuration(it.averageMillis, LocalStrings.current, lang),
                                    formatStayDuration(it.totalMillis, LocalStrings.current, lang)
                                )
                            },
                            patternLine = patternHour?.let {
                                String.format(
                                    LocalStrings.current.patternUsuallyAround,
                                    localizedDigits(it.toString(), lang)
                                )
                            }
                        )
                    }
                } else {
                    item {
                        Text(
                            LocalStrings.current.statsWeekEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ─── بخش ماهانه ───
                item {
                    Text(
                        LocalStrings.current.monthlyStats,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                // گزارش ماهانه (موج ۳): دکمه خروجی کنار بخش ماهانه، با
                // همان سبک دکمه خروجی صفحه تاریخچه (OutlinedButton تمام‌عرض).
                item {
                    OutlinedButton(
                        onClick = { shareMonthlyReport() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(LocalStrings.current.monthlyReport)
                    }
                }
                items(months, key = { it.yearMonth.toString() }) { bucket ->
                    MonthCard(
                        bucket = bucket,
                        maxTotal = maxMonthTotal
                    )
                }
            }
        }
    }
}

/** کارت شاخص کلیدی: عدد بزرگ متحرک + برچسب کوچک و نقطه رنگی وضعیت. */
@Composable
private fun KpiCard(
    label: String,
    value: Int,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val lang = LocalAppLang.current
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 500),
        label = "kpiValue"
    )
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                localizedDigits(animated.toString(), lang),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * نمودار میله‌ای ورودهای ۷ روز اخیر: مقدار بالای هر ستون، ستون‌های
 * گوشه‌گرد روی بوم (فقط drawRoundRect و یک خط مبنا با drawRect)، و نام
 * کوتاه روز هفته زیر هر ستون. ستون امروز رنگ مکمل می‌گیرد و همه
 * ستون‌ها با یک انیمیشن رشد از صفر بالا می‌آیند. در چیدمان راست‌به‌چپ
 * قدیمی‌ترین روز سمت راست و امروز سمت چپ می‌نشیند (جای ستون‌ها در بوم
 * با جهت چیدمان هماهنگ است تا با ردیف برچسب‌ها هم‌تراز بمانند).
 */
@Composable
private fun WeekChart(days: List<Pair<LocalDate, Int>>, lang: AppLang) {
    val scheme = MaterialTheme.colorScheme
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val maxVal = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }
    val primary = scheme.primary
    val secondary = scheme.secondary
    val trackColor = scheme.surfaceVariant
    Column {
        // مقادیر بالای ستون‌ها
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (_, count) ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        localizedDigits(count.toString(), lang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val slotW = size.width / 7f
            val barW = slotW * 0.52f
            val baseY = size.height
            val topPad = 6.dp.toPx()
            val corner = CornerRadius(
                x = minOf(barW / 2f, 10.dp.toPx()),
                y = minOf(barW / 2f, 10.dp.toPx())
            )
            // خط مبنا
            drawRect(
                color = trackColor,
                topLeft = Offset(0f, baseY - 1.dp.toPx()),
                size = Size(size.width, 1.dp.toPx())
            )
            days.forEachIndexed { index, (_, count) ->
                val slotStart = if (isRtl) {
                    size.width - (index + 1) * slotW
                } else {
                    index * slotW
                }
                val x = slotStart + (slotW - barW) / 2f
                val h = (count.toFloat() / maxVal) *
                    (size.height - topPad) * grow.value
                if (h > 0.5f) {
                    drawRoundRect(
                        color = if (index == days.lastIndex) secondary else primary,
                        topLeft = Offset(x, baseY - h),
                        size = Size(barW, h),
                        cornerRadius = corner
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // نام کوتاه روز هفته زیر هر ستون
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (day, _) ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        weekdayNarrow(day, lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * نقشه حرارتی ورودها — جهت‌یابی وارونه (۲۴ ستون ساعت × ۷ ردیف روز هفته).
 * چرا وارونه؟ نمودار ۷ روزه همین صفحه ۷ ستون با برچسب زیر ستون دارد،
 * ولی ۲۴ ردیف ساعت صفحه را بسیار بلند می‌کرد؛ با ۷ ردیف، کل بخش در
 * حدود ۲۰۰dp جا می‌شود و سلول‌ها به‌قدر کافی پهن‌اند. ردیف‌ها از روز
 * اول هفته در زبان فعال شروع می‌شوند (فارسی/عربی: شنبه، انگلیسی:
 * یکشنبه) و برچسب هر ردیف همان سازوکار [weekdayNarrow] است. ستون‌ها
 * مثل WeekChart در چیدمان راست‌به‌چپ آینه می‌شوند (ساعت ۰ سمت راست) تا
 * با ردیف برچسب‌های ساعت ۰/۶/۱۲/۱۸ زیر شبکه هم‌تراز بمانند. رنگ سلول:
 * شمارش صفر → surfaceVariant، وگرنه primary با آلفای ۰٫۲۵ تا ۱ بر حسب
 * نسبت به بیشترین شمارش بخش؛ فقط رنگ‌های یکدست با آلفا، بدون گرادیان
 * یا تاری.
 */
@Composable
private fun EntryHeatmap(buckets: Array<IntArray>, lang: AppLang) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val locale = when (lang) {
        AppLang.FA -> Locale("fa")
        AppLang.EN -> Locale.ENGLISH
        AppLang.AR -> Locale("ar")
    }
    val weekDates = remember(lang) {
        val firstDay = WeekFields.of(locale).firstDayOfWeek
        val start = LocalDate.now().with(TemporalAdjusters.previousOrSame(firstDay))
        (0..6).map { start.plusDays(it.toLong()) }
    }
    val maxCount = buckets.flatMap { it.toList() }.maxOrNull()?.coerceAtLeast(1) ?: 1
    val primary = scheme.primary
    val emptyColor = scheme.surfaceVariant
    val cellH = 26.dp
    val gap = 3.dp
    val labelW = 28.dp
    // ارتفاع شبکه: ۷ ردیف ۲۶dp + ۶ فاصله ۳dp = ۲۰۰dp
    val gridH = 200.dp
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            // برچسب روزهای هفته در کنار شبکه
            Column(
                modifier = Modifier.width(labelW),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                weekDates.forEach { day ->
                    Box(
                        modifier = Modifier.height(cellH),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            weekdayNarrow(day, lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(gridH)
            ) {
                val slotW = size.width / 24f
                val cellPx = cellH.toPx()
                val gapPx = gap.toPx()
                weekDates.forEachIndexed { row, day ->
                    val counts = buckets[day.dayOfWeek.value - 1]
                    val y = row * (cellPx + gapPx)
                    (0..23).forEach { hour ->
                        val count = counts[hour]
                        val colStart = if (isRtl) {
                            size.width - (hour + 1) * slotW
                        } else {
                            hour * slotW
                        }
                        drawRoundRect(
                            color = if (count == 0) {
                                emptyColor
                            } else {
                                primary.copy(
                                    alpha = 0.25f + 0.75f * (count.toFloat() / maxCount)
                                )
                            },
                            topLeft = Offset(colStart + gapPx / 2f, y),
                            size = Size(slotW - gapPx, cellPx),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }
        // برچسب‌های ساعت زیر شبکه؛ فقط ۰/۶/۱۲/۱۸ تا شلوغ نشود.
        // این Row در RTL خودبه‌خود وارونه می‌شود، پس با آینه‌شدن بوم هماهنگ است.
        Row {
            Spacer(Modifier.width(labelW))
            Row(modifier = Modifier.weight(1f)) {
                (0..23).forEach { hour ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hour == 0 || hour == 6 || hour == 12 || hour == 18) {
                            Text(
                                localizedDigits(hour.toString(), lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        // راهنما: نقطه کم‌رنگ = ورود کم، نقطه پررنگ = ورود زیاد
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(emptyColor, CircleShape)
                )
                Text(
                    strings.heatmapLow,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(primary, CircleShape)
                )
                Text(
                    strings.heatmapHigh,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

/** یک ردیف تفکیک محدوده: نقطه رنگ اختصاصی + نام + شمارش و درصد + نوار پیشرفت متحرک.
 * خط ماندگاری فقط وقتی هست که محدوده حداقل یک جفت کامل ENTER→EXIT داشته باشد،
 * و خط الگو فقط وقتی که حداقل ۳ ورود داشته باشد؛ در غیر این صورت هیچ خطی
 * اضافه نمی‌شود و باقی ردیف دست‌نخورده می‌ماند. */
@Composable
private fun ZoneStatRow(
    name: String,
    count: Int,
    percent: Int,
    fraction: Float,
    color: Color,
    durationLine: String? = null,
    patternLine: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val lang = LocalAppLang.current
    val target = if (count > 0) fraction.coerceIn(0.03f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "zoneStatBar"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                localizedDigits(count.toString(), lang),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                formatPercent(percent, lang),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(scheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
        if (durationLine != null) {
            Text(
                durationLine,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        if (patternLine != null) {
            Text(
                patternLine,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/**
 * ردیف براق یک ماه تقویمی: نام ماه + بازه میلادی در یک سو، مجموع بزرگ
 * در سوی دیگر، و نوار انباشته دومقطعه‌ای نسبت به پررویدادترین ماه
 * (مقطع ورود رنگ اصلی، مقطع خروج رنگ سوم).
 */
@Composable
private fun MonthCard(bucket: MonthBucket, maxTotal: Int) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
    val ym = bucket.yearMonth
    val monthLabel = "${monthName(strings, ym.monthValue)} " +
        localizedDigits(ym.year.toString(), lang)
    val gregRange = "${ym.atDay(1).format(GREGORIAN_FORMATTER)} ${strings.rangeTo} " +
        ym.atEndOfMonth().format(GREGORIAN_FORMATTER)
    val total = bucket.enters + bucket.exits
    val animatedFraction by animateFloatAsState(
        targetValue = if (total > 0) {
            (total.toFloat() / maxTotal).coerceIn(0.03f, 1f)
        } else {
            0f
        },
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "monthBar"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        gregRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        localizedDigits(total.toString(), lang),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        strings.totalEvents,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            if (total == 0) {
                Text(
                    strings.statsNoEvents,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            } else {
                // نوار انباشته: پهنای کل نسبت به پررویدادترین ماه؛ داخلش
                // مقطع ورود و خروج به نسبت سهم خودشان از ماه.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(scheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction)
                            .fillMaxHeight()
                    ) {
                        if (bucket.enters > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(bucket.enters.toFloat())
                                    .fillMaxHeight()
                                    .background(scheme.primary)
                            )
                        }
                        if (bucket.exits > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(bucket.exits.toFloat())
                                    .fillMaxHeight()
                                    .background(scheme.tertiary)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(scheme.primary, CircleShape)
                        )
                        Text(
                            "${strings.statsEnterLabel}: " +
                                localizedDigits(bucket.enters.toString(), lang),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(scheme.tertiary, CircleShape)
                        )
                        Text(
                            "${strings.statsExitLabel}: " +
                                localizedDigits(bucket.exits.toString(), lang),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
