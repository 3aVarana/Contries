package com.example.contris.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.SyncError
import com.example.contris.domain.model.SyncStatus
import com.example.contris.ui.countries.CountriesScreen
import com.example.contris.ui.countries.CountriesUiEvent
import com.example.contris.ui.countries.CountriesUiState
import com.example.contris.ui.theme.ContrisTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class CountriesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val countries = listOf(
        CountrySummary("de", "Germany", "Berlin", "Europe", "🇩🇪", null, 84_000_000, 357_114.0),
        CountrySummary("ca", "Canada", "Ottawa", "Americas", "🇨🇦", null, 41_000_000, 9_984_670.0),
    )

    @Test
    fun listRendersAndClickEmitsEvent() {
        val events = mutableListOf<CountriesUiEvent>()
        rule.setContent {
            ContrisTheme {
                CountriesScreen(
                    state = CountriesUiState(countries = countries, isLoadingList = false, totalCount = 2),
                    onEvent = { events += it },
                )
            }
        }
        rule.onNodeWithText("Germany").assertIsDisplayed()
        rule.onNodeWithText("Berlin · Europe").assertIsDisplayed()
        rule.onNodeWithText("41M").assertIsDisplayed()
        rule.onNodeWithText("Canada").performClick()
        assertThat(events).contains(CountriesUiEvent.CountryClicked("ca"))
    }

    @Test
    fun typingEmitsQueryChanged_andFilterButtonOpensSheet() {
        val events = mutableListOf<CountriesUiEvent>()
        rule.setContent {
            ContrisTheme {
                CountriesScreen(
                    state = CountriesUiState(countries = countries, isLoadingList = false, totalCount = 2, availableRegions = listOf("Europe")),
                    onEvent = { events += it },
                )
            }
        }
        rule.onNodeWithTag("search_field").performTextInput("ger")
        assertThat(events).contains(CountriesUiEvent.QueryChanged("ger"))
        rule.onNodeWithTag("filter_button").performClick()
        assertThat(events).contains(CountriesUiEvent.OpenFilters)
    }

    @Test
    fun filterSheetAppliesSelection() {
        val events = mutableListOf<CountriesUiEvent>()
        rule.setContent {
            ContrisTheme {
                CountriesScreen(
                    state = CountriesUiState(countries = countries, isLoadingList = false, totalCount = 2, isFilterSheetOpen = true, availableRegions = listOf("Europe", "Americas")),
                    onEvent = { events += it },
                )
            }
        }
        rule.onNodeWithTag("filter_sheet").assertIsDisplayed()
        // "Europe" is both a region and a continent chip; the region group is laid out first.
        rule.onAllNodesWithText("Europe").onFirst().performClick()
        rule.onNodeWithTag("apply_filters").performClick()
        assertThat(events).contains(CountriesUiEvent.FiltersChanged(CountryFilters(regions = setOf("Europe"))))
        assertThat(events).contains(CountriesUiEvent.CloseFilters)
    }

    @Test
    fun failedSyncShowsBannerWithRetry() {
        val events = mutableListOf<CountriesUiEvent>()
        rule.setContent {
            ContrisTheme {
                CountriesScreen(
                    state = CountriesUiState(countries = countries, isLoadingList = false, totalCount = 2, sync = SyncStatus.Failed(SyncError.Network, null)),
                    onEvent = { events += it },
                )
            }
        }
        rule.onNodeWithTag("sync_banner").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        assertThat(events).contains(CountriesUiEvent.RetrySync)
    }

    @Test
    fun emptyResultShowsEmptyState() {
        rule.setContent {
            ContrisTheme {
                CountriesScreen(
                    state = CountriesUiState(countries = emptyList(), isLoadingList = false, totalCount = 2, query = "zzz"),
                    onEvent = {},
                )
            }
        }
        rule.onNodeWithText("No countries match").assertIsDisplayed()
    }
}
