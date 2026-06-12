package llc.lookatwhataicando.codeoba.core.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonUtils {
    private val json = Json { ignoreUnknownKeys = true }

    fun serializeList(list: List<String>): String {
        return json.encodeToString(list)
    }

    fun deserializeList(jsonStr: String): List<String> {
        if (jsonStr.isEmpty() || jsonStr == "[]") return emptyList()
        return try {
            json.decodeFromString<List<String>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
