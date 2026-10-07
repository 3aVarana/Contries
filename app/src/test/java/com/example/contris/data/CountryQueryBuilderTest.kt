package com.example.contris.data

import com.example.contris.data.local.CountryQueryBuilder
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.Membership
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountryQueryBuilderTest {

    @Test
    fun `no filters produces plain select ordered by name`() {
        val q = CountryQueryBuilder.build("", CountryFilters(), CountrySort.NAME_ASC)
        assertThat(q.sql).doesNotContain("WHERE")
        assertThat(q.sql).endsWith("ORDER BY c.name_common COLLATE NOCASE ASC")
        assertThat(q.argCount).isEqualTo(0)
    }

    @Test
    fun `search binds a lower-cased LIKE argument and escapes wildcards`() {
        val q = CountryQueryBuilder.build("  Ger%_ ", CountryFilters(), CountrySort.NAME_ASC)
        assertThat(q.sql).contains("c.search_text LIKE ? ESCAPE '\\'")
        assertThat(q.argCount).isEqualTo(1)
        assertThat(boundArgs(q)).containsExactly("%ger\\%\\_%")
    }

    @Test
    fun `filters AND together with bound args`() {
        val filters = CountryFilters(
            regions = setOf("Europe", "Asia"),
            subregions = setOf("Western Europe"),
            continents = setOf("Europe"),
            landlocked = true,
            unMember = false,
            sovereignOnly = true,
            memberships = setOf(Membership.EU, Membership.NATO),
            favoritesOnly = true,
        )
        val q = CountryQueryBuilder.build("", filters, CountrySort.POPULATION_DESC)
        val sql = q.sql
        assertThat(sql).contains("c.region IN (?,?)")
        assertThat(sql).contains("c.subregion IN (?)")
        assertThat(sql).contains("c.continents LIKE ? ESCAPE '\\'")
        assertThat(sql).contains("c.landlocked = ?")
        assertThat(sql).contains("c.un_member = ?")
        assertThat(sql).contains("c.sovereign = 1")
        assertThat(sql).contains("c.m_eu = 1")
        assertThat(sql).contains("c.m_nato = 1")
        assertThat(sql).contains("c.uuid IN (SELECT country_uuid FROM favorites)")
        assertThat(sql.split(" AND ")).hasSize(9)
        assertThat(boundArgs(q)).containsExactly("Europe", "Asia", "Western Europe", "%\"Europe\"%", 1, 0).inOrder()
        assertThat(sql).endsWith("ORDER BY c.population IS NULL, c.population DESC, c.name_common COLLATE NOCASE ASC")
    }

    @Test
    fun `area sort puts nulls last`() {
        val q = CountryQueryBuilder.build("", CountryFilters(), CountrySort.AREA_ASC)
        assertThat(q.sql).endsWith("ORDER BY c.area_km2 IS NULL, c.area_km2 ASC, c.name_common COLLATE NOCASE ASC")
    }

    @Test
    fun `every membership has a column`() {
        Membership.entries.forEach { m ->
            assertThat(CountryQueryBuilder.membershipColumn(m)).startsWith("m_")
        }
    }

    private fun boundArgs(q: androidx.sqlite.db.SupportSQLiteQuery): List<Any?> {
        val args = arrayOfNulls<Any>(q.argCount)
        q.bindTo(object : androidx.sqlite.db.SupportSQLiteProgram {
            override fun bindNull(index: Int) { args[index - 1] = null }
            override fun bindLong(index: Int, value: Long) { args[index - 1] = value.toInt() }
            override fun bindDouble(index: Int, value: Double) { args[index - 1] = value }
            override fun bindString(index: Int, value: String) { args[index - 1] = value }
            override fun bindBlob(index: Int, value: ByteArray) { args[index - 1] = value }
            override fun clearBindings() {}
            override fun close() {}
        })
        return args.toList()
    }
}
