package com.example.contris.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contris.domain.model.QuizMode
import com.example.contris.ui.common.Formatters
import com.example.contris.ui.navigation.Navigator
import java.time.Instant

@Composable
fun QuizHomeRoute(navigator: Navigator, viewModel: QuizHomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QuizHomeUiEffect.NavigateToPlay -> navigator.toQuizPlay(effect.mode)
            }
        }
    }
    QuizHomeScreen(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHomeScreen(state: QuizHomeUiState, onEvent: (QuizHomeUiEvent) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Quiz") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(QuizMode.entries, key = { it.name }) { mode ->
                val best = state.bestByMode[mode]
                Card(
                    onClick = { onEvent(QuizHomeUiEvent.StartQuiz(mode)) },
                    enabled = state.canPlay,
                    modifier = Modifier.fillMaxWidth().testTag("quiz_mode_${mode.name}"),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(mode.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(mode.title(), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (best == null) "No games yet" else "Best: ${best.score}/${best.total} · streak ${best.bestStreak}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (!state.canPlay) {
                item {
                    Text(
                        "The quiz needs the countries data. It will unlock once the download finishes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.recent.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent games", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(QuizHomeUiEvent.ClearHistory) }) { Text("Clear") }
                    }
                }
                items(state.recent, key = { it.id }) { r ->
                    ListItem(
                        headlineContent = { Text("${r.mode.shortTitle()} · ${r.score}/${r.total}") },
                        supportingContent = { Text("Best streak ${r.bestStreak} · ${Formatters.formatRelativeTime(r.playedAt, Instant.now())}") },
                    )
                }
            }
        }
    }
}

private fun QuizMode.icon() = when (this) {
    QuizMode.FLAG_TO_COUNTRY -> Icons.Default.Flag
    QuizMode.COUNTRY_TO_CAPITAL -> Icons.Default.LocationCity
    QuizMode.CAPITAL_TO_COUNTRY -> Icons.Default.Public
}
