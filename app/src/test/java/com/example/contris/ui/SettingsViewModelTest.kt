package com.example.contris.ui

import app.cash.turbine.test
import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.model.QuizResult
import com.example.contris.domain.model.SyncError
import com.example.contris.domain.model.SyncException
import com.example.contris.domain.model.ThemeMode
import com.example.contris.domain.model.UnitSystem
import com.example.contris.domain.usecase.SyncCountriesUseCase
import com.example.contris.testutil.FakeCountryRepository
import com.example.contris.testutil.FakeFavoritesRepository
import com.example.contris.testutil.FakeQuizRepository
import com.example.contris.testutil.FakeSettingsRepository
import com.example.contris.testutil.Fixtures
import com.example.contris.testutil.MainDispatcherRule
import com.example.contris.ui.common.UiText
import com.example.contris.ui.settings.SettingsUiEffect
import com.example.contris.ui.settings.SettingsUiEvent
import com.example.contris.ui.settings.SettingsViewModel
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val repo = FakeCountryRepository().apply { countries.value = listOf(Fixtures.country("a", "Alpha")) }
    private val favorites = FakeFavoritesRepository(repo)
    private val quiz = FakeQuizRepository()
    private val settings = FakeSettingsRepository()

    private fun vm() = SettingsViewModel(settings, repo, favorites, quiz, SyncCountriesUseCase(repo), "1.0 (1)")

    @Test
    fun `reflects settings and updates them`() = runTest(dispatcher) {
        val vm = vm()
        vm.uiState.test {
            val initial = awaitItemUntil { it.countryCount == 1 }
            assertThat(initial.appVersion).isEqualTo("1.0 (1)")
            assertThat(initial.themeMode).isEqualTo(ThemeMode.SYSTEM)

            vm.onEvent(SettingsUiEvent.SetThemeMode(ThemeMode.DARK))
            assertThat(awaitItemUntil { it.themeMode == ThemeMode.DARK }.themeMode).isEqualTo(ThemeMode.DARK)
            vm.onEvent(SettingsUiEvent.SetUnitSystem(UnitSystem.IMPERIAL))
            assertThat(awaitItemUntil { it.unitSystem == UnitSystem.IMPERIAL }.unitSystem).isEqualTo(UnitSystem.IMPERIAL)
            vm.onEvent(SettingsUiEvent.SetDynamicColor(false))
            assertThat(awaitItemUntil { !it.dynamicColor }.dynamicColor).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh now forces sync and reports success or error`() = runTest(dispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(SettingsUiEvent.RefreshNow)
            advanceUntilIdle()
            assertThat(((awaitItem() as SettingsUiEffect.ShowMessage).text as UiText.Dynamic).value).contains("up to date")
            assertThat(repo.syncCalls).containsExactly(true)

            repo.syncResult = Result.failure(SyncException(SyncError.RateLimited))
            vm.onEvent(SettingsUiEvent.RefreshNow)
            advanceUntilIdle()
            assertThat(((awaitItem() as SettingsUiEffect.ShowMessage).text as UiText.Dynamic).value).contains("wait a minute")
        }
    }

    @Test
    fun `clear actions empty the repositories`() = runTest(dispatcher) {
        favorites.add("a")
        quiz.save(QuizResult(QuizMode.FLAG_TO_COUNTRY, 5, 10, 2, Instant.EPOCH))
        val vm = vm()
        vm.effects.test {
            vm.onEvent(SettingsUiEvent.ClearFavorites)
            advanceUntilIdle()
            awaitItem()
            assertThat(repo.favoriteUuids.value).isEmpty()
            vm.onEvent(SettingsUiEvent.ClearQuizHistory)
            advanceUntilIdle()
            awaitItem()
            assertThat(quiz.results.value).isEmpty()
        }
    }
}
