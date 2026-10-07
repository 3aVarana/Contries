package com.example.contris.data

import com.example.contris.data.mapper.toDomain
import com.example.contris.data.mapper.toEntityOrNull
import com.example.contris.domain.model.Membership
import com.example.contris.testutil.Fixtures
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class CountryMapperTest {

    @Test
    fun `page fixture parses and skips records without uuid`() {
        val page = Fixtures.page(0)
        assertThat(page.data!!.objects).hasSize(6)
        assertThat(page.data!!.meta!!.more).isTrue()
        val entities = page.data!!.objects.mapNotNull { it.toEntityOrNull() }
        assertThat(entities.map { it.nameCommon }).containsExactly("Canada", "Abkhazia", "Germany", "Austria", "Niger")
    }

    @Test
    fun `canada maps dto to entity to domain`() {
        val entity = Fixtures.dto("Canada").toEntityOrNull()!!
        assertThat(entity.uuid).isEqualTo("5e4b3c2a-0001-4a1b-9c1d-000000000001")
        assertThat(entity.alpha3).isEqualTo("CAN")
        assertThat(entity.capitalName).isEqualTo("Ottawa")
        assertThat(entity.capitalLat).isWithin(0.001).of(45.42)
        assertThat(entity.flagVibrant).isEqualTo("#d52b1e")
        assertThat(entity.flagMuted).isEqualTo("#b5b5b5")
        assertThat(entity.giniLatestYear).isEqualTo(2019)
        assertThat(entity.giniLatest).isWithin(0.001).of(33.1)
        assertThat(entity.searchText).contains("canada|canada|ca|kanada")
        assertThat(entity.searchText).contains("|ca|can|")
        assertThat(entity.mG7).isTrue()
        assertThat(entity.mEu).isFalse()
        assertThat(entity.sovereign).isTrue()

        val c = entity.toDomain()
        assertThat(c.names.common).isEqualTo("Canada")
        assertThat(c.names.alternates).containsExactly("CA", "Kanada")
        assertThat(c.names.native["fra"]!!.official).isEqualTo("Canada")
        assertThat(c.capitals.single().isPrimary).isTrue()
        assertThat(c.capitals.single().roles).containsExactly("legislative")
        assertThat(c.borders).containsExactly("USA")
        assertThat(c.currencies.single().code).isEqualTo("CAD")
        assertThat(c.languages.map { it.nativeName }).containsExactly("English", "Français")
        assertThat(c.timezones).hasSize(6)
        assertThat(c.gini).containsExactly(2017, 33.3, 2019, 33.1)
        assertThat(c.latestGini).isEqualTo(2019 to 33.1)
        assertThat(c.memberships.members).containsExactly(
            Membership.UN, Membership.NATO, Membership.COMMONWEALTH, Membership.OECD, Membership.G7, Membership.G20,
        )
        assertThat(c.links.official).isEqualTo("https://www.canada.ca")
        assertThat(c.postalFormat).isEqualTo("@#@ #@#")
        assertThat(c.lastUpdated).isEqualTo(Instant.ofEpochSecond(1759276800))
        assertThat(c.densityPerKm2!!).isWithin(0.01).of(41012563 / 9984670.0)
        assertThat(c.toSummary().capital).isEqualTo("Ottawa")
    }

    @Test
    fun `abkhazia with empty codes and null flag maps without crashing`() {
        val entity = Fixtures.dto("Abkhazia").toEntityOrNull()!!
        assertThat(entity.alpha2).isNull()
        assertThat(entity.alpha3).isNull()
        assertThat(entity.flagPng).isNull()
        assertThat(entity.flagVibrant).isNull()
        assertThat(entity.giniLatest).isNull()
        assertThat(entity.disputed).isTrue()
        assertThat(entity.sovereign).isFalse()
        assertThat(entity.descriptionShort).isNull()

        val c = entity.toDomain()
        assertThat(c.codes.alpha3).isNull()
        assertThat(c.flag.pngUrl).isNull()
        assertThat(c.descriptions).isNull()
        assertThat(c.primaryCapital!!.name).isEqualTo("Sukhumi")
        assertThat(c.primaryCapital!!.isPrimary).isFalse()
        assertThat(c.classification.disputed).isTrue()
        assertThat(c.gini).isEmpty()
        assertThat(c.latestGini).isNull()
    }

    @Test
    fun `missing optional objects default sensibly`() {
        val entity = Fixtures.dto("Niger").toEntityOrNull()!!
        assertThat(entity.postalFormat).isNull()
        assertThat(entity.flagDominant).isNull()
        assertThat(entity.mAfricanUnion).isTrue()
        assertThat(entity.landlocked).isTrue()
        val c = entity.toDomain()
        assertThat(c.flag.colors.vibrant).isNull()
        assertThat(c.names.alternates).isEmpty()
    }

    @Test
    fun `germany search text includes native name and codes`() {
        val entity = Fixtures.dto("Germany").toEntityOrNull()!!
        assertThat(entity.searchText).contains("deutschland")
        assertThat(entity.searchText).contains("bundesrepublik deutschland")
        assertThat(entity.searchText).contains("|de|deu|")
        assertThat(entity.searchText).contains("berlin")
    }
}
