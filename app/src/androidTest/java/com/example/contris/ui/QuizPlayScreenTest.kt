package com.example.contris.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.model.QuizPrompt
import com.example.contris.domain.model.QuizQuestion
import com.example.contris.domain.model.QuizResult
import com.example.contris.ui.quiz.QuizPlayScreen
import com.example.contris.ui.quiz.QuizPlayUiEvent
import com.example.contris.ui.quiz.QuizPlayUiState
import com.example.contris.ui.theme.ContrisTheme
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class QuizPlayScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val question = QuizQuestion(
        prompt = QuizPrompt.Text("Berlin"),
        options = listOf("France", "Germany", "Austria", "Poland"),
        correctIndex = 1,
        countryUuid = "de",
        countryName = "Germany",
    )

    @Test
    fun selectingAnOptionEmitsEvent_thenRevealedStateShowsNext() {
        val events = mutableListOf<QuizPlayUiEvent>()
        rule.setContent {
            ContrisTheme {
                QuizPlayScreen(
                    state = QuizPlayUiState.Question(index = 2, total = 10, question = question, selectedIndex = null, revealed = false, score = 1, streak = 1, bestStreak = 1),
                    title = "Capitals",
                    onEvent = { events += it },
                )
            }
        }
        rule.onNodeWithText("3 / 10").assertIsDisplayed()
        rule.onNodeWithText("Berlin").assertIsDisplayed()
        rule.onNodeWithTag("option_2").performClick()
        assertThat(events).containsExactly(QuizPlayUiEvent.OptionSelected(2))
    }

    @Test
    fun revealedStateDisablesOptionsAndShowsNext() {
        val events = mutableListOf<QuizPlayUiEvent>()
        rule.setContent {
            ContrisTheme {
                QuizPlayScreen(
                    state = QuizPlayUiState.Question(index = 9, total = 10, question = question, selectedIndex = 2, revealed = true, score = 5, streak = 0, bestStreak = 3),
                    title = "Capitals",
                    onEvent = { events += it },
                )
            }
        }
        rule.onNodeWithTag("option_0").assertIsNotEnabled()
        rule.onNodeWithText("Finish").assertIsDisplayed().performClick()
        assertThat(events).containsExactly(QuizPlayUiEvent.Next)
    }

    @Test
    fun finishedStateShowsScoreAndAnswers() {
        val events = mutableListOf<QuizPlayUiEvent>()
        rule.setContent {
            ContrisTheme {
                QuizPlayScreen(
                    state = QuizPlayUiState.Finished(
                        result = QuizResult(QuizMode.CAPITAL_TO_COUNTRY, 7, 10, 4, Instant.EPOCH),
                        questions = listOf(question),
                        answers = listOf(2),
                    ),
                    title = "Capitals",
                    onEvent = { events += it },
                )
            }
        }
        rule.onNodeWithText("7/10").assertIsDisplayed()
        rule.onNodeWithText("Best streak: 4").assertIsDisplayed()
        rule.onNodeWithText("See answers").performClick()
        rule.onNodeWithTag("answer_0").assertIsDisplayed()
        rule.onNodeWithText("You said Austria · answer: Germany").assertIsDisplayed()
        rule.onNodeWithTag("play_again").performClick()
        assertThat(events).contains(QuizPlayUiEvent.PlayAgain)
    }
}
