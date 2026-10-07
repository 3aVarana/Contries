package com.example.contris.domain.usecase

import com.example.contris.domain.model.Country
import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.model.QuizPrompt
import com.example.contris.domain.model.QuizQuestion
import com.example.contris.domain.model.QuizRound
import com.example.contris.domain.repository.CountryRepository
import javax.inject.Inject
import kotlin.random.Random

/**
 * Builds a quiz round of [QUESTIONS_PER_ROUND] questions from the quiz pool.
 *
 * Invariants: no repeated correct answer, 4 distinct options, exactly one correct option.
 * Distractors prefer countries from the same subregion, then region, then anywhere.
 */
class GenerateQuizRoundUseCase @Inject constructor(private val repository: CountryRepository) {

    suspend operator fun invoke(mode: QuizMode, random: Random = Random.Default): QuizRound {
        val pool = repository.getQuizPool()
        return generate(mode, pool, random)
    }

    fun generate(mode: QuizMode, pool: List<Country>, random: Random = Random.Default): QuizRound {
        val eligible = pool.filter { it.isEligible(mode) }
        require(eligible.size >= OPTIONS_PER_QUESTION) { "Not enough countries to build a quiz" }

        val count = minOf(QUESTIONS_PER_ROUND, eligible.size)
        val answers = eligible.shuffled(random).take(count)
        val questions = answers.map { answer -> buildQuestion(mode, answer, eligible, random) }
        return QuizRound(mode, questions)
    }

    private fun buildQuestion(mode: QuizMode, answer: Country, eligible: List<Country>, random: Random): QuizQuestion {
        val answerLabel = answer.label(mode)
        val distractors = pickDistractors(answer, eligible, mode, random)
        val options = (distractors.map { it.label(mode) } + answerLabel).shuffled(random)
        val prompt = when (mode) {
            QuizMode.FLAG_TO_COUNTRY -> QuizPrompt.Flag(answer.flag.pngUrl!!, answer.flag.emoji)
            QuizMode.COUNTRY_TO_CAPITAL -> QuizPrompt.Text(answer.names.common, answer.flag.emoji)
            QuizMode.CAPITAL_TO_COUNTRY -> QuizPrompt.Text(answer.primaryCapital!!.name)
        }
        return QuizQuestion(
            prompt = prompt,
            options = options,
            correctIndex = options.indexOf(answerLabel),
            countryUuid = answer.uuid,
            countryName = answer.names.common,
        )
    }

    private fun pickDistractors(answer: Country, eligible: List<Country>, mode: QuizMode, random: Random): List<Country> {
        val answerLabel = answer.label(mode)
        val candidates = eligible.filter { it.uuid != answer.uuid && it.label(mode) != answerLabel }
        val picked = LinkedHashMap<String, Country>() // keyed by label to keep options distinct

        fun take(from: List<Country>) {
            for (c in from.shuffled(random)) {
                if (picked.size >= OPTIONS_PER_QUESTION - 1) return
                picked.putIfAbsent(c.label(mode), c)
            }
        }

        take(candidates.filter { it.subregion != null && it.subregion == answer.subregion })
        take(candidates.filter { it.region != null && it.region == answer.region })
        take(candidates)
        return picked.values.toList()
    }

    private fun Country.isEligible(mode: QuizMode): Boolean = when (mode) {
        QuizMode.FLAG_TO_COUNTRY -> !flag.pngUrl.isNullOrBlank()
        QuizMode.COUNTRY_TO_CAPITAL, QuizMode.CAPITAL_TO_COUNTRY -> primaryCapital?.name?.isNotBlank() == true
    }

    private fun Country.label(mode: QuizMode): String = when (mode) {
        QuizMode.FLAG_TO_COUNTRY, QuizMode.CAPITAL_TO_COUNTRY -> names.common
        QuizMode.COUNTRY_TO_CAPITAL -> primaryCapital?.name.orEmpty()
    }

    companion object {
        const val QUESTIONS_PER_ROUND = 10
        const val OPTIONS_PER_QUESTION = 4
    }
}
