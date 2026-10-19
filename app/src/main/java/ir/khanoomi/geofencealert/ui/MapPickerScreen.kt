package ir.khanoomi.geofencealert.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import ir.khanoomi.geofencealert.domain.TimeFormat
import java.util.Locale

/** پل ارتباطی جاوااسکریپت نقشه با کامپوز. */
internal class MapBridge(private val onPick: (Double, Double) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onLocationSelected(lat: Double, lng: Double) {
        mainHandler.post { onPick(lat, lng) }
    }
}

/** انتخاب نقطه محدوده روی نقشه (OpenStreetMap). */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapPickerScreen(
    navController: NavController,
    initialLat: Double,
    initialLng: Double,
    initialRadiusM: Double
) {
    var selected by remember {
        mutableStateOf(
            if (initialLat != 0.0 || initialLng != 0.0) initialLat to initialLng
            else null
        )
    }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("انتخاب روی نقشه", fontWeight = FontWeight.Bold) },
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val (lat, lng) = selected ?: (0.0 to 0.0)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "عرض: ${TimeFormat.toFaDigits(String.format(Locale.US, "%.5f", lat))}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "طول: ${TimeFormat.toFaDigits(String.format(Locale.US, "%.5f", lng))}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            val (la, ln) = selected ?: return@Button
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("picked_lat", la)
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("picked_lng", ln)
                            navController.popBackStack()
                        },
                        enabled = selected != null
                    ) {
                        Text("تأیید", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                }
            }
        )
    }
}
