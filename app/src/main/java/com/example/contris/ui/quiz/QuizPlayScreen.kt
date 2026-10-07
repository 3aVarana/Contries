package com.example.contris.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contris.domain.model.QuizPrompt
import com.example.contris.ui.common.components.EmptyState
import com.example.contris.ui.common.components.FlagImage
import com.example.contris.ui.common.components.LoadingState
import com.example.contris.ui.navigation.Navigator
import com.example.contris.ui.theme.ContrisThemeExtras

@Composable
fun QuizPlayRoute(navigator: Navigator, viewModel: QuizViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                QuizPlayUiEffect.NavigateBack -> navigator.back()
                is QuizPlayUiEffect.NavigateToDetail -> navigator.toDetail(effect.uuid)
            }
        }
    }
    QuizPlayScreen(state = state, title = viewModel.mode.shortTitle(), onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPlayScreen(state: QuizPlayUiState, title: String, onEvent: (QuizPlayUiEvent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(QuizPlayUiEvent.Quit) }) { Icon(Icons.Default.Close, contentDescription = "Quit quiz") }
                },
            )
        },
    ) { padding ->
        when (state) {
            QuizPlayUiState.Generating -> LoadingState(Modifier.padding(padding), "Preparing questions…")
            is QuizPlayUiState.Error -> EmptyState(
                icon = Icons.Default.Close,
                title = "Can't start the quiz",
                body = state.message,
                modifier = Modifier.padding(padding),
                actionLabel = "Back",
                onAction = { onEvent(QuizPlayUiEvent.Quit) },
            )
            is QuizPlayUiState.Question -> QuestionContent(state, onEvent, Modifier.padding(padding))
            is QuizPlayUiState.Finished -> ResultContent(state, onEvent, Modifier.padding(padding))
        }
    }
}

@Composable
private fun QuestionContent(state: QuizPlayUiState.Question, onEvent: (QuizPlayUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val extras = ContrisThemeExtras.colors
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${state.index + 1} / ${state.total}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("Score ${state.score} · streak ${state.streak}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { (state.index + 1).toFloat() / state.total }, modifier = Modifier.fillMaxWidth())

        Card(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                when (val p = state.question.prompt) {
                    is QuizPrompt.Flag -> FlagImage(
                        pngUrl = p.pngUrl, emoji = p.emoji, contentDescription = "Flag to identify",
                        width = 240.dp, height = 160.dp, cornerRadius = 8.dp,
                    )
                    is QuizPrompt.Text -> Text(
                        text = listOfNotNull(p.emoji, p.text).joinToString(" "),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        state.question.options.forEachIndexed { i, option ->
            val isCorrect = i == state.question.correctIndex
            val isSelected = i == state.selectedIndex
            val colors = when {
                state.revealed && isCorrect -> ButtonDefaults.buttonColors(containerColor = extras.successContainer, contentColor = extras.onSuccessContainer)
                state.revealed && isSelected -> ButtonDefaults.buttonColors(containerColor = extras.dangerContainer, contentColor = extras.onDangerContainer)
                else -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Button(
                onClick = { onEvent(QuizPlayUiEvent.OptionSelected(i)) },
                enabled = !state.revealed,
                colors = colors.copy(disabledContainerColor = colors.containerColor, disabledContentColor = colors.contentColor),
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("option_$i"),
            ) {
                if (state.revealed && (isCorrect || isSelected)) {
                    Icon(if (isCorrect) Icons.Default.Check else Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                }
                Text(option, textAlign = TextAlign.Center)
            }
        }

        if (state.revealed) {
            Button(onClick = { onEvent(QuizPlayUiEvent.Next) }, modifier = Modifier.fillMaxWidth().testTag("next_button")) {
                Text(if (state.index + 1 == state.total) "Finish" else "Next")
            }
        }
    }
}

@Composable
private fun ResultContent(state: QuizPlayUiState.Finished, onEvent: (QuizPlayUiEvent) -> Unit, modifier: Modifier = Modifier) {
    var showAnswers by rememberSaveable { mutableStateOf(false) }
    val r = state.result
    LazyColumn(modifier.fillMaxSize().testTag("quiz_result"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { if (r.total == 0) 0f else r.score.toFloat() / r.total },
                        modifier = Modifier.size(140.dp),
                        strokeWidth = 10.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text("${r.score}/${r.total}", style = MaterialTheme.typography.headlineLarge)
                }
                Text("Best streak: ${r.bestStreak}", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        r.score == r.total -> "Perfect round!"
                        r.score >= r.total * 0.7 -> "Great job!"
                        r.score >= r.total * 0.4 -> "Not bad — keep exploring."
                        else -> "Time to browse a few more countries."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { onEvent(QuizPlayUiEvent.PlayAgain) }, modifier = Modifier.fillMaxWidth().testTag("play_again")) { Text("Play again") }
                OutlinedButton(onClick = { showAnswers = !showAnswers }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showAnswers) "Hide answers" else "See answers")
                }
                OutlinedButton(onClick = { onEvent(QuizPlayUiEvent.Quit) }, modifier = Modifier.fillMaxWidth()) { Text("Back to quiz") }
            }
        }
        if (showAnswers) {
            itemsIndexed(state.questions) { i, q ->
                val picked = state.answers.getOrNull(i)
                val correct = picked == q.correctIndex
                ListItem(
                    modifier = Modifier.testTag("answer_$i"),
                    headlineContent = { Text(q.countryName) },
                    supportingContent = {
                        Text(
                            if (correct) "Correct: ${q.options[q.correctIndex]}"
                            else "You said ${picked?.let { q.options[it] } ?: "—"} · answer: ${q.options[q.correctIndex]}",
                        )
                    },
                    leadingContent = {
                        Icon(
                            if (correct) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (correct) ContrisThemeExtras.colors.success else ContrisThemeExtras.colors.danger,
                        )
                    },
                    trailingContent = {
                        androidx.compose.material3.TextButton(onClick = { onEvent(QuizPlayUiEvent.OpenCountry(q.countryUuid)) }) { Text("Open") }
                    },
                )
            }
        }
    }
}
