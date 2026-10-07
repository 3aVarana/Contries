package com.example.contris.ui.quiz

import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.model.QuizQuestion
import com.example.contris.domain.model.QuizResult

data class QuizHomeUiState(
    val bestByMode: Map<QuizMode, QuizResult?> = emptyMap(),
    val recent: List<QuizResult> = emptyList(),
    val canPlay: Boolean = true,
)

sealed interface QuizHomeUiEvent {
    data class StartQuiz(val mode: QuizMode) : QuizHomeUiEvent
    data object ClearHistory : QuizHomeUiEvent
}

sealed interface QuizHomeUiEffect {
    data class NavigateToPlay(val mode: QuizMode) : QuizHomeUiEffect
}

sealed interface QuizPlayUiState {
    data object Generating : QuizPlayUiState
    data class Error(val message: String) : QuizPlayUiState
    data class Question(
        val index: Int,
        val total: Int,
        val question: QuizQuestion,
        val selectedIndex: Int?,
        val revealed: Boolean,
        val score: Int,
        val streak: Int,
        val bestStreak: Int,
    ) : QuizPlayUiState
    data class Finished(
        val result: QuizResult,
        val questions: List<QuizQuestion>,
        val answers: List<Int?>,
    ) : QuizPlayUiState
}

sealed interface QuizPlayUiEvent {
    data class OptionSelected(val index: Int) : QuizPlayUiEvent
    data object Next : QuizPlayUiEvent
    data object Quit : QuizPlayUiEvent
    data object PlayAgain : QuizPlayUiEvent
    data class OpenCountry(val uuid: String) : QuizPlayUiEvent
}

sealed interface QuizPlayUiEffect {
    data object NavigateBack : QuizPlayUiEffect
    data class NavigateToDetail(val uuid: String) : QuizPlayUiEffect
}

fun QuizMode.title(): String = when (this) {
    QuizMode.FLAG_TO_COUNTRY -> "Guess the country from its flag"
    QuizMode.COUNTRY_TO_CAPITAL -> "Guess the capital"
    QuizMode.CAPITAL_TO_COUNTRY -> "Guess the country from its capital"
}

fun QuizMode.shortTitle(): String = when (this) {
    QuizMode.FLAG_TO_COUNTRY -> "Flags"
    QuizMode.COUNTRY_TO_CAPITAL -> "Capitals"
    QuizMode.CAPITAL_TO_COUNTRY -> "Capital → country"
}
