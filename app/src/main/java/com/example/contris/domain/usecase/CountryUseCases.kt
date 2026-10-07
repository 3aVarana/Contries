package com.example.contris.domain.usecase

import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.repository.CountryRepository
import com.example.contris.domain.repository.FavoritesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SyncCountriesUseCase @Inject constructor(private val repository: CountryRepository) {
    suspend operator fun invoke(force: Boolean = false): Result<Unit> = repository.sync(force)
}

class ObserveCountriesUseCase @Inject constructor(private val repository: CountryRepository) {
    operator fun invoke(
        query: String = "",
        filters: CountryFilters = CountryFilters(),
        sort: CountrySort = CountrySort.NAME_ASC,
    ): Flow<List<CountrySummary>> = repository.observeCountries(query, filters, sort)
}

class ObserveCountryUseCase @Inject constructor(private val repository: CountryRepository) {
    operator fun invoke(uuid: String): Flow<Country?> = repository.observeCountry(uuid)
}

class ObserveFavoritesUseCase @Inject constructor(private val repository: FavoritesRepository) {
    operator fun invoke(): Flow<List<CountrySummary>> = repository.observeFavorites()
}

class ToggleFavoriteUseCase @Inject constructor(private val repository: FavoritesRepository) {
    suspend operator fun invoke(uuid: String) = repository.toggle(uuid)
}
