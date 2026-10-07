package com.example.contris.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.repository.FavoritesRepository
import com.example.contris.domain.usecase.ObserveFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<CountrySummary> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface FavoritesUiEvent {
    data class CountryClicked(val uuid: String) : FavoritesUiEvent
    data class Remove(val uuid: String) : FavoritesUiEvent
    data object UndoRemove : FavoritesUiEvent
    data object BrowseCountries : FavoritesUiEvent
}

sealed interface FavoritesUiEffect {
    data class NavigateToDetail(val uuid: String) : FavoritesUiEffect
    data class ShowUndoSnackbar(val countryName: String) : FavoritesUiEffect
    data object NavigateToCountries : FavoritesUiEffect
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavorites: ObserveFavoritesUseCase,
    private val repository: FavoritesRepository,
) : ViewModel() {

    private val _effects = Channel<FavoritesUiEffect>(Channel.BUFFERED, BufferOverflow.DROP_OLDEST)
    val effects: Flow<FavoritesUiEffect> = _effects.receiveAsFlow()

    private var lastRemoved: String? = null

    val uiState: StateFlow<FavoritesUiState> = observeFavorites()
        .map { FavoritesUiState(favorites = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun onEvent(event: FavoritesUiEvent) {
        when (event) {
            is FavoritesUiEvent.CountryClicked -> _effects.trySend(FavoritesUiEffect.NavigateToDetail(event.uuid))
            is FavoritesUiEvent.Remove -> {
                val name = uiState.value.favorites.firstOrNull { it.uuid == event.uuid }?.name ?: "Country"
                lastRemoved = event.uuid
                viewModelScope.launch {
                    repository.remove(event.uuid)
                    _effects.send(FavoritesUiEffect.ShowUndoSnackbar(name))
                }
            }
            FavoritesUiEvent.UndoRemove -> lastRemoved?.let { uuid ->
                lastRemoved = null
                viewModelScope.launch { repository.add(uuid) }
            }
            FavoritesUiEvent.BrowseCountries -> _effects.trySend(FavoritesUiEffect.NavigateToCountries)
        }
    }
}
