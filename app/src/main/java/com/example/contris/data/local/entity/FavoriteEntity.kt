package com.example.contris.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = CountryEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["country_uuid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "country_uuid") val countryUuid: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
