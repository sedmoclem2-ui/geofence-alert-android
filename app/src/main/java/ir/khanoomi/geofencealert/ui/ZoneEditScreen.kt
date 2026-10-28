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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import ir.khanoomi.geofencealert.domain.GeoMath
import ir.khanoomi.geofencealert.domain.ZoneShareCodec
import ir.khanoomi.geofencealert.domain.ZoneValidator
import ir.khanoomi.geofencealert.domain.toMeters
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.ui.theme.ZoneColors
import ir.khanoomi.geofencealert.util.LocationUtil
import ir.khanoomi.geofencealert.util.Permissions
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
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
    /** چندضلعی محدوده به‌صورت JSON (آرایه {lat,lng})؛ خالی یعنی دایره‌ای. */
    var polygonJson by mutableStateOf("")
    /** شکل انتخاب‌شده در فرم: true یعنی چندضلعی، false یعنی دایره. */
    var isPolygon by mutableStateOf(false)
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
    /** رنگ اختصاصی محدوده: ۰ = پیش‌فرض تم، ۱ تا ۸ = رنگ‌های آماده. */
    var colorIndex by mutableStateOf(0)
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
                        colorIndex = z.colorIndex
                        polygonJson = z.polygonJson
                        isPolygon = z.polygonJson.isNotBlank()
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

    /**
     * اعمال چندضلعی برگشتی از نقشه: ذخیره JSON رأس‌ها و نوشتن مرکز ثقل
     * آن در فیلدهای مختصات (در ذخیره هم دوباره از روی مرکز ثقل بازنویسی
     * می‌شود، پس فیلدها فقط نمایشی/قابل‌ویرایش‌اند).
     */
    fun applyPickedPolygon(json: String) {
        val points = GeoMath.parsePolygonJson(json)
        if (points.size < 3) return
        polygonJson = json
        isPolygon = true
        GeoMath.centroid(points)?.let { (cLat, cLng) ->
            latText = coordText(cLat)
            lngText = coordText(cLng)
        }
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

/** برچسب بومی‌سازی‌شده صدای سیستمی. */
private fun soundLabel(strings: UiStrings, key: String): String = when (key) {
    Zone.SOUND_RINGTONE -> strings.soundRingtone
    Zone.SOUND_NOTIFICATION -> strings.soundNotification
    else -> strings.soundAlarm
}

/**
 * کارت بخش‌بندی فرم (نسخه ۲ سامانه طراحی فرم): سربرگ با چیپ مربعی گرد
 * (آیکن روی primaryContainer) و عنوان درشت روی سطح، خط مویی جداکننده و
 * ریتم ثابت فاصله‌ها — حاشیه داخلی ۱۶، سربرگ تا خط ۱۲ و خط تا محتوا ۱۴.
 */
@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

/**
 * قرص یکی از تب‌های موضوعی پنل ویرایش محدوده: انتخاب‌شده با پس‌زمینه
 * primaryContainer و متن پررنگ، انتخاب‌نشده روی سطح نرم تم. اگر تب خطای
 * اعتبارسنجی داشته باشد، یک نقطه قرمز کوچک گوشه بالای آن می‌نشیند.
 */
@Composable
private fun RowScope.ZoneEditTabPill(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    showErrorDot: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) scheme.primaryContainer
                else scheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (showErrorDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(scheme.error)
            )
        }
    }
}

/**
 * نمونه‌رنگ دایره‌ای ۴۰dp برای انتخاب رنگ محدوده: پرشده با یک رنگ ثابت یا
 * گرادیان (نمونه «پیش‌فرض تم»). نمونه انتخاب‌شده یک حلقه بیرونی ۲٫۵dp
 * هم‌رنگ خودش (برای پیش‌فرض تم: رنگ اصلی) با فاصله ۳dp از دایره داخلی
 * می‌گیرد و تیک سفید ۱۸dp وسط آن می‌نشیند.
 */
@Composable
private fun ColorSwatch(
    selected: Boolean,
    brush: Brush?,
    color: Color?,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val ringColor = color ?: scheme.primary
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(BorderStroke(2.5.dp, ringColor), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(if (selected) 29.dp else 40.dp)
                .clip(CircleShape)
                .then(
                    when {
                        brush != null -> Modifier.background(brush)
                        color != null -> Modifier.background(color)
                        else -> Modifier
                    }
                )
                .then(
                    if (selected) Modifier
                    else Modifier.border(
                        BorderStroke(1.dp, scheme.outlineVariant),
                        CircleShape
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * کاشی گزینه تک‌انتخابی سامانه طراحی فرم: گوشه ۱۲dp و حاشیه ظریف؛ حالت
 * انتخاب‌شده حاشیه ۱٫۵dp رنگ اصلی، پس‌زمینه primaryContainer ملایم و دایره
 * رادیویی ۲۰dp با نقطه مرکزی پر می‌گیرد. اسلات [trailing] برای کنش فرعی
 * (مثل پیش‌شنوی صدا) است و کلیک آن مستقل از انتخاب کاشی می‌ماند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionTile(
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) scheme.primaryContainer.copy(alpha = 0.45f) else scheme.surface,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) scheme.primary else scheme.outlineVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        BorderStroke(
                            2.dp,
                            if (selected) scheme.primary else scheme.outline
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(scheme.primary)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    softWrap = true
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        softWrap = true
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

/**
 * حرف کوتاه روز هفته (۱=شنبه … ۷=جمعه) در زبان فعال برنامه، از خود تقویم
 * سیستم — همان روشی که صفحه آمار برای برچسب روزهای نمودار به‌کار می‌برد.
 */
private fun narrowDayLetter(day: Int, lang: AppLang): String {
    val locale = when (lang) {
        AppLang.FA -> Locale("fa")
        AppLang.EN -> Locale.ENGLISH
        AppLang.AR -> Locale("ar")
    }
    val dow = when (day) {
        1 -> DayOfWeek.SATURDAY
        2 -> DayOfWeek.SUNDAY
        3 -> DayOfWeek.MONDAY
        4 -> DayOfWeek.TUESDAY
        5 -> DayOfWeek.WEDNESDAY
        6 -> DayOfWeek.THURSDAY
        else -> DayOfWeek.FRIDAY
    }
    return dow.getDisplayName(TextStyle.NARROW, locale)
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ZoneEditScreen(navController: NavController, zoneId: Long) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
    val app = context.applicationContext as GeofenceAlertApp
    val vm: ZoneEditViewModel = viewModel { ZoneEditViewModel(app, zoneId) }
    val scope = rememberCoroutineScope()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // خطاهای اعتبارسنجی، هرکدام با شماره تب صاحبش (۰=کجاست؟، ۱=هشدار، ۲=زمان‌بندی)؛ -۱ یعنی خطای کلی بدون تب
    var errors by remember { mutableStateOf(listOf<Pair<Int, String>>()) }
    var selectedTab by remember { mutableIntStateOf(0) }
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

    // دریافت چندضلعی برگشتی از نقشه ترسیم (حالت چندضلعیِ انتخاب‌گر نقشه).
    // جمع‌آوری جداگانه است چون collect اول هرگز برنمی‌گردد.
    LaunchedEffect(mapResultHandle) {
        mapResultHandle?.getStateFlow("picked_polygon", "")?.collect { json ->
            if (json.isNotBlank()) {
                vm.applyPickedPolygon(json)
                pasteError = null
                mapResultHandle.remove<String>("picked_polygon")
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
            } ?: strings.customSoundSelected
        } catch (_: Exception) {
            strings.customSoundSelected
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
            pasteError = strings.pasteInvalidCoords
            return
        }
        vm.latText = parts[0]
        vm.lngText = parts[1]
    }

    /**
     * ساخت محدوده از فیلدهای فعلی فرم برای اشتراک‌گذاری — همان نگاشت
     * فیلد→Zone که مسیر ذخیره (doSave) استفاده می‌کند، بدون اعتبارسنجی
     * و بدون اثر جانبی: در حالت چندضلعی مرکز ثقل رأس‌ها مرکز می‌شود و
     * شعاعِ خالیِ چندضلعی ۲۰۰ متر خنثی می‌گیرد؛ اگر نام خالی باشد یا
     * مختصات/شعاع قابل‌پارس نباشند null برمی‌گردد تا دکمه اشتراک‌گذاری
     * غیرفعال بماند (به‌جای ساخت پیوند خراب).
     */
    fun buildShareZone(): Zone? {
        if (vm.name.isBlank()) return null
        val lat = ZoneValidator.parseDoubleFa(vm.latText)
        val lng = ZoneValidator.parseDoubleFa(vm.lngText)
        val radius = ZoneValidator.parseDoubleFa(vm.radiusText)
        val centroid = if (vm.isPolygon) {
            GeoMath.centroid(GeoMath.parsePolygonJson(vm.polygonJson))
        } else {
            null
        }
        val finalLat = centroid?.first ?: lat ?: return null
        val finalLng = centroid?.second ?: lng ?: return null
        val radiusMeters = if (vm.isPolygon) {
            radius?.let { toMeters(it, vm.unit) } ?: 200.0
        } else {
            radius?.let { toMeters(it, vm.unit) } ?: return null
        }
        return Zone(
            id = vm.existing?.id ?: 0,
            name = vm.name.trim(),
            latitude = finalLat,
            longitude = finalLng,
            radiusMeters = radiusMeters,
            polygonJson = if (vm.isPolygon) vm.polygonJson else "",
            isActive = vm.isActive,
            scheduleEnabled = vm.scheduleEnabled,
            scheduleDays = vm.scheduleDays.ifBlank { "1,2,3,4,5,6,7" },
            scheduleStartMin = vm.scheduleStartMin,
            scheduleEndMin = vm.scheduleEndMin,
            exitBehavior = vm.exitBehavior,
            alertMode = vm.alertMode,
            silentEnabled = vm.silentEnabled,
            silentStartMin = vm.silentStartMin,
            silentEndMin = vm.silentEndMin,
            colorIndex = vm.colorIndex
        )
    }

    fun doSave() {
        val lat = ZoneValidator.parseDoubleFa(vm.latText)
        val lng = ZoneValidator.parseDoubleFa(vm.lngText)
        val radius = ZoneValidator.parseDoubleFa(vm.radiusText)
        // در حالت چندضلعی، مختصات ذخیره از روی مرکز ثقل چندضلعی بازنویسی
        // می‌شود، پس اعتبارسنجی هم روی مرکز ثقل انجام می‌شود نه فیلدها؛
        // شعاع هم پنهان است، پس با یک مقدار معتبر خنثی اعتبارسنجی می‌شود.
        val polygonPoints = if (vm.isPolygon) GeoMath.parsePolygonJson(vm.polygonJson) else emptyList()
        val polygonCentroid = if (vm.isPolygon) GeoMath.centroid(polygonPoints) else null
        val validation = ZoneValidator.validate(
            vm.name,
            polygonCentroid?.first ?: lat,
            polygonCentroid?.second ?: lng,
            if (vm.isPolygon) 200.0 else radius,
            vm.unit
        )
        // هر خطا با شماره تب صاحبش برچسب می‌خورد تا پس از تلاش ناموفق برای
        // ذخیره، کاربر خودکار به تب دارای خطا برود و روی قرص آن تب نقطه خطا بنشیند
        val allErrors = validation.errors.map { 0 to it }.toMutableList()
        // چندضلعی بدون دست‌کم ۳ رأس قابل ذخیره نیست.
        if (vm.isPolygon && polygonPoints.size < 3) {
            allErrors += (0 to strings.polygonMinPoints)
        }
        // فاصله بررسی دقیق فقط وقتی معنا دارد که شعاع کمتر از ۲۰۰ متر باشد؛
        // چندضلعی‌ها همیشه با پایش دقیق ارزیابی می‌شوند، پس بازه‌شان همیشه
        // از فیلد خوانده می‌شود (کنترل بازه در حالت چندضلعی پنهان نمی‌شود).
        val radiusMetersForCheck = radius?.let { toMeters(it, vm.unit) }
        val checkIntervalSec: Int =
            if (vm.isPolygon || (radiusMetersForCheck != null && radiusMetersForCheck < 200.0)) {
                if (vm.checkIntervalContinuous) {
                    CheckInterval.CONTINUOUS
                } else {
                    val sec = CheckInterval.toSeconds(vm.checkIntervalText, vm.checkIntervalIsMinutes)
                    CheckInterval.errorFa(sec)?.let { allErrors += (0 to it) }
                    sec ?: 20
                }
            } else {
                20
            }
        // اعتبارسنجی الگوی تکراری پس از خروج
        if (vm.exitBehavior == Zone.EXIT_SEQUENCE) {
            ExitSequenceValidator.validateCount(vm.postExitCountText)?.let { allErrors += (1 to it) }
            ExitSequenceValidator.validateDuration(vm.postExitDurationText)?.let { allErrors += (1 to it) }
            ExitSequenceValidator.validateGap(vm.postExitGapText)?.let { allErrors += (1 to it) }
        }
        // اعتبارسنجی هشدار نزدیک شدن و یادآور ماندن
        val approachDistanceM: Int = if (vm.approachEnabled) {
            val v = ZoneValidator.parseDoubleFa(vm.approachDistanceText)?.toInt()
            if (v == null || v !in 100..5000) {
                allErrors += (2 to strings.approachDistanceError)
            }
            v ?: 0
        } else {
            0
        }
        val stayReminderMin: Int = if (vm.stayEnabled) {
            val v = ZoneValidator.parseDoubleFa(vm.stayMinutesText)?.toInt()
            if (v == null || v !in 5..1440) {
                allErrors += (2 to strings.stayDurationError)
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
                    allErrors += (0 to strings.maxActiveZonesError)
                }
            } catch (e: Exception) {
                allErrors += (-1 to strings.saveError)
            }
            withContext(Dispatchers.Main) {
                errors = allErrors
                if (allErrors.isNotEmpty()) {
                    // پرش خودکار به اولین تبی که خطا دارد تا کاربر همان‌جا اصلاح کند
                    allErrors.firstOrNull { it.first >= 0 }?.let { selectedTab = it.first }
                }
                if (allErrors.isEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        // در حالت چندضلعی، مرکز ذخیره‌شده همیشه مرکز ثقل
                        // رأس‌های فعلی است، حتی اگر فیلدها دستی عوض شده باشند.
                        val saveCentroid = if (vm.isPolygon) {
                            GeoMath.centroid(GeoMath.parsePolygonJson(vm.polygonJson))
                        } else {
                            null
                        }
                        val zone = Zone(
                            id = vm.existing?.id ?: 0,
                            name = vm.name.trim(),
                            latitude = saveCentroid?.first ?: lat!!,
                            longitude = saveCentroid?.second ?: lng!!,
                            radiusMeters = if (vm.isPolygon) {
                                radius?.let { toMeters(it, vm.unit) } ?: 200.0
                            } else {
                                toMeters(radius!!, vm.unit)
                            },
                            polygonJson = if (vm.isPolygon) vm.polygonJson else "", 
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
                            colorIndex = vm.colorIndex,
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
                                errors = listOf(-1 to strings.saveError)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
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
        ) {
            // تب‌های موضوعی فرم: «کجاست؟» (مشخصات، مختصات، شعاع، دقت پایش و وضعیت)،
            // «هشدار» (رفتار خروج، صدا، نوع هشدار، اعلام صوتی و پیامک) و
            // «زمان‌بندی» (زمان‌بندی، سکوت، نزدیک‌شدن و ماندن). همه وضعیت فرم در
            // ویومدل نگه داشته می‌شود، پس جابه‌جایی بین تب‌ها چیزی را از دست نمی‌دهد.
            val errorTabs = errors.map { it.first }.toSet()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoneEditTabPill(
                    selected = selectedTab == 0,
                    icon = Icons.Filled.Place,
                    label = strings.tabPlace,
                    showErrorDot = 0 in errorTabs,
                    onClick = { selectedTab = 0 }
                )
                ZoneEditTabPill(
                    selected = selectedTab == 1,
                    icon = Icons.Filled.Notifications,
                    label = strings.tabAlerts,
                    showErrorDot = 1 in errorTabs,
                    onClick = { selectedTab = 1 }
                )
                ZoneEditTabPill(
                    selected = selectedTab == 2,
                    icon = Icons.Filled.DateRange,
                    label = strings.tabTiming,
                    showErrorDot = 2 in errorTabs,
                    onClick = { selectedTab = 2 }
                )
            }
            // خلاصه خطاها بالای پنل تب‌ها سنجاق شده تا در هر تبی که باشیم دیده شود
            if (errors.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                localizeValidatorError(it.second, strings),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (slideInHorizontally(tween(220)) {
                        if (targetState > initialState) 60 else -60
                    } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) {
                            if (targetState > initialState) -60 else 60
                        } + fadeOut(tween(220)))
                },
                label = "zoneEditTabs",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { tab ->
                when (tab) {
                    0 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // قالب‌های آماده فقط در صفحه محدوده تازه، بالای تب «کجاست؟»
                        if (zoneId == -1L) {
                            ZoneTemplateRow(vm, strings)
                        }
                        SectionCard(icon = Icons.Filled.Info, title = strings.specifications) {
                            OutlinedTextField(
                                value = vm.name,
                                onValueChange = { vm.name = it },
                                label = { Text(strings.zoneNameHint) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            // رنگ اختصاصی محدوده: نمونه اول «پیش‌فرض تم» است و یازده
                            // نمونه بعدی رنگ‌های آماده‌اند؛ رنگ روی موج‌های کارت
                            // محدوده در صفحه اصلی می‌نشیند.
                            Text(
                                strings.editColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                maxItemsInEachRow = 6
                            ) {
                                ColorSwatch(
                                    selected = vm.colorIndex == 0,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    color = null,
                                    contentDescription = strings.colorDefault,
                                    onClick = { vm.colorIndex = 0 }
                                )
                                ZoneColors.presets.forEachIndexed { i, preset ->
                                    ColorSwatch(
                                        selected = vm.colorIndex == i + 1,
                                        brush = null,
                                        color = preset,
                                        contentDescription = null,
                                        onClick = { vm.colorIndex = i + 1 }
                                    )
                                }
                            }
                            if (vm.colorIndex == 0) {
                                Text(
                                    strings.colorDefault,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // اشتراک‌گذاری محدوده (موج ۳): پیوند
                            // geofencealert://zone?d=… با متن ساده فرستاده
                            // می‌شود؛ نوار ذخیره چسبان دست‌نخورده می‌ماند.
                            // دکمه تا وقتی فرم قابل‌اشتراک نیست غیرفعال است.
                            val shareZoneValue = buildShareZone()
                            FilledTonalButton(
                                onClick = {
                                    val zone = shareZoneValue ?: return@FilledTonalButton
                                    try {
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                ZoneShareCodec.linkFor(zone)
                                            )
                                        }
                                        context.startActivity(
                                            Intent.createChooser(send, strings.shareZone)
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            strings.shareFailed,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = shareZoneValue != null
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(strings.shareZone)
                            }
                        }

                        SectionCard(icon = Icons.Filled.Place, title = strings.coordinates) {
                            // کلید شکل محدوده: دایره = رابط امروز، چندضلعی =
                            // ترسیم روی نقشه. داخل کارت مختصات است تا با پنهان
                            // شدن کارت شعاع، کلید همچنان در دسترس بماند.
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !vm.isPolygon,
                                    onClick = { vm.isPolygon = false },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    label = { Text(strings.shapeCircle) }
                                )
                                SegmentedButton(
                                    selected = vm.isPolygon,
                                    onClick = { vm.isPolygon = true },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    label = { Text(strings.shapePolygon) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
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
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                OutlinedTextField(
                                    value = vm.lngText,
                                    onValueChange = { vm.lngText = it },
                                    label = { Text(strings.longitudeFull) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (Permissions.hasAnyLocation(context)) doFetchLocation()
                                        else locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                    enabled = !locLoading
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (locLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Filled.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            strings.currentLocation,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText(
                                                strings.coordinates,
                                                "${vm.latText.trim()},${vm.lngText.trim()}"
                                            )
                                        )
                                        Toast.makeText(context, strings.coordsCopied, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            AppIcons.Copy,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            strings.copyCoords,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { doPaste() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            AppIcons.Paste,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            strings.paste,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            if (vm.isPolygon) {
                                FilledTonalButton(
                                    onClick = {
                                        val lat = ZoneValidator.parseDoubleFa(vm.latText) ?: 0.0
                                        val lng = ZoneValidator.parseDoubleFa(vm.lngText) ?: 0.0
                                        val radiusM = ZoneValidator.parseDoubleFa(vm.radiusText)
                                            ?.let { toMeters(it, vm.unit) } ?: 200.0
                                        // چندضلعیِ در حال ویرایش از همان
                                        // savedStateHandle خوانده می‌شود که
                                        // نتیجه هم به آن برمی‌گردد؛ در نشانی
                                        // ناوبری JSON نمی‌گذاریم تا پارس نشانی
                                        // خراب نشود.
                                        navController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("polygon_initial", vm.polygonJson)
                                        navController.navigate(
                                            "map_picker?lat=$lat&lng=$lng&radius=$radiusM&mode=polygon"
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.drawPolygonOnMap)
                                }
                                Text(
                                    strings.polygonCheckNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // خلاصه چندضلعیِ ذخیره‌شده: تعداد رأس + مرکز ثقل.
                                val savedPolygon = GeoMath.parsePolygonJson(vm.polygonJson)
                                val savedCentroid = GeoMath.centroid(savedPolygon)
                                if (savedPolygon.isNotEmpty() && savedCentroid != null) {
                                    Text(
                                        String.format(
                                            strings.polygonSummary,
                                            localizedDigits(savedPolygon.size.toString(), lang),
                                            localizedDigits(
                                                String.format(
                                                    Locale.US, "%.5f", savedCentroid.first
                                                ), lang
                                            ),
                                            localizedDigits(
                                                String.format(
                                                    Locale.US, "%.5f", savedCentroid.second
                                                ), lang
                                            )
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        val lat = ZoneValidator.parseDoubleFa(vm.latText) ?: 0.0
                                        val lng = ZoneValidator.parseDoubleFa(vm.lngText) ?: 0.0
                                        val radiusM = ZoneValidator.parseDoubleFa(vm.radiusText)
                                            ?.let { toMeters(it, vm.unit) } ?: 200.0
                                        navController.navigate("map_picker?lat=$lat&lng=$lng&radius=$radiusM")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.selectOnMap)
                                }
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

                        // در حالت چندضلعی، همه کنترل‌های شعاع (عنوان،
                            // اسلایدر، چیپ‌های آماده، فیلد و کلید واحد) پنهان‌اند.
                            if (!vm.isPolygon) {
                            SectionCard(icon = Icons.Filled.AddCircle, title = strings.radius) {
                            // نمایش بزرگ شعاع فعلی + اسلایدر و چیپ‌های آماده؛ هر سه
                            // روی همان وضعیت radiusText/unit می‌نویسند و فیلد عددی
                            // دقیق هم پایین‌تر سر جایش می‌ماند. اسلایدر بر حسب متر
                            // است و هنگام حرکت، واحد را هم روی متر می‌گذارد.
                            val sliderMeters = ZoneValidator.parseDoubleFa(vm.radiusText)
                                ?.let { toMeters(it, vm.unit) }
                                ?.takeIf { it > 0 } ?: 200.0
                            Text(
                                formatRadiusLocalized(sliderMeters, strings, lang),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = sliderMeters.toFloat().coerceIn(20f, 2000f),
                                onValueChange = { v ->
                                    vm.unit = RadiusUnit.METER
                                    vm.radiusText = v.roundToInt().toString()
                                },
                                valueRange = 20f..2000f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(50, 100, 200, 500).forEach { preset ->
                                    FilterChip(
                                        selected = sliderMeters == preset.toDouble(),
                                        onClick = {
                                            vm.unit = RadiusUnit.METER
                                            vm.radiusText = preset.toString()
                                        },
                                        label = {
                                            Text(
                                                formatRadiusLocalized(
                                                    preset.toDouble(),
                                                    strings,
                                                    lang
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = vm.radiusText,
                                onValueChange = { vm.radiusText = it },
                                label = { Text(strings.radius) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = vm.unit == RadiusUnit.METER,
                                    onClick = { vm.unit = RadiusUnit.METER },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    label = { Text(strings.meterUnit) }
                                )
                                SegmentedButton(
                                    selected = vm.unit == RadiusUnit.KILOMETER,
                                    onClick = { vm.unit = RadiusUnit.KILOMETER },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    label = { Text(strings.kmUnit) }
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
                                        strings.gpsAccuracyWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        } // پایان if (!vm.isPolygon) — کارت شعاع

                        // فاصله بررسی دقیق: فقط برای شعاع‌های کمتر از ۲۰۰ متر؛
                        // در حالت چندضلعی همیشه دیده می‌شود (چندضلعی‌ها با
                        // همین بازه ارزیابی می‌شوند).
                        val radiusMetersNow =
                            ZoneValidator.parseDoubleFa(vm.radiusText)?.let { toMeters(it, vm.unit) }
                        if (vm.isPolygon || (radiusMetersNow != null && radiusMetersNow < 200.0)) {
                            SectionCard(icon = Icons.Filled.Refresh, title = strings.checkIntervalTitle) {
                                Text(
                                    strings.checkIntervalSub,
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
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    trailingIcon = {
                                        Text(
                                            if (vm.checkIntervalIsMinutes) {
                                                strings.unitMinute
                                            } else {
                                                strings.unitSecond
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    }
                                )
                                val checkModeIndex = when {
                                    vm.checkIntervalContinuous -> 2
                                    vm.checkIntervalIsMinutes -> 1
                                    else -> 0
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf(
                                        strings.unitSecond,
                                        strings.unitMinute,
                                        strings.unitContinuous
                                    ).forEachIndexed { index, label ->
                                        OptionTile(
                                            selected = checkModeIndex == index,
                                            title = label,
                                            onClick = {
                                                vm.checkIntervalContinuous = index == 2
                                                vm.checkIntervalIsMinutes = index == 1
                                            }
                                        )
                                    }
                                }
                                if (vm.checkIntervalContinuous) {
                                    Text(
                                        strings.continuousNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val intervalError = CheckInterval.errorFa(
                                        CheckInterval.toSeconds(vm.checkIntervalText, vm.checkIntervalIsMinutes)
                                    )
                                    if (intervalError != null) {
                                        Text(
                                            localizeValidatorError(intervalError, strings),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        SectionCard(icon = Icons.Filled.CheckCircle, title = strings.status) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        strings.zoneActive,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Switch(checked = vm.isActive, onCheckedChange = { vm.isActive = it })
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                    1 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionCard(icon = Icons.Filled.ExitToApp, title = strings.exitBehaviorTitle) {
                            Text(
                                strings.exitInsideNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    Zone.EXIT_STOP to (strings.exitImmediate to strings.exitStopSub),
                                    Zone.EXIT_SEQUENCE to (strings.exitPattern to strings.exitSequenceSub),
                                    Zone.EXIT_CONTINUE to (strings.exitContinueTitle to strings.exitContinueSub)
                                ).forEach { (behavior, texts) ->
                                    OptionTile(
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
                                        label = { Text(strings.seqCountLabel) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = countError != null,
                                        supportingText = countError?.let { { Text(localizeValidatorError(it, strings)) } },
                                        trailingIcon = {
                                            Text(
                                                strings.unitTimes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        }
                                    )
                                    OutlinedTextField(
                                        value = vm.postExitDurationText,
                                        onValueChange = { vm.postExitDurationText = it },
                                        label = { Text(strings.seqDurationLabel) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = durationError != null,
                                        supportingText = durationError?.let { { Text(localizeValidatorError(it, strings)) } },
                                        trailingIcon = {
                                            Text(
                                                strings.unitSecond,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        }
                                    )
                                    OutlinedTextField(
                                        value = vm.postExitGapText,
                                        onValueChange = { vm.postExitGapText = it },
                                        label = { Text(strings.seqGapLabel) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = gapError != null,
                                        supportingText = gapError?.let { { Text(localizeValidatorError(it, strings)) } },
                                        trailingIcon = {
                                            Text(
                                                strings.unitSecond,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        }
                                    )
                                    Text(
                                        strings.seqManualStopNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        SectionCard(icon = Icons.Filled.PlayArrow, title = strings.alertSound) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SOUND_KEYS.forEach { key ->
                                    val label = soundLabel(strings, key)
                                    OptionTile(
                                        selected = vm.soundKey == key,
                                        title = label,
                                        onClick = { vm.soundKey = key },
                                        trailing = {
                                            IconButton(
                                                onClick = { AlertPlayer.preview(context, key) }
                                            ) {
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = "${strings.preview} $label",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { customSoundLauncher.launch("audio/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
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
                                    strings.customSoundReplaces,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        SectionCard(icon = Icons.Filled.Notifications, title = strings.alertType) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OptionTile(
                                    selected = vm.alertMode == Zone.ALERT_BOTH,
                                    title = strings.alertBothTitle,
                                    subtitle = strings.alertBothSub,
                                    onClick = { vm.alertMode = Zone.ALERT_BOTH }
                                )
                                OptionTile(
                                    selected = vm.alertMode == Zone.ALERT_SOUND,
                                    title = strings.alertSoundOnlyTitle,
                                    subtitle = strings.alertSoundOnlySub,
                                    onClick = { vm.alertMode = Zone.ALERT_SOUND }
                                )
                                OptionTile(
                                    selected = vm.alertMode == Zone.ALERT_VIBRATE,
                                    title = strings.alertVibrateOnlyTitle,
                                    subtitle = strings.alertVibrateOnlySub,
                                    onClick = { vm.alertMode = Zone.ALERT_VIBRATE }
                                )
                                OptionTile(
                                    selected = vm.alertMode == Zone.ALERT_NONE,
                                    title = strings.alertNoneTitle,
                                    subtitle = strings.alertNoneSub,
                                    onClick = { vm.alertMode = Zone.ALERT_NONE }
                                )
                            }
                        }

                        SectionCard(icon = Icons.Filled.Face, title = strings.voiceAnnounce) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        strings.voiceAnnounceTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        strings.voiceAnnounceSub,
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

                        SectionCard(icon = Icons.Filled.Email, title = strings.sms) {
                            OutlinedTextField(
                                value = vm.smsNumbersText,
                                onValueChange = { vm.smsNumbersText = it },
                                label = { Text(strings.smsNumbersLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            OutlinedTextField(
                                value = vm.smsMessageText,
                                onValueChange = { vm.smsMessageText = it },
                                label = { Text(strings.smsMessageLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Text(
                                strings.smsNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilledTonalButton(
                                onClick = {
                                    if (Permissions.hasContacts(context)) {
                                        showContactsPicker = true
                                    } else {
                                        contactsPermissionLauncher.launch(
                                            Manifest.permission.READ_CONTACTS
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(strings.pickFromContacts)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionCard(icon = Icons.Filled.DateRange, title = strings.schedule) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        strings.scheduleEnable,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        strings.scheduleEnableSub,
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
                                val selectedDays = vm.scheduleDaysSet()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    (1..7).forEach { day ->
                                        val daySelected = day in selectedDays
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (daySelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme
                                                                .surfaceVariant
                                                                .copy(alpha = 0.6f)
                                                        }
                                                    )
                                                    .clickable { vm.toggleScheduleDay(day) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    narrowDayLetter(day, lang),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (daySelected) {
                                                        FontWeight.Bold
                                                    } else {
                                                        FontWeight.Normal
                                                    },
                                                    color = if (daySelected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TimePickerField(
                                        label = strings.fromHour,
                                        minutes = vm.scheduleStartMin,
                                        onPick = { vm.scheduleStartMin = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    TimePickerField(
                                        label = strings.toHour,
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

                        SectionCard(icon = Icons.Filled.Warning, title = strings.approachAlert) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        strings.approachTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        strings.approachSub,
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
                                        strings.approachDistanceHint
                                    } else null
                                } else null
                                OutlinedTextField(
                                    value = vm.approachDistanceText,
                                    onValueChange = { vm.approachDistanceText = it },
                                    label = { Text(strings.approachDistanceLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = approachError != null,
                                    supportingText = approachError?.let { { Text(it) } },
                                    trailingIcon = {
                                        Text(
                                            strings.meterUnit,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    }
                                )
                            }
                        }

                        SectionCard(icon = Icons.Filled.Star, title = strings.stayReminder) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        strings.stayTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        strings.staySub,
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
                                        strings.stayDurationHint
                                    } else null
                                } else null
                                OutlinedTextField(
                                    value = vm.stayMinutesText,
                                    onValueChange = { vm.stayMinutesText = it },
                                    label = { Text(strings.stayMinutesLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = stayError != null,
                                    supportingText = stayError?.let { { Text(it) } },
                                    trailingIcon = {
                                        Text(
                                            strings.unitMinute,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    }
                                )
                            }
                        }

                        SectionCard(icon = Icons.Filled.Lock, title = strings.silentWindow) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        strings.silentWindowTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        strings.silentWindowSub,
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

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
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
