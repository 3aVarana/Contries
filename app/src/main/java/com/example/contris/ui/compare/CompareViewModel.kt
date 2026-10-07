package com.example.contris.ui.compare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contris.domain.model.ComparisonRow
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.UnitSystem
import com.example.contris.domain.repository.SettingsRepository
import com.example.contris.domain.usecase.BuildComparisonUseCase
import com.example.contris.domain.usecase.ObserveCountriesUseCase
import com.example.contris.domain.usecase.ObserveCountryUseCase
import com.example.contris.ui.common.Formatters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

enum class Slot { FIRST, SECOND }

data class CompareUiState(
    val first: Country? = null,
    val second: Country? = null,
    val rows: List<ComparisonRow> = emptyList(),
    val pickerFor: Slot? = null,
    val pickerQuery: String = "",
    val pickerResults: List<CountrySummary> = emptyList(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
)

sealed interface CompareUiEvent {
    data class OpenPicker(val slot: Slot) : CompareUiEvent
    data class PickerQueryChanged(val query: String) : CompareUiEvent
    data class CountryPicked(val slot: Slot, val uuid: String) : CompareUiEvent
    data object ClosePicker : CompareUiEvent
    data object Swap : CompareUiEvent
    data class Clear(val slot: Slot) : CompareUiEvent
    data class OpenDetail(val uuid: String) : CompareUiEvent
}

sealed interface CompareUiEffect {
    data class NavigateToDetail(val uuid: String) : CompareUiEffect
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = CompareViewModel.Factory::class)
class CompareViewModel @AssistedInject constructor(
    @Assisted initialFirstUuid: String?,
    private val savedStateHandle: SavedStateHandle,
    observeCountry: ObserveCountryUseCase,
    observeCountries: ObserveCountriesUseCase,
    settingsRepository: SettingsRepository,
    private val buildComparison: BuildComparisonUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(initialFirstUuid: String?): CompareViewModel
    }

    init {
        if (initialFirstUuid != null && !savedStateHandle.contains(KEY_FIRST)) {
            savedStateHandle[KEY_FIRST] = initialFirstUuid
        }
    }

    private val _effects = Channel<CompareUiEffect>(Channel.BUFFERED, BufferOverflow.DROP_OLDEST)
    val effects: Flow<CompareUiEffect> = _effects.receiveAsFlow()

    private val firstUuid: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_FIRST, null)
    private val secondUuid: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_SECOND, null)
    private val pickerFor = MutableStateFlow<Slot?>(null)
    private val pickerQuery = MutableStateFlow("")

    private val first = firstUuid.flatMapLatest { if (it == null) flowOf(null) else observeCountry(it) }
    private val second = secondUuid.flatMapLatest { if (it == null) flowOf(null) else observeCountry(it) }

    private val pickerResults = combine(pickerFor, pickerQuery.debounce(200)) { slot, q -> slot to q }
        .flatMapLatest { (slot, q) ->
            if (slot == null) flowOf(emptyList()) else observeCountries(q, CountryFilters(), CountrySort.NAME_ASC)
        }

    private val formatters = object : BuildComparisonUseCase.Formatters {
        override fun population(value: Long) = Formatters.formatPopulationFull(value)
        override fun area(km2: Double, unitSystem: UnitSystem) = Formatters.formatArea(km2, unitSystem)
        override fun density(perKm2: Double, unitSystem: UnitSystem) = Formatters.formatDensity(perKm2, unitSystem)
        override fun decimal(value: Double) = Formatters.formatDecimal(value)
    }

    val uiState: StateFlow<CompareUiState> = combine(
        first, second, settingsRepository.settings, pickerFor, combine(pickerQuery, pickerResults) { q, r -> q to r },
    ) { a, b, settings, slot, (q, results) ->
        CompareUiState(
            first = a,
            second = b,
            rows = if (a != null && b != null) buildComparison(a, b, settings.unitSystem, formatters).rows else emptyList(),
            pickerFor = slot,
            pickerQuery = q,
            pickerResults = results,
            unitSystem = settings.unitSystem,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompareUiState())

    fun onEvent(event: CompareUiEvent) {
        when (event) {
            is CompareUiEvent.OpenPicker -> { pickerQuery.value = ""; pickerFor.value = event.slot }
            is CompareUiEvent.PickerQueryChanged -> pickerQuery.value = event.query
            is CompareUiEvent.CountryPicked -> {
                savedStateHandle[event.slot.key()] = event.uuid
                pickerFor.value = null
            }
            CompareUiEvent.ClosePicker -> pickerFor.value = null
            CompareUiEvent.Swap -> {
                val a = firstUuid.value
                savedStateHandle[KEY_FIRST] = secondUuid.value
                savedStateHandle[KEY_SECOND] = a
            }
            is CompareUiEvent.Clear -> savedStateHandle[event.slot.key()] = null
            is CompareUiEvent.OpenDetail -> _effects.trySend(CompareUiEffect.NavigateToDetail(event.uuid))
        }
    }

    private fun Slot.key() = if (this == Slot.FIRST) KEY_FIRST else KEY_SECOND

    companion object {
        const val KEY_FIRST = "firstUuid"
        const val KEY_SECOND = "secondUuid"
    }
}
