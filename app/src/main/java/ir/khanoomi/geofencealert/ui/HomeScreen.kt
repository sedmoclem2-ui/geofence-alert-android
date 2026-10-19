package ir.khanoomi.geofencealert.ui

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import ir.khanoomi.geofencealert.domain.formatRadiusFa
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.ui.theme.AlertRed
import ir.khanoomi.geofencealert.ui.theme.AmberWarning
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun toggleActive(zone: Zone) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.upsert(
                zone.copy(
                    isActive = !zone.isActive,
                    updatedAt = System.currentTimeMillis()
                )
            )
            GeofenceHelper.refreshRegistrations(getApplication())
        }
    }

    fun deleteZone(zone: Zone) {
        viewModelScope.launch(Dispatchers.IO) {
            if (zone.isAlerting) AlertController.stopAlert(getApplication(), zone.id)
            repo.delete(zone)
            GeofenceHelper.refreshRegistrations(getApplication())
        }
    }

    fun setMonitoring(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setMonitoringEnabled(enabled)
            GeofenceHelper.refreshRegistrations(getApplication())
        }
    }

    fun stopAlert(zoneId: Long) {
        AlertController.stopAlert(getApplication(), zoneId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as GeofenceAlertApp
    val vm: HomeViewModel = viewModel { HomeViewModel(app) }

    val zones by vm.zones.collectAsStateWithLifecycle()
    val monitoring by vm.monitoringEnabled.collectAsStateWithLifecycle()
    val alerting by vm.alertingZones.collectAsStateWithLifecycle()

    var locationOn by remember { mutableStateOf(true) }
    var permLocation by remember { mutableStateOf(false) }
    var permBackground by remember { mutableStateOf(false) }
    var permNotif by remember { mutableStateOf(false) }
    var zoneToDelete by remember { mutableStateOf<Zone?>(null) }

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
    }

    OnResume { refreshStatus() }
    LaunchedEffect(Unit) {
        refreshStatus()
        withContext(Dispatchers.IO) { GeofenceHelper.refreshRegistrations(context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("هشدار محدوده") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
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
                StatusCard(
                    monitoring = monitoring,
                    onMonitoringChange = vm::setMonitoring,
                    locationOn = locationOn,
                    permLocation = permLocation,
                    permBackground = permBackground,
                    permNotif = permNotif,
                    onOpenSettings = { navController.navigate("settings") }
                )
            }

            if (alerting.isNotEmpty()) {
                items(alerting, key = { it.id }) { zone ->
                    AlertBanner(
                        zoneName = zone.name,
                        onStop = { vm.stopAlert(zone.id) }
                    )
                }
            }

            item {
                Button(
                    onClick = { navController.navigate("edit") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("افزودن محدوده جدید")
                }
            }

            if (zones.isEmpty()) {
                item {
                    Text(
                        "هنوز محدوده‌ای تعریف نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(zones, key = { it.id }) { zone ->
                    ZoneCard(
                        zone = zone,
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
            title = { Text("حذف محدوده") },
            text = { Text("«${zone.name}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteZone(zone)
                    zoneToDelete = null
                }) { Text("حذف", color = AlertRed) }
            },
            dismissButton = {
                TextButton(onClick = { zoneToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun StatusCard(
    monitoring: Boolean,
    onMonitoringChange: (Boolean) -> Unit,
    locationOn: Boolean,
    permLocation: Boolean,
    permBackground: Boolean,
    permNotif: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("پایش محدوده‌ها", fontWeight = FontWeight.Bold)
                Switch(checked = monitoring, onCheckedChange = onMonitoringChange)
            }
            StatusLine("وضعیت مکان دستگاه", locationOn, "روشن", "خاموش است")
            StatusLine("مجوز مکان", permLocation, "داده شده", "داده نشده")
            StatusLine("مکان پس‌زمینه", permBackground, "داده شده", "داده نشده")
            StatusLine("مجوز اعلان", permNotif, "داده شده", "داده نشده")
            if (!locationOn || !permLocation || !permBackground || !permNotif) {
                TextButton(onClick = onOpenSettings) {
                    Text("رفع مشکل از بخش تنظیمات")
                }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean, okText: String, badText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (ok) okText else badText,
            color = if (ok) Color(0xFF2E7D32) else AlertRed,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AlertBanner(zoneName: String, onStop: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "هشدار فعال: $zoneName",
                color = AlertRed,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("توقف هشدار")
            }
        }
    }
}

@Composable
private fun ZoneCard(
    zone: Zone,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStopAlert: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    zone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AlertRed)
                }
            }
            Text("شعاع: ${formatRadiusFa(zone.radiusMeters)}")
            Text(
                "توقف: " + if (zone.stopMode == Zone.STOP_AUTO) "خودکار هنگام خروج"
                else "فقط دستی"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (zone.isActive) "فعال" else "غیرفعال")
                Switch(checked = zone.isActive, onCheckedChange = { onToggleActive() })
            }
            if (zone.isAlerting) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "هشدار فعال است",
                    color = AlertRed,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onStopAlert,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("توقف هشدار")
                }
            } else if (!zone.isActive) {
                Text("در انتظار فعال‌سازی", color = Color.Gray)
            } else {
                Text("در انتظار ورود به محدوده", color = Color(0xFF2E7D32))
            }
        }
    }
}
