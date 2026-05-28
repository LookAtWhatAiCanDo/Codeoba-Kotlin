package llc.lookatwhataicando.codeoba.core.manager

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import java.io.File
import java.security.MessageDigest

@Serializable
data class CacheEntry(
    val filePath: String,
    val lastModified: Long,
    val size: Long,
    val hash: String,
    val session: Session
)

@Serializable
data class SourceCache(
    val entries: List<CacheEntry>
)

object SessionCacheManager {
    @Volatile
    var isCacheEnabled: Boolean = true

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    // InMemory cache of CacheEntries by sourceId and filePath
    private val activeCaches = mutableMapOf<String, MutableMap<String, CacheEntry>>()
    // Keep track of which files/session IDs were seen during the current scan to delete orphans
    private val seenPaths = mutableMapOf<String, MutableSet<String>>()

    private fun getCacheDir(): File {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".codeoba/cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCacheFile(sourceId: String): File {
        return File(getCacheDir(), "cache_$sourceId.json")
    }

    private fun loadCache(sourceId: String): Map<String, CacheEntry> {
        val file = getCacheFile(sourceId)
        if (!file.exists()) return emptyMap()
        return try {
            val text = file.readText()
            val sourceCache = json.decodeFromString<SourceCache>(text)
            sourceCache.entries.associateBy { it.filePath }
        } catch (e: Exception) {
            log("SessionCacheManager: Failed to load cache for $sourceId, resetting.", e)
            emptyMap()
        }
    }

    private fun saveCache(sourceId: String, entries: Collection<CacheEntry>) {
        val file = getCacheFile(sourceId)
        try {
            val sourceCache = SourceCache(entries.toList())
            val text = json.encodeToString(SourceCache.serializer(), sourceCache)
            file.writeText(text)
        } catch (e: Exception) {
            log("SessionCacheManager: Failed to save cache for $sourceId.", e)
        }
    }

    fun startScan(sourceId: String) {
        if (!isCacheEnabled) return
        val cacheMap = loadCache(sourceId).toMutableMap()
        synchronized(activeCaches) {
            activeCaches[sourceId] = cacheMap
            seenPaths[sourceId] = mutableSetOf()
        }
    }

    fun getCachedSessionForFile(sourceId: String, filePath: String, lastModified: Long, size: Long): Session? {
        if (!isCacheEnabled) return null
        val entry = synchronized(activeCaches) {
            activeCaches[sourceId]?.get(filePath)
        } ?: return null

        if (entry.lastModified == lastModified && entry.size == size) {
            synchronized(activeCaches) {
                seenPaths[sourceId]?.add(filePath)
            }
            return entry.session
        }
        return null
    }

    fun getCachedSessionForDb(sourceId: String, filePath: String, hash: String, size: Long): Session? {
        if (!isCacheEnabled) return null
        val entry = synchronized(activeCaches) {
            activeCaches[sourceId]?.get(filePath)
        } ?: return null

        if (entry.hash == hash && entry.size == size) {
            synchronized(activeCaches) {
                seenPaths[sourceId]?.add(filePath)
            }
            return entry.session
        }
        return null
    }

    fun putCachedSession(sourceId: String, filePath: String, lastModified: Long, size: Long, hash: String, session: Session) {
        if (!isCacheEnabled) return
        val entry = CacheEntry(filePath, lastModified, size, hash, session)
        synchronized(activeCaches) {
            activeCaches[sourceId]?.put(filePath, entry)
            seenPaths[sourceId]?.add(filePath)
        }
    }

    fun endScan(sourceId: String) {
        if (!isCacheEnabled) return
        val entriesToSave = synchronized(activeCaches) {
            val cacheMap = activeCaches[sourceId] ?: return
            val seen = seenPaths[sourceId] ?: return
            
            // Clean up any cache entries that weren't seen during this scan (orphans)
            val orphans = cacheMap.keys - seen
            for (orphan in orphans) {
                cacheMap.remove(orphan)
            }
            cacheMap.values.toList()
        }

        saveCache(sourceId, entriesToSave)
        
        // Clear memory cache to release memory after scanning is complete
        synchronized(activeCaches) {
            activeCaches.remove(sourceId)
            seenPaths.remove(sourceId)
        }
    }

    fun calculateMd5(file: File): String {
        if (!file.exists() || !file.isFile) return ""
        return try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    md.update(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun calculateStringMd5(value: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(value.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
