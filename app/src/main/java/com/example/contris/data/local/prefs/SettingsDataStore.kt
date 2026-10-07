package com.example.contris.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.contris.domain.model.AppSettings
import com.example.contris.domain.model.ThemeMode
import com.example.contris.domain.model.UnitSystem
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class SettingsDataStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { prefs ->
            AppSettings(
                unitSystem = prefs[UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.METRIC,
                themeMode = prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
                lastSync = prefs[LAST_SYNC_EPOCH_MS]?.takeIf { it > 0 }?.let(Instant::ofEpochMilli),
                lastSyncTotal = prefs[LAST_SYNC_TOTAL] ?: 0,
            )
        }

    suspend fun setUnitSystem(value: UnitSystem) = dataStore.edit { it[UNIT_SYSTEM] = value.name }
    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[THEME_MODE] = value.name }
    suspend fun setDynamicColor(value: Boolean) = dataStore.edit { it[DYNAMIC_COLOR] = value }
    suspend fun setLastSync(epochMillis: Long, total: Int) = dataStore.edit {
        it[LAST_SYNC_EPOCH_MS] = epochMillis
        it[LAST_SYNC_TOTAL] = total
    }

    private companion object {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LAST_SYNC_EPOCH_MS = longPreferencesKey("last_sync_epoch_ms")
        val LAST_SYNC_TOTAL = intPreferencesKey("last_sync_total")
    }
}
