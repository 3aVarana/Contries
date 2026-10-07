package com.example.contris.ui.countries

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contris.ui.navigation.Navigator

@Composable
fun CountriesRoute(
    navigator: Navigator,
    viewModel: CountriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CountriesUiEffect.NavigateToDetail -> navigator.toDetail(effect.uuid)
                is CountriesUiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text.asString(context))
            }
        }
    }

    CountriesScreen(state = state, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}
