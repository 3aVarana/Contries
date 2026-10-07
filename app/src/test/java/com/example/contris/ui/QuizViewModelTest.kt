package com.example.contris.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.usecase.GenerateQuizRoundUseCase
import com.example.contris.domain.usecase.SaveQuizResultUseCase
import com.example.contris.testutil.FakeCountryRepository
import com.example.contris.testutil.FakeQuizRepository
import com.example.contris.testutil.Fixtures
import com.example.contris.testutil.MainDispatcherRule
import com.example.contris.ui.quiz.QuizPlayUiEffect
import com.example.contris.ui.quiz.QuizPlayUiEvent
import com.example.contris.ui.quiz.QuizPlayUiState
import com.example.contris.ui.quiz.QuizViewModel
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val repo = FakeCountryRepository().apply {
        countries.value = (1..12).map { Fixtures.country("c$it", "Country $it", capital = "Cap $it") }
    }
    private val quizRepo = FakeQuizRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC)

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) = QuizViewModel(
        mode = QuizMode.FLAG_TO_COUNTRY,
        savedStateHandle = handle,
        generateRound = GenerateQuizRoundUseCase(repo),
        saveResult = SaveQuizResultUseCase(quizRepo),
        clock = clock,
    )

    @Test
    fun `full round - select reveals, next advances, finishing persists result`() = runTest(dispatcher) {
        val vm = vm()
        vm.uiState.test {
            assertThat(awaitItem()).isEqualTo(QuizPlayUiState.Generating)
            var q = awaitItem() as QuizPlayUiState.Question
            assertThat(q.index).isEqualTo(0)
            assertThat(q.total).isEqualTo(10)

            // Next before answering is ignored.
            vm.onEvent(QuizPlayUiEvent.Next)
            expectNoEvents()

            var expectedScore = 0
            repeat(10) { i ->
                val pick = if (i % 2 == 0) q.question.correctIndex else (q.question.correctIndex + 1) % 4
                if (i % 2 == 0) expectedScore++
                vm.onEvent(QuizPlayUiEvent.OptionSelected(pick))
                val revealed = awaitItem() as QuizPlayUiState.Question
                assertThat(revealed.revealed).isTrue()
                assertThat(revealed.selectedIndex).isEqualTo(pick)
                assertThat(revealed.score).isEqualTo(expectedScore)

                // Selecting again after reveal is ignored.
                vm.onEvent(QuizPlayUiEvent.OptionSelected((pick + 2) % 4))
                expectNoEvents()

                vm.onEvent(QuizPlayUiEvent.Next)
                val next = awaitItem()
                if (i < 9) q = next as QuizPlayUiState.Question else {
                    val finished = next as QuizPlayUiState.Finished
                    assertThat(finished.result.score).isEqualTo(5)
                    assertThat(finished.result.total).isEqualTo(10)
                    assertThat(finished.result.bestStreak).isEqualTo(1)
                    assertThat(finished.result.playedAt).isEqualTo(clock.instant())
                    assertThat(finished.answers).hasSize(10)
                }
            }
            advanceUntilIdle()
            assertThat(quizRepo.results.value).hasSize(1)
            assertThat(quizRepo.results.value.single().mode).isEqualTo(QuizMode.FLAG_TO_COUNTRY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quitting mid-round does not persist`() = runTest(dispatcher) {
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            val q = awaitItem() as QuizPlayUiState.Question
            vm.onEvent(QuizPlayUiEvent.OptionSelected(q.question.correctIndex))
            awaitItem()
            vm.effects.test {
                vm.onEvent(QuizPlayUiEvent.Quit)
                assertThat(awaitItem()).isEqualTo(QuizPlayUiEffect.NavigateBack)
            }
            advanceUntilIdle()
            assertThat(quizRepo.results.value).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progress survives recreation through SavedStateHandle`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val first = vm(handle)
        first.uiState.test {
            awaitItem()
            val q0 = awaitItem() as QuizPlayUiState.Question
            first.onEvent(QuizPlayUiEvent.OptionSelected(q0.question.correctIndex))
            awaitItem()
            first.onEvent(QuizPlayUiEvent.Next)
            val q1 = awaitItem() as QuizPlayUiState.Question
            first.onEvent(QuizPlayUiEvent.OptionSelected(q1.question.correctIndex))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val restored = vm(handle)
        restored.uiState.test {
            awaitItem()
            val q = awaitItem() as QuizPlayUiState.Question
            assertThat(q.index).isEqualTo(1)
            assertThat(q.revealed).isTrue()
            assertThat(q.score).isEqualTo(2)
            assertThat(q.streak).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `play again starts a fresh round`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = vm(handle)
        vm.uiState.test {
            awaitItem()
            awaitItem()
            val seed = handle.get<Long>(QuizViewModel.KEY_SEED)
            vm.onEvent(QuizPlayUiEvent.PlayAgain)
            assertThat(awaitItem()).isEqualTo(QuizPlayUiState.Generating)
            val q = awaitItem() as QuizPlayUiState.Question
            assertThat(q.index).isEqualTo(0)
            assertThat(q.score).isEqualTo(0)
            assertThat(handle.get<Long>(QuizViewModel.KEY_SEED)).isNotEqualTo(seed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `too little data produces an error state`() = runTest(dispatcher) {
        repo.countries.value = repo.countries.value.take(2)
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            assertThat(awaitItem()).isInstanceOf(QuizPlayUiState.Error::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
