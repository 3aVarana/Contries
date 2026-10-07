package com.example.contris.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.contris.data.local.dao.CountryDao
import com.example.contris.data.local.dao.FavoriteDao
import com.example.contris.data.local.dao.QuizResultDao
import com.example.contris.data.local.entity.CountryEntity
import com.example.contris.data.local.entity.FavoriteEntity
import com.example.contris.data.local.entity.QuizResultEntity

@Database(
    entities = [CountryEntity::class, FavoriteEntity::class, QuizResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ContrisDatabase : RoomDatabase() {
    abstract fun countryDao(): CountryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun quizResultDao(): QuizResultDao

    companion object {
        const val NAME = "contris.db"
    }
}
