package com.example.contris.data.local

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.Membership

/**
 * Builds the list query with bound arguments. User input is never interpolated into SQL.
 */
object CountryQueryBuilder {

    private const val SELECT =
        "SELECT c.uuid, c.name_common, c.capital_name, c.region, c.flag_emoji, c.flag_png, c.population, c.area_km2 FROM countries c"

    fun build(query: String, filters: CountryFilters, sort: CountrySort): SupportSQLiteQuery {
        val where = mutableListOf<String>()
        val args = mutableListOf<Any>()

        val q = query.trim().lowercase()
        if (q.isNotEmpty()) {
            where += "c.search_text LIKE ? ESCAPE '\\'"
            args += "%${escapeLike(q)}%"
        }
        inClause("c.region", filters.regions, where, args)
        inClause("c.subregion", filters.subregions, where, args)
        if (filters.continents.isNotEmpty()) {
            // continents is a JSON array; match the quoted value.
            where += "(" + filters.continents.joinToString(" OR ") { "c.continents LIKE ? ESCAPE '\\'" } + ")"
            filters.continents.forEach { args += "%\"${escapeLike(it)}\"%" }
        }
        filters.landlocked?.let { where += "c.landlocked = ?"; args += if (it) 1 else 0 }
        filters.unMember?.let { where += "c.un_member = ?"; args += if (it) 1 else 0 }
        if (filters.sovereignOnly) where += "c.sovereign = 1"
        filters.memberships.forEach { where += "c.${membershipColumn(it)} = 1" }
        if (filters.favoritesOnly) where += "c.uuid IN (SELECT country_uuid FROM favorites)"

        val sql = buildString {
            append(SELECT)
            if (where.isNotEmpty()) append(" WHERE ").append(where.joinToString(" AND "))
            append(" ORDER BY ").append(orderBy(sort))
        }
        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    fun membershipColumn(m: Membership): String = when (m) {
        Membership.UN -> "m_un"
        Membership.EU -> "m_eu"
        Membership.EUROZONE -> "m_eurozone"
        Membership.SCHENGEN -> "m_schengen"
        Membership.NATO -> "m_nato"
        Membership.COMMONWEALTH -> "m_commonwealth"
        Membership.OECD -> "m_oecd"
        Membership.G7 -> "m_g7"
        Membership.G20 -> "m_g20"
        Membership.BRICS -> "m_brics"
        Membership.OPEC -> "m_opec"
        Membership.AFRICAN_UNION -> "m_african_union"
        Membership.ASEAN -> "m_asean"
        Membership.ARAB_LEAGUE -> "m_arab_league"
    }

    private fun orderBy(sort: CountrySort): String = when (sort) {
        CountrySort.NAME_ASC -> "c.name_common COLLATE NOCASE ASC"
        CountrySort.NAME_DESC -> "c.name_common COLLATE NOCASE DESC"
        CountrySort.POPULATION_DESC -> "c.population IS NULL, c.population DESC, c.name_common COLLATE NOCASE ASC"
        CountrySort.POPULATION_ASC -> "c.population IS NULL, c.population ASC, c.name_common COLLATE NOCASE ASC"
        CountrySort.AREA_DESC -> "c.area_km2 IS NULL, c.area_km2 DESC, c.name_common COLLATE NOCASE ASC"
        CountrySort.AREA_ASC -> "c.area_km2 IS NULL, c.area_km2 ASC, c.name_common COLLATE NOCASE ASC"
    }

    private fun inClause(column: String, values: Set<String>, where: MutableList<String>, args: MutableList<Any>) {
        if (values.isEmpty()) return
        where += "$column IN (${values.joinToString(",") { "?" }})"
        args.addAll(values)
    }

    private fun escapeLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
