package com.example.contris.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contris.domain.model.ComparisonRow
import com.example.contris.domain.model.Country
import com.example.contris.domain.model.Side
import com.example.contris.ui.common.Formatters
import com.example.contris.ui.common.components.CountryListItem
import com.example.contris.ui.common.components.FlagImage
import com.example.contris.ui.common.components.SearchField
import com.example.contris.ui.navigation.Navigator

@Composable
fun CompareRoute(navigator: Navigator, viewModel: CompareViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CompareUiEffect.NavigateToDetail -> navigator.toDetail(effect.uuid)
            }
        }
    }
    CompareScreen(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(state: CompareUiState, onEvent: (CompareUiEvent) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Compare") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SlotCard(state.first, Slot.FIRST, onEvent, Modifier.weight(1f))
                IconButton(onClick = { onEvent(CompareUiEvent.Swap) }, enabled = state.first != null || state.second != null) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap")
                }
                SlotCard(state.second, Slot.SECOND, onEvent, Modifier.weight(1f))
            }

            if (state.first == null || state.second == null) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Pick two countries to compare population, area, languages, memberships and more.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().testTag("compare_rows")) {
                    items(state.rows, key = { it.label }) { row ->
                        ComparisonRowItem(row)
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    state.pickerFor?.let { slot ->
        CountryPickerSheet(
            query = state.pickerQuery,
            results = state.pickerResults,
            onQueryChange = { onEvent(CompareUiEvent.PickerQueryChanged(it)) },
            onPick = { onEvent(CompareUiEvent.CountryPicked(slot, it)) },
            onDismiss = { onEvent(CompareUiEvent.ClosePicker) },
        )
    }
}

@Composable
private fun SlotCard(country: Country?, slot: Slot, onEvent: (CompareUiEvent) -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.height(120.dp).testTag("slot_${slot.name.lowercase()}")) {
        Box(Modifier.fillMaxSize().clickable { onEvent(CompareUiEvent.OpenPicker(slot)) }) {
            if (country == null) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Pick a country", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    FlagImage(pngUrl = country.flag.pngUrl, emoji = country.flag.emoji, contentDescription = null, width = 64.dp, height = 44.dp)
                    Text(
                        country.names.common,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp).clickable { onEvent(CompareUiEvent.OpenDetail(country.uuid)) },
                    )
                }
                IconButton(onClick = { onEvent(CompareUiEvent.Clear(slot)) }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ComparisonRowItem(row: ComparisonRow) {
    Row(Modifier.fillMaxWidth().height(androidx.compose.ui.unit.Dp.Unspecified).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            row.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f),
        )
        ValueCell(row.left ?: Formatters.DASH, highlighted = row.winner == Side.LEFT, Modifier.weight(0.35f))
        ValueCell(row.right ?: Formatters.DASH, highlighted = row.winner == Side.RIGHT, Modifier.weight(0.35f))
    }
}

@Composable
private fun ValueCell(text: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp)
            .background(
                if (highlighted) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                MaterialTheme.shapes.small,
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    query: String,
    results: List<com.example.contris.domain.model.CountrySummary>,
    onQueryChange: (String) -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = Modifier.testTag("country_picker")) {
        Column(Modifier.fillMaxSize()) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onClear = { onQueryChange("") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = "Search countries",
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.uuid }) { c ->
                    CountryListItem(country = c, onClick = { onPick(c.uuid) })
                }
            }
        }
    }
}
