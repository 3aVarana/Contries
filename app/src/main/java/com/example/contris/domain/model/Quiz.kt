package com.example.contris.domain.model

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class QuizMode { FLAG_TO_COUNTRY, COUNTRY_TO_CAPITAL, CAPITAL_TO_COUNTRY }

sealed interface QuizPrompt {
    data class Flag(val pngUrl: String, val emoji: String?) : QuizPrompt
    data class Text(val text: String, val emoji: String? = null) : QuizPrompt
}

data class QuizQuestion(
    val prompt: QuizPrompt,
    val options: List<String>,
    val correctIndex: Int,
    val countryUuid: String,
    val countryName: String,
)

data class QuizRound(val mode: QuizMode, val questions: List<QuizQuestion>)

data class QuizResult(
    val mode: QuizMode,
    val score: Int,
    val total: Int,
    val bestStreak: Int,
    val playedAt: Instant,
    val id: Long = 0,
)
