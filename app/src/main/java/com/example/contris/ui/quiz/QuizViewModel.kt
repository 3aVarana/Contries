package com.example.contris.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.model.QuizResult
import com.example.contris.domain.model.QuizRound
import com.example.contris.domain.usecase.GenerateQuizRoundUseCase
import com.example.contris.domain.usecase.SaveQuizResultUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drives one quiz round. Progress (answers, index) is kept in [SavedStateHandle]; the round itself is
 * regenerated deterministically from a saved seed so rotation / process death restore the same questions.
 */
@HiltViewModel(assistedFactory = QuizViewModel.Factory::class)
class QuizViewModel @AssistedInject constructor(
    @Assisted val mode: QuizMode,
    private val savedStateHandle: SavedStateHandle,
    private val generateRound: GenerateQuizRoundUseCase,
    private val saveResult: SaveQuizResultUseCase,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(mode: QuizMode): QuizViewModel
    }

    private val _effects = Channel<QuizPlayUiEffect>(Channel.BUFFERED, BufferOverflow.DROP_OLDEST)
    val effects: Flow<QuizPlayUiEffect> = _effects.receiveAsFlow()

    private val _uiState = MutableStateFlow<QuizPlayUiState>(QuizPlayUiState.Generating)
    val uiState: StateFlow<QuizPlayUiState> = _uiState.asStateFlow()

    private var round: QuizRound? = null
    private var answers: MutableList<Int?> = mutableListOf()
    private var index: Int = 0
    private var revealed: Boolean = false

    init {
        start(newSeed = !savedStateHandle.contains(KEY_SEED))
    }

    private fun start(newSeed: Boolean) {
        if (newSeed) {
            savedStateHandle[KEY_SEED] = Random.nextLong()
            savedStateHandle[KEY_ANSWERS] = IntArray(0)
            savedStateHandle[KEY_INDEX] = 0
            savedStateHandle[KEY_REVEALED] = false
        }
        _uiState.value = QuizPlayUiState.Generating
        viewModelScope.launch {
            val seed: Long = savedStateHandle[KEY_SEED] ?: Random.nextLong().also { savedStateHandle[KEY_SEED] = it }
            val r = runCatching { generateRound(mode, Random(seed)) }.getOrElse { t ->
                _uiState.value = QuizPlayUiState.Error(t.message ?: "Not enough data for a quiz yet. Try again after the countries have downloaded.")
                return@launch
            }
            round = r
            val saved: IntArray = savedStateHandle[KEY_ANSWERS] ?: IntArray(0)
            answers = MutableList(r.questions.size) { i -> saved.getOrNull(i)?.takeIf { it >= 0 } }
            index = (savedStateHandle[KEY_INDEX] ?: 0).coerceIn(0, r.questions.size)
            revealed = savedStateHandle[KEY_REVEALED] ?: false
            if (index >= r.questions.size) finish(persist = false) else publishQuestion()
        }
    }

    fun onEvent(event: QuizPlayUiEvent) {
        when (event) {
            is QuizPlayUiEvent.OptionSelected -> select(event.index)
            QuizPlayUiEvent.Next -> next()
            QuizPlayUiEvent.Quit -> _effects.trySend(QuizPlayUiEffect.NavigateBack)
            QuizPlayUiEvent.PlayAgain -> start(newSeed = true)
            is QuizPlayUiEvent.OpenCountry -> _effects.trySend(QuizPlayUiEffect.NavigateToDetail(event.uuid))
        }
    }

    private fun select(option: Int) {
        val r = round ?: return
        if (revealed || index !in r.questions.indices) return
        if (option !in r.questions[index].options.indices) return
        answers[index] = option
        revealed = true
        persistProgress()
        publishQuestion()
    }

    private fun next() {
        val r = round ?: return
        if (!revealed) return
        index++
        revealed = false
        persistProgress()
        if (index >= r.questions.size) finish(persist = true) else publishQuestion()
    }

    private fun publishQuestion() {
        val r = round ?: return
        val (score, _, best) = tally(r)
        _uiState.value = QuizPlayUiState.Question(
            index = index,
            total = r.questions.size,
            question = r.questions[index],
            selectedIndex = answers[index],
            revealed = revealed,
            score = score,
            streak = currentStreak(r),
            bestStreak = best,
        )
    }

    private fun finish(persist: Boolean) {
        val r = round ?: return
        val (score, _, best) = tally(r)
        val result = QuizResult(mode = mode, score = score, total = r.questions.size, bestStreak = best, playedAt = clock.instant())
        _uiState.value = QuizPlayUiState.Finished(result, r.questions, answers.toList())
        if (persist) viewModelScope.launch { saveResult(result) }
    }

    /** Returns (score, currentStreak, bestStreak) over answered questions. */
    private fun tally(r: QuizRound): Triple<Int, Int, Int> {
        var score = 0
        var streak = 0
        var best = 0
        r.questions.forEachIndexed { i, q ->
            val a = answers.getOrNull(i) ?: return@forEachIndexed
            if (a == q.correctIndex) { score++; streak++; best = maxOf(best, streak) } else streak = 0
        }
        return Triple(score, streak, best)
    }

    private fun currentStreak(r: QuizRound): Int = tally(r).second

    private fun persistProgress() {
        savedStateHandle[KEY_ANSWERS] = IntArray(answers.size) { answers[it] ?: -1 }
        savedStateHandle[KEY_INDEX] = index
        savedStateHandle[KEY_REVEALED] = revealed
    }

    private fun <T> MutableStateFlow<T>.set(block: (T) -> T) = update(block)

    companion object {
        const val KEY_SEED = "seed"
        const val KEY_ANSWERS = "answers"
        const val KEY_INDEX = "index"
        const val KEY_REVEALED = "revealed"
    }
}
