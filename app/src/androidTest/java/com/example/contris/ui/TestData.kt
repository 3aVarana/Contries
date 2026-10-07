package com.example.contris.ui

import com.example.contris.domain.model.Area
import com.example.contris.domain.model.Capital
import com.example.contris.domain.model.Classification
import com.example.contris.domain.model.Codes
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.FlagInfo
import com.example.contris.domain.model.Links
import com.example.contris.domain.model.Memberships
import com.example.contris.domain.model.Names

fun testCountry(uuid: String, name: String): Country = Country(
    uuid = uuid,
    names = Names(common = name, official = "Federal Republic of $name"),
    codes = Codes(alpha2 = uuid.uppercase(), alpha3 = uuid.uppercase() + "U"),
    capitals = listOf(Capital("Capital of $name", isPrimary = true)),
    flag = FlagInfo(emoji = "🏳"),
    region = "Europe",
    subregion = "Western Europe",
    continents = listOf("Europe"),
    landlocked = false,
    borders = emptyList(),
    area = Area(1000.0, 386.0),
    coordinates = null,
    population = 1_000_000,
    currencies = emptyList(),
    languages = emptyList(),
    callingCodes = listOf("+49"),
    tlds = listOf(".de"),
    timezones = listOf("UTC+01:00"),
    drivingSide = "right",
    carSigns = emptyList(),
    postalFormat = null,
    startOfWeek = "monday",
    gini = emptyMap(),
    governmentType = "Republic",
    classification = Classification(sovereign = true, unMember = true),
    memberships = Memberships(),
    descriptions = null,
    links = Links(),
    measurementSystem = "metric",
    lastUpdated = null,
)
