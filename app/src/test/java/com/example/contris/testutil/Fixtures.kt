package com.example.contris.testutil

import com.example.contris.data.remote.dto.CountriesPageDto
import com.example.contris.data.remote.dto.CountryDto
import com.example.contris.domain.model.Area
import com.example.contris.domain.model.Capital
import com.example.contris.domain.model.Classification
import com.example.contris.domain.model.Codes
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.FlagInfo
import com.example.contris.domain.model.Links
import com.example.contris.domain.model.Memberships
import com.example.contris.domain.model.Names
import kotlinx.serialization.json.Json

object Fixtures {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    fun raw(name: String): String =
        checkNotNull(Fixtures::class.java.classLoader!!.getResourceAsStream("fixtures/$name")) { "missing fixture $name" }
            .bufferedReader().use { it.readText() }

    fun page(index: Int): CountriesPageDto = json.decodeFromString(raw("countries_page_$index.json"))

    fun dto(common: String): CountryDto =
        (page(0).data!!.objects + page(1).data!!.objects).first { it.names?.common == common }

    fun country(
        uuid: String,
        name: String,
        capital: String? = "Capital of $name",
        region: String? = "Europe",
        subregion: String? = "Western Europe",
        png: String? = "https://flags/$uuid.png",
        population: Long? = 1_000_000,
        areaKm2: Double? = 1000.0,
        sovereign: Boolean = true,
    ): Country = Country(
        uuid = uuid,
        names = Names(common = name, official = "Official $name"),
        codes = Codes(alpha3 = uuid.take(3).uppercase()),
        capitals = listOfNotNull(capital?.let { Capital(it, isPrimary = true) }),
        flag = FlagInfo(emoji = "🏳", pngUrl = png),
        region = region,
        subregion = subregion,
        continents = listOfNotNull(region),
        landlocked = false,
        borders = emptyList(),
        area = areaKm2?.let { Area(it, it * 0.386) },
        coordinates = null,
        population = population,
        currencies = emptyList(),
        languages = emptyList(),
        callingCodes = emptyList(),
        tlds = emptyList(),
        timezones = listOf("UTC+01:00"),
        drivingSide = "right",
        carSigns = emptyList(),
        postalFormat = null,
        startOfWeek = "monday",
        gini = emptyMap(),
        governmentType = null,
        classification = Classification(sovereign = sovereign, unMember = sovereign),
        memberships = Memberships(),
        descriptions = null,
        links = Links(),
        measurementSystem = "metric",
        lastUpdated = null,
    )

    fun summary(uuid: String, name: String, population: Long? = 1_000_000): CountrySummary =
        CountrySummary(uuid, name, "Capital", "Europe", "🏳", null, population, 1000.0)
}
