package com.example.contris.data

import app.cash.turbine.test
import com.example.contris.data.local.dao.CountryDao
import com.example.contris.data.local.entity.CountryEntity
import com.example.contris.data.remote.RestCountriesApi
import com.example.contris.data.remote.dto.CountriesPageDto
import com.example.contris.data.sync.CountrySyncManager
import com.example.contris.domain.model.SyncError
import com.example.contris.domain.model.SyncException
import com.example.contris.domain.model.SyncStatus
import com.example.contris.testutil.FakeSettingsRepository
import com.example.contris.testutil.Fixtures
import com.example.contris.testutil.TestDispatcherProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CountrySyncManagerTest {

    private val now = Instant.parse("2026-10-07T12:00:00Z")
    private var clockNow = now
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId) = this
        override fun instant(): Instant = clockNow
    }

    private val api = mockk<RestCountriesApi>()
    private val dao = mockk<CountryDao>(relaxed = true)
    private val settings = FakeSettingsRepository()
    private val dispatcher = StandardTestDispatcher()
    private val stored = mutableListOf<CountryEntity>()

    private lateinit var manager: CountrySyncManager

    @Before
    fun setUp() {
        coEvery { dao.replaceAll(any()) } answers { stored.clear(); stored += firstArg<List<CountryEntity>>() }
        coEvery { dao.count() } answers { stored.size }
        manager = CountrySyncManager(
            api, dao, settings, TestDispatcherProvider(dispatcher), clock,
            CountrySyncManager.Policy(rateLimitBackoff = Duration.ofMillis(10), serverErrorBackoff = Duration.ofMillis(1)),
        )
    }

    private fun stubPages() {
        coEvery { api.getCountries(offset = 0, limit = any(), omit = any()) } returns Fixtures.page(0)
        coEvery { api.getCountries(offset = 100, limit = any(), omit = any()) } returns Fixtures.page(1)
    }

    @Test
    fun `first sync pages until more is false and stores in one replaceAll`() = runTest(dispatcher) {
        stubPages()

        val result = manager.sync(force = false)

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { dao.replaceAll(any()) }
        assertThat(stored.map { it.nameCommon }).containsExactly("Canada", "Abkhazia", "Germany", "Austria", "Niger", "United States")
        assertThat(settings.state.value.lastSync).isEqualTo(now)
        assertThat(settings.state.value.lastSyncTotal).isEqualTo(6)
        assertThat(manager.status.first()).isEqualTo(SyncStatus.Idle(now, 6))
    }

    @Test
    fun `emits syncing progress then idle`() = runTest(dispatcher) {
        stubPages()
        manager.status.test {
            assertThat(awaitItem()).isEqualTo(SyncStatus.Idle(null, 0))
            val job = async { manager.sync(force = false) }
            assertThat(awaitItem()).isEqualTo(SyncStatus.Syncing(1, 1, null))
            assertThat(awaitItem()).isEqualTo(SyncStatus.Syncing(2, 1, null))
            assertThat(awaitItem()).isEqualTo(SyncStatus.Idle(now, 6))
            job.await()
        }
    }

    @Test
    fun `fresh data is not re-synced unless forced`() = runTest(dispatcher) {
        stubPages()
        manager.sync(force = false)
        clockNow = now.plus(Duration.ofHours(1))

        manager.sync(force = false)
        coVerify(exactly = 2) { api.getCountries(any(), any(), any()) } // still only the first sync's 2 pages

        clockNow = now.plus(Duration.ofHours(2))
        manager.sync(force = true)
        coVerify(exactly = 4) { api.getCountries(any(), any(), any()) }
    }

    @Test
    fun `stale data is re-synced after 72 hours`() = runTest(dispatcher) {
        stubPages()
        manager.sync(force = false)
        clockNow = now.plus(Duration.ofHours(73))
        manager.sync(force = false)
        coVerify(exactly = 4) { api.getCountries(any(), any(), any()) }
    }

    @Test
    fun `manual refresh is debounced for 60 seconds`() = runTest(dispatcher) {
        stubPages()
        manager.sync(force = false)
        clockNow = now.plus(Duration.ofSeconds(30))

        val second = manager.sync(force = true)

        assertThat(second.isFailure).isTrue()
        assertThat((second.exceptionOrNull() as SyncException).error).isEqualTo(SyncError.RateLimited)
        coVerify(exactly = 2) { api.getCountries(any(), any(), any()) }
    }

    @Test
    fun `failure keeps existing data and reports error`() = runTest(dispatcher) {
        stubPages()
        manager.sync(force = false)
        clockNow = now.plus(Duration.ofHours(80))
        coEvery { api.getCountries(offset = 0, limit = any(), omit = any()) } throws httpError(403)

        val result = manager.sync(force = false)

        assertThat((result.exceptionOrNull() as SyncException).error).isEqualTo(SyncError.QuotaExceeded)
        assertThat(stored).hasSize(6)
        coVerify(exactly = 1) { dao.replaceAll(any()) }
        assertThat(manager.status.first()).isEqualTo(SyncStatus.Failed(SyncError.QuotaExceeded, now))
    }

    @Test
    fun `maps error codes`() = runTest(dispatcher) {
        coEvery { api.getCountries(offset = 0, limit = any(), omit = any()) } throws httpError(401)
        assertThat(error(manager.sync(false))).isEqualTo(SyncError.Unauthorized)

        coEvery { api.getCountries(offset = 0, limit = any(), omit = any()) } throws IOException("offline")
        assertThat(error(manager.sync(false))).isEqualTo(SyncError.Network)

        coEvery { api.getCountries(offset = 0, limit = any(), omit = any()) } throws httpError(503)
        assertThat(error(manager.sync(false))).isEqualTo(SyncError.Server(503))
    }

    @Test
    fun `429 is retried twice with backoff then fails`() = runTest(dispatcher) {
        coEvery { api.getCountries(offset = 0, limit = any(), omit = any()) } throws httpError(429)
        val result = manager.sync(false)
        assertThat(error(result)).isEqualTo(SyncError.RateLimited)
        coVerify(exactly = 3) { api.getCountries(offset = 0, limit = any(), omit = any()) }
    }

    @Test
    fun `5xx is retried once and can recover`() = runTest(dispatcher) {
        var calls = 0
        coEvery { api.getCountries(offset = 0, limit = any(), omit = any()) } answers {
            if (calls++ == 0) throw httpError(500) else Fixtures.page(0)
        }
        coEvery { api.getCountries(offset = 100, limit = any(), omit = any()) } returns Fixtures.page(1)
        assertThat(manager.sync(false).isSuccess).isTrue()
        assertThat(calls).isEqualTo(2)
    }

    @Test
    fun `error envelope without data fails with message`() = runTest(dispatcher) {
        coEvery { api.getCountries(offset = 0, limit = any(), omit = any()) } returns
            Fixtures.json.decodeFromString<CountriesPageDto>(Fixtures.raw("error_401.json"))
        val e = error(manager.sync(false))
        assertThat(e).isInstanceOf(SyncError.Unknown::class.java)
        assertThat((e as SyncError.Unknown).message).contains("API key")
    }

    @Test
    fun `concurrent syncs are serialised by the mutex`() = runTest(dispatcher) {
        stubPages()
        val a = async { manager.sync(force = false) }
        val b = async { manager.sync(force = false) }
        advanceUntilIdle()
        assertThat(a.await().isSuccess).isTrue()
        assertThat(b.await().isSuccess).isTrue()
        // Second call found fresh data and skipped the network.
        coVerify(exactly = 2) { api.getCountries(any(), any(), any()) }
        coVerify(exactly = 1) { dao.replaceAll(any()) }
    }

    private fun error(result: Result<Unit>): SyncError = (result.exceptionOrNull() as SyncException).error

    private fun httpError(code: Int) =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))
}
