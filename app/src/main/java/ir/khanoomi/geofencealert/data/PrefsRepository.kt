package ir.khanoomi.geofencealert.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import ir.khanoomi.geofencealert.domain.ScheduleChecker
import ir.khanoomi.geofencealert.domain.ZoneSort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "prefs")

/** تنظیمات کلی برنامه (غیر از محدوده‌ها) در DataStore. */
class PrefsRepository(private val context: Context) {

    private val KEY_MONITORING = booleanPreferencesKey("monitoring_enabled")

    /** آیا پایش محدوده‌ها به‌طور کلی فعال است (پیش‌فرض: بله). */
    val monitoringEnabled: Flow<Boolean> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_MONITORING] ?: true }
        .catch { emit(true) }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_MONITORING] = enabled }
    }

    private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_done")

    /** آیا آموزش شروع به کار دیده شده است (پیش‌فرض: نه). */
    val onboardingDone: Flow<Boolean> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_ONBOARDING] ?: false }
        .catch { emit(false) }

    suspend fun setOnboardingDone(done: Boolean) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_ONBOARDING] = done }
    }

    private val KEY_QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
    private val KEY_QUIET_START = intPreferencesKey("quiet_start_min")
    private val KEY_QUIET_END = intPreferencesKey("quiet_end_min")

    /** آیا ساعات سکوت فعال است (پیش‌فرض: نه). */
    val quietEnabled: Flow<Boolean> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_QUIET_ENABLED] ?: false }
        .catch { emit(false) }

    suspend fun setQuietEnabled(enabled: Boolean) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_QUIET_ENABLED] = enabled }
    }

    /** شروع ساعات سکوت به دقیقه از نیمه‌شب (پیش‌فرض: ۲۳:۰۰). */
    val quietStartMin: Flow<Int> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_QUIET_START] ?: 1380 }
        .catch { emit(1380) }

    suspend fun setQuietStartMin(min: Int) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_QUIET_START] = min }
    }

    /** پایان ساعات سکوت به دقیقه از نیمه‌شب (پیش‌فرض: ۰۷:۰۰). */
    val quietEndMin: Flow<Int> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_QUIET_END] ?: 420 }
        .catch { emit(420) }

    suspend fun setQuietEndMin(min: Int) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_QUIET_END] = min }
    }

    /** آیا هم‌اکنون در بازه ساعات سکوت هستیم. */
    suspend fun isQuietNow(): Boolean {
        val prefs = context.prefsDataStore.data.first()
        return ScheduleChecker.isQuietNow(
            enabled = prefs[KEY_QUIET_ENABLED] ?: false,
            startMin = prefs[KEY_QUIET_START] ?: 1380,
            endMin = prefs[KEY_QUIET_END] ?: 420
        )
    }

    private val KEY_THEME = stringPreferencesKey("theme_palette")

    /** پالت رنگی انتخاب‌شده (پیش‌فرض: آبی عمیق «deepblue»، هماهنگ با آیکن برنامه). */
    val themePalette: Flow<String> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_THEME] ?: "deepblue" }
        .catch { emit("deepblue") }

    suspend fun setThemePalette(key: String) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_THEME] = key }
    }

    private val KEY_CUSTOM_PRIMARY = longPreferencesKey("custom_primary_argb")
    private val KEY_CUSTOM_SECONDARY = longPreferencesKey("custom_secondary_argb")
    private val KEY_CUSTOM_DARK = booleanPreferencesKey("custom_dark")

    /** رنگ اصلی تم شخصی‌سازی‌شده (ARGB به‌صورت Long؛ پیش‌فرض: سبزآبی). */
    val customPrimaryArgb: Flow<Long> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_CUSTOM_PRIMARY] ?: 0xFF0E6B5CL }
        .catch { emit(0xFF0E6B5CL) }

    suspend fun setCustomPrimaryArgb(argb: Long) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_CUSTOM_PRIMARY] = argb }
    }

    /** رنگ مکمل دستی تم شخصی‌سازی‌شده (ARGB به‌صورت Long)؛ ۰ یعنی «خودکار» (مکملِ مشتق‌شده از رنگ اصلی). */
    val customSecondaryArgb: Flow<Long> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_CUSTOM_SECONDARY] ?: 0L }
        .catch { emit(0L) }

    suspend fun setCustomSecondaryArgb(argb: Long) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_CUSTOM_SECONDARY] = argb }
    }

    /** تم پایه شخصی‌سازی‌شده: روشن (false) یا تیره (true) — پیش‌فرض: روشن. */
    val customDark: Flow<Boolean> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_CUSTOM_DARK] ?: false }
        .catch { emit(false) }

    suspend fun setCustomDark(dark: Boolean) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_CUSTOM_DARK] = dark }
    }

    private val KEY_WIDGET_ACTIVE = intPreferencesKey("widget_active_count")

    /** تعداد محدوده‌های فعال برای نمایش در ویجت (پیش‌فرض: ۰). */
    val widgetActiveCount: Flow<Int> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_WIDGET_ACTIVE] ?: 0 }
        .catch { emit(0) }

    suspend fun setWidgetActiveCount(count: Int) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_WIDGET_ACTIVE] = count }
    }

    private val KEY_ALERT_VOLUME = intPreferencesKey("alert_volume")

    /** بلندی صدای هشدار، درصد (۰ تا ۱۰۰؛ پیش‌فرض: ۸۰). */
    val alertVolume: Flow<Int> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_ALERT_VOLUME] ?: 80 }
        .catch { emit(80) }

    suspend fun setAlertVolume(v: Int) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_ALERT_VOLUME] = v.coerceIn(0, 100) }
    }

    private val KEY_VIBRATION_INTENSITY = intPreferencesKey("vibration_intensity")

    /** شدت لرزش هشدار، درصد (۰ تا ۱۰۰؛ پیش‌فرض: ۷۰). */
    val vibrationIntensity: Flow<Int> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_VIBRATION_INTENSITY] ?: 70 }
        .catch { emit(70) }

    suspend fun setVibrationIntensity(v: Int) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_VIBRATION_INTENSITY] = v.coerceIn(0, 100) }
    }

    private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

    /** زبان برنامه: «fa» (پیش‌فرض)، «en» یا «ar». */
    val appLanguage: Flow<String> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_APP_LANGUAGE] ?: "fa" }
        .catch { emit("fa") }

    suspend fun setAppLanguage(lang: String) {
        val normalized = when (lang) {
            "en", "ar" -> lang
            else -> "fa"
        }
        context.prefsDataStore.edit { prefs -> prefs[KEY_APP_LANGUAGE] = normalized }
        // آینه هم‌گام برای لایه‌های غیرکامپوزی (اعلان/ویجت/کاشی/سرویس‌ها)
        ir.khanoomi.geofencealert.ui.writeLangMirror(context, normalized)
    }

    private val KEY_LANGUAGE_CHOSEN = booleanPreferencesKey("language_chosen")

    /**
     * آیا کاربر در صحنه نخست اجرای برنامه، زبان را انتخاب کرده است
     * (پیش‌فرض: نه). کاربران قدیمی (که آموزش را دیده‌اند یا محدوده دارند)
     * در [ir.khanoomi.geofencealert.ui.AppNav] «انتخاب‌کرده» فرض می‌شوند.
     */
    val languageChosen: Flow<Boolean> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_LANGUAGE_CHOSEN] ?: false }
        .catch { emit(false) }

    suspend fun setLanguageChosen(chosen: Boolean) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_LANGUAGE_CHOSEN] = chosen }
    }

    private val KEY_SMS_ASKED = booleanPreferencesKey("sms_perm_asked")

    /** آیا مجوز پیامک دست‌کم یک بار از کاربر درخواست شده است (پیش‌فرض: نه). */
    val smsAsked: Flow<Boolean> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_SMS_ASKED] ?: false }
        .catch { emit(false) }

    suspend fun setSmsAsked(asked: Boolean) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_SMS_ASKED] = asked }
    }

    private val KEY_DRAG_HINT_SEEN = booleanPreferencesKey("drag_hint_seen")

    /**
     * آیا پیام راهنمای «برای جابه‌جایی، کارت را نگه دار و بکش» مصرف شده
     * است؛ با اولین درگ موفق true می‌شود و پیام دیگر هرگز نمایش داده
     * نمی‌شود (پیش‌فرض: نه).
     */
    val dragHintSeen: Flow<Boolean> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_DRAG_HINT_SEEN] ?: false }
        .catch { emit(false) }

    suspend fun setDragHintSeen(seen: Boolean) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_DRAG_HINT_SEEN] = seen }
    }

    private val KEY_ZONE_SORT_MODE = stringPreferencesKey("zone_sort_mode")

    /**
     * حالت مرتب‌سازی فهرست محدوده‌ها: یکی از مقادیر [ZoneSort] (پیش‌فرض:
     * «manual»). مقادیر قدیمی «name»/«radius» هنگام خواندن به معادل
     * جهت‌دار تازه نرمال می‌شوند.
     */
    val zoneSortMode: Flow<String> = context.prefsDataStore.data
        .map { prefs -> ZoneSort.normalize(prefs[KEY_ZONE_SORT_MODE]) }
        .catch { emit(ZoneSort.MANUAL) }

    suspend fun setZoneSortMode(mode: String) {
        context.prefsDataStore.edit { prefs ->
            prefs[KEY_ZONE_SORT_MODE] = ZoneSort.normalize(mode)
        }
    }

    private val KEY_WAKE_SCREEN = booleanPreferencesKey("wake_screen_on_alert")

    /**
     * روشن شدن صفحه هنگام شروع هشدار (پیش‌فرض: روشن)؛ کاربر از پنل
     * تنظیمات می‌تواند آن را خاموش کند تا هنگام اعلان، صفحه قفل روشن نشود.
     */
    val wakeScreenOnAlert: Flow<Boolean> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_WAKE_SCREEN] ?: true }
        .catch { emit(true) }

    suspend fun setWakeScreenOnAlert(enabled: Boolean) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_WAKE_SCREEN] = enabled }
    }

    private val KEY_FLASHLIGHT_MODE = stringPreferencesKey("flashlight_mode")

    /**
     * رفتار چراغ قوه هنگام هشدار: «off» (خاموش)، «steady» (ثابت) یا
     * «blink» (چشمک‌زن) — پیش‌فرض: خاموش.
     */
    val flashlightMode: Flow<String> = context.prefsDataStore.data
        .map { prefs -> normalizeFlashlightMode(prefs[KEY_FLASHLIGHT_MODE]) }
        .catch { emit(FLASHLIGHT_OFF) }

    suspend fun setFlashlightMode(mode: String) {
        context.prefsDataStore.edit { prefs ->
            prefs[KEY_FLASHLIGHT_MODE] = normalizeFlashlightMode(mode)
        }
    }

    companion object {
        const val FLASHLIGHT_OFF = "off"
        const val FLASHLIGHT_STEADY = "steady"
        const val FLASHLIGHT_BLINK = "blink"

        /** نرمال‌سازی حالت چراغ قوه؛ هر مقدار ناشناخته «خاموش» حساب می‌شود. */
        fun normalizeFlashlightMode(mode: String?): String = when (mode) {
            FLASHLIGHT_STEADY, FLASHLIGHT_BLINK -> mode
            else -> FLASHLIGHT_OFF
        }
    }
}
