package com.example.contris.ui.countries

import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.SyncStatus
import com.example.contris.domain.model.UnitSystem
import com.example.contris.ui.common.UiText

data class CountriesUiState(
    val query: String = "",
    val filters: CountryFilters = CountryFilters(),
    val sort: CountrySort = CountrySort.NAME_ASC,
    val countries: List<CountrySummary> = emptyList(),
    val isLoadingList: Boolean = true,
    val sync: SyncStatus = SyncStatus.Idle(null, 0),
    val isRefreshing: Boolean = false,
    val availableRegions: List<String> = emptyList(),
    val availableSubregions: List<String> = emptyList(),
    val isFilterSheetOpen: Boolean = false,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val totalCount: Int = 0,
) {
    val isFirstSync: Boolean get() = totalCount == 0 && sync is SyncStatus.Syncing
    val hasNoData: Boolean get() = totalCount == 0 && !isLoadingList && sync !is SyncStatus.Syncing
}

sealed interface CountriesUiEvent {
    data class QueryChanged(val query: String) : CountriesUiEvent
    data object ClearQuery : CountriesUiEvent
    data class SortChanged(val sort: CountrySort) : CountriesUiEvent
    data class FiltersChanged(val filters: CountryFilters) : CountriesUiEvent
    data object ClearFilters : CountriesUiEvent
    data object OpenFilters : CountriesUiEvent
    data object CloseFilters : CountriesUiEvent
    data object Refresh : CountriesUiEvent
    data object RetrySync : CountriesUiEvent
    data class CountryClicked(val uuid: String) : CountriesUiEvent
}

sealed interface CountriesUiEffect {
    data class NavigateToDetail(val uuid: String) : CountriesUiEffect
    data class ShowMessage(val text: UiText) : CountriesUiEffect
}
