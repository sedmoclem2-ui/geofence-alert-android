package ir.khanoomi.geofencealert.receiver

/**
 * زمان‌بندی خالص بیدارسازی دوره‌ای Doze؛ بدون هیچ وابستگی به اندروید تا
 * منطق فاصله زمانی، مستقل از AlarmManager، قابل آزمون بماند. دلیل جدا
 * بودن از [WakeupReceiver]: همراه (companion) یک BroadcastReceiver برای
 * بارگذاری به کلاس‌های چارچوب اندروید نیاز دارد و تست واحد ساده (JUnit
 * بدون Robolectric) نمی‌تواند آن را بار کند.
 */
object WakeupTiming {

    /** فاصله بیدارسازی دوره‌ای در Doze: هر ۲ دقیقه، تا وقتی پایش روشن است. */
    const val WAKEUP_INTERVAL_MS = 120_000L

    /** لحظه بیدارسازی بعدی از روی زمان فعلی. */
    fun nextTriggerAtMs(nowMs: Long): Long = nowMs + WAKEUP_INTERVAL_MS
}
