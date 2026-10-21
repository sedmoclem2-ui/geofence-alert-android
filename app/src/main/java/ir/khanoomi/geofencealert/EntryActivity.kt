package ir.khanoomi.geofencealert

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ir.khanoomi.geofencealert.util.CrashGuard

/**
 * درگاه ورود برنامه (نقطه شروع لانچر).
 *
 * عمداً یک Activity ساده سیستمی است که با ویوهای خام فریم‌ورک ساخته
 * می‌شود و هیچ وابستگی‌ای به Compose، تم برنامه، UiStrings، دیتابیس
 * (Room) یا تنظیمات (DataStore) ندارد؛ چون اگر خطای شروع برنامه در هر
 * کدام از آن لایه‌ها باشد، صفحه اصلی هرگز ساخته نمی‌شود و گزارش خطا هم
 * به دست کاربر نمی‌رسد — اما این درگاه در هر شرایطی سالم باز می‌شود.
 *
 * رفتار: اگر گزارش خطای ذخیره‌شده‌ای از بسته‌شدن قبلی هست
 * (CrashGuard)، فقط صفحه گزارش نشان داده می‌شود تا کاربر آن را کپی
 * کند و بفرستد؛ وگرنه بی‌درنگ [MainActivity] باز می‌شود و این
 * Activity خودش را می‌بندد (در حالت عادی هیچ رابطی دیده نمی‌شود).
 */
class EntryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crash = CrashGuard.read(applicationContext)
        if (crash == null) {
            openMain()
            return
        }
        setContentView(buildReportView(crash))
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun buildReportView(crash: String): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // متن‌های این صفحه فارسی‌اند؛ چیدمان راست‌به‌چپ.
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }

        val title = TextView(this).apply {
            text = "برنامه دفعه قبل با خطا بسته شد"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
        }
        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val body = TextView(this).apply {
            text = "برای این‌که دقیقاً بفهمم مشکل کجاست، گزارش خطا را با دکمه زیر کپی کن و برای خانومی بفرست؛ یا از همین صفحه اسکرین‌شات بگیر."
            textSize = 15f
            setTextColor(Color.DKGRAY)
        }
        root.addView(
            body,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        )

        // خودِ پشته خطا ذاتاً چپ‌به‌راست است؛ پس جهت متنش LTR و فونتش
        // تک‌فاصله (monospace) است تا خوانا بماند.
        val traceView = TextView(this).apply {
            text = crash
            textSize = 11f
            typeface = Typeface.MONOSPACE
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.TOP or Gravity.LEFT
            setTextIsSelectable(true)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.rgb(240, 240, 240))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val traceScroll = ScrollView(this).apply {
            addView(
                traceView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        root.addView(
            traceScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(320)
            ).apply { topMargin = dp(14) }
        )

        val copyButton = Button(this).apply {
            text = "کپی گزارش خطا"
            setOnClickListener {
                try {
                    val clipboard =
                        getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText("crash-report", crash)
                    )
                    text = "کپی شد"
                } catch (_: Throwable) {
                    // کلیپ‌بورد در دسترس نبود؛ کاربر هنوز می‌تواند از
                    // صفحه اسکرین‌شات بگیرد.
                }
            }
        }
        root.addView(
            copyButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        )

        val enterButton = Button(this).apply {
            text = "حذف گزارش و ورود به برنامه"
            setOnClickListener {
                CrashGuard.clear(applicationContext)
                openMain()
            }
        }
        root.addView(
            enterButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        )

        return ScrollView(this).apply {
            isFillViewport = true
            addView(
                root,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }
}
