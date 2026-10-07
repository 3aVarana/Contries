package com.example.contris.domain

import com.example.contris.domain.model.Country
import com.example.contris.domain.model.Currency
import com.example.contris.domain.model.Language
import com.example.contris.domain.model.Membership
import com.example.contris.domain.model.Memberships
import com.example.contris.domain.model.Side
import com.example.contris.domain.model.UnitSystem
import com.example.contris.domain.usecase.BuildComparisonUseCase
import com.example.contris.testutil.Fixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuildComparisonUseCaseTest {

    private val useCase = BuildComparisonUseCase()
    private val f = object : BuildComparisonUseCase.Formatters {
        override fun population(value: Long) = "pop=$value"
        override fun area(km2: Double, unitSystem: UnitSystem) = "area=${km2.toInt()}:${unitSystem.name}"
        override fun density(perKm2: Double, unitSystem: UnitSystem) = "den=${perKm2.toInt()}"
        override fun decimal(value: Double) = value.toString()
    }

    private val a: Country = Fixtures.country("aaa", "Alpha", population = 10_000_000, areaKm2 = 1000.0).copy(
        languages = listOf(Language("English", "English")),
        currencies = listOf(Currency("EUR", "Euro", "€")),
        memberships = Memberships(setOf(Membership.UN, Membership.EU, Membership.NATO)),
        gini = mapOf(2010 to 30.0, 2019 to 28.5),
        callingCodes = listOf("+1"),
        timezones = listOf("UTC", "UTC+1"),
    )
    private val b: Country = Fixtures.country("bbb", "Beta", population = 2_000_000, areaKm2 = 4000.0).copy(
        languages = listOf(Language("French", "Français"), Language("German", "Deutsch")),
        currencies = emptyList(),
        memberships = Memberships(setOf(Membership.UN, Membership.OECD)),
        gini = emptyMap(),
        callingCodes = emptyList(),
        timezones = listOf("UTC"),
        landlocked = null,
    )

    @Test
    fun `numeric rows mark the larger side`() {
        val rows = useCase(a, b, UnitSystem.METRIC, f).rows.associateBy { it.label }
        assertThat(rows["Population"]!!.winner).isEqualTo(Side.LEFT)
        assertThat(rows["Population"]!!.left).isEqualTo("pop=10000000")
        assertThat(rows["Area"]!!.winner).isEqualTo(Side.RIGHT)
        assertThat(rows["Area"]!!.right).isEqualTo("area=4000:METRIC")
        assertThat(rows["Density"]!!.winner).isEqualTo(Side.LEFT) // 10000/km² vs 500/km²
        assertThat(rows["Time zones"]!!.winner).isEqualTo(Side.LEFT)
        assertThat(rows["Time zones"]!!.left).isEqualTo("2")
    }

    @Test
    fun `missing values are null so the UI can render a dash, and text rows have no winner`() {
        val rows = useCase(a, b, UnitSystem.IMPERIAL, f).rows.associateBy { it.label }
        assertThat(rows["Currencies"]!!.left).isEqualTo("EUR")
        assertThat(rows["Currencies"]!!.right).isNull()
        assertThat(rows["Gini (latest)"]!!.left).isEqualTo("28.5 (2019)")
        assertThat(rows["Gini (latest)"]!!.right).isNull()
        assertThat(rows["Calling code"]!!.right).isNull()
        assertThat(rows["Landlocked"]!!.left).isEqualTo("No")
        assertThat(rows["Landlocked"]!!.right).isNull()
        assertThat(rows["Languages"]!!.right).isEqualTo("French, German")
        assertThat(rows["Capital"]!!.winner).isNull()
        assertThat(rows["Area"]!!.left).isEqualTo("area=1000:IMPERIAL")
    }

    @Test
    fun `memberships split into shared and exclusive`() {
        val rows = useCase(a, b, UnitSystem.METRIC, f).rows.associateBy { it.label }
        assertThat(rows["Shared memberships"]!!.left).isEqualTo("UN")
        assertThat(rows["Shared memberships"]!!.right).isEqualTo("UN")
        assertThat(rows["Exclusive memberships"]!!.left).isEqualTo("EU, NATO")
        assertThat(rows["Exclusive memberships"]!!.right).isEqualTo("OECD")
    }

    @Test
    fun `equal numeric values have no winner`() {
        val rows = useCase(a, a, UnitSystem.METRIC, f).rows.associateBy { it.label }
        assertThat(rows["Population"]!!.winner).isNull()
        assertThat(rows["Area"]!!.winner).isNull()
    }
}
