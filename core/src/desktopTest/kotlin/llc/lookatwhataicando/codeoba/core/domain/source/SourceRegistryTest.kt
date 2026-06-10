package llc.lookatwhataicando.codeoba.core.domain.source

import llc.lookatwhataicando.codeoba.core.domain.model.Session
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceRegistryTest {

    private class MockSourceAdapter(
        override val id: String,
        override val displayName: String,
        private val installed: Boolean,
        private val available: Boolean
    ) : SourceAdapter {
        override fun isAvailable(): Boolean = available
        override fun isAppInstalled(): Boolean = installed
        override fun getDefaultLogPaths(): List<String> = emptyList()
        override fun getWatchPaths(): List<String> = emptyList()
        override suspend fun parseSession(filePath: String): Session? = null
        override suspend fun parseAllSessions(): List<Session> = emptyList()
    }

    @Test
    fun testSortingAndBisecting() {
        val registry = SourceRegistry()
        
        // Register adapters in arbitrary/unsorted order
        val cursor = MockSourceAdapter("cursor", "Cursor", installed = true, available = true)
        val claude = MockSourceAdapter("claude", "Claude Code", installed = true, available = true)
        val aider = MockSourceAdapter("aider", "Aider", installed = false, available = false)
        val copilot = MockSourceAdapter("copilot", "GitHub Copilot", installed = false, available = false)
        
        registry.register(cursor)
        registry.register(claude)
        registry.register(aider)
        registry.register(copilot)

        // Sorted: Installed first (Claude Code, Cursor), then Uninstalled (Aider, GitHub Copilot)
        // Within each group, sorted alphabetically.
        val all = registry.getAllAdapters()
        assertEquals(4, all.size)
        assertEquals("Claude Code", all[0].displayName)
        assertEquals("Cursor", all[1].displayName)
        assertEquals("Aider", all[2].displayName)
        assertEquals("GitHub Copilot", all[3].displayName)
    }

    @Test
    fun testActiveAdaptersSorting() {
        val registry = SourceRegistry()
        
        val cursor = MockSourceAdapter("cursor", "Cursor", installed = true, available = true)
        val claude = MockSourceAdapter("claude", "Claude Code", installed = true, available = true)
        val aider = MockSourceAdapter("aider", "Aider", installed = false, available = false)
        
        registry.register(cursor)
        registry.register(claude)
        registry.register(aider)

        val active = registry.getActiveAdapters()
        assertEquals(2, active.size)
        assertEquals("Claude Code", active[0].displayName)
        assertEquals("Cursor", active[1].displayName)
    }
}
