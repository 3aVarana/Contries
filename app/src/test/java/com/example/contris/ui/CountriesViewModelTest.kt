package com.example.contris.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.SyncError
import com.example.contris.domain.model.SyncException
import com.example.contris.domain.model.SyncStatus
import com.example.contris.domain.usecase.ObserveCountriesUseCase
import com.example.contris.domain.usecase.SyncCountriesUseCase
import com.example.contris.testutil.FakeCountryRepository
import com.example.contris.testutil.FakeSettingsRepository
import com.example.contris.testutil.Fixtures
import com.example.contris.testutil.MainDispatcherRule
import com.example.contris.ui.common.UiText
import com.example.contris.ui.countries.CountriesUiEffect
import com.example.contris.ui.countries.CountriesUiEvent
import com.example.contris.ui.countries.CountriesViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountriesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val repo = FakeCountryRepository().apply {
        countries.value = listOf(
            Fixtures.country("de", "Germany", region = "Europe", population = 84_000_000),
            Fixtures.country("dz", "Algeria", region = "Africa", population = 46_000_000),
            Fixtures.country("ne", "Niger", region = "Africa", population = 27_000_000),
            Fixtures.country("at", "Austria", region = "Europe", population = 9_000_000),
        )
    }
    private val settings = FakeSettingsRepository()

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) = CountriesViewModel(
        savedStateHandle = handle,
        observeCountries = ObserveCountriesUseCase(repo),
        syncCountries = SyncCountriesUseCase(repo),
        countryRepository = repo,
        settingsRepository = settings,
    )

    @Test
    fun `initial state lists all countries sorted by name`() = runTest(dispatcher) {
        val vm = vm()
        vm.uiState.test {
            awaitItem() // initial default
            val loaded = awaitItemUntil { it.countries.isNotEmpty() }
            assertThat(loaded.countries.map { it.name }).containsExactly("Algeria", "Austria", "Germany", "Niger").inOrder()
            assertThat(loaded.isLoadingList).isFalse()
            assertThat(loaded.totalCount).isEqualTo(4)
            assertThat(loaded.availableRegions).containsExactly("Africa", "Europe")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `query is debounced and filters the list`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = vm(handle)
        vm.uiState.test {
            awaitItemUntil { it.countries.size == 4 }
            vm.onEvent(CountriesUiEvent.QueryChanged("ger"))
            val typed = awaitItemUntil { it.query == "ger" }
            assertThat(typed.countries).hasSize(4) // not yet debounced
            advanceTimeBy(CountriesViewModel.QUERY_DEBOUNCE_MS + 50)
            val filtered = awaitItemUntil { it.countries.size != 4 }
            assertThat(filtered.countries.map { it.name }).containsExactly("Algeria", "Germany", "Niger")
            assertThat(handle.get<String>(CountriesViewModel.KEY_QUERY)).isEqualTo("ger")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sort and filters are applied and persisted`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = vm(handle)
        vm.uiState.test {
            awaitItemUntil { it.countries.size == 4 }
            vm.onEvent(CountriesUiEvent.SortChanged(CountrySort.POPULATION_DESC))
            val sorted = awaitItemUntil { it.sort == CountrySort.POPULATION_DESC && it.countries.first().name == "Germany" }
            assertThat(sorted.countries.map { it.name }).containsExactly("Germany", "Algeria", "Niger", "Austria").inOrder()

            vm.onEvent(CountriesUiEvent.FiltersChanged(CountryFilters(regions = setOf("Africa"))))
            val filtered = awaitItemUntil { it.filters.activeCount == 1 && it.countries.size == 2 }
            assertThat(filtered.countries.map { it.name }).containsExactly("Algeria", "Niger").inOrder()
            assertThat(handle.get<String>(CountriesViewModel.KEY_SORT)).isEqualTo("POPULATION_DESC")
            assertThat(CountriesViewModel.decodeFilters(handle.get<String>(CountriesViewModel.KEY_FILTERS)!!).regions).containsExactly("Africa")

            vm.onEvent(CountriesUiEvent.ClearFilters)
            assertThat(awaitItemUntil { it.filters.isEmpty && it.countries.size == 4 }.countries).hasSize(4)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state is restored from SavedStateHandle`() = runTest(dispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                CountriesViewModel.KEY_QUERY to "au",
                CountriesViewModel.KEY_SORT to "NAME_DESC",
                CountriesViewModel.KEY_FILTERS to CountriesViewModel.encodeFilters(CountryFilters(sovereignOnly = true)),
            ),
        )
        val vm = vm(handle)
        val state = vm.uiState.first { it.countries.isNotEmpty() }
        assertThat(state.query).isEqualTo("au")
        assertThat(state.sort).isEqualTo(CountrySort.NAME_DESC)
        assertThat(state.filters.sovereignOnly).isTrue()
        assertThat(state.countries.map { it.name }).containsExactly("Austria")
    }

    @Test
    fun `country click emits navigation effect once`() = runTest(dispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(CountriesUiEvent.CountryClicked("de"))
            assertThat(awaitItem()).isEqualTo(CountriesUiEffect.NavigateToDetail("de"))
            expectNoEvents()
        }
    }

    @Test
    fun `refresh forces a sync and surfaces errors as messages`() = runTest(dispatcher) {
        repo.syncResult = Result.failure(SyncException(SyncError.QuotaExceeded))
        val vm = vm()
        vm.effects.test {
            vm.onEvent(CountriesUiEvent.Refresh)
            advanceUntilIdle()
            val effect = awaitItem() as CountriesUiEffect.ShowMessage
            assertThat((effect.text as UiText.Dynamic).value).contains("quota")
            assertThat(repo.syncCalls).containsExactly(true)
        }
    }

    @Test
    fun `retry on failed first sync is not forced so debounce does not apply`() = runTest(dispatcher) {
        repo.countries.value = emptyList()
        repo.syncStatus.value = SyncStatus.Failed(SyncError.Network, null)
        val vm = vm()
        vm.uiState.test {
            awaitItemUntil { it.sync is SyncStatus.Failed }
            vm.onEvent(CountriesUiEvent.RetrySync)
            advanceUntilIdle()
            assertThat(repo.syncCalls).containsExactly(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter sheet open and close`() = runTest(dispatcher) {
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            vm.onEvent(CountriesUiEvent.OpenFilters)
            assertThat(awaitItemUntil { it.isFilterSheetOpen }.isFilterSheetOpen).isTrue()
            vm.onEvent(CountriesUiEvent.CloseFilters)
            assertThat(awaitItemUntil { !it.isFilterSheetOpen }.isFilterSheetOpen).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/** Skips intermediate emissions until [predicate] holds. */
suspend fun <T> app.cash.turbine.ReceiveTurbine<T>.awaitItemUntil(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
