package com.example.contris.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.contris.data.local.entity.CountryEntity
import com.example.contris.data.local.entity.CountrySummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryDao {

    @RawQuery(observedEntities = [CountryEntity::class])
    fun observeSummaries(query: SupportSQLiteQuery): Flow<List<CountrySummaryRow>>

    @Query("SELECT * FROM countries WHERE uuid = :uuid")
    fun observeByUuid(uuid: String): Flow<CountryEntity?>

    @Query("SELECT * FROM countries WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): CountryEntity?

    @Query(
        "SELECT uuid, name_common, capital_name, region, flag_emoji, flag_png, population, area_km2 " +
            "FROM countries WHERE alpha3 IN (:codes) ORDER BY name_common",
    )
    suspend fun getSummariesByAlpha3(codes: List<String>): List<CountrySummaryRow>

    @Query("SELECT COUNT(*) FROM countries")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM countries")
    fun observeCount(): Flow<Int>

    @Query("SELECT DISTINCT region FROM countries WHERE region IS NOT NULL AND region != '' ORDER BY region")
    fun observeRegions(): Flow<List<String>>

    @Query("SELECT DISTINCT subregion FROM countries WHERE subregion IS NOT NULL AND subregion != '' ORDER BY subregion")
    fun observeSubregions(): Flow<List<String>>

    @Query("SELECT * FROM countries WHERE flag_png IS NOT NULL AND capital_name IS NOT NULL AND sovereign = 1")
    suspend fun getQuizPool(): List<CountryEntity>

    @Upsert
    suspend fun upsertAll(items: List<CountryEntity>)

    @Query("DELETE FROM countries WHERE uuid NOT IN (:keep)")
    suspend fun deleteNotIn(keep: List<String>)

    @Transaction
    suspend fun replaceAll(items: List<CountryEntity>) {
        upsertAll(items)
        deleteNotIn(items.map { it.uuid })
    }
}
