package ir.khanoomi.geofencealert.ui

import android.app.Application
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.EventLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

/** قالب تاریخ میلادی: «14 Oct 2026» — ارقام لاتین و نام انگلیسی ماه. */
private val GREGORIAN_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** آمار یک ماه تقویمی. */
private data class MonthBucket(
    val yearMonth: YearMonth,
    val enters: Int,
    val exits: Int
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val eventDao get() = (getApplication<Application>() as GeofenceAlertApp).db.eventDao()

    private val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000

    val stats = eventDao.statsSince(weekAgo)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** رویدادهای ۶ ماه تقویمی اخیر برای آمار ماهانه (یک بار خوانده می‌شود). */
    val monthlyEvents = flow {
        val since = YearMonth.now().minusMonths(5).atDay(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        emit(eventDao.getEventsSince(since))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** آمار ورود و خروج: بخش هفتگی + بخش ماهانه (۶ ماه اخیر). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val app = context.applicationContext as GeofenceAlertApp
    val vm: StatsViewModel = viewModel { StatsViewModel(app) }
    val stats by vm.stats.collectAsStateWithLifecycle()
    val monthlyEvents by vm.monthlyEvents.collectAsStateWithLifecycle()

    val totalEnters = stats.filter { it.type == EventLog.TYPE_ENTER }.sumOf { it.count }
    val totalExits = stats.filter { it.type == EventLog.TYPE_EXIT }.sumOf { it.count }
    val perZone = stats.filter { it.type == EventLog.TYPE_ENTER }
        .groupBy { it.zoneName }
        .mapValues { (_, rows) -> rows.sumOf { it.count } }
        .toList()
        .sortedByDescending { it.second }
        .take(12)
    val maxCount = (perZone.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    // بازه ۷ روز اخیر به میلادی
    val weekRange = remember {
        val end = LocalDate.now()
        val start = end.minusDays(6)
        "${start.format(GREGORIAN_FORMATTER)} ${strings.rangeTo} ${end.format(GREGORIAN_FORMATTER)}"
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
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── بخش هفتگی ───
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            LocalStrings.current.weeklyStats,
                            style = MaterialTheme.typography.titleSmall,
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
                            StatTotalCard(
                                title = LocalStrings.current.totalEntries,
                                value = totalEnters,
                                modifier = Modifier.weight(1f)
                            )
                            StatTotalCard(
                                title = LocalStrings.current.totalExits,
                                value = totalExits,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Text(
                            LocalStrings.current.entriesPerZone,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(perZone, key = { it.first }) { (name, count) ->
                        ZoneBarRow(
                            name = name,
                            count = count,
                            fraction = count.toFloat() / maxCount,
                            isMax = count == perZone.first().second && perZone.size > 1
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        LocalStrings.current.monthlyStats,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(months, key = { it.yearMonth.toString() }) { bucket ->
                    MonthCard(
                        bucket = bucket,
                        fraction = (bucket.enters + bucket.exits).toFloat() / maxMonthTotal
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTotalCard(title: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                localizedDigits(value.toString(), LocalAppLang.current),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ZoneBarRow(
    name: String,
    count: Int,
    fraction: Float,
    isMax: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                localizedDigits(count.toString(), LocalAppLang.current),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.primary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(scheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isMax) Brush.horizontalGradient(
                            listOf(scheme.tertiary, scheme.secondary)
                        )
                        else Brush.horizontalGradient(
                            listOf(scheme.primary, scheme.secondary)
                        )
                    )
            )
        }
    }
}

/** کارت یک ماه تقویمی: نام فارسی ماه، بازه میلادی، شمارش ورود/خروج و نوار پیشرفت. */
@Composable
private fun MonthCard(bucket: MonthBucket, fraction: Float) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
    val ym = bucket.yearMonth
    val faLabel = "${monthName(strings, ym.monthValue)} " +
        localizedDigits(ym.year.toString(), lang)
    val gregRange = "${ym.atDay(1).format(GREGORIAN_FORMATTER)} ${strings.rangeTo} " +
        ym.atEndOfMonth().format(GREGORIAN_FORMATTER)
    val total = bucket.enters + bucket.exits
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    faLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${strings.statsEnterLabel}: ${localizedDigits(bucket.enters.toString(), lang)} • " +
                        "${strings.statsExitLabel}: ${localizedDigits(bucket.exits.toString(), lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )
            }
            Text(
                gregRange,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            if (total == 0) {
                Text(
                    strings.statsNoEvents,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(scheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(scheme.primary, scheme.secondary)
                                )
                            )
                    )
                }
            }
        }
    }
}
