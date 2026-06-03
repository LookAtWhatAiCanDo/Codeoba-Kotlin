package llc.lookatwhataicando.codeoba.core.domain.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import java.io.File
import kotlinx.coroutines.runBlocking

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
    fun testSummarizingParserExceptionSafety() {
        runBlocking {
            val parser = SummarizingLogParser(null) // This will cause runLocalInference to throw Exception
            val dummySession = Session(
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
            
            val file = File("dummy-file")
            val resultSession = parser.parse(file) { dummySession }
            
            assertNotNull(resultSession)
            val summary = resultSession.summary
            assertNotNull(summary)
            assertTrue(summary.keyActions.contains("AI summarization failed"), "Should contain fallback action message")
            assertTrue(summary.errors.first().contains("Inference exception:"), "Should capture the inference failure error message")
        }
    }
}
