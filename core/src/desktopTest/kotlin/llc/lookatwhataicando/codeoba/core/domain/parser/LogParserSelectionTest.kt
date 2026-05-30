package llc.lookatwhataicando.codeoba.core.domain.parser

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
