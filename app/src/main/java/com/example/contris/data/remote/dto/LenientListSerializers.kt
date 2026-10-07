package com.example.contris.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * The API documents `currencies` as an object while live responses have shown an array.
 * These serializers accept both: an object `{ "CAD": { "name": … } }` is flattened into a list and the
 * key is injected under [keyField] when the item does not carry it already. `null` becomes an empty list.
 */
abstract class ObjectOrArraySerializer<T>(itemSerializer: KSerializer<T>, private val keyField: String) :
    JsonTransformingSerializer<List<T>>(ListSerializer(itemSerializer)) {

    override fun transformDeserialize(element: JsonElement): JsonElement = when (element) {
        is JsonArray -> element
        is JsonObject -> JsonArray(
            element.entries.mapNotNull { (key, value) ->
                when (value) {
                    is JsonObject -> if (value.containsKey(keyField)) value else JsonObject(value + (keyField to JsonPrimitive(key)))
                    is JsonNull -> null
                    else -> JsonObject(mapOf(keyField to JsonPrimitive(key)))
                }
            },
        )
        else -> JsonArray(emptyList())
    }
}

object CurrenciesSerializer : ObjectOrArraySerializer<CurrencyDto>(CurrencyDto.serializer(), keyField = "code")

object LanguagesSerializer : ObjectOrArraySerializer<LanguageDto>(LanguageDto.serializer(), keyField = "iso639_3")
