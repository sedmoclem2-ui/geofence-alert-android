package ir.khanoomi.geofencealert.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.BackupManager
import ir.khanoomi.geofencealert.data.PrefsRepository
import ir.khanoomi.geofencealert.domain.ColorMath.complementArgb
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.ui.theme.AppPalette
import ir.khanoomi.geofencealert.ui.theme.titleFor
import ir.khanoomi.geofencealert.ui.theme.CUSTOM_THEME_KEY
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * صفحه تنظیمات — چیدمان آکاردئونی مدرن: پنج بخش تاشو (ظاهر و زبان /
 * دسترسی‌ها و مجوزها / هشدار و اعلان / پشتیبان و داده‌ها / راهنما و
 * درباره) که هر کدام با یک سربرگ آیکن‌دار باز و بسته می‌شوند.
 * بخش «ظاهر و زبان» از نسخه ۲.۵.۰ اولین بخش است و پیش‌فرض باز می‌ماند.
 * همه رفتارهای قبلی (لانچرها، دیالوگ‌ها و وضعیت‌ها) بدون تغییر حفظ شده‌اند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
    val app = context.applicationContext as GeofenceAlertApp
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var stLocation by remember { mutableStateOf(false) }
    var stBackground by remember { mutableStateOf(false) }
    var stNotif by remember { mutableStateOf(false) }
    var stSms by remember { mutableStateOf(false) }
    var stContacts by remember { mutableStateOf(false) }
    var stBattery by remember { mutableStateOf(false) }
    var contactsAsked by remember { mutableStateOf(false) }
    var showSmsBlockedDialog by remember { mutableStateOf(false) }
    // تنها یک بخش در هر لحظه باز است؛ بخش «ظاهر و زبان» پیش‌فرض باز است.
    var expandedSection by remember { mutableStateOf(0) }

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
    val smsAskedVal by app.prefs.smsAsked.collectAsStateWithLifecycle(initialValue = false)
    val wakeScreenVal by app.prefs.wakeScreenOnAlert
        .collectAsStateWithLifecycle(initialValue = true)
    val flashlightModeVal by app.prefs.flashlightMode
        .collectAsStateWithLifecycle(initialValue = PrefsRepository.FLASHLIGHT_OFF)
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
    // خروجی CSV تاریخچه رویدادها (SAF) — همان خروجی صفحه تاریخچه، از اینجا هم.
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val events = app.db.eventDao().observeRecent().first()
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(
                            ir.khanoomi.geofencealert.domain.CsvExport
                                .eventsToCsv(events)
                                .toByteArray(Charsets.UTF_8)
                        )
                    }
                    snackbarHostState.showSnackbar(strings.csvExportDone)
                } catch (e: Exception) {
                    Log.e("Settings", "CSV export failed", e)
                }
            }
        }
    }

    fun refresh() {
        stLocation = Permissions.hasAnyLocation(context)
        stBackground = Permissions.hasBackgroundLocation(context)
        stNotif = Permissions.hasNotifications(context)
        stSms = Permissions.hasSendSms(context)
        stContacts = Permissions.hasContacts(context)
        stBattery = try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Exception) {
            false
        }
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
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refresh()
        if (!granted) {
            // اگر قبلاً هم درخواست شده و سیستم دیگر توضیحی نشان نمی‌دهد
            // (rationale = false)، یعنی درخواست مجوز مسدود شده است؛
            // راهنمای «تنظیمات محدود» را به کاربر نشان بده.
            val activity = context as? Activity
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it, Manifest.permission.SEND_SMS
                )
            } ?: true
            if (smsAskedVal && !rationale) {
                showSmsBlockedDialog = true
            }
            scope.launch { app.prefs.setSmsAsked(true) }
        }
    }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        contactsAsked = true
        refresh()
    }

    OnResume { refresh() }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---- بخش ۱: ظاهر و زبان ----
            item {
                SettingsSection(
                    icon = Icons.Filled.Star,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = strings.sectionAppearance,
                    expanded = expandedSection == 0,
                    onToggle = { expandedSection = if (expandedSection == 0) -1 else 0 }
                ) {
                    LanguageCard(
                        selectedKey = languageKey,
                        onSelect = { key -> scope.launch { app.prefs.setAppLanguage(key) } }
                    )
                    ThemePaletteCard(
                        selectedKey = themeKey,
                        onSelect = { key -> scope.launch { app.prefs.setThemePalette(key) } },
                        onCustomize = { showCustomTheme = true }
                    )
                }
            }

            // ---- بخش ۲: دسترسی‌ها و مجوزها ----
            item {
                SettingsSection(
                    icon = Icons.Filled.Lock,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = strings.sectionPermissions,
                    expanded = expandedSection == 1,
                    onToggle = { expandedSection = if (expandedSection == 1) -1 else 1 }
                ) {
                    Text(
                        strings.permIntro,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PermissionRow(
                        title = strings.permPreciseLocation,
                        desc = strings.permPreciseLocationDesc,
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
                    PermissionRow(
                        title = strings.permBackgroundLocation,
                        desc = strings.permBackgroundLocationDesc,
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            title = strings.chipNotif,
                            desc = strings.permNotificationsDesc,
                            granted = stNotif,
                            onRequest = {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        )
                    }
                    PermissionRow(
                        title = strings.permSmsTitle,
                        desc = strings.permSmsDesc,
                        granted = stSms,
                        onRequest = {
                            if (Permissions.hasSendSms(context)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.smsPermGranted)
                                }
                            } else {
                                smsLauncher.launch(Manifest.permission.SEND_SMS)
                            }
                        }
                    )
                    PermissionRow(
                        title = strings.permContacts,
                        desc = strings.permContactsDesc,
                        granted = stContacts,
                        onRequest = {
                            if (Permissions.hasContacts(context)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.contactsPermGranted)
                                }
                            } else {
                                // اگر قبلاً درخواست شده و سیستم دیگر دیالوگ
                                // نشان نمی‌دهد، مجوز برای همیشه رد شده و تنها
                                // از تنظیمات برنامه داده می‌شود.
                                val activity = context as? Activity
                                val rationale = activity?.let {
                                    ActivityCompat.shouldShowRequestPermissionRationale(
                                        it, Manifest.permission.READ_CONTACTS
                                    )
                                } ?: true
                                if (contactsAsked && !rationale) {
                                    Permissions.openAppSettings(context)
                                } else {
                                    contactsLauncher.launch(
                                        Manifest.permission.READ_CONTACTS
                                    )
                                }
                            }
                        }
                    )
                    PermissionRow(
                        title = strings.permBattery,
                        desc = strings.permBatteryDesc,
                        granted = stBattery,
                        statusText = if (stBattery) strings.batteryUnrestricted
                        else strings.batteryRestricted,
                        actionLabel = strings.permAdjust,
                        onRequest = {
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
                    OutlinedButton(
                        onClick = { Permissions.openAppSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(strings.openAppSettings) }
                }
            }

            // ---- بخش ۳: هشدار و اعلان ----
            item {
                SettingsSection(
                    icon = Icons.Filled.Notifications,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = strings.sectionAlerts,
                    expanded = expandedSection == 2,
                    onToggle = { expandedSection = if (expandedSection == 2) -1 else 2 }
                ) {
                    SoundVibrationCard(
                        volume = alertVolumeVal,
                        vibration = vibrationIntensityVal,
                        onVolumeChange = { v -> scope.launch { app.prefs.setAlertVolume(v) } },
                        onVibrationChange = { v ->
                            scope.launch { app.prefs.setVibrationIntensity(v) }
                        }
                    )
                    QuietHoursCard(
                        enabled = quietEnabled,
                        startMin = quietStart,
                        endMin = quietEnd,
                        onEnabledChange = { scope.launch { app.prefs.setQuietEnabled(it) } },
                        onStartChange = { scope.launch { app.prefs.setQuietStartMin(it) } },
                        onEndChange = { scope.launch { app.prefs.setQuietEndMin(it) } }
                    )
                    WakeScreenRow(
                        enabled = wakeScreenVal,
                        onEnabledChange = {
                            scope.launch { app.prefs.setWakeScreenOnAlert(it) }
                        }
                    )
                    FlashlightCard(
                        mode = flashlightModeVal,
                        onSelect = { scope.launch { app.prefs.setFlashlightMode(it) } }
                    )
                }
            }

            // ---- بخش ۴: پشتیبان و داده‌ها ----
            item {
                SettingsSection(
                    icon = Icons.Filled.List,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = strings.sectionBackupData,
                    expanded = expandedSection == 3,
                    onToggle = { expandedSection = if (expandedSection == 3) -1 else 3 }
                ) {
                    BackupCard(
                        onExport = { exportLauncher.launch("mahdoode-hoshdar-backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json")) },
                        onExportCsv = { csvLauncher.launch("mahdoode-hoshdar-history.csv") }
                    )
                }
            }

            // ---- بخش ۵: راهنما و درباره ----
            item {
                SettingsSection(
                    icon = Icons.Filled.Info,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = strings.sectionHelpAbout,
                    expanded = expandedSection == 4,
                    onToggle = { expandedSection = if (expandedSection == 4) -1 else 4 }
                ) {
                    Button(
                        onClick = { navController.navigate("battery") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(strings.batteryGuide) }
                    LimitationsCard()
                    val pm = context.packageManager
                    val appLabel = try {
                        pm.getApplicationLabel(
                            pm.getApplicationInfo(context.packageName, 0)
                        ).toString()
                    } catch (_: Exception) {
                        strings.appTitle
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
                        "$appLabel — ${strings.version} ${localizedDigits(versionName, lang)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

    if (showSmsBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showSmsBlockedDialog = false },
            title = {
                Text(strings.smsBlockedTitle, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(strings.smsBlockedBody)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSmsBlockedDialog = false
                        Permissions.openAppSettings(context)
                    }
                ) { Text(strings.openAppSettings, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSmsBlockedDialog = false }) {
                    Text(strings.later)
                }
            }
        )
    }
}

/**
 * یک بخش تاشوی تنظیمات: کارت با سربرگ (آیکن رنگی در دایره + عنوان +
 * پیکان) که با لمس، محتوایش با انیمیشن باز و بسته می‌شود.
 */
@Composable
private fun SettingsSection(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .animateContentSize()
                        .padding(
                            start = 14.dp, end = 14.dp, bottom = 14.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * ردیف یک مجوز در بخش دسترسی‌ها: عنوان، توضیح و وضعیت (با متن قابل
 * سفارشی‌سازی، مثلاً وضعیت باتری) + دکمه اقدام وقتی داده نشده است.
 */
@Composable
private fun PermissionRow(
    title: String,
    desc: String,
    granted: Boolean,
    statusText: String? = null,
    actionLabel: String? = null,
    onRequest: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                statusText ?: if (granted) strings.permGranted else strings.permNotGranted,
                color = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (!granted) {
            OutlinedButton(onClick = onRequest) { Text(actionLabel ?: strings.permRequest) }
        }
    }
}

/** ردیف کلید «روشن شدن صفحه هنگام هشدار» (قابل خاموش کردن توسط کاربر). */
@Composable
private fun WakeScreenRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                strings.wakeScreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                strings.wakeScreenSub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

/** انتخاب رفتار چراغ قوه هنگام هشدار: خاموش / ثابت / چشمک‌زن. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashlightCard(
    mode: String,
    onSelect: (String) -> Unit
) {
    val strings = LocalStrings.current
    val options = listOf(
        PrefsRepository.FLASHLIGHT_OFF to strings.flashOff,
        PrefsRepository.FLASHLIGHT_STEADY to strings.flashSteady,
        PrefsRepository.FLASHLIGHT_BLINK to strings.flashBlink
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            strings.flashlight,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (key, label) ->
                SegmentedButton(
                    selected = mode == key,
                    onClick = { onSelect(key) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index, count = options.size
                    ),
                    label = { Text(label) }
                )
            }
        }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    LocalStrings.current.quietHours,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    LocalStrings.current.quietHoursSub,
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
                    label = LocalStrings.current.fromLabel,
                    minutes = startMin,
                    onPick = onStartChange,
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = LocalStrings.current.toLabel,
                    minutes = endMin,
                    onPick = onEndChange,
                    modifier = Modifier.weight(1f)
                )
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            LocalStrings.current.theme,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
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
                            title = choice.palette.titleFor(LocalAppLang.current),
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
        title = { Text(LocalStrings.current.customThemeTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(LocalStrings.current.primaryColor, fontWeight = FontWeight.Bold)
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
                Text(LocalStrings.current.preview, fontWeight = FontWeight.Bold)
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
                        Text(LocalStrings.current.primaryColor, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(complement.toInt()))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Text(LocalStrings.current.complementColorAuto, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(LocalStrings.current.baseTheme, fontWeight = FontWeight.Bold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !dark,
                        onClick = { dark = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text(LocalStrings.current.lightTheme) }
                    )
                    SegmentedButton(
                        selected = dark,
                        onClick = { dark = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text(LocalStrings.current.darkTheme) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(picked, dark) }) {
                Text(LocalStrings.current.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(LocalStrings.current.cancel) }
        }
    )
}

@Composable
private fun LimitationsCard() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            LocalStrings.current.limitations,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        val st = LocalStrings.current
        val items = listOf(
            st.limit1, st.limit2, st.limit3, st.limit4,
            st.limit5, st.limit6, st.limit7
        )
        items.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            strings.language,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        SettingsPrettyOptionRow(
            selected = selectedKey == "fa",
            title = strings.langNameFa,
            subtitle = strings.langSubFa,
            onClick = { onSelect("fa") }
        )
        SettingsPrettyOptionRow(
            selected = selectedKey == "en",
            title = strings.langNameEn,
            subtitle = strings.langSubEn,
            onClick = { onSelect("en") }
        )
        SettingsPrettyOptionRow(
            selected = selectedKey == "ar",
            title = strings.langNameAr,
            subtitle = strings.langSubAr,
            onClick = { onSelect("ar") }
        )
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            strings.soundAndVibration,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${strings.volume}: ${formatPercent(volume)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = volume.toFloat(),
            onValueChange = { onVolumeChange(it.toInt().coerceIn(0, 100)) },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "${strings.vibrationIntensity}: ${formatPercent(vibration)}",
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

/** کارت پشتیبان‌گیری دستی (خروجی/ورودی از طریق SAF) + توضیح پشتیبان خودکار و خروجی CSV تاریخچه. */
@Composable
private fun BackupCard(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onExportCsv: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            strings.backup,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            strings.backupAutoNote,
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
            strings.backupReplaceWarning,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        OutlinedButton(
            onClick = onExportCsv,
            modifier = Modifier.fillMaxWidth()
        ) { Text(strings.csvExport) }
    }
}

/** نمایش درصد با ارقام و نشانه بومی زبان فعال (٪ در فارسی/عربی، % در انگلیسی). */
@Composable
private fun formatPercent(value: Int): String {
    val lang = LocalAppLang.current
    val digits = localizedDigits(value.toString(), lang)
    return if (lang == AppLang.EN) "$digits%" else "٪$digits"
}
