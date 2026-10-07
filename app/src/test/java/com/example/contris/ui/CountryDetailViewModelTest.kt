package com.example.contris.ui

import app.cash.turbine.test
import com.example.contris.domain.model.FlagColors
import com.example.contris.domain.model.FlagInfo
import com.example.contris.domain.model.UnitSystem
import com.example.contris.domain.usecase.ObserveCountryUseCase
import com.example.contris.domain.usecase.ToggleFavoriteUseCase
import com.example.contris.testutil.FakeCountryRepository
import com.example.contris.testutil.FakeFavoritesRepository
import com.example.contris.testutil.FakeSettingsRepository
import com.example.contris.testutil.Fixtures
import com.example.contris.testutil.MainDispatcherRule
import com.example.contris.ui.detail.CountryDetailUiEffect
import com.example.contris.ui.detail.CountryDetailUiEvent
import com.example.contris.ui.detail.CountryDetailUiState
import com.example.contris.ui.detail.CountryDetailViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val repo = FakeCountryRepository().apply {
        countries.value = listOf(
            Fixtures.country("deu", "Germany").copy(
                borders = listOf("AUT", "FRA"),
                flag = FlagInfo(emoji = "🇩🇪", pngUrl = "png", colors = FlagColors(dominant = "#000000", prominent = "#dd0000", vibrant = "#ffce00")),
            ),
            Fixtures.country("aut", "Austria"),
            Fixtures.country("fra", "France"),
        )
    }
    private val favorites = FakeFavoritesRepository(repo)
    private val settings = FakeSettingsRepository()

    private fun vm(uuid: String) = CountryDetailViewModel(
        uuid = uuid,
        observeCountry = ObserveCountryUseCase(repo),
        countryRepository = repo,
        favoritesRepository = favorites,
        settingsRepository = settings,
        toggleFavorite = ToggleFavoriteUseCase(favorites),
    )

    @Test
    fun `content includes resolved borders, favourite flag and vibrant tint`() = runTest(dispatcher) {
        val vm = vm("deu")
        vm.uiState.test {
            assertThat(awaitItem()).isEqualTo(CountryDetailUiState.Loading)
            val content = awaitItemUntil { it is CountryDetailUiState.Content } as CountryDetailUiState.Content
            assertThat(content.country.names.common).isEqualTo("Germany")
            assertThat(content.borders.map { it.name }).containsExactly("Austria", "France")
            assertThat(content.isFavorite).isFalse()
            assertThat(content.flagTintHex).isEqualTo("#ffce00")
            assertThat(content.unitSystem).isEqualTo(UnitSystem.METRIC)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unknown uuid yields NotFound`() = runTest(dispatcher) {
        val vm = vm("nope")
        vm.uiState.test {
            awaitItem()
            assertThat(awaitItemUntil { it !is CountryDetailUiState.Loading }).isEqualTo(CountryDetailUiState.NotFound)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle favourite updates state and settings change propagates`() = runTest(dispatcher) {
        val vm = vm("deu")
        vm.uiState.test {
            awaitItemUntil { it is CountryDetailUiState.Content }
            vm.onEvent(CountryDetailUiEvent.ToggleFavorite)
            advanceUntilIdle()
            assertThat((awaitItemUntil { (it as CountryDetailUiState.Content).isFavorite } as CountryDetailUiState.Content).isFavorite).isTrue()
            settings.setUnitSystem(UnitSystem.IMPERIAL)
            assertThat((awaitItemUntil { (it as CountryDetailUiState.Content).unitSystem == UnitSystem.IMPERIAL } as CountryDetailUiState.Content).unitSystem)
                .isEqualTo(UnitSystem.IMPERIAL)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events map to effects`() = runTest(dispatcher) {
        val vm = vm("deu")
        vm.effects.test {
            vm.onEvent(CountryDetailUiEvent.BorderClicked("aut"))
            assertThat(awaitItem()).isEqualTo(CountryDetailUiEffect.NavigateToDetail("aut"))
            vm.onEvent(CountryDetailUiEvent.OpenLink("https://x"))
            assertThat(awaitItem()).isEqualTo(CountryDetailUiEffect.OpenUrl("https://x"))
            vm.onEvent(CountryDetailUiEvent.Compare)
            assertThat(awaitItem()).isEqualTo(CountryDetailUiEffect.NavigateToCompare("deu"))
            vm.onEvent(CountryDetailUiEvent.Back)
            assertThat(awaitItem()).isEqualTo(CountryDetailUiEffect.NavigateBack)
            expectNoEvents()
        }
    }

    @Test
    fun `share builds text from the loaded country`() = runTest(dispatcher) {
        val vm = vm("deu")
        vm.uiState.test {
            awaitItemUntil { it is CountryDetailUiState.Content }
            vm.effects.test {
                vm.onEvent(CountryDetailUiEvent.ShareClicked)
                val share = awaitItem() as CountryDetailUiEffect.Share
                assertThat(share.text).contains("Germany")
                assertThat(share.text).contains("Capital of Germany")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
