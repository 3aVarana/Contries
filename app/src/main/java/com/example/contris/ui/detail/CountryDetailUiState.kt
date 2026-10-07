package com.example.contris.ui.detail

import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.UnitSystem
import com.example.contris.ui.common.UiText

sealed interface CountryDetailUiState {
    data object Loading : CountryDetailUiState
    data object NotFound : CountryDetailUiState
    data class Content(
        val country: Country,
        val borders: List<CountrySummary>,
        val isFavorite: Boolean,
        val unitSystem: UnitSystem,
        /** Hex colour from the flag used to tint the header, or null. */
        val flagTintHex: String?,
    ) : CountryDetailUiState
}

sealed interface CountryDetailUiEvent {
    data object ToggleFavorite : CountryDetailUiEvent
    data class BorderClicked(val uuid: String) : CountryDetailUiEvent
    data class OpenLink(val url: String) : CountryDetailUiEvent
    data object Compare : CountryDetailUiEvent
    data object Back : CountryDetailUiEvent
    data object ShareClicked : CountryDetailUiEvent
}

sealed interface CountryDetailUiEffect {
    data class NavigateToDetail(val uuid: String) : CountryDetailUiEffect
    data class NavigateToCompare(val firstUuid: String) : CountryDetailUiEffect
    data class OpenUrl(val url: String) : CountryDetailUiEffect
    data class Share(val text: String) : CountryDetailUiEffect
    data object NavigateBack : CountryDetailUiEffect
    data class ShowMessage(val text: UiText) : CountryDetailUiEffect
}
