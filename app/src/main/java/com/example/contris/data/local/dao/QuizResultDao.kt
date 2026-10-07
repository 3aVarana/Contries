package com.example.contris.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.contris.data.local.entity.QuizResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizResultDao {

    @Insert
    suspend fun insert(entity: QuizResultEntity): Long

    @Query("SELECT * FROM quiz_results ORDER BY played_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<QuizResultEntity>>

    @Query("SELECT * FROM quiz_results WHERE mode = :mode ORDER BY score DESC, best_streak DESC, played_at DESC LIMIT 1")
    fun observeBest(mode: String): Flow<QuizResultEntity?>

    @Query("DELETE FROM quiz_results")
    suspend fun clear()
}
