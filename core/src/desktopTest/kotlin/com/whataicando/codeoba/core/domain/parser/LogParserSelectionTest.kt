package com.whataicando.codeoba.core.domain.parser

import kotlinx.coroutines.runBlocking
import com.whataicando.codeoba.core.domain.model.Session
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LogParserSelectionTest {

    @Test
    fun testDefaultParserMode() {
        // Reset/init to STANDARD
        LogParserFactory.setParserMode(ParserMode.STANDARD, null)
        assertEquals(ParserMode.STANDARD, LogParserFactory.getParserMode())
        val parser = LogParserFactory.getParser()
        assertTrue(parser is StandardLogParser, "Default parser should be StandardLogParser")
    }

    @Test
    fun testSwitchingParserMode() {
        val mockPrompts = "{\"systemPrompt\": \"mock\"}"
        LogParserFactory.setParserMode(ParserMode.SUMMARIZING, mockPrompts)
        assertEquals(ParserMode.SUMMARIZING, LogParserFactory.getParserMode())
        val parser = LogParserFactory.getParser()
        assertTrue(parser is SummarizingLogParser, "Parser should be SummarizingLogParser when mode is SUMMARIZING")

        // Switch back to STANDARD
        LogParserFactory.setParserMode(ParserMode.STANDARD, null)
        assertEquals(ParserMode.STANDARD, LogParserFactory.getParserMode())
        val freeParser = LogParserFactory.getParser()
        assertTrue(freeParser is StandardLogParser)
    }

    @Test
    fun testSummarizingParserDefaultStub() {
        runBlocking {
            // Revert/reset to default StubSummarizer
            SummarizerProvider.revertToStub()
            
            val parser = SummarizingLogParser(null)
            val dummySession = createDummySession()
            val file = File("dummy-file")
            val resultSession = parser.parse(file) { dummySession }
            
            assertNotNull(resultSession)
            val summary = resultSession.summary
            assertNotNull(summary)
            assertTrue(summary.keyActions.contains("AI-powered summarization requires an active subscription."))
            assertTrue(summary.errors.first().contains("requires an active Codeoba subscription"))
        }
    }

    @Test
    fun testSummarizingParserWithCustomSummarizer() {
        runBlocking {
            val expectedSummary = SessionSummary(
                keyActions = listOf("Custom action"),
                errors = emptyList(),
                performanceCharts = emptyList()
            )
            
            SummarizerProvider.install(object : Summarizer {
                override fun summarize(session: Session, parserConfigJson: String?): SummaryResult {
                    return SummaryResult.Ok(expectedSummary)
                }
            })
            
            val parser = SummarizingLogParser(null)
            val dummySession = createDummySession()
            val file = File("dummy-file")
            val resultSession = parser.parse(file) { dummySession }
            
            assertNotNull(resultSession)
            assertEquals(expectedSummary, resultSession.summary)
            
            // Clean up
            SummarizerProvider.revertToStub()
        }
    }

    @Test
    fun testSummarizingParserExceptionSafety() {
        runBlocking {
            SummarizerProvider.install(object : Summarizer {
                override fun summarize(session: Session, parserConfigJson: String?): SummaryResult {
                    throw RuntimeException("Simulated inference failure")
                }
            })
            
            val parser = SummarizingLogParser(null)
            val dummySession = createDummySession()
            val file = File("dummy-file")
            val resultSession = parser.parse(file) { dummySession }
            
            assertNotNull(resultSession)
            val summary = resultSession.summary
            assertNotNull(summary)
            assertTrue(summary.keyActions.contains("AI summarization failed"), "Should contain fallback action message")
            assertTrue(summary.errors.first().contains("Inference exception: Simulated inference failure"), "Should capture the exception message")
            
            // Clean up
            SummarizerProvider.revertToStub()
        }
    }

    private fun createDummySession(): Session {
        return Session(
            id = "test-id",
            sourceId = "test-source",
            filePath = "test-path",
            timestamp = 0L,
            updatedAt = 0L,
            cwd = null,
            threadName = null,
            turns = emptyList(),
            isArchived = false,
            isPinned = false,
            summary = null
        )
    }
}
