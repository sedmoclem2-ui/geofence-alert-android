package ir.khanoomi.geofencealert.ui

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import ir.khanoomi.geofencealert.domain.ZoneValidator
import ir.khanoomi.geofencealert.domain.toMeters
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.util.LocationUtil
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ZoneEditViewModel(app: Application, private val zoneId: Long) : AndroidViewModel(app) {
    private val repo get() = (getApplication<Application>() as GeofenceAlertApp).zoneRepo

    var name by mutableStateOf("")
    var latText by mutableStateOf("")
    var lngText by mutableStateOf("")
    var radiusText by mutableStateOf("")
    var unit by mutableStateOf(RadiusUnit.METER)
    var stopMode by mutableStateOf(Zone.STOP_AUTO)
    var soundKey by mutableStateOf(Zone.SOUND_ALARM)
    var isActive by mutableStateOf(true)

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
                        stopMode = z.stopMode
                        soundKey = z.soundKey
                        isActive = z.isActive
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
                            stopMode = vm.stopMode,
                            soundKey = vm.soundKey,
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
                Button(
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
            }

            SectionCard(icon = Icons.Filled.Notifications, title = "شیوه توقف هشدار") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Zone.STOP_AUTO to ("توقف خودکار" to "با خروج از محدوده، صدا قطع می‌شود."),
                        Zone.STOP_MANUAL to ("توقف دستی" to "صدا تا زدن دکمه توقف ادامه دارد.")
                    ).forEach { (mode, texts) ->
                        val selected = vm.stopMode == mode
                        Card(
                            onClick = { vm.stopMode = mode },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (selected)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { vm.stopMode = mode }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        texts.first,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        texts.second,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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
