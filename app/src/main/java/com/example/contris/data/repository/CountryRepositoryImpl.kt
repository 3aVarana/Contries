package com.example.contris.data.repository

import com.example.contris.data.local.CountryQueryBuilder
import com.example.contris.data.local.dao.CountryDao
import com.example.contris.data.mapper.toDomain
import com.example.contris.data.sync.CountrySyncManager
import com.example.contris.domain.DispatcherProvider
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.SyncStatus
import com.example.contris.domain.repository.CountryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class CountryRepositoryImpl @Inject constructor(
    private val dao: CountryDao,
    private val syncManager: CountrySyncManager,
    private val dispatchers: DispatcherProvider,
) : CountryRepository {

    override fun observeCountries(query: String, filters: CountryFilters, sort: CountrySort): Flow<List<CountrySummary>> =
        dao.observeSummaries(CountryQueryBuilder.build(query, filters, sort))
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeCountry(uuid: String): Flow<Country?> =
        dao.observeByUuid(uuid).map { it?.toDomain() }.flowOn(dispatchers.io)

    override suspend fun getByAlpha3(codes: List<String>): List<CountrySummary> {
        if (codes.isEmpty()) return emptyList()
        return withContext(dispatchers.io) { dao.getSummariesByAlpha3(codes).map { it.toDomain() } }
    }

    override fun observeRegions(): Flow<List<String>> = dao.observeRegions()

    override fun observeSubregions(): Flow<List<String>> = dao.observeSubregions()

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override fun observeSyncStatus(): Flow<SyncStatus> = syncManager.status

    override suspend fun sync(force: Boolean): Result<Unit> = syncManager.sync(force)

    override suspend fun getQuizPool(): List<Country> =
        withContext(dispatchers.io) { dao.getQuizPool().map { it.toDomain() } }
}
