package com.example.contris.ui.countries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.SyncStatus
import com.example.contris.ui.common.Formatters
import com.example.contris.ui.common.components.CountryListItem
import com.example.contris.ui.common.components.EmptyState
import com.example.contris.ui.common.components.SearchField
import com.example.contris.ui.common.toUiText
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesScreen(
    state: CountriesUiState,
    onEvent: (CountriesUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Countries") },
                actions = {
                    SortMenu(current = state.sort, onSelect = { onEvent(CountriesUiEvent.SortChanged(it)) })
                    IconButton(onClick = { onEvent(CountriesUiEvent.OpenFilters) }, modifier = Modifier.testTag("filter_button")) {
                        BadgedBox(badge = {
                            if (state.filters.activeCount > 0) Badge { Text(state.filters.activeCount.toString()) }
                        }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = state.query,
                onQueryChange = { onEvent(CountriesUiEvent.QueryChanged(it)) },
                onClear = { onEvent(CountriesUiEvent.ClearQuery) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = "Search countries, capitals, codes",
            )

            SyncBanner(state = state, onRetry = { onEvent(CountriesUiEvent.RetrySync) })

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(CountriesUiEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isFirstSync -> EmptyState(
                        icon = Icons.Outlined.Public,
                        title = "Downloading countries…",
                        body = "This happens once. The data is then kept on your device for three days.",
                    )
                    state.hasNoData && state.sync is SyncStatus.Failed -> EmptyState(
                        icon = Icons.Outlined.CloudOff,
                        title = "Couldn't download countries",
                        body = (state.sync as SyncStatus.Failed).error.toUiText().asString(),
                        actionLabel = "Retry",
                        onAction = { onEvent(CountriesUiEvent.RetrySync) },
                    )
                    state.countries.isEmpty() && !state.isLoadingList -> EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No countries match",
                        body = if (state.filters.activeCount > 0) "Try a different search or clear the filters." else "Try a different search.",
                        actionLabel = if (state.filters.activeCount > 0) "Clear filters" else null,
                        onAction = { onEvent(CountriesUiEvent.ClearFilters) },
                    )
                    else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("countries_list")) {
                        items(state.countries, key = { it.uuid }) { country ->
                            CountryListItem(
                                country = country,
                                onClick = { onEvent(CountriesUiEvent.CountryClicked(country.uuid)) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (state.isFilterSheetOpen) {
        FilterSheet(
            filters = state.filters,
            regions = state.availableRegions,
            subregions = state.availableSubregions,
            onApply = { onEvent(CountriesUiEvent.FiltersChanged(it)); onEvent(CountriesUiEvent.CloseFilters) },
            onDismiss = { onEvent(CountriesUiEvent.CloseFilters) },
        )
    }
}

@Composable
private fun SyncBanner(state: CountriesUiState, onRetry: () -> Unit) {
    when (val sync = state.sync) {
        is SyncStatus.Syncing -> if (state.totalCount > 0) {
            Column(Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { if (sync.totalPages > 0) sync.page.toFloat() / sync.totalPages else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        is SyncStatus.Failed -> if (state.totalCount > 0) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().testTag("sync_banner")) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Showing data from ${Formatters.formatRelativeTime(sync.lastSync, Instant.now())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }
        is SyncStatus.Idle -> Unit
    }
}

@Composable
private fun SortMenu(current: CountrySort, onSelect: (CountrySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("sort_button")) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CountrySort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label()) },
                    leadingIcon = { RadioButton(selected = sort == current, onClick = null) },
                    onClick = { onSelect(sort); expanded = false },
                )
            }
        }
    }
}

fun CountrySort.label(): String = when (this) {
    CountrySort.NAME_ASC -> "Name (A–Z)"
    CountrySort.NAME_DESC -> "Name (Z–A)"
    CountrySort.POPULATION_DESC -> "Population (high → low)"
    CountrySort.POPULATION_ASC -> "Population (low → high)"
    CountrySort.AREA_DESC -> "Area (large → small)"
    CountrySort.AREA_ASC -> "Area (small → large)"
}
