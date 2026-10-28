package ir.khanoomi.geofencealert.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import ir.khanoomi.geofencealert.GeofenceAlertApp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * پشتیبان‌گیری از محدوده‌ها به‌صورت فایل JSON.
 *
 * - پشتیبان خودکار در پوشه عمومی «Documents/محدوده هشدار/backup.json» ذخیره
 *   می‌شود تا با حذف و نصب دوباره برنامه، خود برنامه آن را پیدا و بازیابی کند.
 * - خروجی/ورودی دستی هم از همین قالب JSON استفاده می‌کند (از طریق SAF).
 * پارse/serialize با org.json خود اندروید است و هر خطا در سکوت لاگ می‌شود.
 */
object BackupManager {
    private const val TAG = "BackupManager"

    const val FOLDER_NAME = "محدوده هشدار"
    const val FILE_NAME = "backup.json"
    private const val BACKUP_FORMAT_VERSION = 2

    // ---------- serialize / parse ----------

    private fun zoneToJson(zone: Zone): JSONObject {
        val o = JSONObject()
        o.put("id", zone.id)
        o.put("name", zone.name)
        o.put("latitude", zone.latitude)
        o.put("longitude", zone.longitude)
        o.put("radiusMeters", zone.radiusMeters)
        o.put("isActive", zone.isActive)
        o.put("stopMode", zone.stopMode)
        o.put("soundKey", zone.soundKey)
        o.put("isAlerting", zone.isAlerting)
        o.put("lastTriggeredAt", zone.lastTriggeredAt)
        o.put("scheduleEnabled", zone.scheduleEnabled)
        o.put("scheduleDays", zone.scheduleDays)
        o.put("scheduleStartMin", zone.scheduleStartMin)
        o.put("scheduleEndMin", zone.scheduleEndMin)
        if (zone.customSoundUri == null) {
            o.put("customSoundUri", JSONObject.NULL)
        } else {
            o.put("customSoundUri", zone.customSoundUri)
        }
        o.put("checkIntervalSec", zone.checkIntervalSec)
        o.put("alertAcknowledged", zone.alertAcknowledged)
        o.put("exitBehavior", zone.exitBehavior)
        o.put("postExitCount", zone.postExitCount)
        o.put("postExitDurationSec", zone.postExitDurationSec)
        o.put("postExitGapSec", zone.postExitGapSec)
        o.put("approachDistanceM", zone.approachDistanceM)
        o.put("stayReminderMin", zone.stayReminderMin)
        o.put("alertMode", zone.alertMode)
        o.put("voiceAnnounce", zone.voiceAnnounce)
        o.put("smsNumbers", zone.smsNumbers)
        o.put("smsMessage", zone.smsMessage)
        o.put("silentEnabled", zone.silentEnabled)
        o.put("silentStartMin", zone.silentStartMin)
        o.put("silentEndMin", zone.silentEndMin)
        o.put("insideSince", zone.insideSince)
        o.put("stayReminded", zone.stayReminded)
        o.put("sortOrder", zone.sortOrder)
        o.put("updatedAt", zone.updatedAt)
        o.put("colorIndex", zone.colorIndex)
        o.put("polygonJson", zone.polygonJson)
        return o
    }

    private fun zoneFromJson(o: JSONObject): Zone {
        fun optStringOrNull(key: String): String? =
            if (o.isNull(key)) null else o.optString(key, "").takeIf { it.isNotEmpty() }
        return Zone(
            id = o.optLong("id", 0),
            name = o.optString("name", ""),
            latitude = o.optDouble("latitude", 0.0),
            longitude = o.optDouble("longitude", 0.0),
            radiusMeters = o.optDouble("radiusMeters", 200.0),
            isActive = o.optBoolean("isActive", true),
            stopMode = o.optString("stopMode", Zone.STOP_AUTO),
            soundKey = o.optString("soundKey", Zone.SOUND_ALARM),
            isAlerting = o.optBoolean("isAlerting", false),
            lastTriggeredAt = o.optLong("lastTriggeredAt", 0),
            scheduleEnabled = o.optBoolean("scheduleEnabled", false),
            scheduleDays = o.optString("scheduleDays", "1,2,3,4,5,6,7"),
            scheduleStartMin = o.optInt("scheduleStartMin", 0),
            scheduleEndMin = o.optInt("scheduleEndMin", 1439),
            customSoundUri = optStringOrNull("customSoundUri"),
            checkIntervalSec = o.optInt("checkIntervalSec", 20),
            alertAcknowledged = o.optBoolean("alertAcknowledged", false),
            exitBehavior = o.optInt("exitBehavior", Zone.EXIT_STOP),
            postExitCount = o.optInt("postExitCount", 3),
            postExitDurationSec = o.optInt("postExitDurationSec", 10),
            postExitGapSec = o.optInt("postExitGapSec", 30),
            approachDistanceM = o.optInt("approachDistanceM", 0),
            stayReminderMin = o.optInt("stayReminderMin", 0),
            alertMode = o.optInt("alertMode", Zone.ALERT_BOTH),
            voiceAnnounce = o.optBoolean("voiceAnnounce", false),
            smsNumbers = o.optString("smsNumbers", ""),
            smsMessage = o.optString("smsMessage", ""),
            silentEnabled = o.optBoolean("silentEnabled", false),
            silentStartMin = o.optInt("silentStartMin", 1320),
            silentEndMin = o.optInt("silentEndMin", 360),
            insideSince = o.optLong("insideSince", 0),
            stayReminded = o.optBoolean("stayReminded", false),
            sortOrder = o.optInt("sortOrder", 0),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            colorIndex = o.optInt("colorIndex", 0),
            // پیش‌فرض "" یعنی پشتیبان‌های ۳٫۸٫۰ (بدون این کلید) به‌صورت دایره بازیابی می‌شوند.
            polygonJson = o.optString("polygonJson", "")
        )
    }

    /** ساخت متن JSON پشتیبان از همه محدوده‌ها. */
    suspend fun zonesToJson(context: Context): String {
        val app = context.applicationContext as GeofenceAlertApp
        val zones = try {
            app.zoneRepo.getAll()
        } catch (e: Exception) {
            Log.e(TAG, "getAll failed", e)
            emptyList()
        }
        val arr = JSONArray()
        zones.forEach { arr.put(zoneToJson(it)) }
        val root = JSONObject()
        root.put("app", "geofence-alert")
        root.put("version", BACKUP_FORMAT_VERSION)
        root.put("zones", arr)
        return root.toString()
    }

    /** پارس متن JSON به فهرست محدوده‌ها؛ خطا → null. */
    fun parseZones(json: String): List<Zone>? {
        return try {
            val root = JSONObject(json)
            val arr = root.optJSONArray("zones") ?: return null
            val result = mutableListOf<Zone>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                result += zoneFromJson(o)
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "parseZones failed", e)
            null
        }
    }

    /**
     * جایگزینی همه محدوده‌ها با محتوای JSON (ورودی دستی).
     * شناسه‌ها تازه ساخته می‌شوند و وضعیت‌های گذرای اقامت پاک می‌گردد.
     * @return درست اگر حداقل پارس موفق بوده باشد.
     */
    suspend fun replaceZonesFromJson(context: Context, json: String): Boolean {
        val zones = parseZones(json) ?: return false
        val app = context.applicationContext as GeofenceAlertApp
        return try {
            app.zoneRepo.deleteAll()
            for (z in zones) {
                app.zoneRepo.upsert(
                    z.copy(id = 0, isAlerting = false, insideSince = 0, stayReminded = false)
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "replaceZonesFromJson failed", e)
            false
        }
    }

    // ---------- پشتیبان خودکار در پوشه Documents ----------

    private fun relativePath(): String =
        Environment.DIRECTORY_DOCUMENTS + "/" + FOLDER_NAME + "/"

    /** نوشتن پشتیبان خودکار در Documents عمومی؛ در صورت خطا در سکوت رد می‌شود. */
    suspend fun writeAutoBackup(context: Context) {
        try {
            val json = zonesToJson(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(context, json)
            } else {
                writeViaFile(context, json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "writeAutoBackup failed", e)
        }
    }

    private fun findExistingUri(context: Context): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val args = arrayOf(FILE_NAME, relativePath())
        return try {
            resolver.query(collection, projection, selection, args, null)?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(0)
                    Uri.withAppendedPath(collection, id.toString())
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "findExistingUri failed", e)
            null
        }
    }

    private fun writeViaMediaStore(context: Context, json: String) {
        val resolver = context.contentResolver
        var uri = findExistingUri(context)
        if (uri == null) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath())
            }
            uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
        }
        if (uri == null) {
            Log.w(TAG, "Cannot create backup file uri")
            return
        }
        resolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }
        Log.i(TAG, "Auto backup written")
    }

    @Suppress("DEPRECATION")
    private fun writeViaFile(context: Context, json: String) {
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "WRITE_EXTERNAL_STORAGE not granted; skipping auto backup (pre-29)")
            return
        }
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            FOLDER_NAME
        )
        if (!dir.exists()) dir.mkdirs()
        File(dir, FILE_NAME).writeText(json, Charsets.UTF_8)
        Log.i(TAG, "Auto backup written (file)")
    }

    /**
     * بازیابی خودکار: فقط وقتی جدول محدوده‌ها خالی است، اگر فایل پشتیبان در
     * Documents عمومی پیدا شود، محدوده‌ها از آن ساخته می‌شوند.
     * @return درست اگر بازیابی انجام شده باشد.
     */
    suspend fun tryAutoRestore(context: Context): Boolean {
        return try {
            val app = context.applicationContext as GeofenceAlertApp
            val existing = try {
                app.zoneRepo.getAll()
            } catch (e: Exception) {
                Log.e(TAG, "getAll failed in restore", e)
                return false
            }
            if (existing.isNotEmpty()) return false
            val json = readAutoBackup(context) ?: return false
            val zones = parseZones(json) ?: return false
            if (zones.isEmpty()) return false
            for (z in zones) {
                app.zoneRepo.upsert(
                    z.copy(id = 0, isAlerting = false, insideSince = 0, stayReminded = false)
                )
            }
            Log.i(TAG, "Auto restore completed: ${zones.size} zones")
            true
        } catch (e: Exception) {
            Log.e(TAG, "tryAutoRestore failed", e)
            false
        }
    }

    private fun readAutoBackup(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = findExistingUri(context) ?: return null
            return try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    String(input.readBytes(), Charsets.UTF_8)
                }
            } catch (e: Exception) {
                Log.e(TAG, "readAutoBackup failed", e)
                null
            }
        }
        @Suppress("DEPRECATION")
        val file = File(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                FOLDER_NAME
            ),
            FILE_NAME
        )
        return try {
            if (file.exists()) file.readText(Charsets.UTF_8) else null
        } catch (e: Exception) {
            Log.e(TAG, "readAutoBackup(file) failed", e)
            null
        }
    }

    // ---------- خروجی/ورودی دستی از طریق SAF ----------

    /** نوشتن پشتیبان در URI انتخاب‌شده توسط کاربر (CreateDocument). */
    suspend fun writeBackupToUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = zonesToJson(context)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        } catch (e: Exception) {
            Log.e(TAG, "writeBackupToUri failed", e)
            false
        }
    }

    /** خواندن پشتیبان از URI انتخاب‌شده و جایگزینی همه محدوده‌ها. */
    suspend fun importFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                String(input.readBytes(), Charsets.UTF_8)
            } ?: return false
            replaceZonesFromJson(context, json)
        } catch (e: Exception) {
            Log.e(TAG, "importFromUri failed", e)
            false
        }
    }
}
