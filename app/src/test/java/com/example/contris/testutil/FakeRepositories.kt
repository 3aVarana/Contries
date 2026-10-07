package com.example.contris.testutil

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
import com.example.contris.domain.repository.CountryRepository
import com.example.contris.domain.repository.FavoritesRepository
import com.example.contris.domain.repository.QuizRepository
import com.example.contris.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** In-memory CountryRepository. Search/filter/sort are applied in Kotlin to mirror the SQL behaviour. */
class FakeCountryRepository : CountryRepository {
    val countries = MutableStateFlow<List<Country>>(emptyList())
    val syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle(null, 0))
    val favoriteUuids = MutableStateFlow<Set<String>>(emptySet())
    var syncResult: Result<Unit> = Result.success(Unit)
    val syncCalls = mutableListOf<Boolean>()

    override fun observeCountries(query: String, filters: CountryFilters, sort: CountrySort): Flow<List<CountrySummary>> =
        combine(countries, favoriteUuids) { list, favs ->
            list.asSequence()
                .filter { query.isBlank() || it.names.common.contains(query, ignoreCase = true) }
                .filter { filters.regions.isEmpty() || it.region in filters.regions }
                .filter { !filters.favoritesOnly || it.uuid in favs }
                .filter { !filters.sovereignOnly || it.classification.sovereign }
                .map { it.toSummary() }
                .toList()
                .let { l ->
                    when (sort) {
                        CountrySort.NAME_ASC -> l.sortedBy { it.name }
                        CountrySort.NAME_DESC -> l.sortedByDescending { it.name }
                        CountrySort.POPULATION_DESC -> l.sortedWith(compareBy<CountrySummary> { it.population == null }.thenByDescending { it.population })
                        CountrySort.POPULATION_ASC -> l.sortedWith(compareBy<CountrySummary> { it.population == null }.thenBy { it.population })
                        CountrySort.AREA_DESC -> l.sortedWith(compareBy<CountrySummary> { it.areaKm2 == null }.thenByDescending { it.areaKm2 })
                        CountrySort.AREA_ASC -> l.sortedWith(compareBy<CountrySummary> { it.areaKm2 == null }.thenBy { it.areaKm2 })
                    }
                }
        }

    override fun observeCountry(uuid: String): Flow<Country?> = countries.map { l -> l.firstOrNull { it.uuid == uuid } }

    override suspend fun getByAlpha3(codes: List<String>): List<CountrySummary> =
        countries.value.filter { it.codes.alpha3 in codes }.map { it.toSummary() }

    override fun observeRegions(): Flow<List<String>> = countries.map { l -> l.mapNotNull { it.region }.distinct().sorted() }
    override fun observeSubregions(): Flow<List<String>> = countries.map { l -> l.mapNotNull { it.subregion }.distinct().sorted() }
    override fun observeCount(): Flow<Int> = countries.map { it.size }
    override fun observeSyncStatus(): Flow<SyncStatus> = syncStatus

    override suspend fun sync(force: Boolean): Result<Unit> {
        syncCalls += force
        return syncResult
    }

    override suspend fun getQuizPool(): List<Country> =
        countries.value.filter { it.flag.pngUrl != null && it.primaryCapital != null && it.classification.sovereign }
}

class FakeFavoritesRepository(private val countries: FakeCountryRepository) : FavoritesRepository {
    private val order = mutableListOf<String>()

    override fun observeFavorites(): Flow<List<CountrySummary>> =
        combine(countries.favoriteUuids, countries.countries) { favs, all ->
            order.filter { it in favs }.mapNotNull { id -> all.firstOrNull { it.uuid == id }?.toSummary() }.reversed()
        }

    override fun observeIsFavorite(uuid: String): Flow<Boolean> = countries.favoriteUuids.map { uuid in it }

    override suspend fun toggle(uuid: String) = if (uuid in countries.favoriteUuids.value) remove(uuid) else add(uuid)

    override suspend fun add(uuid: String) {
        if (uuid !in order) order += uuid
        countries.favoriteUuids.value = countries.favoriteUuids.value + uuid
    }

    override suspend fun remove(uuid: String) {
        countries.favoriteUuids.value = countries.favoriteUuids.value - uuid
    }

    override suspend fun clear() {
        countries.favoriteUuids.value = emptySet()
    }
}

class FakeQuizRepository : QuizRepository {
    val results = MutableStateFlow<List<QuizResult>>(emptyList())

    override suspend fun save(result: QuizResult) {
        results.value = results.value + result.copy(id = results.value.size + 1L)
    }

    override fun observeHistory(limit: Int): Flow<List<QuizResult>> =
        results.map { it.sortedByDescending { r -> r.playedAt }.take(limit) }

    override fun observeBest(mode: QuizMode): Flow<QuizResult?> =
        results.map { l -> l.filter { it.mode == mode }.maxWithOrNull(compareBy<QuizResult> { it.score }.thenBy { it.bestStreak }) }

    override suspend fun clear() {
        results.value = emptyList()
    }
}

class FakeSettingsRepository : SettingsRepository {
    val state = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = state
    override suspend fun setUnitSystem(unitSystem: UnitSystem) { state.value = state.value.copy(unitSystem = unitSystem) }
    override suspend fun setThemeMode(themeMode: ThemeMode) { state.value = state.value.copy(themeMode = themeMode) }
    override suspend fun setDynamicColor(enabled: Boolean) { state.value = state.value.copy(dynamicColor = enabled) }
    override suspend fun setLastSync(epochMillis: Long, total: Int) {
        state.value = state.value.copy(lastSync = java.time.Instant.ofEpochMilli(epochMillis), lastSyncTotal = total)
    }
}
