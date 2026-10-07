package com.example.contris.domain.usecase

import com.example.contris.domain.model.Comparison
import com.example.contris.domain.model.ComparisonRow
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.Membership
import com.example.contris.domain.model.Side
import com.example.contris.domain.model.UnitSystem
import javax.inject.Inject

/**
 * Produces the rows shown on the Compare screen. Pure Kotlin so it is trivially testable;
 * number formatting is delegated to the caller through [Formatters].
 */
class BuildComparisonUseCase @Inject constructor() {

    interface Formatters {
        fun population(value: Long): String
        fun area(km2: Double, unitSystem: UnitSystem): String
        fun density(perKm2: Double, unitSystem: UnitSystem): String
        fun decimal(value: Double): String
    }

    operator fun invoke(a: Country, b: Country, unitSystem: UnitSystem, f: Formatters): Comparison {
        val rows = buildList {
            add(numeric("Population", a.population?.toDouble(), b.population?.toDouble()) { f.population(it.toLong()) })
            add(numeric("Area", a.area?.kilometers, b.area?.kilometers) { f.area(it, unitSystem) })
            add(numeric("Density", a.densityPerKm2, b.densityPerKm2) { f.density(it, unitSystem) })
            add(text("Capital", a.primaryCapital?.name, b.primaryCapital?.name))
            add(text("Region", listOfNotNull(a.region, a.subregion).joinToString(" · ").ifBlank { null },
                listOfNotNull(b.region, b.subregion).joinToString(" · ").ifBlank { null }))
            add(text("Languages", a.languages.mapNotNull { it.name }.joinList(), b.languages.mapNotNull { it.name }.joinList()))
            add(text("Currencies", a.currencies.mapNotNull { it.code ?: it.name }.joinList(), b.currencies.mapNotNull { it.code ?: it.name }.joinList()))
            add(text("Driving side", a.drivingSide?.replaceFirstChar(Char::uppercase), b.drivingSide?.replaceFirstChar(Char::uppercase)))
            add(text("Gini (latest)", a.latestGini?.let { "${f.decimal(it.second)} (${it.first})" }, b.latestGini?.let { "${f.decimal(it.second)} (${it.first})" }))
            add(text("Landlocked", a.landlocked?.yesNo(), b.landlocked?.yesNo()))
            add(text("UN member", a.classification.unMember.yesNo(), b.classification.unMember.yesNo()))
            add(text("Shared memberships", sharedMemberships(a, b), sharedMemberships(a, b)))
            add(text("Exclusive memberships", exclusiveMemberships(a, b), exclusiveMemberships(b, a)))
            add(numeric("Time zones", a.timezones.size.toDouble().takeIf { it > 0 }, b.timezones.size.toDouble().takeIf { it > 0 }) { it.toInt().toString() })
            add(text("Calling code", a.callingCodes.joinList(), b.callingCodes.joinList()))
        }
        return Comparison(rows)
    }

    private fun numeric(label: String, l: Double?, r: Double?, format: (Double) -> String): ComparisonRow {
        val winner = when {
            l != null && r != null && l > r -> Side.LEFT
            l != null && r != null && r > l -> Side.RIGHT
            else -> null
        }
        return ComparisonRow(label, l?.let(format), r?.let(format), winner)
    }

    private fun text(label: String, l: String?, r: String?) = ComparisonRow(label, l?.ifBlank { null }, r?.ifBlank { null })

    private fun sharedMemberships(a: Country, b: Country): String? =
        Membership.entries.filter { it in a.memberships && it in b.memberships }.joinToString { it.label }.ifBlank { null }

    private fun exclusiveMemberships(a: Country, b: Country): String? =
        Membership.entries.filter { it in a.memberships && it !in b.memberships }.joinToString { it.label }.ifBlank { null }

    private fun List<String>.joinList(): String? = filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }

    private fun Boolean.yesNo() = if (this) "Yes" else "No"
}
