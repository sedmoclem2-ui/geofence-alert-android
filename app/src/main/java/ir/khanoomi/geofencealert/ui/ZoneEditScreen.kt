package ir.khanoomi.geofencealert.ui

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import ir.khanoomi.geofencealert.ui.theme.AlertRed
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
            TextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = { Text("نام محدوده (مثلاً خانه)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            TextField(
                value = vm.lngText,
                onValueChange = { vm.lngText = it },
                label = { Text("طول جغرافیایی") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    modifier = Modifier.weight(1f),
                    enabled = !locLoading
                ) {
                    if (locLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(20.dp)
                                .height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("موقعیت فعلی")
                    }
                }
                OutlinedButton(
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
                OutlinedButton(
                    onClick = { doPaste() },
                    modifier = Modifier.weight(1f)
                ) { Text("الصاق") }
            }
            locError?.let { Text(it, color = AlertRed, style = MaterialTheme.typography.bodySmall) }
            pasteError?.let { Text(it, color = AlertRed, style = MaterialTheme.typography.bodySmall) }
            Text(
                "راهنما: در گوگل‌مپ نقطه را لمس طولانی کنید تا مختصات نمایش داده شود؛ آن را کپی کنید (مثل 34.633719,50.886485) و با دکمه «الصاق» یا مستقیم در کادر عرض جغرافیایی بچسبانید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = vm.radiusText,
                    onValueChange = { vm.radiusText = it },
                    label = { Text("شعاع") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = if (vm.unit == RadiusUnit.METER) "متر" else "کیلومتر",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("واحد") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("متر") },
                            onClick = { vm.unit = RadiusUnit.METER; unitExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("کیلومتر") },
                            onClick = { vm.unit = RadiusUnit.KILOMETER; unitExpanded = false }
                        )
                    }
                }
            }

            Text("شیوه توقف هشدار", fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = vm.stopMode == Zone.STOP_AUTO,
                                onClick = { vm.stopMode = Zone.STOP_AUTO }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = vm.stopMode == Zone.STOP_AUTO,
                            onClick = { vm.stopMode = Zone.STOP_AUTO }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("توقف خودکار هنگام خروج", fontWeight = FontWeight.Bold)
                            Text(
                                "با خروج از محدوده، صدا خودکار قطع می‌شود.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = vm.stopMode == Zone.STOP_MANUAL,
                                onClick = { vm.stopMode = Zone.STOP_MANUAL }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = vm.stopMode == Zone.STOP_MANUAL,
                            onClick = { vm.stopMode = Zone.STOP_MANUAL }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("توقف فقط دستی", fontWeight = FontWeight.Bold)
                            Text(
                                "صدا پس از خروج هم ادامه دارد تا دکمه توقف زده شود.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Text("صدای هشدار", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var soundExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = soundExpanded,
                    onExpandedChange = { soundExpanded = !soundExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    val currentLabel = SOUND_OPTIONS.first { it.first == vm.soundKey }.second
                    TextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("صدا") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = soundExpanded,
                        onDismissRequest = { soundExpanded = false }
                    ) {
                        SOUND_OPTIONS.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { vm.soundKey = key; soundExpanded = false }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { AlertPlayer.preview(context, vm.soundKey) }) {
                    Text("پیشنمایش")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("محدوده فعال باشد", fontWeight = FontWeight.Bold)
                Switch(checked = vm.isActive, onCheckedChange = { vm.isActive = it })
            }

            if (errors.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        errors.forEach { Text(it, color = AlertRed) }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { doSave() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(20.dp)
                            .height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("ذخیره")
                }
            }
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("انصراف") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
