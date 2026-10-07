package com.example.contris.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.example.contris.domain.model.QuizMode
import kotlinx.serialization.Serializable

/** All destinations. Sealed so the back stacks can be saved with kotlinx.serialization. */
@Serializable
sealed interface ContrisKey : NavKey

// Top-level tabs
@Serializable data object CountriesKey : ContrisKey
@Serializable data object FavoritesKey : ContrisKey
@Serializable data object QuizKey : ContrisKey
@Serializable data class CompareKey(val firstUuid: String? = null) : ContrisKey
@Serializable data object SettingsKey : ContrisKey

// Pushed destinations
@Serializable data class CountryDetailKey(val uuid: String) : ContrisKey
@Serializable data class QuizPlayKey(val mode: QuizMode) : ContrisKey

enum class TopLevelTab(val key: ContrisKey, val label: String) {
    COUNTRIES(CountriesKey, "Countries"),
    FAVORITES(FavoritesKey, "Favorites"),
    QUIZ(QuizKey, "Quiz"),
    COMPARE(CompareKey(), "Compare"),
    SETTINGS(SettingsKey, "Settings"),
    ;

    companion object {
        fun of(key: ContrisKey): TopLevelTab? = entries.firstOrNull { it.matches(key) }
    }

    fun matches(key: ContrisKey): Boolean = when (this) {
        COMPARE -> key is CompareKey
        else -> key == this.key
    }
}
