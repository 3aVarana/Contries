package com.example.contris.data.sync

import com.example.contris.data.local.dao.CountryDao
import com.example.contris.data.mapper.toEntityOrNull
import com.example.contris.data.remote.PAGE_SIZE
import com.example.contris.data.remote.RestCountriesApi
import com.example.contris.domain.DispatcherProvider
import com.example.contris.domain.model.SyncError
import com.example.contris.domain.model.SyncException
import com.example.contris.domain.model.SyncStatus
import com.example.contris.domain.repository.SettingsRepository
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Owns the sync policy described in the implementation plan §5.5:
 * staleness check, sequential paging, single-transaction replace, mutex, debounce and error mapping.
 */
@Singleton
class CountrySyncManager @Inject constructor(
    private val api: RestCountriesApi,
    private val countryDao: CountryDao,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
    private val policy: Policy,
) {
    data class Policy(
        val staleAfter: Duration = Duration.ofHours(72),
        val manualRefreshDebounce: Duration = Duration.ofSeconds(60),
        val maxPages: Int = 10,
        val rateLimitRetries: Int = 2,
        val rateLimitBackoff: Duration = Duration.ofSeconds(10),
        val serverErrorRetries: Int = 1,
        val serverErrorBackoff: Duration = Duration.ofSeconds(2),
    )

    private val mutex = Mutex()
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle(null, 0))
    val status: Flow<SyncStatus> = _status.asStateFlow()

    @Volatile private var lastAttempt: Instant? = null
    @Volatile private var initialised = false

    suspend fun sync(force: Boolean): Result<Unit> = withContext(dispatchers.io) {
        mutex.withLock {
            val settings = settingsRepository.settings.first()
            val lastSync = settings.lastSync
            val count = countryDao.count()
            if (!initialised) {
                _status.value = SyncStatus.Idle(lastSync, count)
                initialised = true
            }

            val now = clock.instant()
            if (!force && count > 0 && lastSync != null && Duration.between(lastSync, now) < policy.staleAfter) {
                return@withLock Result.success(Unit)
            }
            if (force && count > 0) {
                val previous = lastAttempt
                if (previous != null && Duration.between(previous, now) < policy.manualRefreshDebounce) {
                    return@withLock Result.failure(SyncException(SyncError.RateLimited))
                }
            }
            lastAttempt = now

            try {
                val total = fetchAndStore(lastSync)
                val finished = clock.millis()
                settingsRepository.setLastSync(finished, total)
                _status.value = SyncStatus.Idle(Instant.ofEpochMilli(finished), total)
                Result.success(Unit)
            } catch (e: CancellationException) {
                _status.value = SyncStatus.Idle(lastSync, count)
                throw e
            } catch (t: Throwable) {
                val wrapped = t as? SyncException ?: SyncException(t.toSyncError(), t)
                _status.value = SyncStatus.Failed(wrapped.error, lastSync)
                Result.failure(wrapped)
            }
        }
    }

    /** Fetches every page sequentially and replaces the table in one transaction. Returns the stored count. */
    private suspend fun fetchAndStore(lastSync: Instant?): Int {
        val entities = ArrayList<com.example.contris.data.local.entity.CountryEntity>(260)
        var page = 0
        var totalPages = 1
        while (page < policy.maxPages) {
            _status.value = SyncStatus.Syncing(page + 1, totalPages, lastSync)
            val dto = fetchPage(offset = page * PAGE_SIZE)
            if (dto.data == null) {
                throw SyncException(SyncError.Unknown(dto.errors.firstOrNull()?.message))
            }
            val data = dto.data
            entities += data.objects.mapNotNull { it.toEntityOrNull() }
            val total = data.meta?.total
            if (total != null && total > 0) totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE
            val more = data.meta?.more ?: (data.objects.size >= PAGE_SIZE)
            page++
            if (!more || data.objects.isEmpty()) break
        }
        if (entities.isEmpty()) throw SyncException(SyncError.Unknown("Empty response"))
        countryDao.replaceAll(entities)
        return entities.size
    }

    private suspend fun fetchPage(offset: Int): com.example.contris.data.remote.dto.CountriesPageDto {
        var rateLimitAttempts = 0
        var serverAttempts = 0
        while (true) {
            try {
                return api.getCountries(offset = offset)
            } catch (e: HttpException) {
                when {
                    e.code() == 429 && rateLimitAttempts < policy.rateLimitRetries -> {
                        rateLimitAttempts++
                        delay(policy.rateLimitBackoff.toMillis())
                    }
                    e.code() in 500..599 && serverAttempts < policy.serverErrorRetries -> {
                        serverAttempts++
                        delay(policy.serverErrorBackoff.toMillis())
                    }
                    else -> throw SyncException(e.toSyncError(), e)
                }
            } catch (e: IOException) {
                throw SyncException(SyncError.Network, e)
            } catch (e: SerializationException) {
                throw SyncException(SyncError.Unknown(e.message), e)
            }
        }
    }

    private fun Throwable.toSyncError(): SyncError = when (this) {
        is SyncException -> error
        is HttpException -> when (code()) {
            401 -> SyncError.Unauthorized
            403 -> SyncError.QuotaExceeded
            429 -> SyncError.RateLimited
            in 500..599 -> SyncError.Server(code())
            else -> SyncError.Unknown("HTTP ${code()}")
        }
        is IOException -> SyncError.Network
        else -> SyncError.Unknown(message)
    }
}
