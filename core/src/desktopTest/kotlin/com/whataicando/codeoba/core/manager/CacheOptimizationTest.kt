package com.whataicando.codeoba.core.manager

import kotlinx.coroutines.runBlocking
import com.whataicando.codeoba.core.domain.model.Session
import com.whataicando.codeoba.core.domain.model.Turn
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    @Test
    fun testRefreshCachedSession() = runBlocking {
        val tempFile = File.createTempFile("cache_refresh_test_", ".jsonl")
        tempFile.deleteOnExit()
        tempFile.writeText("some session text content")

        var titleOverride = "Initial Thread Name"
        var archiveOverride = false

        val source = object : com.whataicando.codeoba.core.source.DesktopSourceAdapter() {
            override val id: String = "test_refresh_source"
            override val displayName: String = "Test Refresh Source"

            override fun getBaseDir(): File = tempFile.parentFile

            override suspend fun parseAllSessions(): List<Session> = emptyList()

            override fun refreshCachedSession(session: Session): Session {
                if (session.threadName != titleOverride || session.isArchived != archiveOverride) {
                    return session.copy(threadName = titleOverride, isArchived = archiveOverride)
                }
                return session
            }

            override suspend fun parseSessionContent(file: File): Session? {
                return Session(
                    id = "test_session_id",
                    sourceId = id,
                    filePath = file.absolutePath,
                    timestamp = 1000L,
                    updatedAt = 2000L,
                    cwd = null,
                    threadName = titleOverride,
                    turns = emptyList(),
                    isArchived = archiveOverride
                )
            }
        }

        SessionCacheManager.isCacheEnabled = true
        SessionCacheManager.startScan(source.id)

        // 1. Initial Parse (cache miss)
        val s1 = source.parseSession(tempFile.absolutePath)
        assertNotNull(s1)
        assertEquals("Initial Thread Name", s1.threadName)
        assertEquals(false, s1.isArchived)

        // 2. Change metadata properties (simulating user renaming/archiving session)
        titleOverride = "New Thread Name"
        archiveOverride = true

        // 3. Query again (cache hit, but should invoke refreshCachedSession and update)
        val s2 = source.parseSession(tempFile.absolutePath)
        assertNotNull(s2)
        assertEquals("New Thread Name", s2.threadName)
        assertEquals(true, s2.isArchived)

        // Clean up
        SessionCacheManager.endScan(source.id)
    }

    @Test
    fun testCacheBypassesSummarizationOnHit() = runBlocking {
        val tempFile = File.createTempFile("cache_summary_test_", ".jsonl")
        tempFile.deleteOnExit()
        tempFile.writeText("some session text content")

        val source = object : com.whataicando.codeoba.core.source.DesktopSourceAdapter() {
            override val id: String = "test_summary_source"
            override val displayName: String = "Test Summary Source"
            override fun getBaseDir(): File = tempFile.parentFile
            override suspend fun parseAllSessions(): List<Session> = emptyList()
            override suspend fun parseSessionContent(file: File): Session? {
                return Session(
                    id = "test_session_id",
                    sourceId = id,
                    filePath = file.absolutePath,
                    timestamp = 1000L,
                    updatedAt = 2000L,
                    cwd = null,
                    threadName = "Thread",
                    turns = listOf(Turn("turn_1", "Hello", "Hi")),
                    isArchived = false
                )
            }
        }

        // Enable summarizing mode
        com.whataicando.codeoba.core.domain.parser.LogParserFactory.setParserMode(
            com.whataicando.codeoba.core.domain.parser.ParserMode.SUMMARIZING
        )

        SessionCacheManager.isCacheEnabled = true
        SessionCacheManager.startScan(source.id)

        try {
            // First parse: cache miss, runs parser/summarizer, writes finalized session with summary to cache
            val s1 = source.parseSession(tempFile.absolutePath)
            assertNotNull(s1)
            assertNotNull(s1.summary)
            assertEquals("Parsed 1 dialogue exchanges from source 'test_summary_source'", s1.summary?.keyActions?.first())

            // Modify the summary in the cache entry directly to verify hits use cached summary without re-inference
            val cachedEntry = SessionCacheManager.getCachedSessionForFile(
                source.id, tempFile.absolutePath, tempFile.lastModified(), tempFile.length()
            )
            assertNotNull(cachedEntry)
            assertNotNull(cachedEntry.summary)

            val mockedSummary = cachedEntry.summary!!.copy(keyActions = listOf("MOCKED SUMMARY DETECTED"))
            val modifiedSession = cachedEntry.copy(summary = mockedSummary)
            
            // Put modified session back in cache
            val md5 = SessionCacheManager.calculateMd5(tempFile)
            SessionCacheManager.putCachedSession(
                source.id, tempFile.absolutePath, tempFile.lastModified(), tempFile.length(), md5, modifiedSession
            )

            // Second parse: should be cache hit, returning the modified session directly without invoking inference
            val s2 = source.parseSession(tempFile.absolutePath)
            assertNotNull(s2)
            assertNotNull(s2.summary)
            assertEquals("MOCKED SUMMARY DETECTED", s2.summary?.keyActions?.first())

        } finally {
            // Restore default parsing mode
            com.whataicando.codeoba.core.domain.parser.LogParserFactory.setParserMode(
                com.whataicando.codeoba.core.domain.parser.ParserMode.STANDARD
            )
            SessionCacheManager.endScan(source.id)
        }
    }
}
