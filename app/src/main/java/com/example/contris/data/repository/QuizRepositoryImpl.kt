package com.example.contris.data.repository

import com.example.contris.data.local.dao.QuizResultDao
import com.example.contris.data.local.entity.QuizResultEntity
import com.example.contris.domain.DispatcherProvider
import com.example.contris.domain.model.QuizMode
import com.example.contris.domain.model.QuizResult
import com.example.contris.domain.repository.QuizRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val dao: QuizResultDao,
    private val dispatchers: DispatcherProvider,
) : QuizRepository {

    override suspend fun save(result: QuizResult) {
        withContext(dispatchers.io) {
            dao.insert(
                QuizResultEntity(
                    mode = result.mode.name,
                    score = result.score,
                    total = result.total,
                    bestStreak = result.bestStreak,
                    playedAt = result.playedAt.toEpochMilli(),
                ),
            )
        }
    }

    override fun observeHistory(limit: Int): Flow<List<QuizResult>> =
        dao.observeRecent(limit).map { list -> list.mapNotNull { it.toDomainOrNull() } }

    override fun observeBest(mode: QuizMode): Flow<QuizResult?> =
        dao.observeBest(mode.name).map { it?.toDomainOrNull() }

    override suspend fun clear() = withContext(dispatchers.io) { dao.clear() }

    private fun QuizResultEntity.toDomainOrNull(): QuizResult? {
        val m = runCatching { QuizMode.valueOf(mode) }.getOrNull() ?: return null
        return QuizResult(m, score, total, bestStreak, Instant.ofEpochMilli(playedAt), id)
    }
}
