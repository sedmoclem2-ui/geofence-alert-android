package ir.khanoomi.geofencealert.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.ui.stringsForContext
import ir.khanoomi.geofencealert.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow

/**
 * صفحه انتخاب محدوده برای ویجت تک‌محدوده.
 * هنگام افزودن ویجت به صفحه اصلی باز می‌شود؛ با انتخاب یک محدوده،
 * همان ویجت به آن محدوده متصل می‌گردد.
 */
class ZoneWidgetConfigureActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContent {
            AppTheme {
                val strings = stringsForContext(this)
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    strings.widgetPickTitle,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                ) { padding ->
                    ZonePickList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onPick = { zone ->
                            SingleZoneWidgetProvider.setZoneForWidget(
                                this, appWidgetId, zone.id
                            )
                            SingleZoneWidgetProvider.updateOne(
                                this,
                                AppWidgetManager.getInstance(this),
                                appWidgetId
                            )
                            val result = Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId
                            )
                            setResult(RESULT_OK, result)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ZonePickList(
    modifier: Modifier = Modifier,
    onPick: (Zone) -> Unit
) {
    val context = LocalContext.current
    val strings = stringsForContext(context)
    val zonesFlow: Flow<List<Zone>> = remember {
        (context.applicationContext as GeofenceAlertApp).zoneRepo.observeZones()
    }
    val zones by zonesFlow.collectAsState(initial = emptyList())
    if (zones.isEmpty()) {
        Column(
            modifier = modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                strings.widgetNoZones,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                strings.widgetNoZonesSub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }
    LazyColumn(modifier = modifier) {
        items(zones, key = { it.id }) { zone ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(zone) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(zone.name, fontWeight = FontWeight.Bold)
                    Text(
                        if (zone.isActive) strings.active else strings.inactive,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
