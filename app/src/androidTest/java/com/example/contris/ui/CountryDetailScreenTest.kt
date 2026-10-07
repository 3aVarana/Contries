package com.example.contris.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.Membership
import com.example.contris.domain.model.Memberships
import com.example.contris.domain.model.UnitSystem
import com.example.contris.ui.detail.CountryDetailScreen
import com.example.contris.ui.detail.CountryDetailUiEvent
import com.example.contris.ui.detail.CountryDetailUiState
import com.example.contris.ui.theme.ContrisTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class CountryDetailScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val germany: Country = com.example.contris.ui.testCountry("de", "Germany").copy(
        memberships = Memberships(setOf(Membership.EU, Membership.NATO)),
        borders = listOf("AUT"),
    )

    @Test
    fun rendersSections_borderChipNavigates_favoriteToggles() {
        val events = mutableListOf<CountryDetailUiEvent>()
        rule.setContent {
            ContrisTheme {
                CountryDetailScreen(
                    state = CountryDetailUiState.Content(
                        country = germany,
                        borders = listOf(CountrySummary("at", "Austria", "Vienna", "Europe", "🇦🇹", null, 9_000_000, 83_871.0)),
                        isFavorite = false,
                        unitSystem = UnitSystem.METRIC,
                        flagTintHex = "#ffce00",
                    ),
                    onEvent = { events += it },
                )
            }
        }
        rule.onNodeWithText("Overview").assertIsDisplayed()
        rule.onNodeWithTag("detail_list").performScrollToNode(hasText("Memberships"))
        rule.onNodeWithText("EU").assertIsDisplayed()
        rule.onNodeWithTag("detail_list").performScrollToNode(hasText("Austria"))
        rule.onNodeWithText("Austria").performClick()
        assertThat(events).contains(CountryDetailUiEvent.BorderClicked("at"))
        rule.onNodeWithTag("favorite_toggle").performClick()
        assertThat(events).contains(CountryDetailUiEvent.ToggleFavorite)
    }

    @Test
    fun notFoundState() {
        rule.setContent { ContrisTheme { CountryDetailScreen(state = CountryDetailUiState.NotFound, onEvent = {}) } }
        rule.onNodeWithText("Country not found").assertIsDisplayed()
    }
}
