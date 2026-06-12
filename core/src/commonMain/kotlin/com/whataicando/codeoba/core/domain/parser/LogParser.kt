package com.whataicando.codeoba.core.domain.parser

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.whataicando.codeoba.core.domain.model.Session
import java.io.File

interface LogParser {
    suspend fun parse(file: File, delegateParse: suspend () -> Session?): Session?
}

class StandardLogParser : LogParser {
    override suspend fun parse(file: File, delegateParse: suspend () -> Session?): Session? {
        return delegateParse()
    }
}
class SummarizingLogParser(private val parserConfigJson: String?) : LogParser {
    override suspend fun parse(file: File, delegateParse: suspend () -> Session?): Session? {
        val baseSession = delegateParse() ?: return null
        
        val summary = try {
            when (val result = SummarizerProvider.current().summarize(baseSession, parserConfigJson)) {
                is SummaryResult.Ok -> result.summary
                is SummaryResult.Unavailable -> {
                    SessionSummary(
                        keyActions = listOf("AI-powered summarization requires an active subscription."),
                        errors = listOf(result.reason),
                        performanceCharts = emptyList()
                    )
                }
                is SummaryResult.Failed -> {
                    SessionSummary(
                        keyActions = listOf("AI summarization failed"),
                        errors = listOf(result.reason + (result.cause?.let { " Cause: $it" } ?: "")),
                        performanceCharts = emptyList()
                    )
                }
            }
        } catch (e: Exception) {
            SessionSummary(
                keyActions = listOf("AI summarization failed"),
                errors = listOf("Inference exception: ${e.message}"),
                performanceCharts = emptyList()
            )
        }
        
        return baseSession.copy(summary = summary)
    }
}

object LogParserFactory {
    @Volatile
    private var parserMode: ParserMode = ParserMode.STANDARD

    @Volatile
    private var parserConfig: String? = """
    {
      "version": "1.0.0",
      "systemPrompt": "You are the Codeoba assistant. Keep your responses concise and precise."
    }
    """.trimIndent()
    fun setParserMode(mode: ParserMode, configJson: String? = null) {
        parserMode = mode
        if (configJson != null) {
            parserConfig = configJson
        }
    }

    fun getParserMode(): ParserMode = parserMode

    fun getParser(): LogParser {
        return when (parserMode) {
            ParserMode.STANDARD -> StandardLogParser()
            ParserMode.SUMMARIZING -> SummarizingLogParser(parserConfig)
        }
    }
}

