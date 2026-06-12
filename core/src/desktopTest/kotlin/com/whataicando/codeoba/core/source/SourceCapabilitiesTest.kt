package com.whataicando.codeoba.core.source

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceCapabilitiesTest {

    private fun withMockUserHome(block: (File) -> Unit) {
        val originalUserHome = System.getProperty("user.home")
        val tempDir = File(System.getProperty("java.io.tmpdir"), "codeoba_test_home_" + System.currentTimeMillis())
        tempDir.mkdirs()
        System.setProperty("user.home", tempDir.absolutePath)
        try {
            block(tempDir)
        } finally {
            System.setProperty("user.home", originalUserHome)
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testClaudeSourceAppInstalledAndCleanup() = withMockUserHome { home ->
        val source = DesktopClaudeSource()
        // Initially, source data is not available
        assertFalse(source.isAvailable())
        assertEquals(listOf(File(home, ".claude/projects").absolutePath), source.getDataPathsToDelete())

        // Create a mock jsonl file in the projects directory to simulate files present
        val projectsDir = File(home, ".claude/projects")
        projectsDir.mkdirs()
        val mockProjectFile = File(projectsDir, "test.jsonl")
        mockProjectFile.writeText("{\"type\":\"user\",\"timestamp\":\"2026-05-20T02:00:00Z\",\"message\":{\"role\":\"user\",\"content\":\"Hello\"}}")

        // Now, it should detect app as installed (fallback mode) and source as available
        assertTrue(source.isAppInstalled())
        assertTrue(source.isAvailable())

        // Verify deleteDataPaths deletes the projects directory
        assertTrue(source.deleteDataPaths())
        assertFalse(projectsDir.exists())
    }

    @Test
    fun testCodexSourceAppInstalledAndCleanup() = withMockUserHome { home ->
        val source = DesktopCodexSource()
        // Initially, source data is not available
        assertFalse(source.isAvailable())
        assertEquals(listOf(File(home, ".codex").absolutePath), source.getDataPathsToDelete())

        // Create a mock session file
        val sessionsDir = File(home, ".codex/sessions")
        sessionsDir.mkdirs()
        val mockFile = File(sessionsDir, "rollout-123.jsonl")
        mockFile.writeText("{\"timestamp\":\"2026-05-20T02:00:00Z\",\"type\":\"session_meta\",\"payload\":{\"id\":\"codex123\"}}")

        // Should detect as installed (fallback mode) and source as available
        assertTrue(source.isAppInstalled())
        assertTrue(source.isAvailable())

        // Verify cleanup
        assertTrue(source.deleteDataPaths())
        assertFalse(File(home, ".codex").exists())
    }

    @Test
    fun testAiderSourceAppInstalledAndCleanup() = withMockUserHome { home ->
        val source = DesktopAiderSource()
        assertFalse(source.isAvailable())
        assertTrue(source.getDataPathsToDelete().isEmpty())

        // Create Dev directory and a mock aider history file inside it
        val devDir = File(home, "Dev/project")
        devDir.mkdirs()
        val historyFile = File(devDir, ".aider.chat.history.md")
        historyFile.writeText(
            """
            # Aider chat started at 2026-05-20 12:00:00
            
            #### User:
            Hello Aider
            
            #### Assistant:
            Hello
            """.trimIndent()
        )

        // Trigger parseAllSessions so Aider scans and identifies active watch paths
        val list = kotlinx.coroutines.runBlocking { source.parseAllSessions() }
        assertEquals(1, list.size)

        // Now it should be available and count as installed
        assertTrue(source.isAvailable())
        assertTrue(source.isAppInstalled())
        assertEquals(listOf(historyFile.absolutePath), source.getDataPathsToDelete())

        // Verify deleteDataPaths cleans up the history files
        assertTrue(source.deleteDataPaths())
        assertFalse(historyFile.exists())
        assertTrue(source.getDataPathsToDelete().isEmpty())
    }

    @Test
    fun testCursorSourceCleanup() = withMockUserHome { home ->
        val source = DesktopCursorSource()
        assertFalse(source.isAvailable())

        // Create state.vscdb and workspaceStorage
        val dbFile = File(home, "Library/Application Support/Cursor/User/globalStorage/state.vscdb")
        dbFile.parentFile.mkdirs()
        dbFile.writeText("mock db content")

        val wsDir = File(home, "Library/Application Support/Cursor/User/workspaceStorage")
        wsDir.mkdirs()
        val tempWsFile = File(wsDir, "workspace123")
        tempWsFile.mkdirs()

        assertTrue(source.isAvailable())
        assertEquals(
            listOf(dbFile.absolutePath, wsDir.absolutePath),
            source.getDataPathsToDelete()
        )

        // Clean up
        assertTrue(source.deleteDataPaths())
        assertFalse(dbFile.exists())
        assertFalse(wsDir.exists())
    }

    @Test
    fun testAntigravitySourceAppInstalledAndCleanup() = withMockUserHome { home ->
        val source = DesktopAntigravitySource()
        assertEquals(listOf(File(home, ".gemini/antigravity/brain").absolutePath), source.getDataPathsToDelete())

        // Create mock brain and app folders
        val brainDir = File(home, ".gemini/antigravity/brain")
        brainDir.mkdirs()

        // It should be available since logs exist
        assertTrue(source.isAvailable())

        // Verify deleteDataPaths deletes the brain directory
        assertTrue(source.deleteDataPaths())
        assertFalse(brainDir.exists())
    }

    @Test
    fun testCopilotSourceAppInstalledAndCleanup() = withMockUserHome { home ->
        val source = DesktopCopilotSource()
        assertEquals(listOf(File(home, ".copilot/session-state").absolutePath), source.getDataPathsToDelete())

        val brainDir = File(home, ".copilot/session-state")
        brainDir.mkdirs()

        assertTrue(source.isAvailable())

        assertTrue(source.deleteDataPaths())
        assertFalse(brainDir.exists())
    }
}
