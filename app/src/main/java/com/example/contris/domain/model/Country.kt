package com.example.contris.domain.model

import java.time.Instant

data class CountrySummary(
    val uuid: String,
    val name: String,
    val capital: String?,
    val region: String?,
    val flagEmoji: String?,
    val flagPng: String?,
    val population: Long?,
    val areaKm2: Double?,
)

data class Names(
    val common: String,
    val official: String,
    val alternates: List<String> = emptyList(),
    val native: Map<String, NativeName> = emptyMap(),
)

data class NativeName(val common: String?, val official: String?)

data class Codes(
    val alpha2: String? = null,
    val alpha3: String? = null,
    val ccn3: String? = null,
    val cioc: String? = null,
    val fifa: String? = null,
    val fips: String? = null,
    val gec: String? = null,
)

data class LatLng(val lat: Double, val lng: Double)

data class Capital(
    val name: String,
    val coordinates: LatLng? = null,
    val isPrimary: Boolean = false,
    val roles: List<String> = emptyList(),
)

data class FlagColors(
    val dominant: String? = null,
    val prominent: String? = null,
    val vibrant: String? = null,
    val muted: String? = null,
)

data class FlagInfo(
    val emoji: String? = null,
    val pngUrl: String? = null,
    val svgUrl: String? = null,
    val description: String? = null,
    val colors: FlagColors = FlagColors(),
)

data class Area(val kilometers: Double?, val miles: Double?)

data class Currency(val code: String?, val name: String?, val symbol: String?)

data class Language(
    val name: String?,
    val nativeName: String?,
    val bcp47: String? = null,
    val iso6393: String? = null,
)

data class Classification(
    val sovereign: Boolean = false,
    val unMember: Boolean = false,
    val unObserver: Boolean = false,
    val dependency: Boolean = false,
    val dependencyType: String? = null,
    val disputed: Boolean = false,
    val isoStatus: String? = null,
)

enum class Membership(val label: String) {
    UN("UN"),
    EU("EU"),
    EUROZONE("Eurozone"),
    SCHENGEN("Schengen"),
    NATO("NATO"),
    COMMONWEALTH("Commonwealth"),
    OECD("OECD"),
    G7("G7"),
    G20("G20"),
    BRICS("BRICS"),
    OPEC("OPEC"),
    AFRICAN_UNION("African Union"),
    ASEAN("ASEAN"),
    ARAB_LEAGUE("Arab League"),
}

data class Memberships(val members: Set<Membership> = emptySet()) {
    operator fun contains(m: Membership): Boolean = m in members
}

data class Descriptions(val short: String?, val long: String?)

data class Links(
    val wikipedia: String? = null,
    val official: String? = null,
    val googleMaps: String? = null,
    val openStreetMaps: String? = null,
)

data class Country(
    val uuid: String,
    val names: Names,
    val codes: Codes,
    val capitals: List<Capital>,
    val flag: FlagInfo,
    val region: String?,
    val subregion: String?,
    val continents: List<String>,
    val landlocked: Boolean?,
    val borders: List<String>,
    val area: Area?,
    val coordinates: LatLng?,
    val population: Long?,
    val currencies: List<Currency>,
    val languages: List<Language>,
    val callingCodes: List<String>,
    val tlds: List<String>,
    val timezones: List<String>,
    val drivingSide: String?,
    val carSigns: List<String>,
    val postalFormat: String?,
    val startOfWeek: String?,
    val gini: Map<Int, Double>,
    val governmentType: String?,
    val classification: Classification,
    val memberships: Memberships,
    val descriptions: Descriptions?,
    val links: Links,
    val measurementSystem: String?,
    val lastUpdated: Instant?,
) {
    val primaryCapital: Capital? get() = capitals.firstOrNull { it.isPrimary } ?: capitals.firstOrNull()

    val latestGini: Pair<Int, Double>? get() = gini.maxByOrNull { it.key }?.toPair()

    val densityPerKm2: Double?
        get() {
            val pop = population ?: return null
            val km2 = area?.kilometers ?: return null
            if (km2 <= 0.0) return null
            return pop / km2
        }

    fun toSummary(): CountrySummary = CountrySummary(
        uuid = uuid,
        name = names.common,
        capital = primaryCapital?.name,
        region = region,
        flagEmoji = flag.emoji,
        flagPng = flag.pngUrl,
        population = population,
        areaKm2 = area?.kilometers,
    )
}
