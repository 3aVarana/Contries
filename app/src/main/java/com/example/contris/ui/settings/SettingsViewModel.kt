package com.example.contris.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contris.domain.model.SyncException
import com.example.contris.domain.model.SyncStatus
import com.example.contris.domain.model.ThemeMode
import com.example.contris.domain.model.UnitSystem
import com.example.contris.domain.repository.CountryRepository
import com.example.contris.domain.repository.FavoritesRepository
import com.example.contris.domain.repository.QuizRepository
import com.example.contris.domain.repository.SettingsRepository
import com.example.contris.domain.usecase.SyncCountriesUseCase
import com.example.contris.ui.common.UiText
import com.example.contris.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val lastSync: Instant? = null,
    val countryCount: Int = 0,
    val sync: SyncStatus = SyncStatus.Idle(null, 0),
    val isRefreshing: Boolean = false,
    val appVersion: String = "",
)

sealed interface SettingsUiEvent {
    data class SetUnitSystem(val unitSystem: UnitSystem) : SettingsUiEvent
    data class SetThemeMode(val themeMode: ThemeMode) : SettingsUiEvent
    data class SetDynamicColor(val enabled: Boolean) : SettingsUiEvent
    data object RefreshNow : SettingsUiEvent
    data object ClearFavorites : SettingsUiEvent
    data object ClearQuizHistory : SettingsUiEvent
    data class OpenLink(val url: String) : SettingsUiEvent
}

sealed interface SettingsUiEffect {
    data class ShowMessage(val text: UiText) : SettingsUiEffect
    data class OpenUrl(val url: String) : SettingsUiEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    countryRepository: CountryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val quizRepository: QuizRepository,
    private val syncCountries: SyncCountriesUseCase,
    @Named("appVersion") private val appVersion: String,
) : ViewModel() {

    private val _effects = Channel<SettingsUiEffect>(Channel.BUFFERED, BufferOverflow.DROP_OLDEST)
    val effects: Flow<SettingsUiEffect> = _effects.receiveAsFlow()

    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        countryRepository.observeCount(),
        countryRepository.observeSyncStatus(),
        isRefreshing,
    ) { settings, count, sync, refreshing ->
        SettingsUiState(
            unitSystem = settings.unitSystem,
            themeMode = settings.themeMode,
            dynamicColor = settings.dynamicColor,
            lastSync = settings.lastSync,
            countryCount = count,
            sync = sync,
            isRefreshing = refreshing,
            appVersion = appVersion,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(appVersion = appVersion))

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SetUnitSystem -> viewModelScope.launch { settingsRepository.setUnitSystem(event.unitSystem) }
            is SettingsUiEvent.SetThemeMode -> viewModelScope.launch { settingsRepository.setThemeMode(event.themeMode) }
            is SettingsUiEvent.SetDynamicColor -> viewModelScope.launch { settingsRepository.setDynamicColor(event.enabled) }
            SettingsUiEvent.RefreshNow -> refresh()
            SettingsUiEvent.ClearFavorites -> viewModelScope.launch {
                favoritesRepository.clear()
                _effects.send(SettingsUiEffect.ShowMessage(UiText.Dynamic("Favorites cleared")))
            }
            SettingsUiEvent.ClearQuizHistory -> viewModelScope.launch {
                quizRepository.clear()
                _effects.send(SettingsUiEffect.ShowMessage(UiText.Dynamic("Quiz history cleared")))
            }
            is SettingsUiEvent.OpenLink -> _effects.trySend(SettingsUiEffect.OpenUrl(event.url))
        }
    }

    private fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                val result = syncCountries(force = true)
                val message = result.exceptionOrNull()?.let { t ->
                    (t as? SyncException)?.error?.toUiText() ?: UiText.Dynamic(t.message ?: "Refresh failed")
                } ?: UiText.Dynamic("Countries are up to date")
                _effects.send(SettingsUiEffect.ShowMessage(message))
            } finally {
                isRefreshing.value = false
            }
        }
    }
}
