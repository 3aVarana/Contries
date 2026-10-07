package com.example.contris.ui

import app.cash.turbine.test
import com.example.contris.domain.usecase.ObserveFavoritesUseCase
import com.example.contris.testutil.FakeCountryRepository
import com.example.contris.testutil.FakeFavoritesRepository
import com.example.contris.testutil.Fixtures
import com.example.contris.testutil.MainDispatcherRule
import com.example.contris.ui.favorites.FavoritesUiEffect
import com.example.contris.ui.favorites.FavoritesUiEvent
import com.example.contris.ui.favorites.FavoritesViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val repo = FakeCountryRepository().apply {
        countries.value = listOf(Fixtures.country("a", "Alpha"), Fixtures.country("b", "Beta"))
    }
    private val favorites = FakeFavoritesRepository(repo)

    private fun vm() = FavoritesViewModel(ObserveFavoritesUseCase(favorites), favorites)

    @Test
    fun `lists favourites most recent first`() = runTest(dispatcher) {
        favorites.add("a"); favorites.add("b")
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            val loaded = awaitItemUntil { !it.isLoading }
            assertThat(loaded.favorites.map { it.name }).containsExactly("Beta", "Alpha").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `remove emits undo snackbar and undo restores`() = runTest(dispatcher) {
        favorites.add("a")
        val vm = vm()
        vm.uiState.test {
            awaitItemUntil { it.favorites.size == 1 }
            vm.effects.test {
                vm.onEvent(FavoritesUiEvent.Remove("a"))
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(FavoritesUiEffect.ShowUndoSnackbar("Alpha"))
            }
            assertThat(awaitItemUntil { it.favorites.isEmpty() }.favorites).isEmpty()
            vm.onEvent(FavoritesUiEvent.UndoRemove)
            advanceUntilIdle()
            assertThat(awaitItemUntil { it.favorites.size == 1 }.favorites.single().name).isEqualTo("Alpha")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `click and browse emit navigation effects`() = runTest(dispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(FavoritesUiEvent.CountryClicked("a"))
            assertThat(awaitItem()).isEqualTo(FavoritesUiEffect.NavigateToDetail("a"))
            vm.onEvent(FavoritesUiEvent.BrowseCountries)
            assertThat(awaitItem()).isEqualTo(FavoritesUiEffect.NavigateToCountries)
        }
    }
}
