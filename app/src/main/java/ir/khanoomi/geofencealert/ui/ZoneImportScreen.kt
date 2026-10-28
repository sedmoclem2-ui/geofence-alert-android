package ir.khanoomi.geofencealert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.BackupManager
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.GeoMath
import ir.khanoomi.geofencealert.domain.ZoneShareCodec
import ir.khanoomi.geofencealert.ui.theme.ZoneColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * صفحه ورود محدوده از پیوند اشتراک‌گذاری (موج ۳).
 *
 * payload در AppNav با Uri.decode خوانده شده و اینجا رمزگشایی می‌شود.
 * حالت خطا (هر decode ناموفق) هرگز کرش نمی‌کند: فقط پیام دوستانه و
 * یک دکمه «بازگشت» دارد. حالت موفق کارت پیش‌نمایش (نام، شعاع یا کلمه
 * چندضلعی، مختصات، نقطه رنگ محدوده) و دو دکمه دارد: «افزودن به
 * محدوده‌ها» محدوده‌ای تازه با isActive=false درج می‌کند (تا کاربر بی‌خبر
 * هشدار نگیرد) و به خانه می‌رود؛ «انصراف» بدون درج برمی‌گردد.
 * همه فیلدهای payload — polygonJson، colorIndex، زمان‌بندی و سکوت —
 * در محدوده درج‌شده حفظ می‌شوند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneImportScreen(navController: NavController, payload: String) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
    val scheme = MaterialTheme.colorScheme
    val app = context.applicationContext as GeofenceAlertApp
    val scope = rememberCoroutineScope()

    val decoded = remember(payload) { ZoneShareCodec.decode(payload) }
    val draft = decoded.getOrNull()
    var adding by remember { mutableStateOf(false) }
    var addFailed by remember { mutableStateOf(false) }

    /** بازگشت به خانه: اگر home در پشته هست به آن برمی‌گردد، وگرنه می‌سازد. */
    fun goHome() {
        if (!navController.popBackStack("home", false)) {
            navController.navigate("home")
        }
    }

    fun goBack() {
        if (!navController.popBackStack()) {
            goHome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.addZone, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (draft == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    strings.importInvalid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = { goBack() }) {
                    Text(strings.back)
                }
            }
            return@Scaffold
        }

        val color = ZoneColors.zoneColor(draft.colorIndex, scheme.primary)
        val polygonPoints = GeoMath.parsePolygonJson(draft.polygonJson)
        val extentLine = if (draft.polygonJson.isNotBlank() && polygonPoints.isNotEmpty()) {
            // خلاصه چندضلعی موجود: تعداد رأس + مرکز (همان مختصات draft).
            String.format(
                strings.polygonSummary,
                localizedDigits(polygonPoints.size.toString(), lang),
                localizedDigits(String.format(Locale.US, "%.5f", draft.latitude), lang),
                localizedDigits(String.format(Locale.US, "%.5f", draft.longitude), lang)
            )
        } else {
            val radiusText = if (draft.radiusMeters % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f", draft.radiusMeters)
            } else {
                draft.radiusMeters.toString()
            }
            "${localizedDigits(radiusText, lang)} ${strings.meterUnit}"
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, CircleShape)
                        )
                        Text(
                            draft.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        extentLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        "${strings.latitudeFull}: " +
                            localizedDigits(
                                String.format(Locale.US, "%.5f", draft.latitude), lang
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        "${strings.longitudeFull}: " +
                            localizedDigits(
                                String.format(Locale.US, "%.5f", draft.longitude), lang
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            if (addFailed) {
                Text(
                    strings.importFailed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error
                )
            }
            FilledTonalButton(
                onClick = {
                    if (adding) return@FilledTonalButton
                    adding = true
                    addFailed = false
                    scope.launch(Dispatchers.IO) {
                        try {
                            // همان مسیر ساخت repo: upsert با id=0، بعد
                            // ترتیب دستی ته فهرست (مثل مسیر ذخیره) و
                            // تازه‌سازی پشتیبان خودکار.
                            val zone = Zone(
                                name = draft.name,
                                latitude = draft.latitude,
                                longitude = draft.longitude,
                                radiusMeters = draft.radiusMeters,
                                polygonJson = draft.polygonJson,
                                colorIndex = draft.colorIndex,
                                exitBehavior = draft.exitBehavior,
                                alertMode = draft.alertMode,
                                scheduleEnabled = draft.scheduleEnabled,
                                scheduleDays = draft.scheduleDays,
                                scheduleStartMin = draft.scheduleStartMin,
                                scheduleEndMin = draft.scheduleEndMin,
                                silentEnabled = draft.silentEnabled,
                                silentStartMin = draft.silentStartMin,
                                silentEndMin = draft.silentEndMin,
                                isActive = false
                            )
                            val savedId = app.zoneRepo.upsert(zone)
                            if (savedId > 0) {
                                try {
                                    app.zoneRepo.setSortOrder(savedId, savedId.toInt())
                                } catch (_: Exception) {
                                }
                            }
                            try {
                                BackupManager.writeAutoBackup(context)
                            } catch (_: Exception) {
                            }
                            withContext(Dispatchers.Main) { goHome() }
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                adding = false
                                addFailed = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !adding
            ) {
                Text(strings.importAdd, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { goBack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.cancel)
            }
        }
    }
}
