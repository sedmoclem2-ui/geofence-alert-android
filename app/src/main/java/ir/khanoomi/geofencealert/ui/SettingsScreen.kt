package ir.khanoomi.geofencealert.ui

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.ui.theme.AlertRed
import ir.khanoomi.geofencealert.util.Permissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    var stLocation by remember { mutableStateOf(false) }
    var stBackground by remember { mutableStateOf(false) }
    var stNotif by remember { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات و مجوزها") },
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
                OutlinedButton(
                    onClick = { Permissions.openAppSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("باز کردن تنظیمات برنامه") }
            }
            item {
                Button(
                    onClick = { navController.navigate("battery") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("راهنمای مصرف باتری (سامسونگ)") }
            }
            item { LimitationsCard() }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "هشدار محدوده — نسخه ۱٫۰٫۰",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
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
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(
                    if (granted) "داده شده" else "داده نشده",
                    color = if (granted) Color(0xFF2E7D32) else AlertRed,
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
            Text("محدودیت‌های شناخته‌شده", fontWeight = FontWeight.Bold)
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
