package llc.lookatwhataicando.codeoba.core.domain.search

import kotlinx.coroutines.runBlocking
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchEngineArchivalFilterTest {

    private val activeSession = Session(
        id = "session-active",
        sourceId = "claude",
        filePath = "/path/to/active.jsonl",
        timestamp = 1000L,
        updatedAt = 1000L,
        cwd = "/workspace",
        threadName = "Active Session",
        turns = listOf(
            Turn("1", "user message", "assistant response", 1000L)
        ),
        isArchived = false
    )

    private val archivedSession = Session(
        id = "session-archived",
        sourceId = "claude",
        filePath = "/path/to/archived.jsonl",
        timestamp = 2000L,
        updatedAt = 2000L,
        cwd = "/workspace",
        threadName = "Archived Session",
        turns = listOf(
            Turn("2", "user message", "assistant response", 2000L)
        ),
        isArchived = true
    )

    @Test
    fun testLexicalSearchEngineFilters() = runBlocking {
        val engine = LexicalSearchEngine()
        engine.updateIndex(listOf(activeSession, archivedSession))

        // 1. ALL filter returns both
        val allResults = engine.search("message", SearchFilter(archivalFilter = ArchivalFilter.ALL))
        assertEquals(2, allResults.size)

        // 2. ACTIVE filter returns only active
        val activeResults = engine.search("message", SearchFilter(archivalFilter = ArchivalFilter.ACTIVE))
        assertEquals(1, activeResults.size)
        assertEquals("session-active", activeResults[0].session.id)

        // 3. ARCHIVED filter returns only archived
        val archivedResults = engine.search("message", SearchFilter(archivalFilter = ArchivalFilter.ARCHIVED))
        assertEquals(1, archivedResults.size)
        assertEquals("session-archived", archivedResults[0].session.id)
    }

    @Test
    fun testSemanticSearchEngineFilters() = runBlocking {
        val embedder = HashSemanticEmbedder()
        val engine = SemanticSearchEngine(embedder, similarityThreshold = 0.1f)
        engine.updateIndex(listOf(activeSession, archivedSession))

        // 1. ALL filter returns both
        val allResults = engine.search("message", SearchFilter(archivalFilter = ArchivalFilter.ALL))
        assertEquals(2, allResults.size)

        // 2. ACTIVE filter returns only active
        val activeResults = engine.search("message", SearchFilter(archivalFilter = ArchivalFilter.ACTIVE))
        assertEquals(1, activeResults.size)
        assertEquals("session-active", activeResults[0].session.id)

        // 3. ARCHIVED filter returns only archived
        val archivedResults = engine.search("message", SearchFilter(archivalFilter = ArchivalFilter.ARCHIVED))
        assertEquals(1, archivedResults.size)
        assertEquals("session-archived", archivedResults[0].session.id)
    }
}
