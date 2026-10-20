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

    // نوع هشدار و اعلام صوتی
    var alertMode by mutableStateOf(Zone.ALERT_BOTH)
    var voiceAnnounce by mutableStateOf(false)
    // هشدار نزدیک شدن
    var approachEnabled by mutableStateOf(false)
    var approachDistanceText by mutableStateOf("500")
    // یادآور ماندن
    var stayEnabled by mutableStateOf(false)
    var stayMinutesText by mutableStateOf("60")
    // پیامک اطلاع‌رسانی
    var smsNumbersText by mutableStateOf("")
    var smsMessageText by mutableStateOf("")
    // بازه اعلان بی‌صدا
    var silentEnabled by mutableStateOf(false)
    var silentStartMin by mutableStateOf(1320)
    var silentEndMin by mutableStateOf(360)

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
                        alertMode = z.alertMode
                        voiceAnnounce = z.voiceAnnounce
                        approachEnabled = z.approachDistanceM > 0
                        approachDistanceText =
                            (if (z.approachDistanceM > 0) z.approachDistanceM else 500).toString()
                        stayEnabled = z.stayReminderMin > 0
                        stayMinutesText =
                            (if (z.stayReminderMin > 0) z.stayReminderMin else 60).toString()
                        smsNumbersText = z.smsNumbers
                        smsMessageText = z.smsMessage
                        silentEnabled = z.silentEnabled
                        silentStartMin = z.silentStartMin
                        silentEndMin = z.silentEndMin
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

private val SOUND_KEYS = listOf(
    Zone.SOUND_ALARM,
    Zone.SOUND_RINGTONE,
    Zone.SOUND_NOTIFICATION
)

/** برچسب بومی‌شده هر کلید صدای سیستمی. */
private fun soundLabel(strings: UiStrings, key: String): String = when (key) {
    Zone.SOUND_RINGTONE -> strings.soundRingtone
    Zone.SOUND_NOTIFICATION -> strings.soundNotification
    else -> strings.soundSiren
}

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
    val strings = LocalStrings.current
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
                    locError = strings.locNotFound
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
            locError = strings.locPermNeeded
        }
    }

    // درخواست مجوز پیامک هنگام ذخیره محدوده دارای شماره پیامک
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* نتیجه مهم نیست؛ ارسال پیامک بدون مجوز در سکوت رد می‌شود */ }

    // انتخاب شماره پیامک از مخاطبین: با داده شدن مجوز (یا از قبل داده شده)
    // دیالوگ انتخاب باز می‌شود؛ اگر مجوز رد شود هم دیالوگ باز می‌شود تا
    // پیام راهنمای ورود دستی را نشان دهد.
    var showContactsPicker by remember { mutableStateOf(false) }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { showContactsPicker = true }

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
            } ?: strings.selectedSound
        } catch (_: Exception) {
            strings.selectedSound
        }
    }

    fun doPaste() {
        pasteError = null
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
        if (text.isNullOrEmpty()) {
            pasteError = strings.clipboardEmpty
            return
        }
        val parts = text.split(",").map { it.trim() }
        if (parts.size != 2) {
            pasteError = strings.pasteFormatError
            return
        }
        val lat = ZoneValidator.parseDoubleFa(parts[0])
        val lng = ZoneValidator.parseDoubleFa(parts[1])
        if (lat == null || lng == null) {
            pasteError = strings.pasteInvalid
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
        // اعتبارسنجی هشدار نزدیک شدن و یادآور ماندن
        val approachDistanceM: Int = if (vm.approachEnabled) {
            val v = ZoneValidator.parseDoubleFa(vm.approachDistanceText)?.toInt()
            if (v == null || v !in 100..5000) {
                allErrors += strings.approachError
            }
            v ?: 0
        } else {
            0
        }
        val stayReminderMin: Int = if (vm.stayEnabled) {
            val v = ZoneValidator.parseDoubleFa(vm.stayMinutesText)?.toInt()
            if (v == null || v !in 5..1440) {
                allErrors += strings.stayError
            }
            v ?: 0
        } else {
            0
        }
        // اگر شماره پیامک وارد شده و مجوز نیست، درخواست شود (ذخیره متوقف نمی‌شود
        // و پیامک تا زمان داده شدن مجوز، در سکوت ارسال نمی‌شود).
        if (vm.smsNumbersText.isNotBlank() && !Permissions.hasSendSms(context)) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
        saving = true
        scope.launch(Dispatchers.IO) {
            try {
                val needsCountCheck = vm.isActive && (vm.existing == null || !vm.wasActive)
                if (needsCountCheck && app.zoneRepo.countActive() >= GeofenceHelper.MAX_GEOFENCES) {
                    allErrors += strings.maxZonesError
                }
            } catch (e: Exception) {
                allErrors += strings.saveError
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
                            approachDistanceM = approachDistanceM,
                            stayReminderMin = stayReminderMin,
                            alertMode = vm.alertMode,
                            voiceAnnounce = vm.voiceAnnounce,
                            smsNumbers = vm.smsNumbersText.trim(),
                            smsMessage = vm.smsMessageText.trim(),
                            silentEnabled = vm.silentEnabled,
                            silentStartMin = vm.silentStartMin,
                            silentEndMin = vm.silentEndMin,
                            insideSince = vm.existing?.insideSince ?: 0,
                            stayReminded = vm.existing?.stayReminded ?: false,
                            sortOrder = vm.existing?.sortOrder ?: 0,
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
                            val savedId = app.zoneRepo.upsert(zone)
                            // محدوده تازه در حالت «دستی» ته فهرست می‌نشیند:
                            // ترتیبش برابر شناسه‌اش می‌شود (مثل مهاجرت دیتابیس).
                            if (vm.existing == null && savedId > 0) {
                                try {
                                    app.zoneRepo.setSortOrder(savedId, savedId.toInt())
                                } catch (_: Exception) {
                                }
                            }
                            GeofenceHelper.refreshRegistrations(context)
                            // تازه‌سازی پشتیبان خودکار پس از هر تغییر محدوده
                            try {
                                ir.khanoomi.geofencealert.data.BackupManager
                                    .writeAutoBackup(context)
                            } catch (_: Exception) {
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                saving = false
                                errors = listOf(strings.saveError)
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
                title = { Text(if (zoneId == -1L) strings.newZone else strings.editZone) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
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
                        Text(strings.saveZone, fontWeight = FontWeight.Bold)
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
            SectionCard(icon = Icons.Filled.Info, title = strings.specifications) {
                OutlinedTextField(
                    value = vm.name,
                    onValueChange = { vm.name = it },
                    label = { Text(strings.zoneNameHint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            SectionCard(icon = Icons.Filled.LocationOn, title = strings.coordinates) {
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
                    label = { Text(strings.latitudeFull) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = vm.lngText,
                    onValueChange = { vm.lngText = it },
                    label = { Text(strings.longitudeFull) },
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
                            Text(strings.currentLocation)
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "coords",
                                    "${vm.latText.trim()},${vm.lngText.trim()}"
                                )
                            )
                            Toast.makeText(context, strings.coordsCopied, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(strings.copyCoords) }
                    FilledTonalButton(
                        onClick = { doPaste() },
                        modifier = Modifier.weight(1f)
                    ) { Text(strings.paste) }
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
                    Text(strings.selectOnMap)
                }
                val errorColor = MaterialTheme.colorScheme.error
                locError?.let {
                    Text(it, color = errorColor, style = MaterialTheme.typography.bodySmall)
                }
                pasteError?.let {
                    Text(it, color = errorColor, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    strings.coordsGuide,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(icon = Icons.Filled.Place, title = strings.radius) {
                OutlinedTextField(
                    value = vm.radiusText,
                    onValueChange = { vm.radiusText = it },
                    label = { Text(strings.radius) },
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
                        label = { Text(strings.unitMeter) }
                    )
                    SegmentedButton(
                        selected = vm.unit == RadiusUnit.KILOMETER,
                        onClick = { vm.unit = RadiusUnit.KILOMETER },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text(strings.unitKilometer) }
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
                            strings.smallRadiusWarning,
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
                SectionCard(icon = Icons.Filled.Refresh, title = strings.checkIntervalTitle) {
                    Text(
                        strings.checkIntervalDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vm.checkIntervalText,
                        onValueChange = { vm.checkIntervalText = it },
                        label = { Text(strings.checkIntervalLabel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (vm.checkIntervalContinuous) 0.4f else 1f),
                        enabled = !vm.checkIntervalContinuous,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    PrettySegmentedControl(
                        options = listOf(
                            strings.unitSecond,
                            strings.unitMinute,
                            strings.continuousLabel
                        ),
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
                            strings.continuousDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val intervalError = CheckInterval.errorFa(
                            CheckInterval.toSeconds(vm.checkIntervalText, vm.checkIntervalIsMinutes)
                        )
                        if (intervalError != null) {
                            Text(
                                strings.localizeValidatorError(intervalError),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            SectionCard(icon = Icons.Filled.DateRange, title = strings.schedule) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.scheduleEnable, fontWeight = FontWeight.Bold)
                        Text(
                            strings.scheduleEnableDesc,
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
                    val dayNames = strings.weekDayNames.split(",")
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
                            label = strings.fromTime,
                            minutes = vm.scheduleStartMin,
                            onPick = { vm.scheduleStartMin = it },
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerField(
                            label = strings.toTime,
                            minutes = vm.scheduleEndMin,
                            onPick = { vm.scheduleEndMin = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        strings.scheduleExample,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard(icon = Icons.Filled.Notifications, title = strings.exitBehaviorTitle) {
                Text(
                    strings.exitBehaviorNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Zone.EXIT_STOP to (strings.exitStop to strings.exitStopSub),
                        Zone.EXIT_SEQUENCE to (strings.exitSequence to strings.exitSeqSub),
                        Zone.EXIT_CONTINUE to (strings.exitContinue to strings.exitContinueSub)
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
                            label = { Text(strings.repeatCountLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = countError != null,
                            supportingText = countError?.let {
                                { Text(strings.localizeValidatorError(it)) }
                            }
                        )
                        OutlinedTextField(
                            value = vm.postExitDurationText,
                            onValueChange = { vm.postExitDurationText = it },
                            label = { Text(strings.eachDurationLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = durationError != null,
                            supportingText = durationError?.let {
                                { Text(strings.localizeValidatorError(it)) }
                            }
                        )
                        OutlinedTextField(
                            value = vm.postExitGapText,
                            onValueChange = { vm.postExitGapText = it },
                            label = { Text(strings.gapLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = gapError != null,
                            supportingText = gapError?.let {
                                { Text(strings.localizeValidatorError(it)) }
                            }
                        )
                        Text(
                            strings.exitSeqNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionCard(icon = Icons.Filled.PlayArrow, title = strings.alertSound) {
                SOUND_KEYS.forEach { key ->
                    val label = soundLabel(strings, key)
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
                                contentDescription = "${strings.previewCd} $label",
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
                    Text(strings.pickCustomSound)
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
                                    contentDescription = strings.previewCustomSoundCd,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { vm.customSoundUri = null }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = strings.deleteCustomSoundCd,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Text(
                        strings.customSoundNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard(icon = Icons.Filled.Notifications, title = strings.alertType) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrettyOptionRow(
                        selected = vm.alertMode == Zone.ALERT_BOTH,
                        title = strings.alertBoth,
                        subtitle = strings.alertBothSub,
                        onClick = { vm.alertMode = Zone.ALERT_BOTH }
                    )
                    PrettyOptionRow(
                        selected = vm.alertMode == Zone.ALERT_SOUND,
                        title = strings.alertSoundOnly,
                        subtitle = strings.alertSoundOnlySub,
                        onClick = { vm.alertMode = Zone.ALERT_SOUND }
                    )
                    PrettyOptionRow(
                        selected = vm.alertMode == Zone.ALERT_VIBRATE,
                        title = strings.alertVibrateOnly,
                        subtitle = strings.alertVibrateOnlySub,
                        onClick = { vm.alertMode = Zone.ALERT_VIBRATE }
                    )
                    PrettyOptionRow(
                        selected = vm.alertMode == Zone.ALERT_NONE,
                        title = strings.alertNoneOpt,
                        subtitle = strings.alertNoneOptSub,
                        onClick = { vm.alertMode = Zone.ALERT_NONE }
                    )
                }
            }

            SectionCard(icon = Icons.Filled.Notifications, title = strings.voiceAnnounce) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.voiceReadName, fontWeight = FontWeight.Bold)
                        Text(
                            strings.voiceReadNameDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vm.voiceAnnounce,
                        onCheckedChange = { vm.voiceAnnounce = it }
                    )
                }
            }

            SectionCard(icon = Icons.Filled.LocationOn, title = strings.approachAlert) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.approachTitle, fontWeight = FontWeight.Bold)
                        Text(
                            strings.approachDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vm.approachEnabled,
                        onCheckedChange = { vm.approachEnabled = it }
                    )
                }
                if (vm.approachEnabled) {
                    val approachError = if (vm.approachDistanceText.isNotBlank()) {
                        val v = ZoneValidator.parseDoubleFa(vm.approachDistanceText)?.toInt()
                        if (v == null || v !in 100..5000) {
                            strings.approachError
                        } else null
                    } else null
                    OutlinedTextField(
                        value = vm.approachDistanceText,
                        onValueChange = { vm.approachDistanceText = it },
                        label = { Text(strings.approachDistanceLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = approachError != null,
                        supportingText = approachError?.let { { Text(it) } }
                    )
                }
            }

            SectionCard(icon = Icons.Filled.DateRange, title = strings.stayReminder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.stayTitle, fontWeight = FontWeight.Bold)
                        Text(
                            strings.stayDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vm.stayEnabled,
                        onCheckedChange = { vm.stayEnabled = it }
                    )
                }
                if (vm.stayEnabled) {
                    val stayError = if (vm.stayMinutesText.isNotBlank()) {
                        val v = ZoneValidator.parseDoubleFa(vm.stayMinutesText)?.toInt()
                        if (v == null || v !in 5..1440) {
                            strings.stayError
                        } else null
                    } else null
                    OutlinedTextField(
                        value = vm.stayMinutesText,
                        onValueChange = { vm.stayMinutesText = it },
                        label = { Text(strings.stayMinutesLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stayError != null,
                        supportingText = stayError?.let { { Text(it) } }
                    )
                }
            }

            SectionCard(icon = Icons.Filled.Notifications, title = strings.sms) {
                OutlinedTextField(
                    value = vm.smsNumbersText,
                    onValueChange = { vm.smsNumbersText = it },
                    label = { Text(strings.smsNumbersLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = vm.smsMessageText,
                    onValueChange = { vm.smsMessageText = it },
                    label = { Text(strings.smsMessageLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp)
                )
                Text(
                    strings.smsNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        if (Permissions.hasContacts(context)) {
                            showContactsPicker = true
                        } else {
                            contactsPermissionLauncher.launch(
                                Manifest.permission.READ_CONTACTS
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.contactsPickerTitle)
                }
            }

            SectionCard(icon = Icons.Filled.DateRange, title = strings.silentWindow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.silentTitle, fontWeight = FontWeight.Bold)
                        Text(
                            strings.silentDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vm.silentEnabled,
                        onCheckedChange = { vm.silentEnabled = it }
                    )
                }
                if (vm.silentEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimePickerField(
                            label = strings.fromLabel,
                            minutes = vm.silentStartMin,
                            onPick = { vm.silentStartMin = it },
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerField(
                            label = strings.toLabel,
                            minutes = vm.silentEndMin,
                            onPick = { vm.silentEndMin = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SectionCard(icon = Icons.Filled.CheckCircle, title = strings.status) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.zoneActive, fontWeight = FontWeight.Bold)
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
                            Text(
                                strings.localizeValidatorError(it),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showContactsPicker) {
        ContactsPickerDialog(
            pickedNumbers = vm.smsNumbersText
                .split('\n', ';', ',')
                .map { normalizePhoneNumber(it) }
                .filter { it.isNotEmpty() }
                .toSet(),
            onPick = { number ->
                val key = normalizePhoneNumber(number)
                val alreadyThere = key.isNotEmpty() && vm.smsNumbersText
                    .split('\n', ';', ',')
                    .any { normalizePhoneNumber(it) == key }
                if (!alreadyThere) {
                    vm.smsNumbersText = if (vm.smsNumbersText.isBlank()) {
                        number
                    } else {
                        vm.smsNumbersText.trimEnd() + "\n" + number
                    }
                }
            },
            onDismiss = { showContactsPicker = false }
        )
    }
}
