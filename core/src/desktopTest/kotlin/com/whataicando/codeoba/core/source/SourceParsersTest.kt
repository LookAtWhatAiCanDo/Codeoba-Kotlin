package com.whataicando.codeoba.core.source

import kotlinx.coroutines.runBlocking
import com.whataicando.codeoba.core.domain.search.LexicalSearchEngine
import com.whataicando.codeoba.core.domain.search.SearchFilter
import com.whataicando.codeoba.core.manager.SessionCacheManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SourceParsersTest {

    @Test
    fun testClaudeSourceParsing() = runBlocking {
        val tempFile = File.createTempFile("claude_test_", ".jsonl")
        tempFile.deleteOnExit()

        tempFile.writeText(
            """
            {"type":"user","timestamp":"2026-05-20T02:00:00Z","message":{"role":"user","content":"Hello Claude"},"sessionId":"session123","cwd":"/path/to/project","slug":"test-session"}
            {"type":"assistant","timestamp":"2026-05-20T02:01:00Z","message":{"role":"assistant","content":[{"type":"text","text":"Hello User"}]}}
            """.trimIndent()
        )

        val source = DesktopClaudeSource()
        val session = source.parseSession(tempFile.absolutePath)

        assertNotNull(session)
        assertEquals("session123", session.id)
        assertEquals("/path/to/project", session.cwd)
        assertEquals("Test session", session.threadName)
        assertEquals(1, session.turns.size)
        assertEquals("Hello Claude", session.turns[0].userMessage)
        assertEquals("Hello User", session.turns[0].assistantMessage)
    }

    @Test
    fun testClaudeCompactionParsing() = runBlocking {
        val tempFile = File.createTempFile("claude_compaction_", ".jsonl")
        tempFile.deleteOnExit()

        tempFile.writeText(
            """
            {"type":"user","timestamp":"2026-05-20T02:00:00Z","message":{"role":"user","content":"Hello Claude"},"sessionId":"sessionCompact","cwd":"/path/to/project","slug":"test-session"}
            {"parentUuid":null,"logicalParentUuid":"123","isSidechain":false,"type":"system","subtype":"compact_boundary","content":"Conversation compacted","isMeta":false,"timestamp":"2026-05-20T02:00:30Z","uuid":"abc","level":"info","compactMetadata":{"trigger":"auto","preTokens":1000,"postTokens":100,"durationMs":5000},"sessionId":"sessionCompact"}
            {"type":"assistant","timestamp":"2026-05-20T02:01:00Z","message":{"role":"assistant","content":[{"type":"text","text":"Hello User"}]}}
            """.trimIndent()
        )

        val source = DesktopClaudeSource()
        val session = source.parseSession(tempFile.absolutePath)

        assertNotNull(session)
        assertEquals("sessionCompact", session.id)
        assertEquals(1, session.turns.size)
        assertEquals("Hello Claude", session.turns[0].userMessage)
        assertEquals("Hello User", session.turns[0].assistantMessage)
        assertEquals("true", session.turns[0].extraData["isCompaction"])
        assertEquals("5000", session.turns[0].extraData["compactionTimeMs"])
    }


    @Test
    fun testAntigravitySourceParsing() = runBlocking {
        val tempFile = File.createTempFile("antigravity_test_", ".jsonl")
        tempFile.deleteOnExit()

        tempFile.writeText(
            """
            {"step_index":0,"source":"USER_EXPLICIT","type":"USER_INPUT","status":"DONE","created_at":"2026-05-20T02:00:00Z","content":"<USER_REQUEST>Hello Antigravity</USER_REQUEST>"}
            {"step_index":1,"source":"MODEL","type":"PLANNER_RESPONSE","status":"DONE","created_at":"2026-05-20T02:01:00Z","content":"Hello back"}
            {"step_index":2,"source":"USER_EXPLICIT","type":"USER_INPUT","status":"DONE","created_at":"2026-05-20T02:02:00Z","content":"<USER_REQUEST>Another query</USER_REQUEST><USER_SETTINGS_CHANGE>\nThe user changed setting `Model Selection` from Gemini 3.5 Flash (High) to Claude Sonnet 4.6 (Thinking).\n</USER_SETTINGS_CHANGE>"}
            {"step_index":3,"source":"MODEL","type":"PLANNER_RESPONSE","status":"DONE","created_at":"2026-05-20T02:03:00Z","content":"Sure"}
            {"step_index":4,"source":"MODEL","type":"RUN_COMMAND","status":"DONE","created_at":"2026-05-20T02:04:00Z","content":"Running ls","tool_calls":[{"name":"run_command","args":{"CommandLine":"\"ls -la\"","Cwd":"\"/Users/pv/Dev/GitHub/LookAtWhatAiCanDo/Codeoba2\""}}]}
            """.trimIndent()
        )

        val source = DesktopAntigravitySource()
        val session = source.parseSession(tempFile.absolutePath)

        assertNotNull(session)
        assertEquals(2, session.turns.size)
        assertEquals("Hello Antigravity", session.turns[0].userMessage)
        assertEquals("Hello back", session.turns[0].assistantMessage)
        assertEquals("Unknown", session.turns[0].extraData["model"])
        assertEquals("60000", session.turns[0].extraData["computeTimeMs"])

        assertEquals("Another query", session.turns[1].userMessage)
        assertEquals("Sure\n\n[[[TOOL:RUN_COMMAND|⚡ Run Command: ls -la|1779242640000]]]\nRunning ls\n[[[/TOOL]]]", session.turns[1].assistantMessage)
        assertEquals("Claude Sonnet 4.6 (Thinking)", session.turns[1].extraData["model"])
        assertEquals("120000", session.turns[1].extraData["computeTimeMs"])
        assertEquals("/Users/pv/Dev/GitHub/LookAtWhatAiCanDo/Codeoba2", session.cwd)
    }

    @Test
    fun testAntigravitySystemAndErrorParsing() = runBlocking {
        val tempFile = File.createTempFile("antigravity_sys_err_", ".jsonl")
        tempFile.deleteOnExit()

        tempFile.writeText(
            """
            {"step_index":0,"source":"USER_EXPLICIT","type":"USER_INPUT","status":"DONE","created_at":"2026-05-20T02:00:00Z","content":"<USER_REQUEST>Start</USER_REQUEST>"}
            {"step_index":1,"source":"SYSTEM","type":"SYSTEM_MESSAGE","status":"DONE","created_at":"2026-05-20T02:01:00Z","content":"<SYSTEM_MESSAGE>Compilation complete</SYSTEM_MESSAGE>"}
            {"step_index":2,"source":"SYSTEM","type":"ERROR_MESSAGE","status":"DONE","created_at":"2026-05-20T02:02:00Z","content":"Command failed with status 1"}
            {"step_index":3,"source":"MODEL","type":"PLANNER_RESPONSE","status":"DONE","created_at":"2026-05-20T02:03:00Z","content":"Done"}
            """.trimIndent()
        )

        val source = DesktopAntigravitySource()
        val session = source.parseSession(tempFile.absolutePath)

        assertNotNull(session)
        assertEquals(1, session.turns.size)
        assertEquals("Start", session.turns[0].userMessage)
        
        val expectedAssistant = """
            [[[TOOL:SYSTEM_MESSAGE|⚙️ System Message|1779242460000]]]
            Compilation complete
            [[[/TOOL]]]

            [[[TOOL:ERROR_MESSAGE|❌ Error|1779242520000]]]
            Command failed with status 1
            [[[/TOOL]]]

            Done
        """.trimIndent()
        assertEquals(expectedAssistant, session.turns[0].assistantMessage)
    }

    @Test
    fun testCodexSourceParsing() = runBlocking {
        val tempFile = File.createTempFile("rollout-codex_", ".jsonl")
        tempFile.deleteOnExit()

        tempFile.writeText(
            """
            {"timestamp":"2026-05-20T02:00:00Z","type":"session_meta","payload":{"id":"codex123","timestamp":"2026-05-20T02:00:00Z","cwd":"/path/to/codex"}}
            {"timestamp":"2026-05-20T02:01:00Z","type":"response_item","payload":{"role":"user","content":[{"text":"Hi Codex"}]}}
            {"timestamp":"2026-05-20T02:02:00Z","type":"response_item","payload":{"role":"assistant","content":[{"text":"Hi human"}]}}
            """.trimIndent()
        )

        val source = DesktopCodexSource()
        val session = source.parseSession(tempFile.absolutePath)

        assertNotNull(session)
        assertEquals("codex123", session.id)
        assertEquals("/path/to/codex", session.cwd)
        assertEquals(1, session.turns.size)
        assertEquals("Hi Codex", session.turns[0].userMessage)
        assertEquals("Hi human", session.turns[0].assistantMessage)
    }


    @Test
    fun testLexicalSearchEngine() = runBlocking {
        val engine = LexicalSearchEngine()
        val claudeSource = DesktopClaudeSource()
        val tempFile = File.createTempFile("claude_test_", ".jsonl")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            {"type":"user","timestamp":"2026-05-20T02:00:00Z","message":{"role":"user","content":"How do I write a Kotlin test?"},"sessionId":"kotlin123","cwd":"/path","slug":"kotlin-test"}
            {"type":"assistant","timestamp":"2026-05-20T02:01:00Z","message":{"role":"assistant","content":[{"type":"text","text":"Use the @Test annotation."}]}}
            """.trimIndent()
        )

        val session = claudeSource.parseSession(tempFile.absolutePath)
        assertNotNull(session)

        engine.updateIndex(listOf(session))

        val results = engine.search("Kotlin annotation")
        assertEquals(1, results.size)
        assertEquals("kotlin123", results[0].session.id)
        assertEquals(listOf(0), results[0].matchedTurnIndexes)
    }

    @Test
    fun testLexicalSearchEngineModifiers() = runBlocking {
        val engine = LexicalSearchEngine()
        val claudeSource = DesktopClaudeSource()
        val tempFile = File.createTempFile("claude_test_", ".jsonl")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            {"type":"user","timestamp":"2026-05-20T02:00:00Z","message":{"role":"user","content":"How do I write a Kotlin test?"},"sessionId":"kotlin123","cwd":"/path","slug":"kotlin-test"}
            {"type":"assistant","timestamp":"2026-05-20T02:01:00Z","message":{"role":"assistant","content":[{"type":"text","text":"Use the @Test annotation. Kotlin is great!"}]}}
            """.trimIndent()
        )

        val session = claudeSource.parseSession(tempFile.absolutePath)
        assertNotNull(session)
        engine.updateIndex(listOf(session))

        // 1. Case insensitive (default) matches "kotlin" and "Kotlin"
        val filterDefault = SearchFilter(matchCase = false)
        val resDefault = engine.search("kotlin", filterDefault)
        assertEquals(1, resDefault.size)

        // 2. Case sensitive does NOT match "kotlin" for "Kotlin"
        val filterCaseSensitive = SearchFilter(matchCase = true)
        val resCaseSensitive = engine.search("kotlin", filterCaseSensitive)
        assertEquals(0, resCaseSensitive.size)

        // 3. Whole word "Kot" does not match "Kotlin"
        val filterWholeWord = SearchFilter(wholeWord = true)
        val resWholeWord = engine.search("Kot", filterWholeWord)
        assertEquals(0, resWholeWord.size)

        // 4. Whole word "Kotlin" matches "Kotlin"
        val resWholeWordMatch = engine.search("Kotlin", filterWholeWord)
        assertEquals(1, resWholeWordMatch.size)

        // 5. Regex "K.t.*n" matches "Kotlin"
        val filterRegex = SearchFilter(useRegex = true)
        val resRegex = engine.search("K.t.*n", filterRegex)
        assertEquals(1, resRegex.size)
    }

    private fun encodeVarint(value: Long): ByteArray {
        val list = mutableListOf<Byte>()
        var temp = value
        while (true) {
            if ((temp and 0x7F.inv()) == 0L) {
                list.add(temp.toByte())
                break
            } else {
                list.add(((temp and 0x7F) or 0x80).toByte())
                temp = temp ushr 7
            }
        }
        return list.toByteArray()
    }

    private fun encodeLengthDelimited(fieldNumber: Int, bytes: ByteArray): ByteArray {
        val tag = (fieldNumber shl 3) or 2
        return encodeVarint(tag.toLong()) + encodeVarint(bytes.size.toLong()) + bytes
    }

    @Test
    fun testAntigravityProtobufWireFormatTitleResolution() = runBlocking {
        val originalUserHome = System.getProperty("user.home")
        val tempDir = java.nio.file.Files.createTempDirectory("antigravity_test_home_").toFile()
        tempDir.deleteOnExit()

        try {
            System.setProperty("user.home", tempDir.absolutePath)

            // Setup directories
            val geminiDir = File(tempDir, ".gemini/antigravity")
            geminiDir.mkdirs()
            val brainDir = File(geminiDir, "brain")
            val sessionDir = File(brainDir, "session-12345/.system_generated/logs")
            sessionDir.mkdirs()

            // Construct initial mock protobuf file
            val uuidBytes = "session-12345".toByteArray(kotlin.text.Charsets.UTF_8)
            val uuidField = encodeLengthDelimited(1, uuidBytes)

            val titleBytes = "Exploring Quantum Physics".toByteArray(kotlin.text.Charsets.UTF_8)
            val titleField = encodeLengthDelimited(1, titleBytes)
            val infoField = encodeLengthDelimited(2, titleField)
            val entryField = encodeLengthDelimited(1, uuidField + infoField)

            val pbFile = File(geminiDir, "agyhub_summaries_proto.pb")
            pbFile.writeBytes(entryField)
            pbFile.setLastModified(System.currentTimeMillis() - 10000)

            // Write transcript file
            val transcriptFile = File(sessionDir, "transcript.jsonl")
            transcriptFile.writeText(
                """
                {"step_index":0,"source":"USER_EXPLICIT","type":"USER_INPUT","status":"DONE","created_at":"2026-05-20T02:00:00Z","content":"<USER_REQUEST>Explain quantum physics</USER_REQUEST>"}
                {"step_index":1,"source":"MODEL","type":"PLANNER_RESPONSE","status":"DONE","created_at":"2026-05-20T02:01:00Z","content":"Quantum physics is..."}
                """.trimIndent()
            )

            val source = DesktopAntigravitySource()
            val sessions = source.parseAllSessions()

            assertEquals(1, sessions.size)
            assertEquals("session-12345", sessions[0].id)
            assertEquals("Exploring Quantum Physics", sessions[0].threadName)

            // Now, update the title in the protobuf file and set a newer modified timestamp
            val updatedTitleBytes = "Quantum Mechanics Advanced".toByteArray(kotlin.text.Charsets.UTF_8)
            val updatedTitleField = encodeLengthDelimited(1, updatedTitleBytes)
            val updatedInfoField = encodeLengthDelimited(2, updatedTitleField)
            val updatedEntryField = encodeLengthDelimited(1, uuidField + updatedInfoField)

            pbFile.writeBytes(updatedEntryField)
            pbFile.setLastModified(System.currentTimeMillis())

            // Re-parse the session and verify that the new title is picked up
            val updatedSession = source.parseSession(transcriptFile.absolutePath)
            assertNotNull(updatedSession)
            assertEquals("Quantum Mechanics Advanced", updatedSession.threadName)
        } finally {
            System.setProperty("user.home", originalUserHome)
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testClaudePlanTitleResolution() = runBlocking {
        val originalUserHome = System.getProperty("user.home")
        val tempDir = java.nio.file.Files.createTempDirectory("claude_test_home_").toFile()
        tempDir.deleteOnExit()

        try {
            System.setProperty("user.home", tempDir.absolutePath)

            // Setup directories
            val claudeDir = File(tempDir, ".claude")
            val plansDir = File(claudeDir, "plans")
            val projectsDir = File(claudeDir, "projects")
            plansDir.mkdirs()
            projectsDir.mkdirs()

            // 1. Create a session that has a plan file with "Plan: ..."
            val planFile1 = File(plansDir, "test-slug-1.md")
            planFile1.writeText("# Plan: Clean up UI layout\nSome plan details.")

            val sessionFile1 = File(projectsDir, "session-1.jsonl")
            sessionFile1.writeText(
                """
                {"type":"user","timestamp":"2026-05-20T02:00:00Z","message":{"role":"user","content":"Update UI layout"},"sessionId":"session-1","slug":"test-slug-1"}
                """.trimIndent()
            )

            // 2. Create a session that has a plan file with "Goal: ..."
            val planFile2 = File(plansDir, "test-slug-2.md")
            planFile2.writeText("# Goal: Optimize database indexes\nSome goal details.")

            val sessionFile2 = File(projectsDir, "session-2.jsonl")
            sessionFile2.writeText(
                """
                {"type":"user","timestamp":"2026-05-20T02:00:00Z","message":{"role":"user","content":"Optimize db"},"sessionId":"session-2","slug":"test-slug-2"}
                """.trimIndent()
            )

            // 3. Create a session that has a plan file with a plain title
            val planFile3 = File(plansDir, "test-slug-3.md")
            planFile3.writeText("# Plain header title\nSome other details.")

            val sessionFile3 = File(projectsDir, "session-3.jsonl")
            sessionFile3.writeText(
                """
                {"type":"user","timestamp":"2026-05-20T02:00:00Z","message":{"role":"user","content":"Plain header"},"sessionId":"session-3","slug":"test-slug-3"}
                """.trimIndent()
            )

            // 4. Create a session without a plan file (fallback to formatted slug)
            val sessionFile4 = File(projectsDir, "session-4.jsonl")
            sessionFile4.writeText(
                """
                {"type":"user","timestamp":"2026-05-20T02:00:00Z","message":{"role":"user","content":"No plan"},"sessionId":"session-4","slug":"another-custom-slug"}
                """.trimIndent()
            )

            val source = DesktopClaudeSource()
            val sessions = source.parseAllSessions().sortedBy { it.id }

            assertEquals(4, sessions.size)
            assertEquals("Clean up UI layout", sessions[0].threadName)
            assertEquals("Optimize database indexes", sessions[1].threadName)
            assertEquals("Plain header title", sessions[2].threadName)
            assertEquals("Another custom slug", sessions[3].threadName)
        } finally {
            System.setProperty("user.home", originalUserHome)
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testAntigravityArchivedParsing() = runBlocking {
        val originalUserHome = System.getProperty("user.home")
        val tempDir = java.nio.file.Files.createTempDirectory("antigravity_test_archived_").toFile()
        tempDir.deleteOnExit()

        try {
            System.setProperty("user.home", tempDir.absolutePath)

            val geminiDir = File(tempDir, ".gemini/antigravity")
            geminiDir.mkdirs()
            val annotationsDir = File(geminiDir, "annotations")
            annotationsDir.mkdirs()
            val brainDir = File(geminiDir, "brain")
            val sessionDir = File(brainDir, "session-archived/.system_generated/logs")
            sessionDir.mkdirs()

            val transcriptFile = File(sessionDir, "transcript.jsonl")
            transcriptFile.writeText(
                """
                {"step_index":0,"source":"USER_EXPLICIT","type":"USER_INPUT","status":"DONE","created_at":"2026-05-20T02:00:00Z","content":"<USER_REQUEST>Archived test</USER_REQUEST>"}
                """.trimIndent()
            )

            val source = DesktopAntigravitySource()
            
            val session1 = source.parseSession(transcriptFile.absolutePath)
            assertNotNull(session1)
            assertEquals(false, session1.isArchived)

            val annotationFile = File(annotationsDir, "session-archived.pbtxt")
            annotationFile.writeText("archived:true last_user_view_time:{seconds:1234 nanos:567}")

            val session2 = source.parseSession(transcriptFile.absolutePath)
            assertNotNull(session2)
            assertEquals(true, session2.isArchived)

            annotationFile.writeText("archived:false last_user_view_time:{seconds:1234 nanos:567}")

            val session3 = source.parseSession(transcriptFile.absolutePath)
            assertNotNull(session3)
            assertEquals(false, session3.isArchived)

        } finally {
            System.setProperty("user.home", originalUserHome)
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testCodexArchivedParsing() = runBlocking {
        val tempDir = java.nio.file.Files.createTempDirectory("codex_test_archived_").toFile()
        tempDir.deleteOnExit()

        try {
            val sessionsDir = File(tempDir, "sessions")
            val archivedSessionsDir = File(tempDir, "archived_sessions")
            sessionsDir.mkdirs()
            archivedSessionsDir.mkdirs()

            val activeFile = File(sessionsDir, "rollout-codex123.jsonl")
            activeFile.writeText(
                """
                {"timestamp":"2026-05-20T02:00:00Z","type":"session_meta","payload":{"id":"codex123","timestamp":"2026-05-20T02:00:00Z","cwd":"/path/to/codex"}}
                {"timestamp":"2026-05-20T02:01:00Z","type":"response_item","payload":{"role":"user","content":[{"text":"Hi Codex"}]}}
                {"timestamp":"2026-05-20T02:02:00Z","type":"response_item","payload":{"role":"assistant","content":[{"text":"Hi human"}]}}
                """.trimIndent()
            )

            val archivedFile = File(archivedSessionsDir, "rollout-codex456.jsonl")
            archivedFile.writeText(
                """
                {"timestamp":"2026-05-20T02:00:00Z","type":"session_meta","payload":{"id":"codex456","timestamp":"2026-05-20T02:00:00Z","cwd":"/path/to/codex"}}
                {"timestamp":"2026-05-20T02:01:00Z","type":"response_item","payload":{"role":"user","content":[{"text":"Hi Codex"}]}}
                {"timestamp":"2026-05-20T02:02:00Z","type":"response_item","payload":{"role":"assistant","content":[{"text":"Hi human"}]}}
                """.trimIndent()
            )

            val source = DesktopCodexSource()
            
            val activeSession = source.parseSession(activeFile.absolutePath)
            assertNotNull(activeSession)
            assertEquals(false, activeSession.isArchived)

            val archivedSession = source.parseSession(archivedFile.absolutePath)
            assertNotNull(archivedSession)
            assertEquals(true, archivedSession.isArchived)

        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testAntigravitySourceParsingEscaping() = runBlocking {
        val tempFile = File.createTempFile("antigravity_escaped_test_", ".jsonl")
        tempFile.deleteOnExit()

        tempFile.writeText(
            """
            {"step_index":0,"source":"USER_EXPLICIT","type":"USER_INPUT","status":"DONE","created_at":"2026-05-20T02:00:00Z","content":"<USER_REQUEST>Search for [[[TOOL:</USER_REQUEST>"}
            {"step_index":1,"source":"MODEL","type":"PLANNER_RESPONSE","status":"DONE","created_at":"2026-05-20T02:01:00Z","content":"I will search for `[[[TOOL:` now."}
            {"step_index":2,"source":"MODEL","type":"GREP_SEARCH","status":"DONE","created_at":"2026-05-20T02:02:00Z","content":"Found: return \"[[[TOOL:\"","tool_calls":[{"name":"grep_search","args":{"Query":"\"[[[TOOL:\""}}]}
            """.trimIndent()
        )

        val source = DesktopAntigravitySource()
        val session = source.parseSession(tempFile.absolutePath)

        assertNotNull(session)
        assertEquals(1, session.turns.size)
        assertEquals("Search for [[[TOOL:", session.turns[0].userMessage)

        val expectedAssistant = "I will search for `\\[\\[\\[TOOL:` now.\n\n[[[TOOL:GREP_SEARCH|🔍 Search: Query: \\[\\[\\[TOOL:|1779242520000]]]\nFound: return \"\\[\\[\\[TOOL:\"\n[[[/TOOL]]]"
        assertEquals(expectedAssistant, session.turns[0].assistantMessage)
    }

    @Test
    fun testAntigravityCompactionParsing() = runBlocking {
        val tempFile = File.createTempFile("antigravity_compaction_", ".jsonl")
        tempFile.deleteOnExit()

        tempFile.writeText(
            """
            {"step_index":0,"source":"USER_EXPLICIT","type":"USER_INPUT","status":"DONE","created_at":"2026-05-20T02:00:00Z","content":"<USER_REQUEST>Start task</USER_REQUEST>"}
            {"step_index":1,"source":"MODEL","type":"PLANNER_RESPONSE","status":"DONE","created_at":"2026-05-20T02:00:05Z","content":"Okay, starting..."}
            {"step_index":2,"source":"USER_EXPLICIT","type":"USER_INPUT","status":"DONE","created_at":"2026-05-20T02:01:00Z","content":"<USER_REQUEST>Continue task</USER_REQUEST>"}
            {"step_index":3,"source":"SYSTEM","type":"CHECKPOINT","status":"DONE","created_at":"2026-05-20T02:01:05Z","content":"# Resuming from a compaction\nSummary info"}
            {"step_index":4,"source":"MODEL","type":"PLANNER_RESPONSE","status":"DONE","created_at":"2026-05-20T02:01:10Z","content":"Continued."}
            """.trimIndent()
        )

        val source = DesktopAntigravitySource()
        val session = source.parseSession(tempFile.absolutePath)

        assertNotNull(session)
        assertEquals(2, session.turns.size)

        // Turn 0: should not have compaction
        assertEquals("Start task", session.turns[0].userMessage)
        assertEquals("Okay, starting...", session.turns[0].assistantMessage)
        assertEquals(null, session.turns[0].extraData["isCompaction"])

        // Turn 1: should have compaction with 5000ms duration (02:01:05 - 02:01:00)
        assertEquals("Continue task", session.turns[1].userMessage)
        assertEquals("Continued.", session.turns[1].assistantMessage)
        assertEquals("true", session.turns[1].extraData["isCompaction"])
        assertEquals("5000", session.turns[1].extraData["compactionTimeMs"])
    }

    sealed class TempMessagePart {
        data class Text(val content: String) : TempMessagePart()
        data class Tool(
            val type: String,
            val header: String,
            val content: String,
            val timestamp: Long = 0L
        ) : TempMessagePart()
    }

    private fun String.tempUnescapeToolTags(): String {
        return this.replace("\\[\\[\\[TOOL", "[[[TOOL")
            .replace("\\[\\[\\[/TOOL", "[[[/TOOL")
    }

    private fun tempIsEscaped(text: String, index: Int): Boolean {
        var count = 0
        var i = index - 1
        while (i >= 0 && text[i] == '\\') {
            count++
            i--
        }
        return count % 2 != 0
    }

    private fun tempParseAssistantMessage(message: String): List<TempMessagePart> {
        val parts = mutableListOf<TempMessagePart>()
        var currentIndex = 0
        while (currentIndex < message.length) {
            var startIdx = message.indexOf("[[[TOOL:", currentIndex)
            while (startIdx != -1 && tempIsEscaped(message, startIdx)) {
                startIdx = message.indexOf("[[[TOOL:", startIdx + 8)
            }

            if (startIdx == -1) {
                val remaining = message.substring(currentIndex)
                if (remaining.isNotEmpty()) {
                    parts.add(TempMessagePart.Text(remaining.tempUnescapeToolTags()))
                }
                break
            }

            // Add preceding text if any
            if (startIdx > currentIndex) {
                val preceding = message.substring(currentIndex, startIdx)
                if (preceding.isNotEmpty()) {
                    parts.add(TempMessagePart.Text(preceding.tempUnescapeToolTags()))
                }
            }

            val headerEndIdx = message.indexOf("]]]", startIdx)
            if (headerEndIdx == -1) {
                parts.add(TempMessagePart.Text(message.substring(startIdx).tempUnescapeToolTags()))
                break
            }

            val headerContent = message.substring(startIdx + 8, headerEndIdx)
            val partsOfHeader = headerContent.split('|')
            val type = partsOfHeader.getOrNull(0) ?: ""
            val header = partsOfHeader.getOrNull(1) ?: ""
            val timestamp = partsOfHeader.getOrNull(2)?.toLongOrNull() ?: 0L

            var endIdx = message.indexOf("[[[/TOOL]]]", headerEndIdx + 3)
            while (endIdx != -1 && tempIsEscaped(message, endIdx)) {
                endIdx = message.indexOf("[[[/TOOL]]]", endIdx + 11)
            }

            if (endIdx == -1) {
                val tagStart = message.substring(startIdx, startIdx + 8)
                parts.add(TempMessagePart.Text(tagStart.tempUnescapeToolTags()))
                currentIndex = startIdx + 8
                continue
            }

            val content = message.substring(headerEndIdx + 3, endIdx)
            parts.add(TempMessagePart.Tool(
                type.tempUnescapeToolTags(),
                header.tempUnescapeToolTags(),
                content.tempUnescapeToolTags(),
                timestamp
            ))
            currentIndex = endIdx + 11
        }
        return parts
    }

    @Test
    fun testAntigravityToolTagsEdgeCases() = runBlocking {
        // 1. Missing closing tag should not swallow the rest of the message
        val text1 = "Preceding text [[[TOOL:GREP_SEARCH|Search|123]]] Tool content without closing tag.\nSubsequent dialogue text."
        val parts1 = tempParseAssistantMessage(text1)
        assertEquals(3, parts1.size)
        assertEquals(TempMessagePart.Text("Preceding text "), parts1[0])
        assertEquals(TempMessagePart.Text("[[[TOOL:"), parts1[1])
        assertEquals(TempMessagePart.Text("GREP_SEARCH|Search|123]]] Tool content without closing tag.\nSubsequent dialogue text."), parts1[2])

        // 2. Escaped tags should be unescaped properly
        val text2 = "This is an escaped tag: \\[\\[\\[TOOL:GREP_SEARCH]]], and an unescaped tag: [[[TOOL:VIEW_FILE|View|456]]]\nContent\n[[[/TOOL]]]"
        val parts2 = tempParseAssistantMessage(text2)
        assertEquals(2, parts2.size)
        assertEquals(TempMessagePart.Text("This is an escaped tag: [[[TOOL:GREP_SEARCH]]], and an unescaped tag: "), parts2[0])
        assertEquals(TempMessagePart.Tool("VIEW_FILE", "View", "\nContent\n", 456L), parts2[1])
    }


    @Test
    fun testCursorWindowsPathStripping() {
        val paths = listOf(
            "file:///C:/Users/pv/Dev/Project" to "C:/Users/pv/Dev/Project",
            "file:///D:/Work" to "D:/Work",
            "file:///etc/hosts" to "/etc/hosts",
            "/Users/pv/Dev" to "/Users/pv/Dev"
        )
        for ((input, expected) in paths) {
            var folderPath = if (input.startsWith("file://"))
                input.substringAfter("file://") else input
            if (folderPath.startsWith("/") && folderPath.length > 2 && folderPath[2] == ':') {
                folderPath = folderPath.substring(1)
            }
            assertEquals(expected, folderPath)
        }
    }

    @Test
    fun testTargetTranscriptParsing() = runBlocking {
        val targetPath = "/Users/pv/.gemini/antigravity/brain/9a9a9b5b-fa07-418a-b169-ed17f2a92c01/.system_generated/logs/transcript.jsonl"
        val file = File(targetPath)
        if (file.exists()) {
            val source = DesktopAntigravitySource()
            
            // 1. Initial parse
            val session = source.parseSession(targetPath)
            assertNotNull(session)
            println("INITIAL SESSION PARSED: Turns: ${session.turns.size}")
            
            // 2. Put in cache and end scan (saves to cache file)
            SessionCacheManager.isCacheEnabled = true
            SessionCacheManager.startScan(source.id)
            val md5 = SessionCacheManager.calculateMd5(file)
            SessionCacheManager.putCachedSession(
                sourceId = source.id,
                filePath = targetPath,
                lastModified = file.lastModified(),
                size = file.length(),
                hash = md5,
                session = session
            )
            SessionCacheManager.endScan(source.id)
            
            // 3. Load from cache in a new scan
            SessionCacheManager.startScan(source.id)
            val cachedSession = SessionCacheManager.getCachedSessionForFile(
                sourceId = source.id,
                filePath = targetPath,
                lastModified = file.lastModified(),
                size = file.length()
            )
            assertNotNull(cachedSession)
            println("CACHED SESSION LOADED: Turns: ${cachedSession.turns.size}")
            SessionCacheManager.endScan(source.id)
            
            // 4. Parse the cached session turns
            val turn = cachedSession.turns[0]
            val parts = tempParseAssistantMessage(turn.assistantMessage)
            for (pIdx in 22..28) {
                if (pIdx < parts.size) {
                    println("  CACHED PART $pIdx: ${parts[pIdx]}")
                }
            }
        } else {
            println("Target file does not exist at $targetPath")
        }
    }

    @Test
    fun testCopilotSourceParsing() = runBlocking {
        val tempDir = java.nio.file.Files.createTempDirectory("copilot_test_").toFile()
        tempDir.deleteOnExit()

        val workspaceYaml = File(tempDir, "workspace.yaml")
        workspaceYaml.writeText(
            """
            id: copilot-session-123
            name: Code review audit
            cwd: /path/to/project
            branch: main
            repository: LookAtWhatAiCanDo/Codeoba
            created_at: 2026-06-10T14:10:14.691Z
            updated_at: 2026-06-10T21:10:21.486Z
            """.trimIndent()
        )

        val eventsJsonl = File(tempDir, "events.jsonl")
        eventsJsonl.writeText(
            """
            {"type":"user.message","timestamp":"2026-06-10T21:10:16.036Z","data":{"content":"review and audit this code"}}
            {"type":"tool.execution_start","timestamp":"2026-06-10T21:10:21.480Z","data":{"toolCallId":"call_1","toolName":"run_command","arguments":{"CommandLine":"ls -la"}}}
            {"type":"tool.execution_complete","timestamp":"2026-06-10T21:10:21.483Z","data":{"toolCallId":"call_1","success":true,"result":{"content":"Intent logged","detailedContent":"Reviewing codebase"}}}
            {"type":"assistant.message","timestamp":"2026-06-10T21:10:21.479Z","data":{"content":"Reviewing the current diff now...","reasoningText":"Let me start by checking files.","model":"gpt-4o"}}
            """.trimIndent()
        )

        val source = DesktopCopilotSource()
        val session = source.parseSession(eventsJsonl.absolutePath)

        assertNotNull(session)
        assertEquals("copilot-session-123", session.id)
        assertEquals("/path/to/project", session.cwd)
        assertEquals("Code review audit", session.threadName)
        assertEquals(1, session.turns.size)
        assertEquals("review and audit this code", session.turns[0].userMessage)
        
        val assistantText = session.turns[0].assistantMessage
        kotlin.test.assertTrue(assistantText.contains("> [!NOTE]"))
        kotlin.test.assertTrue(assistantText.contains("**Reasoning:**"))
        kotlin.test.assertTrue(assistantText.contains("Let me start by checking files."))
        kotlin.test.assertTrue(assistantText.contains("Reviewing the current diff now..."))
        kotlin.test.assertTrue(assistantText.contains("[[[TOOL:RUN_COMMAND|⚡ Run Command: ls -la"))
        kotlin.test.assertTrue(assistantText.contains("Reviewing codebase"))
        
        assertEquals("gpt-4o", session.turns[0].extraData["model"])
        tempDir.deleteRecursively()
        Unit
    }
}
