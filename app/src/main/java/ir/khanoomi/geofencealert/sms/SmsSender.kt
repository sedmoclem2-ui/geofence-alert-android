package ir.khanoomi.geofencealert.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * ارسال پیامک اطلاع‌رسانی ورود به محدوده.
 * هر خطا (نداشتن مجوز، شماره نامعتبر، خطای مودم) در سکوت ثبت می‌شود و
 * هرگز به فراخواننده برنمی‌گردد تا جریان هشدار مختل نشود.
 */
object SmsSender {
    private const val TAG = "SmsSender"

    private fun smsManager(context: Context): SmsManager? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot get SmsManager", e)
            null
        }
    }

    /**
     * ارسال [message] به همه شماره‌های [numbersRaw] (هر خط/ویرگول/نقطه‌ویرگول
     * یک شماره). بدون مجوز SEND_SMS یا با ورودی خالی، هیچ کاری نمی‌شود.
     */
    fun send(context: Context, numbersRaw: String, message: String) {
        // فقط applicationContext: این مسیر از رسیور پس‌زمینه (در حالی که اپ
        // بسته است و فرایند تازه از سرد استارت خورده) هم فراخوانی می‌شود و
        // نباید به هیچ Activity یا Context گذرا وابسته باشد.
        val appContext = context.applicationContext
        try {
            if (message.isBlank()) return
            if (ContextCompat.checkSelfPermission(
                    appContext, Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "SEND_SMS not granted; skipping SMS")
                return
            }
            val numbers = numbersRaw
                .split('\n', ',', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (numbers.isEmpty()) return
            val sm = smsManager(appContext) ?: return
            for (number in numbers) {
                try {
                    val parts = sm.divideMessage(message)
                    if (parts.size <= 1) {
                        sm.sendTextMessage(number, null, message, null, null)
                    } else {
                        sm.sendMultipartTextMessage(number, null, parts, null, null)
                    }
                    Log.i(TAG, "SMS sent to $number")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SMS to $number", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SmsSender.send failed", e)
        }
    }
}
