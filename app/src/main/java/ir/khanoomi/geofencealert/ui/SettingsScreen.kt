package ir.khanoomi.geofencealert.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.BackupManager
import ir.khanoomi.geofencealert.domain.ColorMath.complementArgb
import ir.khanoomi.geofencealert.domain.TimeFormat
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.ui.theme.AppPalette
import ir.khanoomi.geofencealert.ui.theme.CUSTOM_THEME_KEY
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.pm.PackageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val app = context.applicationContext as GeofenceAlertApp
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var stLocation by remember { mutableStateOf(false) }
    var stBackground by remember { mutableStateOf(false) }
    var stNotif by remember { mutableStateOf(false) }

    val quietEnabled by app.prefs.quietEnabled.collectAsStateWithLifecycle(initialValue = false)
    val quietStart by app.prefs.quietStartMin.collectAsStateWithLifecycle(initialValue = 1380)
    val quietEnd by app.prefs.quietEndMin.collectAsStateWithLifecycle(initialValue = 420)
    val themeKey by app.prefs.themePalette.collectAsStateWithLifecycle(initialValue = "deepblue")
    val customArgb by app.prefs.customPrimaryArgb.collectAsStateWithLifecycle(initialValue = 0xFF0E6B5CL)
    val customDarkVal by app.prefs.customDark.collectAsStateWithLifecycle(initialValue = false)
    val languageKey by app.prefs.appLanguage.collectAsStateWithLifecycle(initialValue = "fa")
    val alertVolumeVal by app.prefs.alertVolume.collectAsStateWithLifecycle(initialValue = 80)
    val vibrationIntensityVal by app.prefs.vibrationIntensity
        .collectAsStateWithLifecycle(initialValue = 70)
    var showCustomTheme by remember { mutableStateOf(false) }

    // خروجی پشتیبان دستی (SAF): نوشتن JSON همه محدوده‌ها
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val ok = BackupManager.writeBackupToUri(context, uri)
                snackbarHostState.showSnackbar(
                    if (ok) strings.backupDone else strings.backupFailed
                )
            }
        }
    }
    // ورودی پشتیبان دستی (SAF): جایگزینی همه محدوده‌ها
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val ok = BackupManager.importFromUri(context, uri)
                if (ok) {
                    GeofenceHelper.refreshRegistrations(context)
                    BackupManager.writeAutoBackup(context)
                    snackbarHostState.showSnackbar(strings.backupDone)
                } else {
                    snackbarHostState.showSnackbar(strings.backupFailed)
                }
            }
        }
    }

    fun refresh() {
        stLocation = Permissions.hasAnyLocation(context)
        stBackground = Permissions.hasBackgroundLocation(context)
        stNotif = Permissions.hasNotifications(context)
    }

    val fineLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh() }
    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh() }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh() }

    OnResume { refresh() }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "برای کارکرد درست هشدارها، مجوزهای زیر لازم است. اگر مجوزی رد شده، " +
                        "از همین‌جا دوباره درخواست کنید یا به تنظیمات برنامه بروید.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                PermissionRow(
                    title = "مکان دقیق",
                    desc = "برای تشخیص ورود به محدوده لازم است.",
                    granted = stLocation,
                    onRequest = {
                        fineLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
            item {
                PermissionRow(
                    title = "مکان در پس‌زمینه",
                    desc = "برای هشدار وقتی برنامه باز نیست (اندروید ۱۰ به بالا).",
                    granted = stBackground,
                    onRequest = {
                        // در اندروید ۱۱ به بالا باید از تنظیمات سیستم داده شود
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            Permissions.openAppSettings(context)
                        }
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    PermissionRow(
                        title = "اعلان‌ها",
                        desc = "برای نمایش هشدار و دکمه توقف (اندروید ۱۳ به بالا).",
                        granted = stNotif,
                        onRequest = {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
            }
            item {
                QuietHoursCard(
                    enabled = quietEnabled,
                    startMin = quietStart,
                    endMin = quietEnd,
                    onEnabledChange = { scope.launch { app.prefs.setQuietEnabled(it) } },
                    onStartChange = { scope.launch { app.prefs.setQuietStartMin(it) } },
                    onEndChange = { scope.launch { app.prefs.setQuietEndMin(it) } }
                )
            }
            item {
                ThemePaletteCard(
                    selectedKey = themeKey,
                    onSelect = { key -> scope.launch { app.prefs.setThemePalette(key) } },
                    onCustomize = { showCustomTheme = true }
                )
            }
            item {
                LanguageCard(
                    selectedKey = languageKey,
                    onSelect = { key -> scope.launch { app.prefs.setAppLanguage(key) } }
                )
            }
            item {
                SoundVibrationCard(
                    volume = alertVolumeVal,
                    vibration = vibrationIntensityVal,
                    onVolumeChange = { v -> scope.launch { app.prefs.setAlertVolume(v) } },
                    onVibrationChange = { v -> scope.launch { app.prefs.setVibrationIntensity(v) } }
                )
            }
            item {
                BackupCard(
                    onExport = { exportLauncher.launch("mahdoode-hoshdar-backup.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) }
                )
            }
            item {
                OutlinedButton(
                    onClick = { Permissions.openAppSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("باز کردن تنظیمات برنامه") }
            }
            item {
                Button(
                    onClick = { navController.navigate("battery") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.batteryGuide) }
            }
            item { LimitationsCard() }
            item {
                Spacer(Modifier.height(8.dp))
                val pm = context.packageManager
                val appLabel = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(context.packageName, 0)).toString()
                } catch (_: Exception) {
                    "محدوده هشدار"
                }
                val versionName = try {
                    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(
                            context.packageName,
                            PackageManager.PackageInfoFlags.of(0)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(context.packageName, 0)
                    }
                    info.versionName ?: ""
                } catch (_: Exception) {
                    ""
                }
                Text(
                    "$appLabel — ${strings.version} ${TimeFormat.toFaDigits(versionName)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showCustomTheme) {
        // مقادیر اولیه مستقیم پاس داده می‌شوند؛ وضعیت دیالوگ با remember(initial…) ساخته می‌شود.
        CustomThemeDialog(
            initialArgb = customArgb,
            initialDark = customDarkVal,
            onDismiss = { showCustomTheme = false },
            onSave = { argb, dark ->
                scope.launch {
                    app.prefs.setCustomPrimaryArgb(argb)
                    app.prefs.setCustomDark(dark)
                    app.prefs.setThemePalette(CUSTOM_THEME_KEY)
                }
                showCustomTheme = false
            }
        )
    }
}

@Composable
private fun QuietHoursCard(
    enabled: Boolean,
    startMin: Int,
    endMin: Int,
    onEnabledChange: (Boolean) -> Unit,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(LocalStrings.current.quietHours, fontWeight = FontWeight.Bold)
                    Text(
                        "در این بازه، هشدار فقط با اعلان و بدون صدا نمایش داده می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimePickerField(
                        label = "از",
                        minutes = startMin,
                        onPick = onStartChange,
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = "تا",
                        minutes = endMin,
                        onPick = onEndChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** یک گزینه در شبکه انتخاب تم: یا پالت آماده، یا شخصی‌سازی. */
private sealed interface ThemeChoice {
    data class Preset(val palette: AppPalette) : ThemeChoice
    data object Custom : ThemeChoice
}

@Composable
private fun ThemePaletteCard(
    selectedKey: String,
    onSelect: (String) -> Unit,
    onCustomize: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(LocalStrings.current.theme, fontWeight = FontWeight.Bold)
            val choices: List<ThemeChoice> =
                AppPalette.entries.map { ThemeChoice.Preset(it) } + ThemeChoice.Custom
            choices.chunked(4).forEach { rowChoices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowChoices.forEach { choice ->
                        when (choice) {
                            is ThemeChoice.Preset -> PaletteTile(
                                title = choice.palette.titleFa,
                                selected = choice.palette.key == selectedKey,
                                circleModifier = Modifier.background(choice.palette.light.primary),
                                onClick = { onSelect(choice.palette.key) }
                            )
                            ThemeChoice.Custom -> PaletteTile(
                                title = LocalStrings.current.customizeTheme,
                                selected = selectedKey == CUSTOM_THEME_KEY,
                                circleModifier = Modifier.background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFE53935), Color(0xFFFFB300),
                                            Color(0xFF43A047), Color(0xFF1E88E5),
                                            Color(0xFF8E24AA), Color(0xFFE53935)
                                        )
                                    )
                                ),
                                onClick = onCustomize
                            )
                        }
                    }
                    // تکمیل ستون‌های خالی ردیف آخر تا چیدمان شبکه‌ای به هم نریزد
                    repeat(4 - rowChoices.size) {
                        Spacer(Modifier.size(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteTile(
    title: String,
    selected: Boolean,
    circleModifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(circleModifier)
                .then(
                    if (selected) Modifier.border(
                        3.dp,
                        MaterialTheme.colorScheme.onSurface,
                        CircleShape
                    ) else Modifier
                )
        )
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** رنگ‌های ثابت قابل انتخاب برای تم شخصی‌سازی. */
private val CUSTOM_SWATCHES = listOf(
    0xFFE53935L, 0xFFD81B60L, 0xFF8E24AAL, 0xFF3949ABL,
    0xFF1E88E5L, 0xFF00ACC1L, 0xFF00897BL, 0xFF43A047L,
    0xFF7CB342L, 0xFFFFB300L, 0xFFFB8C00L, 0xFF8D6E63L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomThemeDialog(
    initialArgb: Long,
    initialDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, Boolean) -> Unit
) {
    var picked by remember(initialArgb) { mutableStateOf(initialArgb) }
    var dark by remember(initialDark) { mutableStateOf(initialDark) }
    // رنگ مکمل به‌صورت خودکار از رنگ اصلی ساخته می‌شود (قانون رنگ مکمل).
    val complement = complementArgb(picked)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("شخصی‌سازی تم", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("رنگ اصلی", fontWeight = FontWeight.Bold)
                CUSTOM_SWATCHES.chunked(4).forEach { rowSwatches ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowSwatches.forEach { argb ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    // تبدیل از مسیر @ColorInt؛ Color(argb.toULong()) رنگ
                                    // نامعتبر می‌ساخت و همین‌جا کرش می‌کرد.
                                    .background(Color(argb.toInt()))
                                    .border(
                                        if (picked == argb) 3.dp else 1.dp,
                                        if (picked == argb) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                    .clickable { picked = argb }
                            )
                        }
                    }
                }
                Text("پیش‌نمایش", fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(picked.toInt()), Color(complement.toInt()))
                            )
                        )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(picked.toInt()))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Text("رنگ اصلی", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(complement.toInt()))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Text("رنگ مکمل (خودکار)", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("تم پایه", fontWeight = FontWeight.Bold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !dark,
                        onClick = { dark = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("روشن") }
                    )
                    SegmentedButton(
                        selected = dark,
                        onClick = { dark = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("تیره") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(picked, dark) }) {
                Text("ذخیره", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun PermissionRow(
    title: String,
    desc: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (granted) "داده شده" else "داده نشده",
                    color = if (granted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!granted) {
                OutlinedButton(onClick = onRequest) { Text("درخواست") }
            }
        }
    }
}

@Composable
private fun LimitationsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(LocalStrings.current.limitations, fontWeight = FontWeight.Bold)
            val items = listOf(
                "حداکثر ۱۰۰ ژئوفنس هم‌زمان توسط اندروید پشتیبانی می‌شود.",
                "پس از «Force Stop» کردن برنامه از تنظیمات، ژئوفنس‌ها توسط سیستم حذف می‌شوند و تا باز شدن دوباره برنامه کار نمی‌کنند.",
                "در حالت ذخیره باتری (Doze) یا بهینه‌سازی تهاجمی باتری — به‌ویژه در گوشی‌های سامسونگ — ممکن است رویداد ورود/خروج با تأخیر برسد.",
                "برای هشدار در پس‌زمینه باید دسترسی مکان «همیشه» (Allow all the time) داده شود.",
                "دقت تشخیص به GPS و شبکه بستگی دارد؛ داخل ساختمان ممکن است خطا داشته باشد.",
                "اگر هنگام ثبت محدوده داخل آن باشید، هشداری فعال نمی‌شود؛ فقط «ورود جدید» هشدار می‌دهد.",
                "خاموش بودن گوشی یا لغو مجوزها، تشخیص ورود را متوقف می‌کند؛ برنامه چنین چیزی را تضمین نمی‌کند."
            )
            items.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}


/** ردیف گزینه انتخابی (هم‌خانواده PrettyOptionRow صفحه ویرایش، مستقل برای این فایل). */
@Composable
private fun SettingsPrettyOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val sg = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
    else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = fg)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = sg
                    )
                }
            }
        }
    }
}

/** کارت انتخاب زبان برنامه: فقط فارسی، انگلیسی و عربی. */
@Composable
private fun LanguageCard(
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(strings.language, fontWeight = FontWeight.Bold)
            SettingsPrettyOptionRow(
                selected = selectedKey == "fa",
                title = "فارسی",
                subtitle = "Persian",
                onClick = { onSelect("fa") }
            )
            SettingsPrettyOptionRow(
                selected = selectedKey == "en",
                title = "English",
                subtitle = "English",
                onClick = { onSelect("en") }
            )
            SettingsPrettyOptionRow(
                selected = selectedKey == "ar",
                title = "العربية",
                subtitle = "Arabic",
                onClick = { onSelect("ar") }
            )
        }
    }
}

/** کارت تنظیم سراسری بلندی صدای هشدار و شدت لرزش. */
@Composable
private fun SoundVibrationCard(
    volume: Int,
    vibration: Int,
    onVolumeChange: (Int) -> Unit,
    onVibrationChange: (Int) -> Unit
) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(strings.soundAndVibration, fontWeight = FontWeight.Bold)
            Text(
                "${strings.volume}: ٪${TimeFormat.toFaDigits(volume.toString())}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = volume.toFloat(),
                onValueChange = { onVolumeChange(it.toInt().coerceIn(0, 100)) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${strings.vibrationIntensity}: ٪${TimeFormat.toFaDigits(vibration.toString())}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = vibration.toFloat(),
                onValueChange = { onVibrationChange(it.toInt().coerceIn(0, 100)) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** کارت پشتیبان‌گیری دستی (خروجی/ورودی از طریق SAF) + توضیح پشتیبان خودکار. */
@Composable
private fun BackupCard(
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(strings.backup, fontWeight = FontWeight.Bold)
            Text(
                "برنامه به‌طور خودکار از محدوده‌ها در پوشه Documents گوشی پشتیبان می‌گیرد؛ " +
                    "اگر برنامه را حذف و دوباره نصب کنید، محدوده‌ها خودبه‌خود برمی‌گردند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.backupExport) }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.backupImport) }
            Text(
                "ورودی پشتیبان همه محدوده‌های فعلی را با محتوای فایل جایگزین می‌کند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
