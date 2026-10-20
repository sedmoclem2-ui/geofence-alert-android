package ir.khanoomi.geofencealert.ui

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.audio.AlertPlayer
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.RadiusUnit
import ir.khanoomi.geofencealert.domain.CheckInterval
import ir.khanoomi.geofencealert.domain.ExitSequenceValidator
import ir.khanoomi.geofencealert.domain.ZoneValidator
import ir.khanoomi.geofencealert.domain.toMeters
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.util.LocationUtil
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ZoneEditViewModel(app: Application, private val zoneId: Long) : AndroidViewModel(app) {
    private val repo get() = (getApplication<Application>() as GeofenceAlertApp).zoneRepo

    var name by mutableStateOf("")
    var latText by mutableStateOf("")
    var lngText by mutableStateOf("")
    var radiusText by mutableStateOf("200")
    var unit by mutableStateOf(RadiusUnit.METER)
    /** رفتار پس از خروج از محدوده: [Zone.EXIT_STOP]، [Zone.EXIT_SEQUENCE] یا [Zone.EXIT_CONTINUE] */
    var exitBehavior by mutableStateOf(Zone.EXIT_STOP)
    /** ورودی‌های الگوی تکراری پس از خروج (فقط وقتی exitBehavior == EXIT_SEQUENCE) */
    var postExitCountText by mutableStateOf("3")
    var postExitDurationText by mutableStateOf("10")
    var postExitGapText by mutableStateOf("30")
    var soundKey by mutableStateOf(Zone.SOUND_ALARM)
    var customSoundUri by mutableStateOf<String?>(null)
    var isActive by mutableStateOf(true)
    /** فاصله بررسی دقیق به‌صورت متنی (ثانیه یا دقیقه)؛ فقط برای شعاع < ۲۰۰ متر */
    var checkIntervalText by mutableStateOf("20")
    var checkIntervalIsMinutes by mutableStateOf(false)
    /** حالت «مداوم»: بررسی تقریباً پیوسته (checkIntervalSec = 0) */
    var checkIntervalContinuous by mutableStateOf(false)
    // زمان‌بندی: ۱=شنبه … ۷=جمعه
    var scheduleEnabled by mutableStateOf(false)
    var scheduleDays by mutableStateOf("1,2,3,4,5,6,7")
    var scheduleStartMin by mutableStateOf(0)
    var scheduleEndMin by mutableStateOf(1439)

    var existing: Zone? by mutableStateOf(null)
        private set
    var wasActive = false
        private set
    var loaded by mutableStateOf(false)
        private set

    init {
        if (zoneId != -1L) {
            viewModelScope.launch(Dispatchers.IO) {
                val z = try {
                    repo.getById(zoneId)
                } catch (_: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    if (z != null) {
                        existing = z
                        wasActive = z.isActive
                        name = z.name
                        latText = coordText(z.latitude)
                        lngText = coordText(z.longitude)
                        radiusText = coordText(z.radiusMeters)
                        unit = RadiusUnit.METER // شعاع ذخیره‌شده همیشه به متر است
                        exitBehavior = z.exitBehavior
                        postExitCountText = z.postExitCount.toString()
                        postExitDurationText = z.postExitDurationSec.toString()
                        postExitGapText = z.postExitGapSec.toString()
                        soundKey = z.soundKey
                        customSoundUri = z.customSoundUri
                        scheduleEnabled = z.scheduleEnabled
                        scheduleDays = z.scheduleDays
                        scheduleStartMin = z.scheduleStartMin
                        scheduleEndMin = z.scheduleEndMin
                        isActive = z.isActive
                        // نمایش فاصله بررسی ذخیره‌شده: «مداوم» برای ۰؛ دقیقه اگر مضرب ۶۰ و دست‌کم ۶۰ ثانیه
                        if (z.checkIntervalSec == CheckInterval.CONTINUOUS) {
                            checkIntervalContinuous = true
                            checkIntervalText = "20"
                            checkIntervalIsMinutes = false
                        } else if (z.checkIntervalSec >= 60 && z.checkIntervalSec % 60 == 0) {
                            checkIntervalText = (z.checkIntervalSec / 60).toString()
                            checkIntervalIsMinutes = true
                        } else {
                            checkIntervalText = z.checkIntervalSec.toString()
                            checkIntervalIsMinutes = false
                        }
                    }
                    loaded = true
                }
            }
        } else {
            loaded = true
        }
    }

    private fun coordText(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    /** اعمال مختصات انتخاب‌شده روی نقشه. */
    fun applyPickedLocation(lat: Double, lng: Double) {
        latText = coordText(lat)
        lngText = coordText(lng)
    }

    /** مجموعه روزهای فعال زمان‌بندی (۱=شنبه … ۷=جمعه). */
    fun scheduleDaysSet(): Set<Int> =
        scheduleDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    fun toggleScheduleDay(day: Int) {
        val set = scheduleDaysSet().toMutableSet()
        if (day in set) set.remove(day) else set.add(day)
        scheduleDays = set.sorted().joinToString(",")
    }
}

private val SOUND_OPTIONS = listOf(
    Zone.SOUND_ALARM to "آژیر",
    Zone.SOUND_RINGTONE to "زنگ",
    Zone.SOUND_NOTIFICATION to "اعلان"
)

/** کارت بخش‌بندی فرم با آیکون و عنوان. */
@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

/**
 * ردیف گزینه انتخابی شیک (کپسولی ۲۰dp):
 * انتخاب‌شده = پس‌زمینه گرادیانی تم (primary → secondary) + متن سفید پررنگ + تیک؛
 * انتخاب‌نشده = سطح نرم تم + حاشیه ظریف. رنگ‌ها متحرک‌اند و از تم فعال می‌آیند.
 */
@Composable
private fun PrettyOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val gradStart by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.surfaceVariant,
        label = "optStart"
    )
    val gradEnd by animateColorAsState(
        targetValue = if (selected) scheme.secondary else scheme.surfaceVariant,
        label = "optEnd"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else scheme.onSurface,
        label = "optContent"
    )
    val subColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.85f) else scheme.onSurfaceVariant,
        label = "optSub"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
            .border(
                BorderStroke(1.dp, if (selected) Color.Transparent else scheme.outlineVariant),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White
            )
        } else {
            RadioButton(selected = false, onClick = onClick)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                softWrap = true
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subColor,
                    softWrap = true
                )
            }
        }
    }
}

/**
 * کنترل سگمنتی کپسولی شیک: سگمنت انتخاب‌شده گرادیانی (primary → secondary)
 * با متن سفید پررنگ و تیک؛ بقیه روی سطح نرم تم قرار می‌گیرند.
 */
@Composable
private fun PrettySegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceVariant)
            .border(
                BorderStroke(1.dp, scheme.outlineVariant),
                RoundedCornerShape(20.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val sel = index == selectedIndex
            val gradStart by animateColorAsState(
                targetValue = if (sel) scheme.primary else Color.Transparent,
                label = "segStart$index"
            )
            val gradEnd by animateColorAsState(
                targetValue = if (sel) scheme.secondary else Color.Transparent,
                label = "segEnd$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (sel) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        label,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel) Color.White else scheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneEditScreen(navController: NavController, zoneId: Long) {
    val context = LocalContext.current
    val app = context.applicationContext as GeofenceAlertApp
    val vm: ZoneEditViewModel = viewModel { ZoneEditViewModel(app, zoneId) }
    val scope = rememberCoroutineScope()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var errors by remember { mutableStateOf(listOf<String>()) }
    var locLoading by remember { mutableStateOf(false) }
    var locError by remember { mutableStateOf<String?>(null) }
    var pasteError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun doFetchLocation() {
        locError = null
        locLoading = true
        scope.launch(Dispatchers.IO) {
            val loc = LocationUtil.getCurrentLocation(context)
            withContext(Dispatchers.Main) {
                locLoading = false
                if (loc != null) {
                    vm.latText = loc.latitude.toString()
                    vm.lngText = loc.longitude.toString()
                } else {
                    locError = "موقعیت یافت نشد؛ از روشن بودن مکان دستگاه مطمئن شوید."
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            doFetchLocation()
        } else {
            locError = "برای دریافت موقعیت فعلی، مجوز مکان لازم است."
        }
    }

    // انتخاب فایل صوتی دلخواه از حافظه گوشی
    val customSoundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            vm.customSoundUri = uri.toString()
        }
    }

    // دریافت مختصات برگشتی از صفحه انتخاب روی نقشه
    val mapResultHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(mapResultHandle) {
        mapResultHandle?.getStateFlow("picked_lat", Double.NaN)?.collect { lat ->
            if (!lat.isNaN()) {
                val lng = mapResultHandle.get<Double>("picked_lng") ?: Double.NaN
                if (!lng.isNaN()) {
                    vm.applyPickedLocation(lat, lng)
                    pasteError = null
                }
                mapResultHandle.remove<Double>("picked_lat")
                mapResultHandle.remove<Double>("picked_lng")
            }
        }
    }

    /** نام نمایشی فایل صوتی انتخاب‌شده. */
    fun customSoundName(uriString: String): String {
        return try {
            context.contentResolver.query(
                Uri.parse(uriString),
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "صدای انتخاب‌شده"
        } catch (_: Exception) {
            "صدای انتخاب‌شده"
        }
    }

    fun doPaste() {
        pasteError = null
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
        if (text.isNullOrEmpty()) {
            pasteError = "حافظه موقت خالی است."
            return
        }
        val parts = text.split(",").map { it.trim() }
        if (parts.size != 2) {
            pasteError = "قالب باید «عرض،طول» باشد؛ مثلاً 35.7,51.4"
            return
        }
        val lat = ZoneValidator.parseDoubleFa(parts[0])
        val lng = ZoneValidator.parseDoubleFa(parts[1])
        if (lat == null || lng == null) {
            pasteError = "مختصات الصاق‌شده معتبر نیست."
            return
        }
        vm.latText = parts[0]
        vm.lngText = parts[1]
    }

    fun doSave() {
        val lat = ZoneValidator.parseDoubleFa(vm.latText)
        val lng = ZoneValidator.parseDoubleFa(vm.lngText)
        val radius = ZoneValidator.parseDoubleFa(vm.radiusText)
        val validation = ZoneValidator.validate(vm.name, lat, lng, radius, vm.unit)
        val allErrors = validation.errors.toMutableList()
        // فاصله بررسی دقیق فقط وقتی معنا دارد که شعاع کمتر از ۲۰۰ متر باشد
        val radiusMetersForCheck = radius?.let { toMeters(it, vm.unit) }
        val checkIntervalSec: Int =
            if (radiusMetersForCheck != null && radiusMetersForCheck < 200.0) {
                if (vm.checkIntervalContinuous) {
                    CheckInterval.CONTINUOUS
                } else {
                    val sec = CheckInterval.toSeconds(vm.checkIntervalText, vm.checkIntervalIsMinutes)
                    CheckInterval.errorFa(sec)?.let { allErrors += it }
                    sec ?: 20
                }
            } else {
                20
            }
        // اعتبارسنجی الگوی تکراری پس از خروج
        if (vm.exitBehavior == Zone.EXIT_SEQUENCE) {
            ExitSequenceValidator.validateCount(vm.postExitCountText)?.let { allErrors += it }
            ExitSequenceValidator.validateDuration(vm.postExitDurationText)?.let { allErrors += it }
            ExitSequenceValidator.validateGap(vm.postExitGapText)?.let { allErrors += it }
        }
        saving = true
        scope.launch(Dispatchers.IO) {
            try {
                val needsCountCheck = vm.isActive && (vm.existing == null || !vm.wasActive)
                if (needsCountCheck && app.zoneRepo.countActive() >= GeofenceHelper.MAX_GEOFENCES) {
                    allErrors += "حداکثر ۱۰۰ محدوده فعال پشتیبانی می‌شود."
                }
            } catch (e: Exception) {
                allErrors += "خطا در ذخیره‌سازی رخ داد."
            }
            withContext(Dispatchers.Main) {
                errors = allErrors
                if (allErrors.isEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        val zone = Zone(
                            id = vm.existing?.id ?: 0,
                            name = vm.name.trim(),
                            latitude = lat!!,
                            longitude = lng!!,
                            radiusMeters = toMeters(radius!!, vm.unit),
                            isActive = vm.isActive,
                            soundKey = vm.soundKey,
                            customSoundUri = vm.customSoundUri,
                            scheduleEnabled = vm.scheduleEnabled,
                            scheduleDays = vm.scheduleDays.ifBlank { "1,2,3,4,5,6,7" },
                            scheduleStartMin = vm.scheduleStartMin,
                            scheduleEndMin = vm.scheduleEndMin,
                            checkIntervalSec = checkIntervalSec,
                            alertAcknowledged = vm.existing?.alertAcknowledged ?: false,
                            exitBehavior = vm.exitBehavior,
                            postExitCount = ZoneValidator.parseDoubleFa(vm.postExitCountText)
                                ?.toInt()?.coerceIn(1, 20) ?: 3,
                            postExitDurationSec = ZoneValidator.parseDoubleFa(vm.postExitDurationText)
                                ?.toInt()?.coerceIn(3, 120) ?: 10,
                            postExitGapSec = ZoneValidator.parseDoubleFa(vm.postExitGapText)
                                ?.toInt()?.coerceIn(0, 600) ?: 30,
                            isAlerting = vm.existing?.isAlerting ?: false,
                            lastTriggeredAt = vm.existing?.lastTriggeredAt ?: 0,
                            updatedAt = System.currentTimeMillis()
                        )
                        try {
                            app.zoneRepo.upsert(zone)
                            GeofenceHelper.refreshRegistrations(context)
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                saving = false
                                errors = listOf("خطا در ذخیره‌سازی رخ داد.")
                            }
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            saving = false
                            navController.popBackStack()
                        }
                    }
                } else {
                    saving = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (zoneId == -1L) "محدوده جدید" else "ویرایش محدوده") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                GradientButton(
                    onClick = { doSave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !saving
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("ذخیره محدوده", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (!vm.loaded) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(icon = Icons.Filled.Info, title = "مشخصات") {
                OutlinedTextField(
                    value = vm.name,
                    onValueChange = { vm.name = it },
                    label = { Text("نام محدوده (مثلاً خانه)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            SectionCard(icon = Icons.Filled.LocationOn, title = "مختصات") {
                OutlinedTextField(
                    value = vm.latText,
                    onValueChange = { input ->
                        // اگر کاربر یک جفت «عرض،طول» (مثل خروجی گوگل‌مپ) را الصاق کرد، خودکار جدا شود
                        val parts = input.split(",").map { it.trim() }
                        if (parts.size == 2 && parts.all { ZoneValidator.parseDoubleFa(it) != null }) {
                            vm.latText = parts[0]
                            vm.lngText = parts[1]
                            pasteError = null
                        } else {
                            vm.latText = input
                        }
                    },
                    label = { Text("عرض جغرافیایی") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = vm.lngText,
                    onValueChange = { vm.lngText = it },
                    label = { Text("طول جغرافیایی") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (Permissions.hasAnyLocation(context)) doFetchLocation()
                            else locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !locLoading
                    ) {
                        if (locLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("موقعیت فعلی")
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "مختصات",
                                    "${vm.latText.trim()},${vm.lngText.trim()}"
                                )
                            )
                            Toast.makeText(context, "مختصات کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("کپی مختصات") }
                    FilledTonalButton(
                        onClick = { doPaste() },
                        modifier = Modifier.weight(1f)
                    ) { Text("الصاق") }
                }
                OutlinedButton(
                    onClick = {
                        val lat = ZoneValidator.parseDoubleFa(vm.latText) ?: 0.0
                        val lng = ZoneValidator.parseDoubleFa(vm.lngText) ?: 0.0
                        val radiusM = ZoneValidator.parseDoubleFa(vm.radiusText)
                            ?.let { toMeters(it, vm.unit) } ?: 200.0
                        navController.navigate("map_picker?lat=$lat&lng=$lng&radius=$radiusM")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("انتخاب روی نقشه")
                }
                val errorColor = MaterialTheme.colorScheme.error
                locError?.let {
                    Text(it, color = errorColor, style = MaterialTheme.typography.bodySmall)
                }
                pasteError?.let {
                    Text(it, color = errorColor, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "راهنما: در گوگل‌مپ نقطه را لمس طولانی کنید تا مختصات نمایش داده شود؛ آن را کپی کنید (مثل 34.633719,50.886485) و با دکمه «الصاق» یا مستقیم در کادر عرض جغرافیایی بچسبانید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(icon = Icons.Filled.Place, title = "شعاع") {
                OutlinedTextField(
                    value = vm.radiusText,
                    onValueChange = { vm.radiusText = it },
                    label = { Text("شعاع") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = vm.unit == RadiusUnit.METER,
                        onClick = { vm.unit = RadiusUnit.METER },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("متر") }
                    )
                    SegmentedButton(
                        selected = vm.unit == RadiusUnit.KILOMETER,
                        onClick = { vm.unit = RadiusUnit.KILOMETER },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("کیلومتر") }
                    )
                }
                // هشدار برای شعاع‌های خیلی کوچک: دقت GPS گوشی محدود است
                val radiusMeters = vm.radiusText.toDoubleOrNull()?.let { toMeters(it, vm.unit) }
                if (radiusMeters != null && radiusMeters > 0 && radiusMeters < 50) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "دقت مکان‌یابی گوشی حدود ۱۰ تا ۲۰ متر است؛ شعاع کمتر از ۵۰ متر ممکن است دقیق عمل نکند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // فاصله بررسی دقیق: فقط برای شعاع‌های کمتر از ۲۰۰ متر
            val radiusMetersNow =
                ZoneValidator.parseDoubleFa(vm.radiusText)?.let { toMeters(it, vm.unit) }
            if (radiusMetersNow != null && radiusMetersNow < 200.0) {
                SectionCard(icon = Icons.Filled.Refresh, title = "فاصله بررسی دقیق") {
                    Text(
                        "چون شعاع کمتر از ۲۰۰ متر است، موقعیت شما با همین فاصله زمانی بررسی می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vm.checkIntervalText,
                        onValueChange = { vm.checkIntervalText = it },
                        label = { Text("هر چند وقت یک بار بررسی شود") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (vm.checkIntervalContinuous) 0.4f else 1f),
                        enabled = !vm.checkIntervalContinuous,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    PrettySegmentedControl(
                        options = listOf("ثانیه", "دقیقه", "مداوم"),
                        selectedIndex = when {
                            vm.checkIntervalContinuous -> 2
                            vm.checkIntervalIsMinutes -> 1
                            else -> 0
                        },
                        onSelect = { index ->
                            vm.checkIntervalContinuous = index == 2
                            vm.checkIntervalIsMinutes = index == 1
                        }
                    )
                    if (vm.checkIntervalContinuous) {
                        Text(
                            "در حالت مداوم، موقعیت تقریباً هر لحظه (هر ۲ ثانیه) بررسی می‌شود؛ مصرف باتری بیشتر است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val intervalError = CheckInterval.errorFa(
                            CheckInterval.toSeconds(vm.checkIntervalText, vm.checkIntervalIsMinutes)
                        )
                        if (intervalError != null) {
                            Text(
                                intervalError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            SectionCard(icon = Icons.Filled.DateRange, title = "زمان‌بندی") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("فعال‌سازی زمان‌بندی", fontWeight = FontWeight.Bold)
                        Text(
                            "هشدار فقط در روزها و ساعات انتخاب‌شده فعال باشد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vm.scheduleEnabled,
                        onCheckedChange = { vm.scheduleEnabled = it }
                    )
                }
                if (vm.scheduleEnabled) {
                    val dayNames = listOf(
                        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه",
                        "چهارشنبه", "پنجشنبه", "جمعه"
                    )
                    val selectedDays = vm.scheduleDaysSet()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..4).forEach { day ->
                            FilterChip(
                                selected = day in selectedDays,
                                onClick = { vm.toggleScheduleDay(day) },
                                label = { Text(dayNames[day - 1]) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (5..7).forEach { day ->
                            FilterChip(
                                selected = day in selectedDays,
                                onClick = { vm.toggleScheduleDay(day) },
                                label = { Text(dayNames[day - 1]) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimePickerField(
                            label = "از ساعت",
                            minutes = vm.scheduleStartMin,
                            onPick = { vm.scheduleStartMin = it },
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerField(
                            label = "تا ساعت",
                            minutes = vm.scheduleEndMin,
                            onPick = { vm.scheduleEndMin = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "مثال: برای محل کار، شنبه تا چهارشنبه از ۸ تا ۱۸.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard(icon = Icons.Filled.Notifications, title = "پس از خروج از محدوده") {
                Text(
                    "داخل محدوده، هشدار فقط با توقف دستی قطع می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Zone.EXIT_STOP to ("توقف فوری" to "با خروج از محدوده، هشدار قطع می‌شود."),
                        Zone.EXIT_SEQUENCE to ("الگوی تکراری" to "پس از خروج، الگوی تکراری پخش می‌شود."),
                        Zone.EXIT_CONTINUE to ("ادامه تا توقف دستی" to "هشدار تا توقف دستی ادامه دارد.")
                    ).forEach { (behavior, texts) ->
                        PrettyOptionRow(
                            selected = vm.exitBehavior == behavior,
                            title = texts.first,
                            subtitle = texts.second,
                            onClick = { vm.exitBehavior = behavior }
                        )
                    }
                }
                // تنظیمات الگوی تکراری: تعداد دفعات، مدت هر آلارم، مکث بین آلارم‌ها
                if (vm.exitBehavior == Zone.EXIT_SEQUENCE) {
                    val countError = ExitSequenceValidator.validateCount(vm.postExitCountText)
                    val durationError =
                        ExitSequenceValidator.validateDuration(vm.postExitDurationText)
                    val gapError = ExitSequenceValidator.validateGap(vm.postExitGapText)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = vm.postExitCountText,
                            onValueChange = { vm.postExitCountText = it },
                            label = { Text("تعداد دفعات") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = countError != null,
                            supportingText = countError?.let { { Text(it) } }
                        )
                        OutlinedTextField(
                            value = vm.postExitDurationText,
                            onValueChange = { vm.postExitDurationText = it },
                            label = { Text("مدت هر آلارم (ثانیه)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = durationError != null,
                            supportingText = durationError?.let { { Text(it) } }
                        )
                        OutlinedTextField(
                            value = vm.postExitGapText,
                            onValueChange = { vm.postExitGapText = it },
                            label = { Text("مکث بین آلارم‌ها (ثانیه)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = gapError != null,
                            supportingText = gapError?.let { { Text(it) } }
                        )
                        Text(
                            "اگر هر کدام را دستی متوقف کنید، آلارم‌های بعدی این محدوده به صدا درنمی‌آید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionCard(icon = Icons.Filled.PlayArrow, title = "صدای هشدار") {
                SOUND_OPTIONS.forEach { (key, label) ->
                    val selected = vm.soundKey == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                onClick = { vm.soundKey = key }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { vm.soundKey = key }
                        )
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { AlertPlayer.preview(context, key) }) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "پیشنمایش $label",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { customSoundLauncher.launch("audio/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("انتخاب صدای دلخواه از گوشی")
                }
                vm.customSoundUri?.let { uriString ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                customSoundName(uriString),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            IconButton(
                                onClick = {
                                    AlertPlayer.preview(context, vm.soundKey, uriString)
                                }
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "پیشنمایش صدای دلخواه",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { vm.customSoundUri = null }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "حذف صدای دلخواه",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Text(
                        "صدای دلخواه جایگزین صدای سیستمی بالا می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard(icon = Icons.Filled.CheckCircle, title = "وضعیت") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("محدوده فعال باشد", fontWeight = FontWeight.Bold)
                    Switch(checked = vm.isActive, onCheckedChange = { vm.isActive = it })
                }
            }

            if (errors.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        errors.forEach {
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
