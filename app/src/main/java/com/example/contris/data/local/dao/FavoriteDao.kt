package com.example.contris.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.contris.data.local.entity.CountrySummaryRow
import com.example.contris.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY added_at DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query(
        "SELECT c.uuid, c.name_common, c.capital_name, c.region, c.flag_emoji, c.flag_png, c.population, c.area_km2 " +
            "FROM favorites f INNER JOIN countries c ON c.uuid = f.country_uuid ORDER BY f.added_at DESC",
    )
    fun observeFavoriteSummaries(): Flow<List<CountrySummaryRow>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE country_uuid = :uuid)")
    fun observeIsFavorite(uuid: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE country_uuid = :uuid)")
    suspend fun isFavorite(uuid: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE country_uuid = :uuid")
    suspend fun delete(uuid: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()
}
