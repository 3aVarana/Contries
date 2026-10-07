package com.example.contris.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.contris.domain.model.Side
import com.example.contris.domain.usecase.BuildComparisonUseCase
import com.example.contris.domain.usecase.ObserveCountriesUseCase
import com.example.contris.domain.usecase.ObserveCountryUseCase
import com.example.contris.testutil.FakeCountryRepository
import com.example.contris.testutil.FakeSettingsRepository
import com.example.contris.testutil.Fixtures
import com.example.contris.testutil.MainDispatcherRule
import com.example.contris.ui.compare.CompareUiEffect
import com.example.contris.ui.compare.CompareUiEvent
import com.example.contris.ui.compare.CompareViewModel
import com.example.contris.ui.compare.Slot
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompareViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val repo = FakeCountryRepository().apply {
        countries.value = listOf(
            Fixtures.country("a", "Alpha", population = 10),
            Fixtures.country("b", "Beta", population = 20),
            Fixtures.country("c", "Gamma", population = 30),
        )
    }

    private fun vm(initial: String? = null, handle: SavedStateHandle = SavedStateHandle()) = CompareViewModel(
        initialFirstUuid = initial,
        savedStateHandle = handle,
        observeCountry = ObserveCountryUseCase(repo),
        observeCountries = ObserveCountriesUseCase(repo),
        settingsRepository = FakeSettingsRepository(),
        buildComparison = BuildComparisonUseCase(),
    )

    @Test
    fun `initial uuid pre-fills slot A and rows appear when both picked`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = vm(initial = "a", handle = handle)
        vm.uiState.test {
            awaitItem()
            val withFirst = awaitItemUntil { it.first != null }
            assertThat(withFirst.first!!.names.common).isEqualTo("Alpha")
            assertThat(withFirst.rows).isEmpty()

            vm.onEvent(CompareUiEvent.OpenPicker(Slot.SECOND))
            advanceTimeBy(300)
            val picker = awaitItemUntil { it.pickerFor == Slot.SECOND && it.pickerResults.isNotEmpty() }
            assertThat(picker.pickerResults.map { it.name }).containsExactly("Alpha", "Beta", "Gamma").inOrder()

            vm.onEvent(CompareUiEvent.CountryPicked(Slot.SECOND, "c"))
            val both = awaitItemUntil { it.second != null && it.rows.isNotEmpty() && it.pickerFor == null }
            val population = both.rows.first { it.label == "Population" }
            assertThat(population.winner).isEqualTo(Side.RIGHT)
            assertThat(handle.get<String>(CompareViewModel.KEY_FIRST)).isEqualTo("a")
            assertThat(handle.get<String>(CompareViewModel.KEY_SECOND)).isEqualTo("c")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saved state wins over initial uuid, swap and clear work`() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf(CompareViewModel.KEY_FIRST to "b", CompareViewModel.KEY_SECOND to "c"))
        val vm = vm(initial = "a", handle = handle)
        vm.uiState.test {
            val both = awaitItemUntil { it.first != null && it.second != null }
            assertThat(both.first!!.names.common).isEqualTo("Beta")
            assertThat(both.second!!.names.common).isEqualTo("Gamma")

            vm.onEvent(CompareUiEvent.Swap)
            val swapped = awaitItemUntil { it.first?.uuid == "c" && it.second?.uuid == "b" }
            assertThat(swapped.first!!.names.common).isEqualTo("Gamma")

            vm.onEvent(CompareUiEvent.Clear(Slot.FIRST))
            val cleared = awaitItemUntil { it.first == null }
            assertThat(cleared.rows).isEmpty()
            assertThat(cleared.second!!.names.common).isEqualTo("Beta")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picker query filters results`() = runTest(dispatcher) {
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            vm.onEvent(CompareUiEvent.OpenPicker(Slot.FIRST))
            vm.onEvent(CompareUiEvent.PickerQueryChanged("gam"))
            advanceTimeBy(300)
            val results = awaitItemUntil { it.pickerQuery == "gam" && it.pickerResults.size == 1 }
            assertThat(results.pickerResults.single().name).isEqualTo("Gamma")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `open detail emits effect`() = runTest(dispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(CompareUiEvent.OpenDetail("b"))
            assertThat(awaitItem()).isEqualTo(CompareUiEffect.NavigateToDetail("b"))
        }
    }
}
