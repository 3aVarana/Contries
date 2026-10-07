package com.example.contris.ui.detail.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.UnitSystem
import com.example.contris.ui.common.Formatters
import com.example.contris.ui.common.components.ChipRow
import com.example.contris.ui.common.components.FlagImage
import com.example.contris.ui.common.components.SectionCard
import com.example.contris.ui.common.components.StatRow
import com.example.contris.ui.detail.CountryDetailUiEvent
import com.example.contris.ui.detail.CountryDetailUiState
import com.example.contris.ui.theme.parseHexColor

/** Adds every detail card to the LazyColumn. Cards with no data are omitted entirely. */
fun LazyListScope.detailSections(state: CountryDetailUiState.Content, onEvent: (CountryDetailUiEvent) -> Unit) {
    val c = state.country
    val unit = state.unitSystem

    item("overview") { OverviewSection(c, unit) }
    if (c.flag.description != null || c.flag.emoji != null || hasColors(c)) item("flag") { FlagSection(c) }
    item("geography") { GeographySection(state, onEvent) }
    if (c.languages.isNotEmpty() || c.startOfWeek != null || c.drivingSide != null || c.measurementSystem != null) {
        item("people") { PeopleSection(c) }
    }
    if (c.currencies.isNotEmpty() || c.gini.isNotEmpty()) item("economy") { EconomySection(c) }
    if (c.memberships.members.isNotEmpty()) item("memberships") { MembershipsSection(c) }
    item("codes") { CodesSection(c) }
    if (c.descriptions?.long != null) item("about") { AboutSection(c) }
    item("footer") {
        Text(
            text = "Data updated ${Formatters.formatDate(c.lastUpdated)} · Source: REST Countries",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun hasColors(c: Country) = with(c.flag.colors) { dominant != null || prominent != null || vibrant != null || muted != null }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewSection(c: Country, unit: UnitSystem) {
    SectionCard("Overview") {
        c.descriptions?.short?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (c.capitals.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                c.capitals.forEach { cap ->
                    val roles = buildList { if (cap.isPrimary) add("capital"); addAll(cap.roles) }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(if (roles.isEmpty()) cap.name else "${cap.name} · ${roles.joinToString()}") },
                    )
                }
            }
        }
        StatRow("Region", listOfNotNull(c.region, c.subregion).joinToString(" · ").ifBlank { null })
        StatRow("Continents", c.continents.joinToString().ifBlank { null })
        StatRow("Population", Formatters.formatPopulationFull(c.population).takeIf { c.population != null })
        StatRow("Area", c.area?.kilometers?.let { Formatters.formatArea(it, unit) })
        StatRow("Density", c.densityPerKm2?.let { Formatters.formatDensity(it, unit) })
        StatRow("Government", c.governmentType)
        val badges = buildList {
            if (c.classification.sovereign) add("Sovereign")
            if (c.classification.unMember) add("UN member")
            if (c.classification.unObserver) add("UN observer")
            if (c.classification.dependency) add(c.classification.dependencyType?.let { "Dependency · $it" } ?: "Dependency")
            if (c.classification.disputed) add("Disputed")
        }
        ChipRow(badges)
    }
}

@Composable
private fun FlagSection(c: Country) {
    SectionCard("Flag") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FlagImage(
                pngUrl = c.flag.pngUrl, svgUrl = c.flag.svgUrl, preferSvg = true,
                emoji = c.flag.emoji, contentDescription = c.flag.description,
                width = 120.dp, height = 80.dp, cornerRadius = 8.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.flag.emoji?.let { Text(it, style = MaterialTheme.typography.headlineMedium) }
                val swatches = listOfNotNull(
                    c.flag.colors.dominant, c.flag.colors.prominent, c.flag.colors.vibrant, c.flag.colors.muted,
                ).distinct()
                if (swatches.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        swatches.forEach { hex ->
                            parseHexColor(hex)?.let { color ->
                                androidx.compose.foundation.layout.Box(
                                    Modifier.size(22.dp).clip(CircleShape).background(color)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
        c.flag.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun GeographySection(state: CountryDetailUiState.Content, onEvent: (CountryDetailUiEvent) -> Unit) {
    val c = state.country
    SectionCard("Geography") {
        StatRow("Coordinates", c.coordinates?.let { Formatters.formatCoordinates(it.lat, it.lng) })
        StatRow("Landlocked", c.landlocked?.let { if (it) "Yes" else "No" })
        StatRow("Time zones", c.timezones.joinToString().ifBlank { null })
        if (state.borders.isNotEmpty()) {
            Text("Borders", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BorderChips(state, onEvent)
        } else if (c.borders.isEmpty() && c.landlocked == false) {
            StatRow("Borders", "None (island or coastal enclave)")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BorderChips(state: CountryDetailUiState.Content, onEvent: (CountryDetailUiEvent) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.borders.forEach { b ->
            AssistChip(
                onClick = { onEvent(CountryDetailUiEvent.BorderClicked(b.uuid)) },
                label = { Text(b.name) },
                leadingIcon = {
                    FlagImage(pngUrl = b.flagPng, emoji = b.flagEmoji, contentDescription = null, width = 24.dp, height = 16.dp, cornerRadius = 2.dp)
                },
            )
        }
    }
}

@Composable
private fun PeopleSection(c: Country) {
    SectionCard("People & culture") {
        if (c.languages.isNotEmpty()) {
            StatRow(
                "Languages",
                c.languages.joinToString("\n") { l ->
                    val n = l.name ?: l.nativeName ?: return@joinToString ""
                    if (l.nativeName != null && l.nativeName != l.name) "$n (${l.nativeName})" else n
                }.ifBlank { null },
            )
        }
        StatRow("Start of week", c.startOfWeek?.replaceFirstChar(Char::uppercase))
        StatRow("Driving side", c.drivingSide?.replaceFirstChar(Char::uppercase))
        StatRow("Car signs", c.carSigns.joinToString().ifBlank { null })
        StatRow("Measurement system", c.measurementSystem?.replaceFirstChar(Char::uppercase))
    }
}

@Composable
private fun EconomySection(c: Country) {
    SectionCard("Economy") {
        if (c.currencies.isNotEmpty()) {
            StatRow(
                "Currencies",
                c.currencies.joinToString("\n") { cur ->
                    listOfNotNull(cur.code, cur.name, cur.symbol?.let { "($it)" }).joinToString(" ")
                },
            )
        }
        c.latestGini?.let { (year, value) ->
            StatRow("Gini index ($year)", Formatters.formatDecimal(value))
        }
        if (c.gini.size >= 2) {
            Spacer(Modifier.height(4.dp))
            GiniHistory(c.gini)
        }
    }
}

@Composable
private fun GiniHistory(gini: Map<Int, Double>) {
    val entries = gini.entries.sortedBy { it.key }
    val max = entries.maxOf { it.value }.coerceAtLeast(1.0)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.forEach { (year, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(year.toString(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxWidth((value / max).toFloat().coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(Formatters.formatDecimal(value), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MembershipsSection(c: Country) {
    SectionCard("Memberships") {
        ChipRow(c.memberships.members.map { it.label })
    }
}

@Composable
private fun CodesSection(c: Country) {
    SectionCard("Codes & misc") {
        StatRow("ISO alpha-2", c.codes.alpha2)
        StatRow("ISO alpha-3", c.codes.alpha3)
        StatRow("ISO numeric", c.codes.ccn3)
        StatRow("IOC", c.codes.cioc)
        StatRow("FIFA", c.codes.fifa)
        StatRow("Calling codes", c.callingCodes.joinToString().ifBlank { null })
        StatRow("Top-level domains", c.tlds.joinToString().ifBlank { null })
        StatRow("Postal code format", c.postalFormat)
        StatRow("ISO status", c.classification.isoStatus)
        if (c.names.alternates.isNotEmpty()) StatRow("Also known as", c.names.alternates.joinToString())
        if (c.names.native.isNotEmpty()) {
            StatRow(
                "Native names",
                c.names.native.entries.joinToString("\n") { (lang, n) -> "${n.common ?: n.official ?: ""} ($lang)" }.ifBlank { null },
            )
        }
    }
}

@Composable
private fun AboutSection(c: Country) {
    SectionCard("About") {
        Text(c.descriptions?.long.orEmpty(), style = MaterialTheme.typography.bodyMedium)
    }
}
