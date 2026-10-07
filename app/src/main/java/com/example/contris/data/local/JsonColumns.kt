package com.example.contris.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Structured data stored as JSON text inside `countries` columns.
 * Kept separate from DTOs so API shape changes don't silently change the DB format.
 */
object JsonColumns {
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    inline fun <reified T> encode(value: T): String = json.encodeToString(value)

    inline fun <reified T> decodeOr(text: String?, fallback: T): T =
        if (text.isNullOrBlank()) fallback else runCatching { json.decodeFromString<T>(text) }.getOrDefault(fallback)
}

@Serializable
data class StoredNativeName(val common: String? = null, val official: String? = null)

@Serializable
data class StoredCapital(
    val name: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val primary: Boolean = false,
    val roles: List<String> = emptyList(),
)

@Serializable
data class StoredCurrency(val code: String? = null, val name: String? = null, val symbol: String? = null)

@Serializable
data class StoredLanguage(
    val name: String? = null,
    val nativeName: String? = null,
    val bcp47: String? = null,
    val iso6393: String? = null,
)
