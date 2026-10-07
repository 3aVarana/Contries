package com.example.contris.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.contris.data.local.ContrisDatabase
import com.example.contris.data.local.CountryQueryBuilder
import com.example.contris.data.local.entity.FavoriteEntity
import com.example.contris.data.local.entity.QuizResultEntity
import com.example.contris.data.mapper.toEntityOrNull
import com.example.contris.domain.model.CountryFilters
import com.example.contris.domain.model.CountrySort
import com.example.contris.domain.model.Membership
import com.example.contris.testutil.Fixtures
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CountryDaoTest {

    private lateinit var db: ContrisDatabase
    private val entities by lazy {
        (Fixtures.page(0).data!!.objects + Fixtures.page(1).data!!.objects).mapNotNull { it.toEntityOrNull() }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ContrisDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun summaries(query: String = "", filters: CountryFilters = CountryFilters(), sort: CountrySort = CountrySort.NAME_ASC) =
        db.countryDao().observeSummaries(CountryQueryBuilder.build(query, filters, sort)).first()

    @Test
    fun `replaceAll inserts and removes stale rows`() = runTest {
        val dao = db.countryDao()
        dao.replaceAll(entities)
        assertThat(dao.count()).isEqualTo(6)

        dao.replaceAll(entities.filter { it.nameCommon != "Niger" })
        assertThat(dao.count()).isEqualTo(5)
        assertThat(summaries().map { it.nameCommon }).doesNotContain("Niger")
    }

    @Test
    fun `search matches common, alpha-2 and native names`() = runTest {
        db.countryDao().replaceAll(entities)
        assertThat(summaries("ger").map { it.nameCommon }).containsExactly("Germany", "Niger")
        assertThat(summaries("DE").map { it.nameCommon }).containsExactly("Germany")
        assertThat(summaries("Deutschland").map { it.nameCommon }).containsExactly("Germany")
        assertThat(summaries("Österreich").map { it.nameCommon }).containsExactly("Austria")
        assertThat(summaries("ottawa").map { it.nameCommon }).containsExactly("Canada")
        assertThat(summaries("zzz")).isEmpty()
    }

    @Test
    fun `filters combine and sort puts nulls last`() = runTest {
        db.countryDao().replaceAll(entities)

        assertThat(summaries(filters = CountryFilters(regions = setOf("Europe"))).map { it.nameCommon }).containsExactly("Austria", "Germany").inOrder()
        assertThat(summaries(filters = CountryFilters(landlocked = true)).map { it.nameCommon }).containsExactly("Austria", "Niger")
        assertThat(summaries(filters = CountryFilters(regions = setOf("Europe"), landlocked = true)).map { it.nameCommon }).containsExactly("Austria")
        assertThat(summaries(filters = CountryFilters(memberships = setOf(Membership.G7))).map { it.nameCommon }).containsExactly("Canada", "Germany", "United States")
        assertThat(summaries(filters = CountryFilters(sovereignOnly = true)).map { it.nameCommon }).doesNotContain("Abkhazia")
        assertThat(summaries(filters = CountryFilters(unMember = false)).map { it.nameCommon }).containsExactly("Abkhazia")
        assertThat(summaries(filters = CountryFilters(continents = setOf("North America"))).map { it.nameCommon }).containsExactly("Canada", "United States")

        assertThat(summaries(sort = CountrySort.POPULATION_DESC).map { it.nameCommon })
            .containsExactly("United States", "Germany", "Canada", "Niger", "Austria", "Abkhazia").inOrder()
        assertThat(summaries(sort = CountrySort.AREA_ASC).map { it.nameCommon })
            .containsExactly("Abkhazia", "Austria", "Germany", "Niger", "United States", "Canada").inOrder()
    }

    @Test
    fun `favorites join, cascade and favoritesOnly filter`() = runTest {
        val dao = db.countryDao()
        dao.replaceAll(entities)
        val canada = entities.first { it.nameCommon == "Canada" }.uuid
        val niger = entities.first { it.nameCommon == "Niger" }.uuid
        db.favoriteDao().insert(FavoriteEntity(canada, 1))
        db.favoriteDao().insert(FavoriteEntity(niger, 2))

        assertThat(db.favoriteDao().observeFavoriteSummaries().first().map { it.nameCommon }).containsExactly("Niger", "Canada").inOrder()
        assertThat(db.favoriteDao().observeIsFavorite(canada).first()).isTrue()
        assertThat(summaries(filters = CountryFilters(favoritesOnly = true)).map { it.nameCommon }).containsExactly("Canada", "Niger")

        // Removing the country removes the favourite through the FK cascade.
        dao.replaceAll(entities.filter { it.uuid != niger })
        assertThat(db.favoriteDao().observeAll().first().map { it.countryUuid }).containsExactly(canada)
    }

    @Test
    fun `quiz pool requires flag, capital and sovereignty`() = runTest {
        db.countryDao().replaceAll(entities)
        val pool = db.countryDao().getQuizPool().map { it.nameCommon }
        assertThat(pool).containsExactly("Canada", "Germany", "Austria", "Niger", "United States")
    }

    @Test
    fun `border lookup by alpha3 and regions`() = runTest {
        db.countryDao().replaceAll(entities)
        assertThat(db.countryDao().getSummariesByAlpha3(listOf("USA", "CAN", "XXX")).map { it.nameCommon }).containsExactly("Canada", "United States").inOrder()
        assertThat(db.countryDao().observeRegions().first()).containsExactly("Africa", "Americas", "Asia", "Europe").inOrder()
        assertThat(db.countryDao().observeSubregions().first()).contains("Western Europe")
    }

    @Test
    fun `quiz results best and recent`() = runTest {
        val dao = db.quizResultDao()
        dao.insert(QuizResultEntity(mode = "FLAG_TO_COUNTRY", score = 6, total = 10, bestStreak = 3, playedAt = 1))
        dao.insert(QuizResultEntity(mode = "FLAG_TO_COUNTRY", score = 9, total = 10, bestStreak = 5, playedAt = 2))
        dao.insert(QuizResultEntity(mode = "COUNTRY_TO_CAPITAL", score = 2, total = 10, bestStreak = 1, playedAt = 3))

        assertThat(dao.observeBest("FLAG_TO_COUNTRY").first()!!.score).isEqualTo(9)
        assertThat(dao.observeRecent(2).first().map { it.playedAt }).containsExactly(3L, 2L).inOrder()
        dao.clear()
        assertThat(dao.observeRecent(10).first()).isEmpty()
    }
}
