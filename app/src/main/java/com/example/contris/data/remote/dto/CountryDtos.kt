package com.example.contris.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * All DTO fields are nullable or defaulted so a schema addition/removal never crashes the sync.
 * Parsed with `ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false`.
 */

@Serializable
data class CountriesPageDto(
    val data: PageDataDto? = null,
    val errors: List<ApiErrorDto> = emptyList(),
)

@Serializable
data class ApiErrorDto(val message: String? = null)

@Serializable
data class PageDataDto(
    val objects: List<CountryDto> = emptyList(),
    val meta: MetaDto? = null,
)

@Serializable
data class MetaDto(
    val total: Int? = null,
    val count: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val more: Boolean? = null,
    @SerialName("request_id") val requestId: String? = null,
)

@Serializable
data class CountryDto(
    val uuid: String? = null,
    val names: NamesDto? = null,
    val codes: CodesDto? = null,
    val capitals: List<CapitalDto> = emptyList(),
    val flag: FlagDto? = null,
    val region: String? = null,
    val subregion: String? = null,
    val continents: List<String> = emptyList(),
    val landlocked: Boolean? = null,
    val borders: List<String> = emptyList(),
    val area: AreaDto? = null,
    val coordinates: CoordinatesDto? = null,
    val population: Long? = null,
    @Serializable(with = CurrenciesSerializer::class) val currencies: List<CurrencyDto> = emptyList(),
    @Serializable(with = LanguagesSerializer::class) val languages: List<LanguageDto> = emptyList(),
    @SerialName("calling_codes") val callingCodes: List<String> = emptyList(),
    val tlds: List<String> = emptyList(),
    val timezones: List<String> = emptyList(),
    val cars: CarsDto? = null,
    @SerialName("postal_code") val postalCode: PostalCodeDto? = null,
    val date: DateDto? = null,
    val economy: EconomyDto? = null,
    @SerialName("government_type") val governmentType: String? = null,
    val classification: ClassificationDto? = null,
    val memberships: MembershipsDto? = null,
    val descriptions: DescriptionsDto? = null,
    val links: LinksDto? = null,
    val units: UnitsDto? = null,
    @SerialName("_meta") val meta: RecordMetaDto? = null,
)

@Serializable
data class NamesDto(
    val common: String? = null,
    val official: String? = null,
    val alternates: List<String> = emptyList(),
    val native: Map<String, NativeNameDto> = emptyMap(),
)

@Serializable
data class NativeNameDto(val common: String? = null, val official: String? = null)

@Serializable
data class CodesDto(
    @SerialName("alpha_2") val alpha2: String? = null,
    @SerialName("alpha_3") val alpha3: String? = null,
    val ccn3: String? = null,
    val cioc: String? = null,
    val fifa: String? = null,
    val fips: String? = null,
    val gec: String? = null,
)

@Serializable
data class CapitalDto(
    val name: String? = null,
    val coordinates: CoordinatesDto? = null,
    /** e.g. `{ "primary": true, "legislative": true }` — kept loose on purpose. */
    val attributes: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class CoordinatesDto(val lat: Double? = null, val lng: Double? = null)

@Serializable
data class FlagDto(
    val emoji: String? = null,
    @SerialName("url_png") val urlPng: String? = null,
    @SerialName("url_svg") val urlSvg: String? = null,
    val description: String? = null,
    val colors: FlagColorsDto? = null,
)

@Serializable
data class FlagColorsDto(
    val dominant: String? = null,
    val prominent: String? = null,
    /** Swatch values may be hex strings or objects depending on the record; parsed leniently. */
    val swatches: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class AreaDto(val kilometers: Double? = null, val miles: Double? = null)

@Serializable
data class CurrencyDto(val code: String? = null, val name: String? = null, val symbol: String? = null)

@Serializable
data class LanguageDto(
    val name: String? = null,
    @SerialName("native_name") val nativeName: String? = null,
    val bcp47: String? = null,
    @SerialName("iso639_3") val iso6393: String? = null,
)

@Serializable
data class CarsDto(
    @SerialName("driving_side") val drivingSide: String? = null,
    val signs: List<String> = emptyList(),
)

@Serializable
data class PostalCodeDto(val format: String? = null, val regex: String? = null)

@Serializable
data class DateDto(@SerialName("start_of_week") val startOfWeek: String? = null)

@Serializable
data class EconomyDto(
    @SerialName("gini_coefficient") val giniCoefficient: Map<String, Double?> = emptyMap(),
)

@Serializable
data class ClassificationDto(
    val sovereign: Boolean? = null,
    @SerialName("un_member") val unMember: Boolean? = null,
    @SerialName("un_observer") val unObserver: Boolean? = null,
    val dependency: Boolean? = null,
    @SerialName("dependency_type") val dependencyType: String? = null,
    val disputed: Boolean? = null,
    @SerialName("iso_status") val isoStatus: String? = null,
)

@Serializable
data class MembershipsDto(
    val un: Boolean? = null,
    val eu: Boolean? = null,
    val eurozone: Boolean? = null,
    val schengen: Boolean? = null,
    val nato: Boolean? = null,
    val commonwealth: Boolean? = null,
    val oecd: Boolean? = null,
    val g7: Boolean? = null,
    val g20: Boolean? = null,
    val brics: Boolean? = null,
    val opec: Boolean? = null,
    @SerialName("african_union") val africanUnion: Boolean? = null,
    val asean: Boolean? = null,
    @SerialName("arab_league") val arabLeague: Boolean? = null,
)

@Serializable
data class DescriptionsDto(val short: String? = null, val long: String? = null)

@Serializable
data class LinksDto(
    val wikipedia: String? = null,
    val official: String? = null,
    @SerialName("google_maps") val googleMaps: String? = null,
    @SerialName("open_street_maps") val openStreetMaps: String? = null,
)

@Serializable
data class UnitsDto(@SerialName("measurement_system") val measurementSystem: String? = null)

@Serializable
data class RecordMetaDto(
    @SerialName("lastUpdatedTimestamp") val lastUpdatedTimestamp: Long? = null,
)
