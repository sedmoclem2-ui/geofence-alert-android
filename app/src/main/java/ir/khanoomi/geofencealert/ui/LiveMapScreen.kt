package ir.khanoomi.geofencealert.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.domain.GeoMath
import ir.khanoomi.geofencealert.ui.theme.ZoneColors
import ir.khanoomi.geofencealert.util.LocationUtil
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * پل ارتباطی جاوااسکریپت نقشه زنده با کامپوز: فقط شناسه محدوده‌ای که
 * کاربر روی شکلش زده است. شناسه به‌صورت Double می‌آید (اعداد جاوااسکریپت)
 * و همان‌جا به Long برگردانده می‌شود؛ شناسه‌های Room بسیار کوچک‌تر از
 * سقف دقت اعداد جاوااسکریپت‌اند.
 */
internal class LiveMapBridge(private val onZoneSelected: (Long) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onZoneSelected(id: Double) {
        mainHandler.post { onZoneSelected(id.toLong()) }
    }
}

/**
 * نقشه زنده محدوده‌ها (فقط‌خواندنی): همه محدوده‌ها با رنگ خودشان —
 * دایره با L.circle و چندضلعی با L.polygon — و موقعیت فعلی گوشی با
 * نشان آبی متمایز. ضربه روی یک شکل، کارت اطلاعات همان محدوده را در
 * کامپوز (نه دیالوگ جاوااسکریپت) باز می‌کند. داده محدوده‌ها با همان
 * الگوی انتخاب‌گر نقشه، یعنی JSON از طریق evaluateJavascript پس از
 * بارگذاری صفحه، به صفحه داده می‌شود. موقعیت از همان منبع انتخاب‌گر
 * نقشه می‌آید (FusedLocationProviderClient با دقت بالا، از طریق
 * [LocationUtil])؛ اگر در دسترس نباشد، نقشه با محدوده‌ها می‌ماند و فقط
 * پیام کوتاهی نشان داده می‌شود.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveMapScreen(navController: NavController) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalAppLang.current
    val app = context.applicationContext as GeofenceAlertApp
    val scope = rememberCoroutineScope()
    // تغییر وضعیت فعال/غیرفعال دقیقاً همان فراخوانی کارت صفحه اصلی است
    // (HomeViewModel.toggleActive)؛ مسیر تازه‌ای ساخته نشده است.
    val homeVm: HomeViewModel = viewModel { HomeViewModel(app) }

    val zones by app.zoneRepo.observeZones()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }
    var fitted by remember { mutableStateOf(false) }
    var myLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var selectedZoneId by remember { mutableStateOf<Long?>(null) }
    val selectedZone = zones.firstOrNull { it.id == selectedZoneId }

    val fallbackColor = MaterialTheme.colorScheme.primary
    val bridge = remember {
        LiveMapBridge { id -> selectedZoneId = id }
    }

    /** JSON محدوده‌ها برای setZones: فقط عدد و رنگ؛ نام در JS لازم نیست. */
    fun zonesJson(): String {
        return zones.joinToString(",", "[", "]") { zone ->
            val color = ZoneColors.zoneColor(zone.colorIndex, fallbackColor)
            val hex = String.format("#%06X", color.toArgb() and 0xFFFFFF)
            val la = String.format(Locale.US, "%.6f", zone.latitude)
            val ln = String.format(Locale.US, "%.6f", zone.longitude)
            val radius = String.format(Locale.US, "%.1f", zone.radiusMeters)
            val vertices = GeoMath.parsePolygonJson(zone.polygonJson)
                .joinToString(",", "[", "]") { (vLat, vLng) ->
                    val a = String.format(Locale.US, "%.6f", vLat)
                    val b = String.format(Locale.US, "%.6f", vLng)
                    "{\"lat\":$a,\"lng\":$b}"
                }
            "{\"id\":${zone.id},\"lat\":$la,\"lng\":$ln,\"radius\":$radius,\"color\":\"$hex\",\"vertices\":$vertices}"
        }
    }

    // ارسال محدوده‌ها پس از بارگذاری صفحه؛ جاگذاری در کادر فقط در
    // نخستین ارسال انجام می‌شود تا تغییر وضعیت یک محدوده، نمای کاربر
    // را ناگهان جابه‌جا نکند.
    LaunchedEffect(pageLoaded, zones) {
        if (!pageLoaded) return@LaunchedEffect
        webViewRef?.evaluateJavascript("window.setZones(${zonesJson()})", null)
        if (!fitted) {
            webViewRef?.evaluateJavascript("window.fitZones()", null)
            fitted = true
        }
    }

    // موقعیت فعلی: همان منبع انتخاب‌گر نقشه. نبودن مجوز یا ناموفق
    // بودن دریافت، نقشه را خالی نمی‌کند؛ فقط پیام کوتاهی می‌آید.
    LaunchedEffect(Unit) {
        if (!Permissions.hasAnyLocation(context)) {
            Toast.makeText(context, strings.mapLocationPermNeeded, Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }
        scope.launch {
            val loc = LocationUtil.getCurrentLocation(context)
            if (loc != null) {
                myLocation = loc.latitude to loc.longitude
            } else {
                Toast.makeText(context, strings.mapLocationFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(myLocation, pageLoaded) {
        val (la, ln) = myLocation ?: return@LaunchedEffect
        if (!pageLoaded) return@LaunchedEffect
        val a = String.format(Locale.US, "%.6f", la)
        val b = String.format(Locale.US, "%.6f", ln)
        webViewRef?.evaluateJavascript("window.setMyLocation($a, $b)", null)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── نوار بالا (بیرون از نقشه) ──
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                }
                Text(
                    strings.liveMapTitle,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── نقشه + روکش‌های کامپوز ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                pageLoaded = true
                                view.evaluateJavascript("window.initLiveMap()", null)
                            }
                        }
                        addJavascriptInterface(bridge, "Android")
                        loadUrl("file:///android_asset/live_map.html")
                        webViewRef = this
                    }
                }
            )

            if (zones.isEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Text(
                        strings.liveMapEmpty,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }

            // کارت اطلاعات محدوده انتخاب‌شده (روکش کامپوز، پایین نقشه).
            selectedZone?.let { zone ->
                Surface(
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                zone.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { selectedZoneId = null }) {
                                Icon(Icons.Filled.Close, contentDescription = strings.back)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (zone.polygonJson.isNotBlank()) {
                                    strings.shapePolygon
                                } else {
                                    formatRadiusLocalized(zone.radiusMeters, strings, lang)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                if (zone.isActive) strings.active else strings.inactive,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (zone.isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GradientButton(
                                onClick = {
                                    navController.navigate("edit?zoneId=${zone.id}")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.editZone, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { homeVm.toggleActive(zone) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (zone.isActive) strings.inactive else strings.active,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
