package com.example.contris.ui.countries

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.SyncException
import com.example.contris.domain.model.SyncStatus
import com.example.contris.domain.repository.CountryRepository
import com.example.contris.domain.repository.SettingsRepository
import com.example.contris.domain.usecase.ObserveCountriesUseCase
import com.example.contris.domain.usecase.SyncCountriesUseCase
import com.example.contris.ui.common.UiText
import com.example.contris.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CountriesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeCountries: ObserveCountriesUseCase,
    private val syncCountries: SyncCountriesUseCase,
    countryRepository: CountryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val sort: StateFlow<CountrySort> = savedStateHandle.getStateFlow(KEY_SORT, CountrySort.NAME_ASC.name)
        .map { runCatching { CountrySort.valueOf(it) }.getOrDefault(CountrySort.NAME_ASC) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CountrySort.NAME_ASC)
    private val filters: StateFlow<CountryFilters> = savedStateHandle.getStateFlow(KEY_FILTERS, "")
        .map { decodeFilters(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CountryFilters())

    private val isFilterSheetOpen = MutableStateFlow(false)
    private val isRefreshing = MutableStateFlow(false)
    private val isLoadingList = MutableStateFlow(true)

    private val _effects = Channel<CountriesUiEffect>(Channel.BUFFERED, BufferOverflow.DROP_OLDEST)
    val effects: Flow<CountriesUiEffect> = _effects.receiveAsFlow()

    private val countries = combine(
        query.debounce(QUERY_DEBOUNCE_MS).onStart { emit(query.value) }.distinctUntilChanged(),
        filters,
        sort,
    ) { q, f, s -> Triple(q, f, s) }
        .flatMapLatest { (q, f, s) -> observeCountries(q, f, s) }
        .map { list -> isLoadingList.value = false; list }

    private val inputs = combine(query, filters, sort, isFilterSheetOpen) { q, f, s, open -> Inputs(q, f, s, open) }
    private val data = combine(
        countries,
        countryRepository.observeSyncStatus(),
        countryRepository.observeRegions(),
        countryRepository.observeSubregions(),
        countryRepository.observeCount(),
    ) { list, sync, regions, subregions, count -> Data(list, sync, regions, subregions, count) }

    val uiState: StateFlow<CountriesUiState> = combine(
        inputs,
        data,
        settingsRepository.settings,
        isRefreshing,
        isLoadingList,
    ) { input, d, settings, refreshing, loading ->
        CountriesUiState(
            query = input.query,
            filters = input.filters,
            sort = input.sort,
            countries = d.countries,
            isLoadingList = loading,
            sync = d.sync,
            isRefreshing = refreshing,
            availableRegions = d.regions,
            availableSubregions = d.subregions,
            isFilterSheetOpen = input.sheetOpen,
            unitSystem = settings.unitSystem,
            totalCount = d.count,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CountriesUiState())

    fun onEvent(event: CountriesUiEvent) {
        when (event) {
            is CountriesUiEvent.QueryChanged -> savedStateHandle[KEY_QUERY] = event.query
            CountriesUiEvent.ClearQuery -> savedStateHandle[KEY_QUERY] = ""
            is CountriesUiEvent.SortChanged -> savedStateHandle[KEY_SORT] = event.sort.name
            is CountriesUiEvent.FiltersChanged -> savedStateHandle[KEY_FILTERS] = encodeFilters(event.filters)
            CountriesUiEvent.ClearFilters -> savedStateHandle[KEY_FILTERS] = ""
            CountriesUiEvent.OpenFilters -> isFilterSheetOpen.value = true
            CountriesUiEvent.CloseFilters -> isFilterSheetOpen.value = false
            CountriesUiEvent.Refresh -> refresh(force = true)
            CountriesUiEvent.RetrySync -> refresh(force = uiState.value.totalCount > 0)
            is CountriesUiEvent.CountryClicked -> _effects.trySend(CountriesUiEffect.NavigateToDetail(event.uuid))
        }
    }

    private fun refresh(force: Boolean) {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                val result = syncCountries(force)
                result.exceptionOrNull()?.let { t ->
                    val text = (t as? SyncException)?.error?.toUiText() ?: UiText.Dynamic(t.message ?: "Refresh failed")
                    _effects.trySend(CountriesUiEffect.ShowMessage(text))
                }
                if (result.isSuccess && force) {
                    val s = uiState.value.sync
                    if (s is SyncStatus.Idle && s.total > 0) {
                        _effects.trySend(CountriesUiEffect.ShowMessage(UiText.Dynamic("Updated ${s.total} countries")))
                    }
                }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private data class Inputs(val query: String, val filters: CountryFilters, val sort: CountrySort, val sheetOpen: Boolean)
    private data class Data(val countries: List<com.example.contris.domain.model.CountrySummary>, val sync: SyncStatus, val regions: List<String>, val subregions: List<String>, val count: Int)

    companion object {
        const val KEY_QUERY = "query"
        const val KEY_SORT = "sort"
        const val KEY_FILTERS = "filters"
        const val QUERY_DEBOUNCE_MS = 250L

        private val json = Json { ignoreUnknownKeys = true }
        fun encodeFilters(f: CountryFilters): String = json.encodeToString(f)
        fun decodeFilters(s: String): CountryFilters =
            if (s.isBlank()) CountryFilters() else runCatching { json.decodeFromString<CountryFilters>(s) }.getOrDefault(CountryFilters())
    }
}
