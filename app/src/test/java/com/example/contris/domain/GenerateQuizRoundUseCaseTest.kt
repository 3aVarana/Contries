package com.example.contris.domain

import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.model.QuizPrompt
import com.example.contris.domain.usecase.GenerateQuizRoundUseCase
import com.example.contris.testutil.FakeCountryRepository
import com.example.contris.testutil.Fixtures
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GenerateQuizRoundUseCaseTest {

    private val repo = FakeCountryRepository()
    private val useCase = GenerateQuizRoundUseCase(repo)

    private val pool = buildList {
        // 6 Western Europe, 6 Eastern Europe, 8 Africa — enough same-region distractors.
        repeat(6) { add(Fixtures.country("we$it", "WestEu $it", capital = "WCap $it", region = "Europe", subregion = "Western Europe")) }
        repeat(6) { add(Fixtures.country("ee$it", "EastEu $it", capital = "ECap $it", region = "Europe", subregion = "Eastern Europe")) }
        repeat(8) { add(Fixtures.country("af$it", "Africa $it", capital = "ACap $it", region = "Africa", subregion = "Western Africa")) }
    }

    @Test
    fun `round invariants hold for every mode and many seeds`() {
        for (mode in QuizMode.entries) {
            repeat(50) { seed ->
                val round = useCase.generate(mode, pool, Random(seed))
                assertThat(round.mode).isEqualTo(mode)
                assertThat(round.questions).hasSize(GenerateQuizRoundUseCase.QUESTIONS_PER_ROUND)
                assertThat(round.questions.map { it.countryUuid }).containsNoDuplicates()
                round.questions.forEach { q ->
                    assertThat(q.options).hasSize(GenerateQuizRoundUseCase.OPTIONS_PER_QUESTION)
                    assertThat(q.options).containsNoDuplicates()
                    assertThat(q.correctIndex).isIn(q.options.indices)
                    val answer = pool.first { it.uuid == q.countryUuid }
                    val expected = if (mode == QuizMode.COUNTRY_TO_CAPITAL) answer.primaryCapital!!.name else answer.names.common
                    assertThat(q.options[q.correctIndex]).isEqualTo(expected)
                    assertThat(q.options.count { it == expected }).isEqualTo(1)
                }
            }
        }
    }

    @Test
    fun `distractors prefer the same subregion`() {
        val round = useCase.generate(QuizMode.FLAG_TO_COUNTRY, pool, Random(7))
        round.questions.forEach { q ->
            val answer = pool.first { it.uuid == q.countryUuid }
            val distractors = q.options.filterIndexed { i, _ -> i != q.correctIndex }.map { name -> pool.first { it.names.common == name } }
            assertThat(distractors.map { it.subregion }).containsExactly(answer.subregion, answer.subregion, answer.subregion)
        }
    }

    @Test
    fun `prompts match the mode`() {
        val flag = useCase.generate(QuizMode.FLAG_TO_COUNTRY, pool, Random(1)).questions.first()
        assertThat(flag.prompt).isInstanceOf(QuizPrompt.Flag::class.java)

        val cap = useCase.generate(QuizMode.COUNTRY_TO_CAPITAL, pool, Random(1)).questions.first()
        assertThat((cap.prompt as QuizPrompt.Text).text).isEqualTo(cap.countryName)
        assertThat(cap.options[cap.correctIndex]).matches("[WEA]Cap \\d+")

        val c2c = useCase.generate(QuizMode.CAPITAL_TO_COUNTRY, pool, Random(1)).questions.first()
        val answer = pool.first { it.uuid == c2c.countryUuid }
        assertThat((c2c.prompt as QuizPrompt.Text).text).isEqualTo(answer.primaryCapital!!.name)
    }

    @Test
    fun `same seed yields same round`() {
        val a = useCase.generate(QuizMode.CAPITAL_TO_COUNTRY, pool, Random(42))
        val b = useCase.generate(QuizMode.CAPITAL_TO_COUNTRY, pool, Random(42))
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `countries without flag are excluded from flag mode but not capital modes`() {
        val noFlag = Fixtures.country("nf", "NoFlag", png = null)
        val round = useCase.generate(QuizMode.FLAG_TO_COUNTRY, pool + noFlag, Random(3))
        assertThat(round.questions.map { it.countryUuid }).doesNotContain("nf")
        assertThat(round.questions.flatMap { it.options }).doesNotContain("NoFlag")
    }

    @Test
    fun `too small pool throws`() {
        val error = runCatching { useCase.generate(QuizMode.FLAG_TO_COUNTRY, pool.take(3), Random(1)) }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `invoke reads the repository quiz pool`() = runTest {
        repo.countries.value = pool
        val round = useCase(QuizMode.FLAG_TO_COUNTRY, Random(5))
        assertThat(round.questions).hasSize(10)
    }
}
