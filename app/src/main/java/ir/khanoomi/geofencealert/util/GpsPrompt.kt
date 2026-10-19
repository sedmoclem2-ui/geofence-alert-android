package ir.khanoomi.geofencealert.util

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

/**
 * درخواست روشن شدن GPS از طریق دیالوگ سیستمی.
 *
 * نکته مهم: در اندروید مدرن هیچ برنامه‌ای (حتی سیستمیِ غیرمجاز) نمی‌تواند
 * مستقیم GPS را روشن/خاموش کند؛ تنها راه، همین دیالوگ رسمی گوگل است که
 * کاربر با یک لمس، مکان‌یابی را فعال می‌کند.
 */
private const val TAG = "GpsPrompt"

fun promptEnableGpsIfNeeded(
    activity: Activity,
    launcher: ActivityResultLauncher<IntentSenderRequest>
) {
    try {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .build()
        LocationServices.getSettingsClient(activity)
            .checkLocationSettings(settingsRequest)
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        launcher.launch(IntentSenderRequest.Builder(e.resolution).build())
                    } catch (ex: Exception) {
                        Log.w(TAG, "Could not launch GPS resolution", ex)
                    }
                } else {
                    Log.i(TAG, "Location settings not resolvable: ${e.message}")
                }
            }
    } catch (e: Exception) {
        Log.w(TAG, "promptEnableGpsIfNeeded failed", e)
    }
}
