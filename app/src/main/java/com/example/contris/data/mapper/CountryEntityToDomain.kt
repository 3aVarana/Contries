package com.example.contris.data.mapper

import com.example.contris.data.local.JsonColumns
import com.example.contris.data.local.StoredCapital
import com.example.contris.data.local.StoredCurrency
import com.example.contris.data.local.StoredLanguage
import com.example.contris.data.local.StoredNativeName
import com.example.contris.data.local.entity.CountryEntity
import com.example.contris.data.local.entity.CountrySummaryRow
import com.example.contris.domain.model.Area
import com.example.contris.domain.model.Capital
import com.example.contris.domain.model.Classification
import com.example.contris.domain.model.Codes
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.model.Currency
import com.example.contris.domain.model.Descriptions
import com.example.contris.domain.model.FlagColors
import com.example.contris.domain.model.FlagInfo
import com.example.contris.domain.model.Language
import com.example.contris.domain.model.LatLng
import com.example.contris.domain.model.Links
import com.example.contris.domain.model.Membership
import com.example.contris.domain.model.Memberships
import com.example.contris.domain.model.Names
import com.example.contris.domain.model.NativeName
import java.time.Instant

fun CountrySummaryRow.toDomain(): CountrySummary = CountrySummary(
    uuid = uuid,
    name = nameCommon,
    capital = capitalName,
    region = region,
    flagEmoji = flagEmoji,
    flagPng = flagPng,
    population = population,
    areaKm2 = areaKm2,
)

fun CountryEntity.toDomain(): Country {
    val native = JsonColumns.decodeOr<Map<String, StoredNativeName>>(nameNative, emptyMap())
    val capitals = JsonColumns.decodeOr<List<StoredCapital>>(capitals, emptyList())
    val currencies = JsonColumns.decodeOr<List<StoredCurrency>>(currencies, emptyList())
    val languages = JsonColumns.decodeOr<List<StoredLanguage>>(languages, emptyList())
    val gini = JsonColumns.decodeOr<Map<String, Double>>(giniJson, emptyMap())
        .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()

    val memberships = buildSet {
        if (mUn) add(Membership.UN)
        if (mEu) add(Membership.EU)
        if (mEurozone) add(Membership.EUROZONE)
        if (mSchengen) add(Membership.SCHENGEN)
        if (mNato) add(Membership.NATO)
        if (mCommonwealth) add(Membership.COMMONWEALTH)
        if (mOecd) add(Membership.OECD)
        if (mG7) add(Membership.G7)
        if (mG20) add(Membership.G20)
        if (mBrics) add(Membership.BRICS)
        if (mOpec) add(Membership.OPEC)
        if (mAfricanUnion) add(Membership.AFRICAN_UNION)
        if (mAsean) add(Membership.ASEAN)
        if (mArabLeague) add(Membership.ARAB_LEAGUE)
    }

    return Country(
        uuid = uuid,
        names = Names(
            common = nameCommon,
            official = nameOfficial,
            alternates = JsonColumns.decodeOr(nameAlternates, emptyList()),
            native = native.mapValues { NativeName(it.value.common, it.value.official) },
        ),
        codes = Codes(alpha2, alpha3, ccn3, cioc, fifa, fips, gec),
        capitals = capitals.map {
            Capital(
                name = it.name,
                coordinates = if (it.lat != null && it.lng != null) LatLng(it.lat, it.lng) else null,
                isPrimary = it.primary,
                roles = it.roles,
            )
        },
        flag = FlagInfo(
            emoji = flagEmoji,
            pngUrl = flagPng,
            svgUrl = flagSvg,
            description = flagDescription,
            colors = FlagColors(flagDominant, flagProminent, flagVibrant, flagMuted),
        ),
        region = region,
        subregion = subregion,
        continents = JsonColumns.decodeOr(continents, emptyList()),
        landlocked = landlocked,
        borders = JsonColumns.decodeOr(borders, emptyList()),
        area = if (areaKm2 != null || areaMi2 != null) Area(areaKm2, areaMi2) else null,
        coordinates = if (lat != null && lng != null) LatLng(lat, lng) else null,
        population = population,
        currencies = currencies.map { Currency(it.code, it.name, it.symbol) },
        languages = languages.map { Language(it.name, it.nativeName, it.bcp47, it.iso6393) },
        callingCodes = JsonColumns.decodeOr(callingCodes, emptyList()),
        tlds = JsonColumns.decodeOr(tlds, emptyList()),
        timezones = JsonColumns.decodeOr(timezones, emptyList()),
        drivingSide = drivingSide,
        carSigns = JsonColumns.decodeOr(carSigns, emptyList()),
        postalFormat = postalFormat,
        startOfWeek = startOfWeek,
        gini = gini,
        governmentType = governmentType,
        classification = Classification(
            sovereign = sovereign,
            unMember = unMember,
            unObserver = unObserver,
            dependency = dependency,
            dependencyType = dependencyType,
            disputed = disputed,
            isoStatus = isoStatus,
        ),
        memberships = Memberships(memberships),
        descriptions = if (descriptionShort != null || descriptionLong != null) Descriptions(descriptionShort, descriptionLong) else null,
        links = Links(linkWikipedia, linkOfficial, linkGoogleMaps, linkOsm),
        measurementSystem = measurementSystem,
        lastUpdated = lastUpdatedEpoch?.let { Instant.ofEpochSecond(it) },
    )
}
