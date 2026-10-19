package ir.khanoomi.geofencealert.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** دریافت یک‌باره موقعیت فعلی گوشی (برای دکمه «موقعیت فعلی» در فرم). */
object LocationUtil {
    private const val TAG = "LocationUtil"

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val tokenSource = CancellationTokenSource()
            cont.invokeOnCancellation { tokenSource.cancel() }
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "getCurrentLocation failed", e)
                        if (cont.isActive) cont.resume(null)
                    }
                    .addOnCanceledListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: SecurityException) {
                Log.w(TAG, "Missing location permission", e)
                if (cont.isActive) cont.resume(null)
            } catch (e: Exception) {
                Log.w(TAG, "getCurrentLocation error", e)
                if (cont.isActive) cont.resume(null)
            }
        }
}
