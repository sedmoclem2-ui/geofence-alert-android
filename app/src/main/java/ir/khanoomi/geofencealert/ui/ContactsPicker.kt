package ir.khanoomi.geofencealert.ui

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** یک مخاطب دارای شماره تلفن (از دفترچه تلفن گوشی). */
data class ContactEntry(val name: String, val number: String)

/**
 * نرمال‌سازی شماره تلفن برای مقایسه و حذف تکراری‌ها: فقط ارقام و «+» ابتدای
 * شماره نگه داشته می‌شود و ارقام فارسی/عربی به لاتین برگردانده می‌شوند.
 */
internal fun normalizePhoneNumber(raw: String): String {
    val sb = StringBuilder()
    for (ch in raw) {
        when {
            ch in '0'..'9' -> sb.append(ch)
            ch in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
            ch in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
            ch == '+' && sb.isEmpty() -> sb.append(ch)
        }
    }
    return sb.toString()
}

/** خواندن همه مخاطبان دارای شماره از دفترچه تلفن (روی نخ IO، بدون تکرار). */
private suspend fun loadContacts(context: Context): List<ContactEntry> =
    withContext(Dispatchers.IO) {
        val result = LinkedHashMap<String, ContactEntry>()
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                val numIdx = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                while (cursor.moveToNext()) {
                    val name = if (nameIdx >= 0) {
                        cursor.getString(nameIdx)?.trim().orEmpty()
                    } else {
                        ""
                    }
                    val number = if (numIdx >= 0) {
                        cursor.getString(numIdx)?.trim().orEmpty()
                    } else {
                        ""
                    }
                    if (number.isEmpty()) continue
                    val key = normalizePhoneNumber(number)
                    if (key.length < 5) continue
                    if (!result.containsKey(key)) {
                        result[key] = ContactEntry(
                            name = name.ifEmpty { number },
                            number = number
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ContactsPicker", "Loading contacts failed", e)
        }
        result.values.toList()
    }

/**
 * دیالوگ انتخاب شماره از مخاطبین برای پیامک اطلاع‌رسانی.
 * با لمس هر ردیف، شماره‌اش به فهرست شماره‌های محدوده اضافه می‌شود (اگر
 * قبلاً نباشد) و ردیف تیک می‌خورد؛ دیالوگ برای انتخاب چند شماره باز
 * می‌ماند و با دکمه «تمام» بسته می‌شود.
 *
 * [pickedNumbers] مجموعه شماره‌های نرمال‌شده‌ای است که هم‌اکنون در
 * کادر شماره‌های فرم وجود دارند. [onPick] شماره خام انتخاب‌شده را می‌گیرد.
 */
@Composable
fun ContactsPickerDialog(
    pickedNumbers: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val granted = remember { Permissions.hasContacts(context) }
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<ContactEntry>?>(null) }

    LaunchedEffect(granted) {
        if (granted) {
            contacts = loadContacts(context)
        }
    }

    val filtered = remember(query, contacts) {
        val q = query.trim()
        val list = contacts.orEmpty()
        if (q.isEmpty()) {
            list
        } else {
            val qDigits = normalizePhoneNumber(q)
            list.filter { entry ->
                entry.name.contains(q, ignoreCase = true) ||
                    (qDigits.length >= 2 &&
                        normalizePhoneNumber(entry.number).contains(qDigits))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.contactsPickerTitle, fontWeight = FontWeight.Bold) },
        text = {
            when {
                !granted -> Text(
                    strings.contactsDenied
                )
                contacts == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    Text(strings.loading)
                }
                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(strings.contactsSearch) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    if (filtered.isEmpty()) {
                        Text(
                            strings.contactsEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(
                                filtered,
                                key = { normalizePhoneNumber(it.number) }
                            ) { entry ->
                                val key = normalizePhoneNumber(entry.number)
                                val picked = key in pickedNumbers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(entry.number) }
                                        .padding(horizontal = 4.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            entry.name,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            // جداکننده‌های جهت متن تا شماره در چیدمان
                                            // راست‌به‌چپ به‌هم نریزد.
                                            "‪${entry.number}‬",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme
                                                .colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (picked) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = strings.selectedCd,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.done, fontWeight = FontWeight.Bold)
            }
        }
    )
}
