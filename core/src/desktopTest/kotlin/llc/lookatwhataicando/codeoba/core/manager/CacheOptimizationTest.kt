package llc.lookatwhataicando.codeoba.core.manager

import kotlinx.coroutines.runBlocking
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import java.io.File
import kotlin.test.*

class CacheOptimizationTest {

    @Test
    fun testCachePruningAndCachingBehavior() = runBlocking {
        val tempFile = File.createTempFile("cache_opt_test_", ".jsonl")
        tempFile.deleteOnExit()

        tempFile.writeText("some session text content")

        val sourceId = "test_source"
        val filePath = tempFile.absolutePath

        // 1. Enable cache and start initial scan
        SessionCacheManager.isCacheEnabled = true
        SessionCacheManager.startScan(sourceId)

        val session = Session(
            id = "test_session_1",
            sourceId = sourceId,
            filePath = filePath,
            timestamp = 1000L,
            updatedAt = 2000L,
            cwd = "/test/cwd",
            threadName = "Test Thread",
            turns = listOf(Turn("turn_1", "Hello", "Hi")),
            isArchived = false
        )

        val md5 = SessionCacheManager.calculateMd5(tempFile)
        SessionCacheManager.putCachedSession(
            sourceId = sourceId,
            filePath = filePath,
            lastModified = tempFile.lastModified(),
            size = tempFile.length(),
            hash = md5,
            session = session
        )

        // Verify it was put into active cache
        val cached = SessionCacheManager.getCachedSessionForFile(
            sourceId = sourceId,
            filePath = filePath,
            lastModified = tempFile.lastModified(),
            size = tempFile.length()
        )
        assertNotNull(cached)
        assertEquals("test_session_1", cached.id)

        // End scan (saves test_session_1 to file and clears memory cache)
        SessionCacheManager.endScan(sourceId)

        // 2. Start a new scan and verify it loads from file cache
        SessionCacheManager.startScan(sourceId)

        val cachedOnNewScan = SessionCacheManager.getCachedSessionForFile(
            sourceId = sourceId,
            filePath = filePath,
            lastModified = tempFile.lastModified(),
            size = tempFile.length()
        )
        assertNotNull(cachedOnNewScan)
        assertEquals("test_session_1", cachedOnNewScan.id)

        // End scan (marks it seen, so it stays in cache file)
        SessionCacheManager.endScan(sourceId)

        // 3. Test orphan cleanup: start a scan, DO NOT check/get the session (simulating that the file is gone), and end scan.
        SessionCacheManager.startScan(sourceId)
        SessionCacheManager.endScan(sourceId) // Prunes the entry from cache since it was never marked seen

        // Start another scan and verify it is indeed gone/pruned
        SessionCacheManager.startScan(sourceId)
        val pruned = SessionCacheManager.getCachedSessionForFile(
            sourceId = sourceId,
            filePath = filePath,
            lastModified = tempFile.lastModified(),
            size = tempFile.length()
        )
        assertNull(pruned)
        SessionCacheManager.endScan(sourceId)
    }

    @Test
    fun testDbHashing() {
        val value = "{\"key\": \"val\"}"
        val hash = SessionCacheManager.calculateStringMd5(value)
        assertNotEquals("", hash)
        assertEquals(32, hash.length)

        val hash2 = SessionCacheManager.calculateStringMd5(value)
        assertEquals(hash, hash2)
    }
}
