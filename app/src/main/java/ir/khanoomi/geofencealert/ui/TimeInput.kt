package ir.khanoomi.geofencealert.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** قالب‌بندی دقیقه‌از‌نیمه‌شب به صورت «ساعت:دقیقه» با ارقام بومی زبان. */
@Composable
fun formatClockLocalized(minutes: Int): String =
    localizedDigits(
        "%02d:%02d".format(minutes / 60, minutes % 60),
        LocalAppLang.current
    )

/**
 * دکمه انتخاب ساعت (۲۴ساعته) با دیالوگ TimePicker متریال.
 * [minutes] دقیقه از نیمه‌شب است و نتیجه هم همین واحد برمی‌گردد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    minutes: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }
    val strings = LocalStrings.current
    OutlinedButton(onClick = { show = true }, modifier = modifier) {
        Text("$label: ${formatClockLocalized(minutes)}")
    }
    if (show) {
        val state = rememberTimePickerState(
            initialHour = (minutes / 60).coerceIn(0, 23),
            initialMinute = (minutes % 60).coerceIn(0, 59),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    onPick(state.hour * 60 + state.minute)
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text(strings.cancel) }
            },
            text = { TimePicker(state = state) }
        )
    }
}
