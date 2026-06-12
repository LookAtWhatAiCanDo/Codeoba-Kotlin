package com.whataicando.codeoba.core.manager

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.whataicando.codeoba.core.domain.search.EmbeddingCache
import com.whataicando.codeoba.core.util.Logger.log
import java.io.File
import java.security.MessageDigest

@Serializable
data class SerializedEmbeddingCache(
    val embeddings: Map<String, FloatArray>
)

object EmbeddingCacheManager : EmbeddingCache {
    private val cacheMap = mutableMapOf<String, FloatArray>()
    private var isModified = false

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private fun getCacheDir(): File {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".codeoba/cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCacheFile(): File {
        return File(getCacheDir(), "embeddings_cache.json")
    }

    fun loadCache() {
        val file = getCacheFile()
        if (!file.exists()) return
        try {
            val start = System.currentTimeMillis()
            val text = file.readText()
            val cache = json.decodeFromString<SerializedEmbeddingCache>(text)
            synchronized(cacheMap) {
                cacheMap.clear()
                cacheMap.putAll(cache.embeddings)
            }
            log("EmbeddingCacheManager: Loaded ${cache.embeddings.size} cached embeddings in ${System.currentTimeMillis() - start}ms.")
        } catch (e: Exception) {
            log("EmbeddingCacheManager: Failed to load embedding cache, resetting.", e)
        }
    }

    fun saveCache() {
        if (!isModified) return
        val file = getCacheFile()
        try {
            val start = System.currentTimeMillis()
            val entries = synchronized(cacheMap) { cacheMap.toMap() }
            val cache = SerializedEmbeddingCache(entries)
            val text = json.encodeToString(SerializedEmbeddingCache.serializer(), cache)
            file.writeText(text)
            isModified = false
            log("EmbeddingCacheManager: Saved ${entries.size} embeddings to cache file in ${System.currentTimeMillis() - start}ms.")
        } catch (e: Exception) {
            log("EmbeddingCacheManager: Failed to save embedding cache.", e)
        }
    }

    override fun get(text: String): FloatArray? {
        val hash = calculateStringMd5(text)
        return synchronized(cacheMap) {
            cacheMap[hash]?.clone()
        }
    }

    override fun put(text: String, vector: FloatArray) {
        val hash = calculateStringMd5(text)
        synchronized(cacheMap) {
            if (!cacheMap.containsKey(hash)) {
                cacheMap[hash] = vector.clone()
                isModified = true
            }
        }
    }

    private fun calculateStringMd5(value: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(value.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            value.hashCode().toString()
        }
    }
}
