package ir.khanoomi.geofencealert.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

/** پل ارتباطی جاوااسکریپت نقشه با کامپوز. */
internal class MapBridge(private val onPick: (Double, Double) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onLocationSelected(lat: Double, lng: Double) {
        mainHandler.post { onPick(lat, lng) }
    }
}

/** انتخاب نقطه محدوده روی نقشه (OpenStreetMap). */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapPickerScreen(
    navController: NavController,
    initialLat: Double,
    initialLng: Double,
    initialRadiusM: Double
) {
    val strings = LocalStrings.current
    val appLang = LocalAppLang.current
    var selected by remember {
        mutableStateOf(
            if (initialLat != 0.0 || initialLng != 0.0) initialLat to initialLng
            else null
        )
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val bridge = remember {
        MapBridge { lat, lng -> selected = lat to lng }
    }
    // مقداردهی نقشه از طریق evaluateJavascript انجام می‌شود؛
    // خواندن query-string از روی file:///android_asset در WebView قابل اعتماد نیست.
    val initJs = remember {
        val la = String.format(Locale.US, "%.6f", initialLat)
        val ln = String.format(Locale.US, "%.6f", initialLng)
        val r = String.format(Locale.US, "%.1f", initialRadiusM.coerceAtLeast(1.0))
        "window.initMap($la, $ln, $r, 15)"
    }

    fun fetchLocation() {
        if (!Permissions.hasAnyLocation(context)) {
            Toast.makeText(
                context,
                strings.mapLocPermNeeded,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        scope.launch(Dispatchers.IO) {
            val loc = try {
                val client = LocationServices
                    .getFusedLocationProviderClient(context)
                Tasks.await(
                    client.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ),
                    15,
                    TimeUnit.SECONDS
                )
            } catch (_: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                if (loc != null) {
                    val la = String.format(Locale.US, "%.6f", loc.latitude)
                    val ln = String.format(Locale.US, "%.6f", loc.longitude)
                    webViewRef?.evaluateJavascript(
                        "window.moveMap($la, $ln)",
                        null
                    )
                } else {
                    Toast.makeText(
                        context,
                        strings.mapLocFailed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun confirm() {
        val (la, ln) = selected ?: return
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("picked_lat", la)
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("picked_lng", ln)
        navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── نوار بالا (بیرون از نقشه): بازگشت و عنوان در سمت راست، تأیید در گوشه بالا سمت چپ ──
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
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back
                    )
                }
                Text(
                    LocalStrings.current.mapPickerTitle,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    onClick = { confirm() },
                    enabled = selected != null
                ) {
                    Text(LocalStrings.current.confirm, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── نقشه (تمام فضای میانی) ──
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
                            view.evaluateJavascript(initJs, null)
                        }
                    }
                    addJavascriptInterface(bridge, "Android")
                    loadUrl("file:///android_asset/map_picker.html")
                    webViewRef = this
                }
            }
        )

        // ── نوار پایین (بیرون از نقشه): موقعیت فعلی (راست) | زوم (وسط) | مختصات (چپ) ──
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // در چیدمان راست‌به‌چپ، اولین عنصر در سمت راست قرار می‌گیرد
                TextButton(onClick = { fetchLocation() }) {
                    Text(LocalStrings.current.currentLocation, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = { webViewRef?.evaluateJavascript("window.zoomIn()", null) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    FilledTonalIconButton(
                        onClick = { webViewRef?.evaluateJavascript("window.zoomOut()", null) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text("−", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
                val (lat, lng) = selected ?: (0.0 to 0.0)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${LocalStrings.current.latitude}: ${
                            localizedDigits(String.format(Locale.US, "%.5f", lat), appLang)
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${LocalStrings.current.longitude}: ${
                            localizedDigits(String.format(Locale.US, "%.5f", lng), appLang)
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
