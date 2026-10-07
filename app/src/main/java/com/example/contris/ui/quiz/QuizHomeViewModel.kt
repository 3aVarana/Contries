package com.example.contris.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.repository.CountryRepository
import com.example.contris.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class QuizHomeViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    countryRepository: CountryRepository,
) : ViewModel() {

    private val _effects = Channel<QuizHomeUiEffect>(Channel.BUFFERED, BufferOverflow.DROP_OLDEST)
    val effects: Flow<QuizHomeUiEffect> = _effects.receiveAsFlow()

    private val bestByMode = combine(QuizMode.entries.map { mode -> quizRepository.observeBest(mode) }) { bests ->
        QuizMode.entries.zip(bests.toList()).toMap()
    }

    val uiState: StateFlow<QuizHomeUiState> = combine(
        bestByMode,
        quizRepository.observeHistory(20),
        countryRepository.observeCount(),
    ) { best, recent, count ->
        QuizHomeUiState(bestByMode = best, recent = recent, canPlay = count >= 4)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizHomeUiState())

    fun onEvent(event: QuizHomeUiEvent) {
        when (event) {
            is QuizHomeUiEvent.StartQuiz -> _effects.trySend(QuizHomeUiEffect.NavigateToPlay(event.mode))
            QuizHomeUiEvent.ClearHistory -> viewModelScope.launch { quizRepository.clear() }
        }
    }
}
