package com.example.contris.data.mapper

import com.example.contris.data.local.JsonColumns
import com.example.contris.data.local.StoredCapital
import com.example.contris.data.local.StoredCurrency
import com.example.contris.data.local.StoredLanguage
import com.example.contris.data.local.StoredNativeName
import com.example.contris.data.local.entity.CountryEntity
import com.example.contris.data.remote.dto.CapitalDto
import com.example.contris.data.remote.dto.CountryDto
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Returns null when the record lacks the minimum required fields (`uuid`, `names.common`). */
fun CountryDto.toEntityOrNull(): CountryEntity? {
    val id = uuid?.takeIf { it.isNotBlank() } ?: return null
    val common = names?.common?.takeIf { it.isNotBlank() } ?: return null
    val official = names.official?.takeIf { it.isNotBlank() } ?: common

    val native = names.native.mapValues { StoredNativeName(it.value.common, it.value.official) }
    val storedCapitals = capitals.mapNotNull { it.toStored() }
    val primaryCapital = storedCapitals.firstOrNull { it.primary } ?: storedCapitals.firstOrNull()

    val gini = economy?.giniCoefficient.orEmpty()
        .mapNotNull { (year, value) -> year.toIntOrNull()?.let { y -> value?.let { y to it } } }
        .toMap()
    val latestGini = gini.maxByOrNull { it.key }

    val searchText = buildList {
        add(common); add(official)
        addAll(names.alternates)
        native.values.forEach { addAll(listOfNotNull(it.common, it.official)) }
        addAll(listOfNotNull(codes?.alpha2, codes?.alpha3))
        storedCapitals.forEach { add(it.name) }
    }.filter { it.isNotBlank() }.joinToString("|").lowercase()

    val swatches = flag?.colors?.swatches.orEmpty()
    val m = memberships
    val c = classification

    return CountryEntity(
        uuid = id,
        nameCommon = common,
        nameOfficial = official,
        nameAlternates = JsonColumns.encode(names.alternates),
        nameNative = JsonColumns.encode(native),
        alpha2 = codes?.alpha2.blankToNull(),
        alpha3 = codes?.alpha3.blankToNull(),
        ccn3 = codes?.ccn3.blankToNull(),
        cioc = codes?.cioc.blankToNull(),
        fifa = codes?.fifa.blankToNull(),
        fips = codes?.fips.blankToNull(),
        gec = codes?.gec.blankToNull(),
        searchText = searchText,
        capitalName = primaryCapital?.name,
        capitalLat = primaryCapital?.lat,
        capitalLng = primaryCapital?.lng,
        capitals = JsonColumns.encode(storedCapitals),
        flagEmoji = flag?.emoji.blankToNull(),
        flagPng = flag?.urlPng.blankToNull(),
        flagSvg = flag?.urlSvg.blankToNull(),
        flagDescription = flag?.description.blankToNull(),
        flagDominant = flag?.colors?.dominant.blankToNull(),
        flagProminent = flag?.colors?.prominent.blankToNull(),
        flagVibrant = swatches.hex("vibrant"),
        flagMuted = swatches.hex("muted"),
        region = region.blankToNull(),
        subregion = subregion.blankToNull(),
        continents = JsonColumns.encode(continents.filter { it.isNotBlank() }),
        landlocked = landlocked,
        borders = JsonColumns.encode(borders.filter { it.isNotBlank() }),
        areaKm2 = area?.kilometers,
        areaMi2 = area?.miles,
        lat = coordinates?.lat,
        lng = coordinates?.lng,
        population = population,
        currencies = JsonColumns.encode(currencies.map { StoredCurrency(it.code, it.name, it.symbol) }),
        languages = JsonColumns.encode(languages.map { StoredLanguage(it.name, it.nativeName, it.bcp47, it.iso6393) }),
        callingCodes = JsonColumns.encode(callingCodes.filter { it.isNotBlank() }),
        tlds = JsonColumns.encode(tlds.filter { it.isNotBlank() }),
        timezones = JsonColumns.encode(timezones.filter { it.isNotBlank() }),
        carSigns = JsonColumns.encode(cars?.signs.orEmpty().filter { it.isNotBlank() }),
        drivingSide = cars?.drivingSide.blankToNull(),
        postalFormat = postalCode?.format.blankToNull(),
        startOfWeek = date?.startOfWeek.blankToNull(),
        governmentType = governmentType.blankToNull(),
        measurementSystem = units?.measurementSystem.blankToNull(),
        giniLatestYear = latestGini?.key,
        giniLatest = latestGini?.value,
        giniJson = JsonColumns.encode(gini.mapKeys { it.key.toString() }),
        sovereign = c?.sovereign == true,
        unMember = c?.unMember == true,
        unObserver = c?.unObserver == true,
        dependency = c?.dependency == true,
        disputed = c?.disputed == true,
        dependencyType = c?.dependencyType.blankToNull(),
        isoStatus = c?.isoStatus.blankToNull(),
        mUn = m?.un == true,
        mEu = m?.eu == true,
        mEurozone = m?.eurozone == true,
        mSchengen = m?.schengen == true,
        mNato = m?.nato == true,
        mCommonwealth = m?.commonwealth == true,
        mOecd = m?.oecd == true,
        mG7 = m?.g7 == true,
        mG20 = m?.g20 == true,
        mBrics = m?.brics == true,
        mOpec = m?.opec == true,
        mAfricanUnion = m?.africanUnion == true,
        mAsean = m?.asean == true,
        mArabLeague = m?.arabLeague == true,
        descriptionShort = descriptions?.short.blankToNull(),
        descriptionLong = descriptions?.long.blankToNull(),
        linkWikipedia = links?.wikipedia.blankToNull(),
        linkOfficial = links?.official.blankToNull(),
        linkGoogleMaps = links?.googleMaps.blankToNull(),
        linkOsm = links?.openStreetMaps.blankToNull(),
        lastUpdatedEpoch = meta?.lastUpdatedTimestamp,
    )
}

private fun CapitalDto.toStored(): StoredCapital? {
    val n = name?.takeIf { it.isNotBlank() } ?: return null
    val primary = attributes["primary"].asBooleanOrNull() == true
    val roles = attributes.filter { (k, v) -> k != "primary" && v.asBooleanOrNull() == true }.keys.sorted()
    return StoredCapital(n, coordinates?.lat, coordinates?.lng, primary, roles)
}

private fun JsonElement?.asBooleanOrNull(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

/** Swatch may be a hex string, or an object such as `{ "hex": "#abcdef" }` / `{ "color": ... }`. */
private fun Map<String, JsonElement>.hex(key: String): String? = when (val v = this[key]) {
    is JsonPrimitive -> v.contentOrNull.blankToNull()
    is JsonObject -> (v["hex"] ?: v["color"] ?: v["value"])?.let { (it as? JsonPrimitive)?.contentOrNull }.blankToNull()
    else -> null
}

internal fun String?.blankToNull(): String? = this?.takeIf { it.isNotBlank() }
