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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
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
import ir.khanoomi.geofencealert.domain.ZoneSort
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

    /**
     * ترتیب دستیِ تازه رها‌شده که هنوز از دیتابیس برنگشته است. تا وقتی
     * انتشار فهرست محدوده‌ها دقیقاً همین ترتیب را نشان ندهد، نمایش بر
     * اساس همین نگه داشته می‌شود تا یک انتشار کهنه، کارت را به جای
     * قبلی‌اش برنگرداند (رفع پرش در پایان درگ).
     */
    private val pendingOrderIds =
        kotlinx.coroutines.flow.MutableStateFlow<List<Long>?>(null)

    /** فهرست نمایشی محدوده‌ها، مرتب‌شده بر اساس حالت انتخاب‌شده. */
    val displayZones = combine(zones, sortMode, pendingOrderIds) { list, mode, pending ->
        if (mode == ZoneSort.MANUAL && pending != null) {
            val byId = list.associateBy { it.id }
            val ordered = pending.mapNotNull { byId[it] }
            val rest = list.filter { it.id !in pending.toSet() }
            ordered + rest
        } else {
            ZoneSort.sortZones(list, mode)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** پاک‌کردن ترتیب معلق وقتی فهرست واقعی دقیقاً همان را نشان می‌دهد. */
    fun confirmPendingOrder(currentIds: List<Long>) {
        val pending = pendingOrderIds.value ?: return
        if (pending == currentIds) pendingOrderIds.value = null
    }

    fun setSortMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                prefs.setZoneSortMode(mode)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "setSortMode($mode) failed", e)
            }
        }
    }

    /** ثبت ترتیب دستی تازه پس از کشیدن و رها کردن کارت‌ها (یک تراکنش). */
    fun reorderZones(orderedIds: List<Long>) {
        // اول ترتیب معلق ثبت می‌شود تا فهرست نمایشی فوراً همان بماند و
        // انتشار کهنه دیتابیس آن را برنگرداند؛ بعد در پس‌زمینه ذخیره می‌شود.
        pendingOrderIds.value = orderedIds
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
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
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
    var permSms by remember { mutableStateOf(false) }
    var permContacts by remember { mutableStateOf(false) }
    var zoneToDelete by remember { mutableStateOf<Zone?>(null) }
    // آیا بهینه‌سازی باتری برای این برنامه فعال است؟ (پایش را در پس‌زمینه خفه می‌کند)
    var batteryRestricted by remember { mutableStateOf(false) }
    // پنل کشویی مرتب‌سازی (زیر دکمه مرتب‌سازی، بالای کارت پایش)
    var sortPanelOpen by remember { mutableStateOf(false) }

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
    // وقتی انتشار واقعی دیتابیس دقیقاً ترتیب رهاشده را نشان داد،
    // نگه‌داری معلق تمام می‌شود.
    LaunchedEffect(zones) {
        vm.confirmPendingOrder(zones.map { it.id })
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
        permSms = Permissions.hasSendSms(context)
        permContacts = Permissions.hasContacts(context)
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
                        Icon(Icons.Filled.DateRange, contentDescription = strings.statistics)
                    }
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Filled.List, contentDescription = strings.history)
                    }
                    Box {
                        IconButton(onClick = { sortPanelOpen = !sortPanelOpen }) {
                            Icon(Icons.Filled.List, contentDescription = strings.sortTitle)
                        }
                        if (ZoneSort.normalize(sortMode) != ZoneSort.MANUAL) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(9.dp)
                                    .background(
                                        MaterialTheme.colorScheme.error,
                                        CircleShape
                                    )
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settingsCd)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        LazyColumn(
            state = listState,
            // هنگام درگ، اسکرول فهرست قفل است تا کارت زیر انگشت نپرد یا
            // فهرست هم‌زمان اسکرول نشود.
            userScrollEnabled = !reorderController.isDragging,
            modifier = Modifier
                .fillMaxSize()
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
                    permSms = permSms,
                    permContacts = permContacts,
                    batteryOk = !batteryRestricted,
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
                if (isManualSort) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                strings.dragHint,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                itemsIndexed(shownZones, key = { _, zone -> zone.id }) { index, zone ->
                    // ورود پلکانی کارت‌ها در نخستین ترکیب: محو + سرخوردن ملایم
                    val entrance = remember(zone.id) { Animatable(0f) }
                    LaunchedEffect(zone.id) {
                        entrance.animateTo(
                            1f,
                            tween(
                                durationMillis = 420,
                                delayMillis = (index % 10) * 55
                            )
                        )
                    }
                    val isDragging = isManualSort &&
                        reorderController.draggingKey == zone.id
                    val cardModifier = if (isManualSort) {
                        Modifier
                            .animateItem()
                            .graphicsLayer {
                                alpha = entrance.value
                                translationY = (1f - entrance.value) * 26.dp.toPx()
                            }
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
                        Modifier
                            .animateItem()
                            .graphicsLayer {
                                alpha = entrance.value
                                translationY = (1f - entrance.value) * 26.dp.toPx()
                            }
                    }
                    ZoneCard(
                        zone = zone,
                        modifier = cardModifier,
                        toggleEnabled = !isManualSort,
                        onToggleActive = { vm.toggleActive(zone) },
                        onEdit = { navController.navigate("edit?zoneId=${zone.id}") },
                        onDelete = { zoneToDelete = zone },
                        onStopAlert = { vm.stopAlert(zone.id) }
                    )
                }
            }
        }

        // پنل مرتب‌سازی: روکش واقعی بالای کارت پایش — با اسکریم محو و
        // پنل شناور زیر هدر؛ محتوای صفحه اصلاً جابه‌جا نمی‌شود.
        AnimatedVisibility(
            visible = sortPanelOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { sortPanelOpen = false }
            )
        }
        AnimatedVisibility(
            visible = sortPanelOpen,
            enter = fadeIn(tween(200)) +
                androidx.compose.animation.slideInVertically(
                    tween(240)
                ) { -it / 3 } +
                androidx.compose.animation.scaleIn(
                    animationSpec = tween(240),
                    initialScale = 0.96f
                ),
            exit = fadeOut(tween(140)) +
                androidx.compose.animation.slideOutVertically(
                    tween(180)
                ) { -it / 3 } +
                androidx.compose.animation.scaleOut(
                    animationSpec = tween(180),
                    targetScale = 0.97f
                ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SortPanel(
                sortMode = sortMode,
                onSelect = { mode ->
                    // انتخاب حالت، پنل را نمی‌بندد؛ بستن با اسکریم یا
                    // دکمه مرتب‌سازی است. لمس ردیف جهت‌دار فعال فقط جهتش
                    // را برعکس می‌کند و پنل باز می‌ماند.
                    vm.setSortMode(mode)
                }
            )
        }
        }
    }

    zoneToDelete?.let { zone ->
        AlertDialog(
            onDismissRequest = { zoneToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(LocalStrings.current.deleteZoneTitle, fontWeight = FontWeight.Bold) },
            text = { Text(String.format(strings.deleteZoneConfirm, zone.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteZone(zone)
                    zoneToDelete = null
                }) { Text(strings.delete, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { zoneToDelete = null }) { Text(strings.cancel) }
            }
        )
    }
}

/** کارت قهرمان بالای صفحه اصلی با گرادیان و رادار متحرک. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RadarHeroCard(
    monitoring: Boolean,
    activeCount: Int,
    alertingCount: Int,
    locationOn: Boolean,
    permLocation: Boolean,
    permBackground: Boolean,
    permNotif: Boolean,
    permSms: Boolean,
    permContacts: Boolean,
    batteryOk: Boolean,
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
    val strings = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val heroInteraction = remember { MutableInteractionSource() }
    val heroPressScale = rememberPressScale(heroInteraction, pressedScale = 0.985f)
    val hasIssue = !monitoring || !locationOn || !permLocation || !permBackground ||
        !permNotif || !permSms || !permContacts || !batteryOk
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
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onMonitoringChange(!monitoring)
        },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = heroPressScale
                scaleY = heroPressScale
            }
            .border(2.dp, shimmerBorder, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        interactionSource = heroInteraction
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val titleColor by animateColorAsState(
                            targetValue = if (monitoring) scheme.onPrimary
                            else scheme.onPrimary.copy(alpha = 0.72f),
                            animationSpec = tween(400),
                            label = "heroTitleColor"
                        )
                        Text(
                            text = if (monitoring) strings.monitoringOn
                            else strings.monitoringOff,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "${localizedDigits(activeCount.toString(), LocalAppLang.current)} ${strings.activeZonesSuffix} • " +
                            "${localizedDigits(alertingCount.toString(), LocalAppLang.current)} ${strings.activeAlertsSuffix}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimary.copy(alpha = 0.85f)
                    )
                    // چیپ‌های وضعیت: مکان، پس‌زمینه، باتری، اعلان،
                    // پیامک، مخاطبین و GPS — نقطه سبز یعنی سالم، قرمز
                    // یعنی نیازمند اقدام.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HeroPermChip(strings.chipLocation, permLocation)
                        HeroPermChip(strings.chipBackground, permBackground)
                        HeroPermChip(strings.chipBattery, batteryOk)
                        HeroPermChip(strings.chipNotif, permNotif)
                        HeroPermChip(strings.permSmsTitle, permSms)
                        HeroPermChip(strings.permContacts, permContacts)
                        HeroPermChip("GPS", locationOn)
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
                RadarWaves(
                    active = monitoring,
                    waveColor = Color.White,
                    idleColor = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(96.dp),
                    coreRadiusDp = 7.dp
                )
            }
        }
    }
}

@Composable
private fun HeroPermChip(label: String, ok: Boolean) {
    val onGradient = MaterialTheme.colorScheme.onPrimary
    val dotColor by animateColorAsState(
        targetValue = if (ok) Color(0xFF7CFC9A) else Color(0xFFFF8A80),
        animationSpec = tween(400),
        label = "chipDot"
    )
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
                    .background(dotColor, CircleShape)
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
    val (targetBg, targetFg, label) = when {
        zone.isAlerting -> Triple(scheme.errorContainer, scheme.onErrorContainer, strings.alertActive)
        !zone.isActive -> Triple(scheme.surfaceVariant, scheme.onSurfaceVariant, strings.inactive)
        else -> Triple(scheme.primaryContainer, scheme.onPrimaryContainer, strings.waitingEntry)
    }
    val bg by animateColorAsState(targetBg, tween(400), label = "pillBg")
    val fg by animateColorAsState(targetFg, tween(400), label = "pillFg")
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
    toggleEnabled: Boolean = true,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStopAlert: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
    val haptics = LocalHapticFeedback.current
    val cardInteraction = remember { MutableInteractionSource() }
    val cardPressScale = rememberPressScale(cardInteraction, pressedScale = 0.98f)
    // مقیاس حلقه‌های مینی‌رادار با لگاریتم شعاع محدوده (مثل قبل)
    val ringScale = remember(zone.radiusMeters) {
        val t = (log10(zone.radiusMeters.toFloat()).coerceIn(1f, 6f) - 1f) / 5f
        0.72f + 0.28f * t
    }
    val borderColor by animateColorAsState(
        targetValue = when {
            zone.isAlerting -> scheme.error
            zone.isActive -> scheme.primary.copy(alpha = 0.55f)
            else -> scheme.outlineVariant
        },
        animationSpec = tween(450),
        label = "zoneBorder"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .graphicsLayer {
                scaleX = cardPressScale
                scaleY = cardPressScale
            }
            .border(1.5.dp, borderColor, MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                // کارت در حال هشدار با لمس خاموش/روشن نمی‌شود؛ در حالت
                // مرتب‌سازی دستی هم لمس برای درگ نگه داشته می‌شود.
                enabled = toggleEnabled && !zone.isAlerting
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleActive()
            },
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
                RadarWaves(
                    active = zone.isActive,
                    waveColor = scheme.primary,
                    idleColor = scheme.outline,
                    modifier = Modifier.size(72.dp),
                    ringScale = ringScale,
                    coreRadiusDp = 6.dp
                )
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
                        TonalChip(formatRadiusLocalized(zone.radiusMeters, strings, lang))
                        TonalChip(
                            when (zone.exitBehavior) {
                                Zone.EXIT_SEQUENCE -> strings.exitPattern
                                Zone.EXIT_CONTINUE -> strings.exitUntilManual
                                else -> strings.exitImmediate
                            }
                        )
                    }
                    StatusPill(zone = zone)
                    if (zone.lastTriggeredAt > 0) {
                        Text(
                            "${strings.lastEntry}: " +
                                formatRelativeTimeLocalized(
                                    strings, lang, zone.lastTriggeredAt
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
            // دکمه‌های ویرایش/حذف: دایره‌ای نرم ۴۲dp در پایین کارت، با
            // انیمیشن فشردن فنری و بازخورد لمسی.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                ZoneActionButton(
                    icon = Icons.Default.Edit,
                    contentDescription = strings.editCd,
                    containerColor = scheme.primaryContainer,
                    iconColor = scheme.onPrimaryContainer,
                    onClick = onEdit
                )
                Spacer(Modifier.width(10.dp))
                ZoneActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = strings.delete,
                    containerColor = scheme.errorContainer,
                    iconColor = scheme.error,
                    onClick = onDelete
                )
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
                    Text(strings.stopAlert, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** دکمهٔ گرد نرم کارت محدوده (ویرایش/حذف) با فشردن فنری و هپتیک. */
@Composable
private fun ZoneActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, pressedScale = 0.86f)
    val bg by animateColorAsState(containerColor, tween(300), label = "zoneBtnBg")
    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** حالت خالی با تصویرسازی رادار. */
@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
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
            Text(strings.createZone)
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

/**
 * پنل مرتب‌سازی (روکش شناور زیر هدر): چهار ردیف (دستی/نام/ایجاد/شعاع).
 * پیکان‌ها یکدست‌اند: جهت اصلی هر ردیف KeyboardArrowDown و جهت برعکس
 * KeyboardArrowUp است. لمس ردیف جهت‌دارِ فعال، جهتش را برعکس می‌کند
 * (و پنل باز می‌ماند)؛ نخستین لمس روی «شعاع» از «بزرگ‌ترین» شروع می‌کند
 * و روی بقیه از جهت پیش‌فرضشان (الفبا/تازه‌ترین).
 */
@Composable
private fun SortPanel(sortMode: String, onSelect: (String) -> Unit) {
    val strings = LocalStrings.current
    val mode = ZoneSort.normalize(sortMode)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SortPanelRow(
                title = strings.sortManual,
                caption = strings.sortManualCaption,
                arrowUp = null,
                selected = mode == ZoneSort.MANUAL,
                onClick = { onSelect(ZoneSort.MANUAL) }
            )
            SortPanelRow(
                title = strings.sortName,
                caption = if (mode == ZoneSort.NAME_DESC) strings.sortNameDesc
                else strings.sortNameAsc,
                arrowUp = mode == ZoneSort.NAME_DESC,
                selected = mode == ZoneSort.NAME_ASC || mode == ZoneSort.NAME_DESC,
                onClick = {
                    onSelect(
                        if (mode == ZoneSort.NAME_ASC) ZoneSort.NAME_DESC
                        else ZoneSort.NAME_ASC
                    )
                }
            )
            SortPanelRow(
                title = strings.sortCreated,
                caption = if (mode == ZoneSort.OLDEST) strings.sortOldest
                else strings.sortNewest,
                arrowUp = mode == ZoneSort.OLDEST,
                selected = mode == ZoneSort.NEWEST || mode == ZoneSort.OLDEST,
                onClick = {
                    onSelect(
                        if (mode == ZoneSort.NEWEST) ZoneSort.OLDEST
                        else ZoneSort.NEWEST
                    )
                }
            )
            SortPanelRow(
                title = strings.radius,
                caption = if (mode == ZoneSort.RADIUS_ASC) strings.sortSmallest
                else strings.sortLargest,
                arrowUp = mode == ZoneSort.RADIUS_ASC,
                selected = mode == ZoneSort.RADIUS_DESC || mode == ZoneSort.RADIUS_ASC,
                onClick = {
                    onSelect(
                        if (mode == ZoneSort.RADIUS_DESC) ZoneSort.RADIUS_ASC
                        else ZoneSort.RADIUS_DESC
                    )
                }
            )
        }
    }
}

/** یک ردیف از پنل مرتب‌سازی: عنوان + زیرنویس حالت + پیکان جهت (اگر جهت‌دار است). */
@Composable
private fun SortPanelRow(
    title: String,
    caption: String,
    arrowUp: Boolean?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val titleColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface
    val captionColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    Surface(
        color = if (selected) scheme.primaryContainer else Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = captionColor
            )
            if (arrowUp != null) {
                Icon(
                    if (arrowUp) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = captionColor
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.onPrimaryContainer
                )
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
                    LocalStrings.current.batteryWarning,
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
                Text(LocalStrings.current.batteryWarningAction, fontWeight = FontWeight.Bold)
            }
        }
    }
}
