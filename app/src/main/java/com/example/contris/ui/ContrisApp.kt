package com.example.contris.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.contris.ui.compare.CompareRoute
import com.example.contris.ui.compare.CompareViewModel
import com.example.contris.ui.countries.CountriesRoute
import com.example.contris.ui.detail.CountryDetailRoute
import com.example.contris.ui.detail.CountryDetailViewModel
import com.example.contris.ui.favorites.FavoritesRoute
import com.example.contris.ui.navigation.CompareKey
import com.example.contris.ui.navigation.ContrisKey
import com.example.contris.ui.navigation.CountriesKey
import com.example.contris.ui.navigation.CountryDetailKey
import com.example.contris.ui.navigation.FavoritesKey
import com.example.contris.ui.navigation.Navigator
import com.example.contris.ui.navigation.QuizKey
import com.example.contris.ui.navigation.QuizPlayKey
import com.example.contris.ui.navigation.SettingsKey
import com.example.contris.ui.navigation.TopLevelTab
import com.example.contris.ui.navigation.rememberTopLevelBackStack
import com.example.contris.ui.quiz.QuizHomeRoute
import com.example.contris.ui.quiz.QuizPlayRoute
import com.example.contris.ui.quiz.QuizViewModel
import com.example.contris.ui.settings.SettingsRoute

@Composable
fun ContrisApp(appViewModel: AppViewModel = hiltViewModel()) {
    val backStack = rememberTopLevelBackStack()
    val navigator = remember(backStack) { Navigator(backStack) }

    // Initial (non-forced) sync: skipped when the local data is fresh.
    LaunchedEffect(Unit) { appViewModel.syncIfStale() }

    val isAtRoot = backStack.currentTab == TopLevelTab.COUNTRIES && backStack.backStack.size == 1
    BackHandler(enabled = !isAtRoot) { backStack.removeLast() }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelTab.entries.forEach { tab ->
                item(
                    icon = { Icon(tab.icon(), contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    selected = tab == backStack.currentTab,
                    onClick = { backStack.selectTab(tab) },
                )
            }
        },
    ) {
        NavDisplay(
            backStack = backStack.backStack,
            onBack = { backStack.removeLast() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider<ContrisKey> {
                entry<CountriesKey> { CountriesRoute(navigator = navigator) }
                entry<FavoritesKey> { FavoritesRoute(navigator = navigator) }
                entry<QuizKey> { QuizHomeRoute(navigator = navigator) }
                entry<CompareKey> { key ->
                    CompareRoute(
                        navigator = navigator,
                        viewModel = hiltViewModel<CompareViewModel, CompareViewModel.Factory>(
                            creationCallback = { it.create(key.firstUuid) },
                        ),
                    )
                }
                entry<SettingsKey> { SettingsRoute() }
                entry<CountryDetailKey> { key ->
                    CountryDetailRoute(
                        navigator = navigator,
                        viewModel = hiltViewModel<CountryDetailViewModel, CountryDetailViewModel.Factory>(
                            creationCallback = { it.create(key.uuid) },
                        ),
                    )
                }
                entry<QuizPlayKey> { key ->
                    QuizPlayRoute(
                        navigator = navigator,
                        viewModel = hiltViewModel<QuizViewModel, QuizViewModel.Factory>(
                            creationCallback = { it.create(key.mode) },
                        ),
                    )
                }
            },
            modifier = Modifier,
        )
    }
}

private fun TopLevelTab.icon(): ImageVector = when (this) {
    TopLevelTab.COUNTRIES -> Icons.Default.Public
    TopLevelTab.FAVORITES -> Icons.Default.Favorite
    TopLevelTab.QUIZ -> Icons.Default.Quiz
    TopLevelTab.COMPARE -> Icons.Default.CompareArrows
    TopLevelTab.SETTINGS -> Icons.Default.Settings
}
