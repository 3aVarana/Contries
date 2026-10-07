package com.example.contris.di

import android.content.Context
import androidx.room.Room
import com.example.contris.data.local.ContrisDatabase
import com.example.contris.data.local.dao.CountryDao
import com.example.contris.data.local.dao.FavoriteDao
import com.example.contris.data.local.dao.QuizResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ContrisDatabase =
        Room.databaseBuilder(context, ContrisDatabase::class.java, ContrisDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideCountryDao(db: ContrisDatabase): CountryDao = db.countryDao()
    @Provides fun provideFavoriteDao(db: ContrisDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideQuizResultDao(db: ContrisDatabase): QuizResultDao = db.quizResultDao()
}
