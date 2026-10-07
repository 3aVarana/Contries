package com.example.contris.domain.repository

import com.example.contris.domain.model.AppSettings
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.model.QuizResult
import com.example.contris.domain.model.SyncStatus
import com.example.contris.domain.model.ThemeMode
import com.example.contris.domain.model.UnitSystem
import kotlinx.coroutines.flow.Flow

interface CountryRepository {
    fun observeCountries(query: String, filters: CountryFilters, sort: CountrySort): Flow<List<CountrySummary>>
    fun observeCountry(uuid: String): Flow<Country?>
    suspend fun getByAlpha3(codes: List<String>): List<CountrySummary>
    fun observeRegions(): Flow<List<String>>
    fun observeSubregions(): Flow<List<String>>
    fun observeCount(): Flow<Int>
    fun observeSyncStatus(): Flow<SyncStatus>

    /**
     * Synchronises the local database with the API.
     * No-op (returns success) when the data is fresh and [force] is false.
     * Failures are reported as [com.example.contris.domain.model.SyncException] inside the [Result].
     */
    suspend fun sync(force: Boolean = false): Result<Unit>

    suspend fun getQuizPool(): List<Country>
}

interface FavoritesRepository {
    fun observeFavorites(): Flow<List<CountrySummary>>
    fun observeIsFavorite(uuid: String): Flow<Boolean>
    suspend fun toggle(uuid: String)
    suspend fun add(uuid: String)
    suspend fun remove(uuid: String)
    suspend fun clear()
}

interface QuizRepository {
    suspend fun save(result: QuizResult)
    fun observeHistory(limit: Int = 20): Flow<List<QuizResult>>
    fun observeBest(mode: QuizMode): Flow<QuizResult?>
    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setUnitSystem(unitSystem: UnitSystem)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setLastSync(epochMillis: Long, total: Int)
}
