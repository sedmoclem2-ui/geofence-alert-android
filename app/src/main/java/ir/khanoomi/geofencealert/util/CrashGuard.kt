package ir.khanoomi.geofencealert.util

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نگهبان خطاهای مرگبار: اگر برنامه به هر دلیلی با استثنای مدیریت‌نشده بسته
 * شود، اول گزارش کامل خطا (زمان، نام نخ، پشته فراخوانی و نسخه برنامه) در
 * فایل داخلی `last_crash.txt` ذخیره می‌شود و بعد رفتار پیش‌فرض سیستم
 * (بسته شدن برنامه) ادامه پیدا می‌کند؛ دفعه بعد که برنامه باز شود، اگر
 * این فایل وجود داشته باشد، صفحه گزارش خطا به کاربر نشان داده می‌شود تا
 * آن را برای ما بفرستد. هیچ‌کدام از توابع این شیء نباید خطا پرتاب کنند.
 */
object CrashGuard {

    private const val FILE_NAME = "last_crash.txt"

    /** نصب نگهبان؛ باید اولین کارِ Application.onCreate باشد. */
    fun install(app: Application) {
        try {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    writeCrash(app, thread, throwable)
                } catch (_: Throwable) {
                    // خودِ نگهبان هرگز نباید خطا بدهد.
                }
                try {
                    previous?.uncaughtException(thread, throwable)
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    /**
     * متن گزارش خطای ذخیره‌شده؛ اگر فایلی نباشد یا خالی باشد null.
     * فقط به [Context] نیاز دارد (filesDir در همه کانتکست‌های برنامه
     * یکسان است) تا درگاه ورودی ساده برنامه (EntryActivity) هم بدون
     * وابستگی به کلاس Application بتواند از آن استفاده کند.
     */
    fun read(context: Context): String? = try {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            file.readText(Charsets.UTF_8).takeIf { it.isNotBlank() }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    /** حذف گزارش ذخیره‌شده (بعد از این‌که کاربر آن را دید/فرستاد). */
    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (_: Exception) {
        }
    }

    private fun writeCrash(app: Application, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        sw.appendLine(
            "time: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        )
        val version = try {
            @Suppress("DEPRECATION")
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
        sw.appendLine("appVersion: " + (version ?: "?"))
        sw.appendLine("thread: " + thread.name)
        sw.appendLine(Log.getStackTraceString(throwable))
        val file = File(app.filesDir, FILE_NAME)
        file.outputStream().use { out ->
            out.write(sw.toString().toByteArray(Charsets.UTF_8))
            out.flush()
            try {
                out.fd.sync()
            } catch (_: Exception) {
            }
        }
    }
}
