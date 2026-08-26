package vn.loi.learning.infrastructure.persistence.sqlite

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SqliteJsonUtils {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    inline fun <reified T> encode(value: T): String =
        json.encodeToString(value)

    inline fun <reified T> decode(raw: String): T =
        json.decodeFromString(raw)

    inline fun <reified T> decodeOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            default
        }
    }
}
