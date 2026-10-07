package com.example.contris.ui.countries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.Membership

private val CONTINENTS = listOf("Africa", "Antarctica", "Asia", "Europe", "North America", "Oceania", "South America")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filters: CountryFilters,
    regions: List<String>,
    subregions: List<String>,
    onApply: (CountryFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by rememberSaveable(filters, stateSaver = FiltersSaver) { mutableStateOf(filters) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = Modifier.testTag("filter_sheet")) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { draft = CountryFilters() }) { Text("Clear all") }
            }

            if (regions.isNotEmpty()) {
                FilterGroup("Region") {
                    regions.forEach { r ->
                        FilterChip(
                            selected = r in draft.regions,
                            onClick = { draft = draft.copy(regions = draft.regions.toggle(r)) },
                            label = { Text(r) },
                        )
                    }
                }
            }

            if (subregions.isNotEmpty()) {
                FilterGroup("Subregion") {
                    subregions.forEach { s ->
                        FilterChip(
                            selected = s in draft.subregions,
                            onClick = { draft = draft.copy(subregions = draft.subregions.toggle(s)) },
                            label = { Text(s) },
                        )
                    }
                }
            }

            FilterGroup("Continent") {
                CONTINENTS.forEach { c ->
                    FilterChip(
                        selected = c in draft.continents,
                        onClick = { draft = draft.copy(continents = draft.continents.toggle(c)) },
                        label = { Text(c) },
                    )
                }
            }

            Text("Coastline", style = MaterialTheme.typography.titleSmall)
            TriStateRow(
                value = draft.landlocked,
                labels = Triple("Any", "Landlocked", "Coastal"),
                onChange = { draft = draft.copy(landlocked = it) },
            )

            Text("UN membership", style = MaterialTheme.typography.titleSmall)
            TriStateRow(
                value = draft.unMember,
                labels = Triple("Any", "Member", "Non-member"),
                onChange = { draft = draft.copy(unMember = it) },
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Sovereign states only", modifier = Modifier.weight(1f))
                Switch(checked = draft.sovereignOnly, onCheckedChange = { draft = draft.copy(sovereignOnly = it) })
            }

            FilterGroup("Organisations") {
                Membership.entries.forEach { m ->
                    FilterChip(
                        selected = m in draft.memberships,
                        onClick = { draft = draft.copy(memberships = draft.memberships.toggle(m)) },
                        label = { Text(m.label) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { onApply(draft) }, modifier = Modifier.fillMaxWidth().testTag("apply_filters")) {
                Text("Apply")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriStateRow(value: Boolean?, labels: Triple<String, String, String>, onChange: (Boolean?) -> Unit) {
    val options = listOf(null to labels.first, true to labels.second, false to labels.third)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (v, label) ->
            SegmentedButton(
                selected = value == v,
                onClick = { onChange(v) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) { Text(label) }
        }
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

private val FiltersSaver = androidx.compose.runtime.saveable.Saver<CountryFilters, String>(
    save = { CountriesViewModel.encodeFilters(it) },
    restore = { CountriesViewModel.decodeFilters(it) },
)
