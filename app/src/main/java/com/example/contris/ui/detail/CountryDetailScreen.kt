package com.example.contris.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.contris.ui.common.components.EmptyState
import com.example.contris.ui.common.components.FlagImage
import com.example.contris.ui.common.components.LoadingState
import com.example.contris.ui.detail.sections.detailSections
import com.example.contris.ui.theme.flagTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDetailScreen(
    state: CountryDetailUiState,
    onEvent: (CountryDetailUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val content = state as? CountryDetailUiState.Content
    val surface = MaterialTheme.colorScheme.surface
    val tint = content?.let { flagTint(it.flagTintHex, surface) } ?: surface

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = tint, scrolledContainerColor = tint),
                navigationIcon = {
                    IconButton(onClick = { onEvent(CountryDetailUiEvent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    if (content != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FlagImage(
                                pngUrl = content.country.flag.pngUrl,
                                svgUrl = content.country.flag.svgUrl,
                                preferSvg = true,
                                emoji = content.country.flag.emoji,
                                contentDescription = content.country.flag.description,
                                width = 48.dp,
                                height = 34.dp,
                            )
                            Column {
                                Text(content.country.names.common, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    content.country.names.official,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (content != null) {
                        IconButton(onClick = { onEvent(CountryDetailUiEvent.ToggleFavorite) }, modifier = Modifier.testTag("favorite_toggle")) {
                            Icon(
                                if (content.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (content.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (content.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { onEvent(CountryDetailUiEvent.Compare) }) {
                            Icon(Icons.Default.CompareArrows, contentDescription = "Compare")
                        }
                        IconButton(onClick = { onEvent(CountryDetailUiEvent.ShareClicked) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        LinksMenu(content, onEvent)
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            CountryDetailUiState.Loading -> LoadingState(Modifier.padding(padding))
            CountryDetailUiState.NotFound -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = "Country not found",
                body = "It may have been removed in the latest data update.",
                modifier = Modifier.padding(padding),
            )
            is CountryDetailUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag("detail_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                detailSections(state, onEvent)
            }
        }
    }
}

@Composable
private fun LinksMenu(content: CountryDetailUiState.Content, onEvent: (CountryDetailUiEvent) -> Unit) {
    val links = listOfNotNull(
        content.country.links.wikipedia?.let { "Wikipedia" to it },
        content.country.links.official?.let { "Official site" to it },
        content.country.links.googleMaps?.let { "Google Maps" to it },
        content.country.links.openStreetMaps?.let { "OpenStreetMap" to it },
    )
    if (links.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            links.forEach { (label, url) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onEvent(CountryDetailUiEvent.OpenLink(url)) })
            }
        }
    }
}
