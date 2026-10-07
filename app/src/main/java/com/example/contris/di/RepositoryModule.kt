package com.example.contris.di

import com.example.contris.data.repository.CountryRepositoryImpl
import com.example.contris.data.repository.FavoritesRepositoryImpl
import com.example.contris.data.repository.QuizRepositoryImpl
import com.example.contris.data.repository.SettingsRepositoryImpl
import com.example.contris.domain.repository.CountryRepository
import com.example.contris.domain.repository.FavoritesRepository
import com.example.contris.domain.repository.QuizRepository
import com.example.contris.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindCountryRepository(impl: CountryRepositoryImpl): CountryRepository
    @Binds abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
    @Binds abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository
    @Binds abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
