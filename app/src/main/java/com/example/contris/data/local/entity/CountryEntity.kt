package com.example.contris.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "countries",
    indices = [
        Index("name_common"),
        Index("alpha3"),
        Index("region"),
        Index("subregion"),
    ],
)
data class CountryEntity(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "name_common") val nameCommon: String,
    @ColumnInfo(name = "name_official") val nameOfficial: String,
    /** JSON array of alternate names. */
    @ColumnInfo(name = "name_alternates") val nameAlternates: String,
    /** JSON object iso639-3 → {common, official}. */
    @ColumnInfo(name = "name_native") val nameNative: String,
    val alpha2: String?,
    val alpha3: String?,
    val ccn3: String?,
    val cioc: String?,
    val fifa: String?,
    val fips: String?,
    val gec: String?,
    /** Lower-cased concatenation of searchable names and codes. */
    @ColumnInfo(name = "search_text") val searchText: String,
    @ColumnInfo(name = "capital_name") val capitalName: String?,
    @ColumnInfo(name = "capital_lat") val capitalLat: Double?,
    @ColumnInfo(name = "capital_lng") val capitalLng: Double?,
    /** JSON array of all capitals. */
    val capitals: String,
    @ColumnInfo(name = "flag_emoji") val flagEmoji: String?,
    @ColumnInfo(name = "flag_png") val flagPng: String?,
    @ColumnInfo(name = "flag_svg") val flagSvg: String?,
    @ColumnInfo(name = "flag_description") val flagDescription: String?,
    @ColumnInfo(name = "flag_dominant") val flagDominant: String?,
    @ColumnInfo(name = "flag_prominent") val flagProminent: String?,
    @ColumnInfo(name = "flag_vibrant") val flagVibrant: String?,
    @ColumnInfo(name = "flag_muted") val flagMuted: String?,
    val region: String?,
    val subregion: String?,
    /** JSON array. */
    val continents: String,
    val landlocked: Boolean?,
    /** JSON array of alpha3 codes. */
    val borders: String,
    @ColumnInfo(name = "area_km2") val areaKm2: Double?,
    @ColumnInfo(name = "area_mi2") val areaMi2: Double?,
    val lat: Double?,
    val lng: Double?,
    val population: Long?,
    /** JSON arrays. */
    val currencies: String,
    val languages: String,
    @ColumnInfo(name = "calling_codes") val callingCodes: String,
    val tlds: String,
    val timezones: String,
    @ColumnInfo(name = "car_signs") val carSigns: String,
    @ColumnInfo(name = "driving_side") val drivingSide: String?,
    @ColumnInfo(name = "postal_format") val postalFormat: String?,
    @ColumnInfo(name = "start_of_week") val startOfWeek: String?,
    @ColumnInfo(name = "government_type") val governmentType: String?,
    @ColumnInfo(name = "measurement_system") val measurementSystem: String?,
    @ColumnInfo(name = "gini_latest_year") val giniLatestYear: Int?,
    @ColumnInfo(name = "gini_latest") val giniLatest: Double?,
    /** JSON object year → value. */
    @ColumnInfo(name = "gini_json") val giniJson: String,
    val sovereign: Boolean,
    @ColumnInfo(name = "un_member") val unMember: Boolean,
    @ColumnInfo(name = "un_observer") val unObserver: Boolean,
    val dependency: Boolean,
    val disputed: Boolean,
    @ColumnInfo(name = "dependency_type") val dependencyType: String?,
    @ColumnInfo(name = "iso_status") val isoStatus: String?,
    @ColumnInfo(name = "m_un") val mUn: Boolean,
    @ColumnInfo(name = "m_eu") val mEu: Boolean,
    @ColumnInfo(name = "m_eurozone") val mEurozone: Boolean,
    @ColumnInfo(name = "m_schengen") val mSchengen: Boolean,
    @ColumnInfo(name = "m_nato") val mNato: Boolean,
    @ColumnInfo(name = "m_commonwealth") val mCommonwealth: Boolean,
    @ColumnInfo(name = "m_oecd") val mOecd: Boolean,
    @ColumnInfo(name = "m_g7") val mG7: Boolean,
    @ColumnInfo(name = "m_g20") val mG20: Boolean,
    @ColumnInfo(name = "m_brics") val mBrics: Boolean,
    @ColumnInfo(name = "m_opec") val mOpec: Boolean,
    @ColumnInfo(name = "m_african_union") val mAfricanUnion: Boolean,
    @ColumnInfo(name = "m_asean") val mAsean: Boolean,
    @ColumnInfo(name = "m_arab_league") val mArabLeague: Boolean,
    @ColumnInfo(name = "description_short") val descriptionShort: String?,
    @ColumnInfo(name = "description_long") val descriptionLong: String?,
    @ColumnInfo(name = "link_wikipedia") val linkWikipedia: String?,
    @ColumnInfo(name = "link_official") val linkOfficial: String?,
    @ColumnInfo(name = "link_google_maps") val linkGoogleMaps: String?,
    @ColumnInfo(name = "link_osm") val linkOsm: String?,
    @ColumnInfo(name = "last_updated_epoch") val lastUpdatedEpoch: Long?,
)

/** Projection used by the list screens. */
data class CountrySummaryRow(
    val uuid: String,
    @ColumnInfo(name = "name_common") val nameCommon: String,
    @ColumnInfo(name = "capital_name") val capitalName: String?,
    val region: String?,
    @ColumnInfo(name = "flag_emoji") val flagEmoji: String?,
    @ColumnInfo(name = "flag_png") val flagPng: String?,
    val population: Long?,
    @ColumnInfo(name = "area_km2") val areaKm2: Double?,
)
