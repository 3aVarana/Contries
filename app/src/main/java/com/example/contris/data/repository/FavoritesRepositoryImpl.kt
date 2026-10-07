package com.example.contris.data.repository

import com.example.contris.data.local.dao.FavoriteDao
import com.example.contris.data.local.entity.FavoriteEntity
import com.example.contris.data.mapper.toDomain
import com.example.contris.domain.DispatcherProvider
import com.example.contris.domain.model.CountrySummary
import com.example.contris.domain.repository.FavoritesRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : FavoritesRepository {

    override fun observeFavorites(): Flow<List<CountrySummary>> =
        dao.observeFavoriteSummaries().map { rows -> rows.map { it.toDomain() } }

    override fun observeIsFavorite(uuid: String): Flow<Boolean> = dao.observeIsFavorite(uuid)

    override suspend fun toggle(uuid: String) = withContext(dispatchers.io) {
        if (dao.isFavorite(uuid)) dao.delete(uuid) else dao.insert(FavoriteEntity(uuid, clock.millis()))
    }

    override suspend fun add(uuid: String) = withContext(dispatchers.io) {
        dao.insert(FavoriteEntity(uuid, clock.millis()))
    }

    override suspend fun remove(uuid: String) = withContext(dispatchers.io) { dao.delete(uuid) }

    override suspend fun clear() = withContext(dispatchers.io) { dao.clear() }
}
