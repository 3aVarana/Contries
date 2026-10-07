package com.example.contris.ui.detail

import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contris.ui.navigation.Navigator

@Composable
fun CountryDetailRoute(
    navigator: Navigator,
    viewModel: CountryDetailViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CountryDetailUiEffect.NavigateToDetail -> navigator.toDetail(effect.uuid)
                is CountryDetailUiEffect.NavigateToCompare -> navigator.toCompare(effect.firstUuid)
                CountryDetailUiEffect.NavigateBack -> navigator.back()
                is CountryDetailUiEffect.OpenUrl -> runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                }.onFailure { snackbarHostState.showSnackbar("No app can open this link") }
                is CountryDetailUiEffect.Share -> runCatching {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }
                is CountryDetailUiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text.asString(context))
            }
        }
    }

    CountryDetailScreen(state = state, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}
