package com.example.contris.domain.usecase

import com.example.contris.domain.model.QuizResult
import com.example.contris.domain.repository.QuizRepository
import javax.inject.Inject

class SaveQuizResultUseCase @Inject constructor(private val repository: QuizRepository) {
    suspend operator fun invoke(result: QuizResult) = repository.save(result)
}
