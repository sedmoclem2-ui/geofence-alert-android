package ir.khanoomi.geofencealert.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.TimeFormat
import ir.khanoomi.geofencealert.domain.ZoneSort
import ir.khanoomi.geofencealert.domain.formatRadiusFa
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.ui.theme.RedDeep
import ir.khanoomi.geofencealert.ui.theme.mixColor
import ir.khanoomi.geofencealert.util.Permissions
import ir.khanoomi.geofencealert.util.promptEnableGpsIfNeeded
import ir.khanoomi.geofencealert.widget.ZoneWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10

/** اجرای [onResume] هر بار که صفحه به پیش‌زمینه برمی‌گردد. */
@Composable
fun OnResume(onResume: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo get() = (getApplication<Application>() as GeofenceAlertApp).zoneRepo
    private val prefs get() = (getApplication<Application>() as GeofenceAlertApp).prefs

    val zones = repo.observeZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monitoringEnabled = prefs.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val alertingZones = zones.map { list -> list.filter { it.isAlerting } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** حالت مرتب‌سازی فهرست (دستی/نام/تازه‌ترین/شعاع) — ذخیره‌شده در تنظیمات. */
    val sortMode = prefs.zoneSortMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneSort.MANUAL)

    /** فهرست نمایشی محدوده‌ها، مرتب‌شده بر اساس حالت انتخاب‌شده. */
    val displayZones = combine(zones, sortMode) { list, mode ->
        ZoneSort.sortZones(list, mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                prefs.setZoneSortMode(mode)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "setSortMode($mode) failed", e)
            }
        }
    }

    /** ثبت ترتیب دستی تازه پس از کشیدن و رها کردن کارت‌ها. */
    fun reorderZones(orderedIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.updateSortOrders(orderedIds)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "reorderZones failed", e)
            }
        }
    }

    init {
        // با هر تغییر محدوده‌ها، شمارنده ویجت به‌روز شود
        viewModelScope.launch(Dispatchers.IO) {
            zones.collect { list ->
                try {
                    prefs.setWidgetActiveCount(list.count { it.isActive })
                } catch (_: Exception) {
                }
                ZoneWidgetProvider.updateAll(getApplication())
                // تازه‌سازی پشتیبان خودکار بعد از هر تغییر مجموعه محدوده‌ها
                ir.khanoomi.geofencealert.data.BackupManager
                    .writeAutoBackup(getApplication())
            }
        }
    }

    fun toggleActive(zone: Zone) {
        viewModelScope.launch(Dispatchers.IO) {
            // مسیر تغییر وضعیت هرگز نباید برنامه را بکشد: هر خطای پلتفرمی
            // (دیتابیس، ژئوفنس، سرویس) اینجا گرفته و فقط ثبت می‌شود.
            try {
                // B1: اگر هنگام خاموش کردن محدوده، هشدارش فعال است، اول صدا را قطع کن.
                if (zone.isAlerting) {
                    AlertController.stopAlert(getApplication(), zone.id, manual = true)
                }
                repo.upsert(
                    zone.copy(
                        isActive = !zone.isActive,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                GeofenceHelper.refreshRegistrations(getApplication())
            } catch (e: Exception) {
                Log.e("HomeViewModel", "toggleActive failed for zone ${zone.id}", e)
            }
        }
    }

    fun deleteZone(zone: Zone) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (zone.isAlerting) AlertController.stopAlert(getApplication(), zone.id, manual = true)
                repo.delete(zone)
                GeofenceHelper.refreshRegistrations(getApplication())
            } catch (e: Exception) {
                Log.e("HomeViewModel", "deleteZone failed for zone ${zone.id}", e)
            }
        }
    }

    fun setMonitoring(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                prefs.setMonitoringEnabled(enabled)
                GeofenceHelper.refreshRegistrations(getApplication())
                ZoneWidgetProvider.updateAll(getApplication())
            } catch (e: Exception) {
                Log.e("HomeViewModel", "setMonitoring($enabled) failed", e)
            }
        }
    }

    fun stopAlert(zoneId: Long) {
        AlertController.stopAlert(getApplication(), zoneId, manual = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as GeofenceAlertApp
    val vm: HomeViewModel = viewModel { HomeViewModel(app) }

    val zones by vm.zones.collectAsStateWithLifecycle()
    val displayZones by vm.displayZones.collectAsStateWithLifecycle()
    val sortMode by vm.sortMode.collectAsStateWithLifecycle()
    val monitoring by vm.monitoringEnabled.collectAsStateWithLifecycle()
    val alerting by vm.alertingZones.collectAsStateWithLifecycle()

    var locationOn by remember { mutableStateOf(true) }
    var permLocation by remember { mutableStateOf(false) }
    var permBackground by remember { mutableStateOf(false) }
    var permNotif by remember { mutableStateOf(false) }
    var zoneToDelete by remember { mutableStateOf<Zone?>(null) }
    // آیا بهینه‌سازی باتری برای این برنامه فعال است؟ (پایش را در پس‌زمینه خفه می‌کند)
    var batteryRestricted by remember { mutableStateOf(false) }

    // ---- جابه‌جایی با کشیدن در حالت «دستی» ----
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    var localZones by remember { mutableStateOf<List<Zone>>(emptyList()) }
    val isManualSort = sortMode == ZoneSort.MANUAL
    val reorderController = remember {
        ZoneReorderController(
            listState = listState,
            currentList = { localZones },
            onSwap = { from, to ->
                val m = localZones.toMutableList()
                if (from in m.indices && to in m.indices) {
                    m.add(to, m.removeAt(from))
                    localZones = m
                }
            },
            onDragStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
            onDragEnd = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.reorderZones(localZones.map { it.id })
            }
        )
    }
    // آینه محلی فقط وقتی با داده واقعی همگام می‌شود که درگ در جریان نیست
    // تا وسط کشیدن، فهرست زیر دست کاربر جابه‌جا نشود.
    LaunchedEffect(displayZones, sortMode, reorderController.isDragging) {
        if (isManualSort && !reorderController.isDragging) {
            localZones = displayZones
        }
    }
    val shownZones = if (isManualSort) localZones else displayZones

    // لانچر دیالوگ سیستمی روشن کردن GPS (هنگام فعال‌سازی پایش)
    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    fun refreshStatus() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationOn = try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            true
        }
        permLocation = Permissions.hasAnyLocation(context)
        permBackground = Permissions.hasBackgroundLocation(context)
        permNotif = Permissions.hasNotifications(context)
        batteryRestricted = try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName) == false
        } catch (_: Exception) {
            false
        }
    }

    OnResume { refreshStatus() }
    LaunchedEffect(Unit) {
        refreshStatus()
        withContext(Dispatchers.IO) { GeofenceHelper.refreshRegistrations(context) }
    }

    val activeCount = zones.count { it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalStrings.current.appTitle, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("stats") }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "آمار")
                    }
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Filled.List, contentDescription = "تاریخچه")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                    }
                }
            )
        },
        floatingActionButton = {
            val scheme = MaterialTheme.colorScheme
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("edit") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(LocalStrings.current.addZone) },
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary)),
                    CircleShape
                ),
                containerColor = Color.Transparent,
                contentColor = scheme.onPrimary,
                shape = CircleShape
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RadarHeroCard(
                    monitoring = monitoring,
                    activeCount = activeCount,
                    alertingCount = alerting.size,
                    locationOn = locationOn,
                    permLocation = permLocation,
                    permBackground = permBackground,
                    permNotif = permNotif,
                    // روشن شدن پایش → دیالوگ سیستمی روشن کردن GPS؛
                    // خاموش شدن پایش هیچ کاری با GPS نمی‌کند.
                    onMonitoringChange = { enabled ->
                        vm.setMonitoring(enabled)
                        if (enabled) {
                            (context as? Activity)?.let { activity ->
                                promptEnableGpsIfNeeded(activity, gpsLauncher)
                            }
                        }
                    },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }

            // هشدار بهینه‌سازی باتری: وقتی پایش روشن است اما سیستم برنامه را
            // در پس‌زمینه خفه می‌کند، هشدارها ممکن است دیر برسند یا نرسند.
            if (monitoring && batteryRestricted) {
                item {
                    BatteryWarningBanner(
                        onDisableOptimization = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:" + context.packageName)
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        }
                    )
                }
            }

            item {
                AnimatedVisibility(visible = alerting.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        alerting.forEach { zone ->
                            AlertBanner(
                                zoneName = zone.name,
                                onStop = { vm.stopAlert(zone.id) }
                            )
                        }
                    }
                }
            }

            if (zones.isEmpty()) {
                item { EmptyState(onAdd = { navController.navigate("edit") }) }
            } else {
                item {
                    ZoneSortBar(
                        sortMode = sortMode,
                        onSelect = { vm.setSortMode(it) }
                    )
                }
                if (isManualSort) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                "برای جابه‌جایی، کارت را نگه دار و بکش",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                items(shownZones, key = { it.id }) { zone ->
                    val isDragging = isManualSort &&
                        reorderController.draggingKey == zone.id
                    val cardModifier = if (isManualSort) {
                        Modifier
                            .animateItem()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) {
                                    reorderController.dragOffsetY
                                } else {
                                    0f
                                }
                                scaleX = if (isDragging) 1.03f else 1f
                                scaleY = if (isDragging) 1.03f else 1f
                            }
                            .shadow(
                                elevation = if (isDragging) 16.dp else 0.dp,
                                shape = MaterialTheme.shapes.medium
                            )
                            .pointerInput(zone.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        reorderController.onDragStart(zone.id)
                                    },
                                    onDragEnd = { reorderController.onDragEnd() },
                                    onDragCancel = { reorderController.onDragEnd() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        reorderController.onDrag(dragAmount.y)
                                    }
                                )
                            }
                    } else {
                        Modifier.animateItem()
                    }
                    ZoneCard(
                        zone = zone,
                        modifier = cardModifier,
                        onToggleActive = { vm.toggleActive(zone) },
                        onEdit = { navController.navigate("edit?zoneId=${zone.id}") },
                        onDelete = { zoneToDelete = zone },
                        onStopAlert = { vm.stopAlert(zone.id) }
                    )
                }
            }
        }
    }

    zoneToDelete?.let { zone ->
        AlertDialog(
            onDismissRequest = { zoneToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(LocalStrings.current.deleteZoneTitle, fontWeight = FontWeight.Bold) },
            text = { Text("«${zone.name}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteZone(zone)
                    zoneToDelete = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { zoneToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

/** پالس رادار: سه دایره متحدالمرکز با انیمیشن بی‌پایان. */
@Composable
private fun RadarPulse(
    modifier: Modifier = Modifier,
    pulseColor: Color,
    dotColor: Color
) {
    val infinite = rememberInfiniteTransition(label = "radar")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarProgress"
    )
    Canvas(modifier = modifier) {
        val maxR = size.minDimension / 2f
        for (i in 0 until 3) {
            val f = (progress + i / 3f) % 1f
            drawCircle(
                color = pulseColor.copy(alpha = 0.5f * (1f - f)),
                radius = maxR * (0.55f + 0.45f * f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        drawCircle(color = dotColor, radius = 7.dp.toPx())
    }
}

/** کارت قهرمان بالای صفحه اصلی با گرادیان و رادار متحرک. */
@Composable
private fun RadarHeroCard(
    monitoring: Boolean,
    activeCount: Int,
    alertingCount: Int,
    locationOn: Boolean,
    permLocation: Boolean,
    permBackground: Boolean,
    permNotif: Boolean,
    onMonitoringChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    // گرادیان از تم فعال ساخته می‌شود (دیگر همیشه سبز نیست):
    // بالای کارت تیره‌تر از رنگ اصلی و پایین آن آمیخته با رنگ مکمل (لهجه).
    val scheme = MaterialTheme.colorScheme
    val gradient = Brush.verticalGradient(
        listOf(
            mixColor(scheme.primary, Color.Black, 0.25f),
            scheme.primary,
            mixColor(scheme.primary, scheme.secondary, 0.45f)
        )
    )
    val hasIssue = !monitoring || !locationOn || !permLocation || !permBackground || !permNotif
    // درخشش متحرک روی لبه کارت: حس آینده‌نگرانه، با هزینه کم (فقط همین کارت بازسازی می‌شود)
    val shimmerTransition = rememberInfiniteTransition(label = "heroShimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    val shimmerBorder = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.75f),
            Color.Transparent
        ),
        start = Offset(shimmerX * 900f, 0f),
        end = Offset(shimmerX * 900f + 320f, 420f)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(2.dp, shimmerBorder, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (monitoring) LocalStrings.current.monitoringOn
                            else LocalStrings.current.monitoringOff,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = monitoring,
                            onCheckedChange = onMonitoringChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = scheme.onPrimary,
                                checkedTrackColor = scheme.onPrimary.copy(alpha = 0.35f),
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = scheme.onPrimary.copy(alpha = 0.75f),
                                uncheckedTrackColor = scheme.onPrimary.copy(alpha = 0.18f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    Text(
                        "$activeCount ${LocalStrings.current.activeZonesSuffix} • " +
                            "$alertingCount ${LocalStrings.current.activeAlertsSuffix}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HeroPermChip("مکان", permLocation)
                            HeroPermChip("پس‌زمینه", permBackground)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HeroPermChip("اعلان", permNotif)
                            HeroPermChip("GPS", locationOn)
                        }
                    }
                    if (hasIssue) {
                        TextButton(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.textButtonColors(contentColor = scheme.onPrimary),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(LocalStrings.current.fixIssue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                RadarPulse(
                    modifier = Modifier.size(96.dp),
                    pulseColor = scheme.secondary,
                    dotColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun HeroPermChip(label: String, ok: Boolean) {
    val onGradient = MaterialTheme.colorScheme.onPrimary
    Surface(
        color = onGradient.copy(alpha = 0.16f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (ok) Color(0xFF7CFC9A) else Color(0xFFFF8A80),
                        CircleShape
                    )
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = onGradient
            )
        }
    }
}

/** بنر هشدار فعال با گرادیان قرمز و نقطه چشمک‌زن. */
@Composable
private fun AlertBanner(zoneName: String, onStop: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "alertDot")
    val dotAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(scheme.error, RedDeep)))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.White.copy(alpha = dotAlpha), CircleShape)
                    )
                    Text(
                        "${LocalStrings.current.alertActive}: $zoneName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = scheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(LocalStrings.current.stopAlert, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** مینی‌رادار کارت محدوده؛ شعاع دایره‌ها با لگاریتم شعاع محدوده مقیاس می‌شود. */
@Composable
private fun RadarMini(radiusMeters: Double, active: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val ringBase = if (active) scheme.primary else scheme.outline
    val dot = if (active) scheme.primary else scheme.outline
    Canvas(modifier = modifier.size(72.dp)) {
        val t = (log10(radiusMeters.toFloat()).coerceIn(1f, 6f) - 1f) / 5f
        val maxR = size.minDimension / 2f
        listOf(1f, 0.72f, 0.46f).forEachIndexed { i, frac ->
            drawCircle(
                color = ringBase.copy(alpha = 0.55f - i * 0.12f),
                radius = maxR * frac * (0.72f + 0.28f * t),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        drawCircle(color = dot, radius = 6.dp.toPx())
    }
}

@Composable
private fun TonalChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun StatusPill(zone: Zone) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val (bg, fg, label) = when {
        zone.isAlerting -> Triple(scheme.errorContainer, scheme.onErrorContainer, strings.alertActive)
        !zone.isActive -> Triple(scheme.surfaceVariant, scheme.onSurfaceVariant, strings.inactive)
        else -> Triple(scheme.primaryContainer, scheme.onPrimaryContainer, strings.waitingEntry)
    }
    Surface(color = bg, shape = CircleShape) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ZoneCard(
    zone: Zone,
    modifier: Modifier = Modifier,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStopAlert: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        color = scheme.surface
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadarMini(radiusMeters = zone.radiusMeters, active = zone.isActive)
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        zone.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TonalChip(formatRadiusFa(zone.radiusMeters))
                        TonalChip(
                            when (zone.exitBehavior) {
                                Zone.EXIT_SEQUENCE -> "الگوی تکراری"
                                Zone.EXIT_CONTINUE -> "تا توقف دستی"
                                else -> "توقف فوری"
                            }
                        )
                    }
                    StatusPill(zone = zone)
                    if (zone.lastTriggeredAt > 0) {
                        Text(
                            "${LocalStrings.current.lastEntry}: " +
                                TimeFormat.formatRelativeTimeFa(zone.lastTriggeredAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Switch(checked = zone.isActive, onCheckedChange = { onToggleActive() })
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = scheme.error
                            )
                        }
                    }
                }
            }
            if (zone.isAlerting) {
                Button(
                    onClick = onStopAlert,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.secondary,
                        contentColor = scheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(LocalStrings.current.stopAlert, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** حالت خالی با تصویرسازی رادار. */
@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val maxR = size.minDimension / 2f
            listOf(1f, 0.68f, 0.42f).forEach { f ->
                drawCircle(color = scheme.primaryContainer, radius = maxR * f)
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.55f),
                    radius = maxR * f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            // سنجاق مکان
            val c = center
            val pinPath = Path().apply {
                moveTo(c.x - 11.dp.toPx(), c.y - 4.dp.toPx())
                lineTo(c.x + 11.dp.toPx(), c.y - 4.dp.toPx())
                lineTo(c.x, c.y + 16.dp.toPx())
                close()
            }
            drawCircle(color = scheme.primary, radius = 13.dp.toPx(), center = c + Offset(0f, -12.dp.toPx()))
            drawPath(path = pinPath, color = scheme.primary)
            drawCircle(
                color = scheme.onPrimary,
                radius = 5.dp.toPx(),
                center = c + Offset(0f, -12.dp.toPx())
            )
        }
        Text(
            LocalStrings.current.noZonesYet,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            LocalStrings.current.noZonesYetSub,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        GradientButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("ساخت محدوده")
        }
    }
}

/**
 * نگهدارنده وضعیت کشیدن و جابه‌جایی کارت‌ها در حالت مرتب‌سازی «دستی».
 *
 * الگوی استاندارد بدون کتابخانه: با لمس طولانی، کارت بلند می‌شود؛ هنگام
 * کشیدن، هر وقت مرکز کارت از مرز آیتم همسایه رد شد جابه‌جایی (swap) روی
 * فهرست آینه‌ای اعمال می‌شود و [dragOffsetY] به‌اندازه اختلاف جایگاه دو
 * آیتم اصلاح می‌شود تا کارت زیر انگشت نپرد. در پایان، ترتیب تازه از راه
 * [onDragEnd] به لایه داده سپرده می‌شود.
 */
private class ZoneReorderController(
    private val listState: LazyListState,
    private val currentList: () -> List<Zone>,
    private val onSwap: (fromIndex: Int, toIndex: Int) -> Unit,
    private val onDragStart: () -> Unit,
    private val onDragEnd: () -> Unit
) {
    /** شناسه محدوده در حال کشیده شدن؛ null یعنی درگی در جریان نیست. */
    var draggingKey by mutableStateOf<Long?>(null)
        private set

    /** جابه‌جایی عمودی کارت در حال کشیده شدن نسبت به جایگاه چیدمانش. */
    var dragOffsetY by mutableStateOf(0f)
        private set

    val isDragging: Boolean get() = draggingKey != null

    private var draggingIndex = -1

    fun onDragStart(key: Long) {
        // با شناسه محدوده شروع می‌کنیم نه اندیس: اندیسِ گرفته‌شده در
        // pointerInput ممکن است پس از جابه‌جایی‌ها کهنه شود، شناسه هرگز.
        val list = currentList()
        val index = list.indexOfFirst { it.id == key }
        if (index < 0) return
        draggingIndex = index
        draggingKey = key
        dragOffsetY = 0f
        onDragStart()
    }

    fun onDrag(deltaY: Float) {
        val key = draggingKey ?: return
        dragOffsetY += deltaY
        val info = listState.layoutInfo
        val draggingItem = info.visibleItemsInfo
            .firstOrNull { it.key == key } ?: return
        val list = currentList()
        val currentIndex = list.indexOfFirst { it.id == key }
        if (currentIndex < 0) return
        val draggedCenter =
            draggingItem.offset + dragOffsetY + draggingItem.size / 2f
        val target = info.visibleItemsInfo.firstOrNull { item ->
            item.key != key &&
                draggedCenter.toInt() in item.offset until (item.offset + item.size)
        } ?: return
        val targetIndex = list.indexOfFirst { it.id == target.key }
        if (targetIndex < 0 || targetIndex == currentIndex) return
        onSwap(currentIndex, targetIndex)
        // جبران پرش: کارت به جایگاه همسایه می‌نشیند، پس آفست انگشت
        // به‌اندازه اختلاف جایگاه دو آیتم اصلاح می‌شود.
        dragOffsetY += (draggingItem.offset - target.offset).toFloat()
        draggingIndex = targetIndex
    }

    fun onDragEnd() {
        if (draggingKey == null) return
        draggingKey = null
        draggingIndex = -1
        dragOffsetY = 0f
        onDragEnd()
    }
}

/** نوار مرتب‌سازی فهرست محدوده‌ها: چهار کپسول انتخابی با انیمیشن رنگ. */
@Composable
private fun ZoneSortBar(sortMode: String, onSelect: (String) -> Unit) {
    val options = listOf(
        ZoneSort.MANUAL to "دستی",
        ZoneSort.NAME to "نام",
        ZoneSort.NEWEST to "تازه‌ترین",
        ZoneSort.RADIUS to "کوچک‌ترین شعاع"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "مرتب‌سازی",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (mode, label) ->
                val selected = sortMode == mode
                val bg by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    label = "sortChipBg"
                )
                val fg by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "sortChipFg"
                )
                Surface(
                    color = bg,
                    shape = CircleShape,
                    modifier = Modifier.clickable { onSelect(mode) }
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * بنر هشدار بهینه‌سازی باتری: وقتی پایش روشن است اما سیستم اجازه معافیت
 * باتری نداده، با یک دکمه کاربر مستقیم به صفحه معافیت همان برنامه می‌رود.
 */
@Composable
private fun BatteryWarningBanner(onDisableOptimization: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = scheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = scheme.onErrorContainer
                )
                Text(
                    "بهینه‌سازی باتری فعال است؛ ممکن است هشدارها با تأخیر برسند یا متوقف شوند.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onErrorContainer
                )
            }
            Button(
                onClick = onDisableOptimization,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.error,
                    contentColor = scheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("غیرفعال کردن بهینه‌سازی", fontWeight = FontWeight.Bold)
            }
        }
    }
}
