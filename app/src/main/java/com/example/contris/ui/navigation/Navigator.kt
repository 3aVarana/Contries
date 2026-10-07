package com.example.contris.ui.navigation

import androidx.compose.runtime.Stable

/** The small navigation surface that feature `Route` composables need. */
@Stable
class Navigator(private val backStack: TopLevelBackStack) {
    fun toDetail(uuid: String) = backStack.add(CountryDetailKey(uuid))
    fun toCompare(firstUuid: String?) = backStack.selectTab(TopLevelTab.COMPARE, CompareKey(firstUuid))
    fun toCountriesTab() = backStack.selectTab(TopLevelTab.COUNTRIES)
    fun toQuizPlay(mode: com.example.contris.domain.model.QuizMode) = backStack.add(QuizPlayKey(mode))
    fun back() { backStack.removeLast() }
    fun popToRoot() = backStack.popToRoot()
}
