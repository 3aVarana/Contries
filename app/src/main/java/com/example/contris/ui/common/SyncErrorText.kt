package com.example.contris.ui.common

import com.example.contris.domain.model.SyncError

fun SyncError.toUiText(): UiText = UiText.Dynamic(
    when (this) {
        SyncError.Network -> "No internet connection. Showing cached data."
        SyncError.Unauthorized -> "The API key was rejected. Check REST_COUNTRIES_API_KEY in local.properties."
        SyncError.QuotaExceeded -> "Monthly API quota exhausted — showing cached data."
        SyncError.RateLimited -> "Too many refreshes. Please wait a minute and try again."
        is SyncError.Server -> "The countries service returned an error ($code). Try again later."
        is SyncError.Unknown -> message?.let { "Couldn't refresh: $it" } ?: "Couldn't refresh countries."
    },
)
