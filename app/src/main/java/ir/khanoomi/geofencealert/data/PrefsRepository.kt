package ir.khanoomi.geofencealert.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
}
