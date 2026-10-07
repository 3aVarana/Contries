package com.example.contris.ui.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contris.domain.model.SyncStatus
import com.example.contris.domain.model.ThemeMode
import com.example.contris.domain.model.UnitSystem
import com.example.contris.ui.common.Formatters
import java.time.Instant

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsUiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text.asString(context))
                is SettingsUiEffect.OpenUrl -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri())) }
            }
        }
    }
    SettingsScreen(state = state, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var confirm by remember { mutableStateOf<SettingsUiEvent?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            GroupTitle("Display")
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = state.themeMode == mode,
                            onClick = { onEvent(SettingsUiEvent.SetThemeMode(mode)) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            modifier = Modifier.testTag("theme_${mode.name}"),
                        ) { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Dynamic colour") },
                    supportingContent = { Text("Use colours from your wallpaper") },
                    trailingContent = {
                        Switch(checked = state.dynamicColor, onCheckedChange = { onEvent(SettingsUiEvent.SetDynamicColor(it)) })
                    },
                )
            }

            HorizontalDivider()
            GroupTitle("Units")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                UnitSystem.entries.forEachIndexed { i, unit ->
                    SegmentedButton(
                        selected = state.unitSystem == unit,
                        onClick = { onEvent(SettingsUiEvent.SetUnitSystem(unit)) },
                        shape = SegmentedButtonDefaults.itemShape(i, UnitSystem.entries.size),
                        modifier = Modifier.testTag("unit_${unit.name}"),
                    ) { Text(if (unit == UnitSystem.METRIC) "Metric (km²)" else "Imperial (mi²)") }
                }
            }

            HorizontalDivider()
            GroupTitle("Data")
            ListItem(
                headlineContent = { Text("Last updated ${Formatters.formatRelativeTime(state.lastSync, Instant.now())}") },
                supportingContent = {
                    Text(
                        when (val s = state.sync) {
                            is SyncStatus.Syncing -> "Downloading page ${s.page} of ${s.totalPages}…"
                            is SyncStatus.Failed -> "Last refresh failed · ${state.countryCount} countries cached"
                            is SyncStatus.Idle -> "${state.countryCount} countries · refreshed automatically every 3 days"
                        },
                    )
                },
                trailingContent = {
                    Button(onClick = { onEvent(SettingsUiEvent.RefreshNow) }, enabled = !state.isRefreshing, modifier = Modifier.testTag("refresh_now")) {
                        Text(if (state.isRefreshing) "Refreshing…" else "Refresh now")
                    }
                },
            )

            HorizontalDivider()
            GroupTitle("Reset")
            ListItem(
                headlineContent = { Text("Clear favorites") },
                trailingContent = { TextButton(onClick = { confirm = SettingsUiEvent.ClearFavorites }) { Text("Clear") } },
            )
            ListItem(
                headlineContent = { Text("Clear quiz history") },
                trailingContent = { TextButton(onClick = { confirm = SettingsUiEvent.ClearQuizHistory }) { Text("Clear") } },
            )

            HorizontalDivider()
            GroupTitle("About")
            ListItem(headlineContent = { Text("Version") }, supportingContent = { Text(state.appVersion) })
            ListItem(
                headlineContent = { Text("Data by REST Countries") },
                supportingContent = { Text("restcountries.com") },
                modifier = Modifier.padding(0.dp),
                trailingContent = {
                    TextButton(onClick = { onEvent(SettingsUiEvent.OpenLink("https://restcountries.com")) }) { Text("Open") }
                },
            )
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Flags and country data are provided by the REST Countries API and cached on your device for up to three days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    confirm?.let { event ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (event is SettingsUiEvent.ClearFavorites) "Clear favorites?" else "Clear quiz history?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { TextButton(onClick = { onEvent(event); confirm = null }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
