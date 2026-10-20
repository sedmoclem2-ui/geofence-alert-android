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

    /** پالت رنگی انتخاب‌شده: "teal" | "indigo" | "rose" (پیش‌فرض: teal). */
    val themePalette: Flow<String> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_THEME] ?: "teal" }
        .catch { emit("teal") }

    suspend fun setThemePalette(key: String) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_THEME] = key }
    }

    private val KEY_CUSTOM_PRIMARY = longPreferencesKey("custom_primary_argb")
    private val KEY_CUSTOM_DARK = booleanPreferencesKey("custom_dark")

    /** رنگ اصلی تم شخصی‌سازی‌شده (ARGB به‌صورت Long؛ پیش‌فرض: سبزآبی). */
    val customPrimaryArgb: Flow<Long> = context.prefsDataStore.data
        .map { prefs -> prefs[KEY_CUSTOM_PRIMARY] ?: 0xFF0E6B5CL }
        .catch { emit(0xFF0E6B5CL) }

    suspend fun setCustomPrimaryArgb(argb: Long) {
        context.prefsDataStore.edit { prefs -> prefs[KEY_CUSTOM_PRIMARY] = argb }
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
}
