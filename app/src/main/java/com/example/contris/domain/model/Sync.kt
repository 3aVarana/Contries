package com.example.contris.domain.model

import java.time.Instant

sealed interface SyncError {
    data object Network : SyncError
    data object Unauthorized : SyncError
    data object QuotaExceeded : SyncError
    data object RateLimited : SyncError
    data class Server(val code: Int) : SyncError
    data class Unknown(val message: String?) : SyncError
}

sealed interface SyncStatus {
    val lastSync: Instant?

    data class Idle(override val lastSync: Instant?, val total: Int) : SyncStatus
    data class Syncing(val page: Int, val totalPages: Int, override val lastSync: Instant?) : SyncStatus
    data class Failed(val error: SyncError, override val lastSync: Instant?) : SyncStatus
}

class SyncException(val error: SyncError, cause: Throwable? = null) : Exception(error.toString(), cause)
