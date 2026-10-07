package com.example.contris.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.repository.CountryRepository
import com.example.contris.domain.repository.FavoritesRepository
import com.example.contris.domain.repository.SettingsRepository
import com.example.contris.domain.usecase.ObserveCountryUseCase
import com.example.contris.domain.usecase.ToggleFavoriteUseCase
import com.example.contris.ui.common.UiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CountryDetailViewModel.Factory::class)
class CountryDetailViewModel @AssistedInject constructor(
    @Assisted val uuid: String,
    observeCountry: ObserveCountryUseCase,
    private val countryRepository: CountryRepository,
    favoritesRepository: FavoritesRepository,
    settingsRepository: SettingsRepository,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(uuid: String): CountryDetailViewModel
    }

    private val _effects = Channel<CountryDetailUiEffect>(Channel.BUFFERED, BufferOverflow.DROP_OLDEST)
    val effects: Flow<CountryDetailUiEffect> = _effects.receiveAsFlow()

    private val country: Flow<Country?> = observeCountry(uuid)

    /** Borders are resolved whenever the border list changes (not on every country emission). */
    private val borders: Flow<List<CountrySummary>> = country
        .map { it?.borders.orEmpty() }
        .distinctUntilChanged()
        .transformLatest { codes -> emit(countryRepository.getByAlpha3(codes)) }

    val uiState: StateFlow<CountryDetailUiState> = combine(
        country,
        borders,
        favoritesRepository.observeIsFavorite(uuid),
        settingsRepository.settings,
    ) { c, b, fav, settings ->
        if (c == null) {
            CountryDetailUiState.NotFound
        } else {
            CountryDetailUiState.Content(
                country = c,
                borders = b,
                isFavorite = fav,
                unitSystem = settings.unitSystem,
                flagTintHex = c.flag.colors.vibrant ?: c.flag.colors.prominent ?: c.flag.colors.dominant,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CountryDetailUiState.Loading)

    fun onEvent(event: CountryDetailUiEvent) {
        when (event) {
            CountryDetailUiEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite(uuid) }
            is CountryDetailUiEvent.BorderClicked -> _effects.trySend(CountryDetailUiEffect.NavigateToDetail(event.uuid))
            is CountryDetailUiEvent.OpenLink -> _effects.trySend(CountryDetailUiEffect.OpenUrl(event.url))
            CountryDetailUiEvent.Compare -> _effects.trySend(CountryDetailUiEffect.NavigateToCompare(uuid))
            CountryDetailUiEvent.Back -> _effects.trySend(CountryDetailUiEffect.NavigateBack)
            CountryDetailUiEvent.ShareClicked -> {
                val c = (uiState.value as? CountryDetailUiState.Content)?.country
                if (c == null) {
                    _effects.trySend(CountryDetailUiEffect.ShowMessage(UiText.Dynamic("Nothing to share yet")))
                } else {
                    val text = buildString {
                        append(c.flag.emoji?.let { "$it " }.orEmpty()).append(c.names.common)
                        c.primaryCapital?.let { append(" — capital ").append(it.name) }
                        c.links.wikipedia?.let { append("\n").append(it) }
                    }
                    _effects.trySend(CountryDetailUiEffect.Share(text))
                }
            }
        }
    }
}
