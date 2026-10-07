package com.example.contris.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.json.Json

/**
 * One back stack per top-level tab (the Navigation 3 "common navigation UI" recipe).
 *
 * - Selecting a tab switches to that tab's stack; its history is preserved.
 * - Re-selecting the current tab pops it to its root.
 * - Back pops the current tab's stack; on a non-Countries root it returns to Countries.
 */
@Stable
class TopLevelBackStack(
    initialTab: TopLevelTab,
    initialStacks: Map<TopLevelTab, List<ContrisKey>> = emptyMap(),
) {
    private val stacks: Map<TopLevelTab, SnapshotStateList<ContrisKey>> =
        TopLevelTab.entries.associateWith { tab ->
            (initialStacks[tab] ?: listOf(tab.key)).toMutableStateList()
        }

    var currentTab: TopLevelTab by mutableStateOf(initialTab)
        private set

    private val currentStack: SnapshotStateList<ContrisKey> get() = stacks.getValue(currentTab)

    /** What `NavDisplay` renders: the current tab's stack, with Countries' root underneath so back works. */
    val backStack: List<ContrisKey> by derivedStateOf {
        if (currentTab == TopLevelTab.COUNTRIES) currentStack.toList()
        else listOf(CountriesKey) + currentStack
    }

    val currentKey: ContrisKey get() = currentStack.last()

    fun selectTab(tab: TopLevelTab) {
        if (tab == currentTab) {
            popToRoot()
        } else {
            currentTab = tab
        }
    }

    /** Switch to [tab] and replace its root with [root] (used by Compare pre-fill). */
    fun selectTab(tab: TopLevelTab, root: ContrisKey) {
        val stack = stacks.getValue(tab)
        stack.clear()
        stack.add(root)
        currentTab = tab
    }

    fun add(key: ContrisKey) {
        if (currentStack.lastOrNull() != key) currentStack.add(key)
    }

    /** Pops one entry. Returns false when nothing could be popped (caller should finish the activity). */
    fun removeLast(): Boolean {
        return when {
            currentStack.size > 1 -> { currentStack.removeAt(currentStack.lastIndex); true }
            currentTab != TopLevelTab.COUNTRIES -> { currentTab = TopLevelTab.COUNTRIES; true }
            else -> false
        }
    }

    fun popToRoot() {
        while (currentStack.size > 1) currentStack.removeAt(currentStack.lastIndex)
    }

    fun snapshot(): Map<TopLevelTab, List<ContrisKey>> = stacks.mapValues { it.value.toList() }

    companion object {
        private val json = Json

        val Saver: Saver<TopLevelBackStack, List<String>> = Saver(
            save = { state ->
                buildList {
                    add(state.currentTab.name)
                    TopLevelTab.entries.forEach { tab ->
                        add(json.encodeToString(state.snapshot().getValue(tab)))
                    }
                }
            },
            restore = { saved ->
                val tab = runCatching { TopLevelTab.valueOf(saved[0]) }.getOrDefault(TopLevelTab.COUNTRIES)
                val stacks = TopLevelTab.entries.mapIndexed { i, t ->
                    t to runCatching { json.decodeFromString<List<ContrisKey>>(saved[i + 1]) }
                        .getOrDefault(listOf(t.key))
                        .ifEmpty { listOf(t.key) }
                }.toMap()
                TopLevelBackStack(tab, stacks)
            },
        )
    }
}

@Composable
fun rememberTopLevelBackStack(): TopLevelBackStack =
    rememberSaveable(saver = TopLevelBackStack.Saver) { TopLevelBackStack(TopLevelTab.COUNTRIES) }
