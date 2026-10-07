package com.example.contris.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contris.domain.model.CountrySummary
import com.example.contris.ui.common.components.CountryListItem
import com.example.contris.ui.common.components.EmptyState
import com.example.contris.ui.common.components.LoadingState
import com.example.contris.ui.navigation.Navigator

@Composable
fun FavoritesRoute(navigator: Navigator, viewModel: FavoritesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FavoritesUiEffect.NavigateToDetail -> navigator.toDetail(effect.uuid)
                FavoritesUiEffect.NavigateToCountries -> navigator.toCountriesTab()
                is FavoritesUiEffect.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "${effect.countryName} removed from favorites",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onEvent(FavoritesUiEvent.UndoRemove)
                }
            }
        }
    }

    FavoritesScreen(state = state, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onEvent: (FavoritesUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Favorites") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.favorites.isEmpty() -> EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = "No favorites yet",
                body = "Tap the heart on any country to keep it here.",
                modifier = Modifier.padding(padding),
                actionLabel = "Browse countries",
                onAction = { onEvent(FavoritesUiEvent.BrowseCountries) },
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("favorites_list")) {
                items(state.favorites, key = { it.uuid }) { country ->
                    SwipeToRemoveItem(country = country, onRemove = { onEvent(FavoritesUiEvent.Remove(country.uuid)) }) {
                        CountryListItem(country = country, onClick = { onEvent(FavoritesUiEvent.CountryClicked(country.uuid)) })
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemoveItem(country: CountrySummary, onRemove: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onRemove(); true
            } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${country.name}", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        content = { content() },
    )
}
