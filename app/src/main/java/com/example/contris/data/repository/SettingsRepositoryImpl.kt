package com.example.contris.data.repository

import com.example.contris.data.local.prefs.SettingsDataStore
import com.example.contris.domain.model.AppSettings
import com.example.contris.domain.model.ThemeMode
import com.example.contris.domain.model.UnitSystem
import com.example.contris.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.settings
    override suspend fun setUnitSystem(unitSystem: UnitSystem) { dataStore.setUnitSystem(unitSystem) }
    override suspend fun setThemeMode(themeMode: ThemeMode) { dataStore.setThemeMode(themeMode) }
    override suspend fun setDynamicColor(enabled: Boolean) { dataStore.setDynamicColor(enabled) }
    override suspend fun setLastSync(epochMillis: Long, total: Int) { dataStore.setLastSync(epochMillis, total) }
}
