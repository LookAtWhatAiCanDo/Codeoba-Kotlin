package llc.lookatwhataicando.codeoba.core.domain.parser

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import llc.lookatwhataicando.codeoba.core.domain.model.Session
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
        
        // Local model stub simulation using parser config configuration parameters
        val summaryText = LocalModelRunner.runLocalInference(baseSession, parserConfigJson)
        val summary = parseSummaryJson(summaryText)
        
        return baseSession.copy(summary = summary)
    }

    private fun parseSummaryJson(jsonStr: String): SessionSummary {
        return try {
            val trimmed = jsonStr.trim()
            kotlinx.serialization.json.Json.decodeFromString(SessionSummary.serializer(), trimmed)
        } catch (e: Exception) {
            SessionSummary(
                keyActions = listOf("Unable to parse AI summary outputs"),
                errors = listOf("Format exception: ${e.message}"),
                performanceCharts = emptyList()
            )
        }
    }
}

object LogParserFactory {
    private var parserMode: ParserMode = ParserMode.STANDARD
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

object LocalModelRunner {
    fun runLocalInference(session: Session, parserConfigJson: String?): String {
        // In a real production environment, this is where we load the model weights
        // (e.g. weights.bin / model.onnx) and run them via llama.cpp or ONNX Runtime JVM bindings.
        // The parserConfigJson contains configuration parameters retrieved from the server.
        // If parserConfigJson is missing or invalid, we fail inference.
        if (parserConfigJson.isNullOrBlank()) {
            throw IllegalStateException("Parser configuration is missing or invalid.")
        }
        
        // Simulating structured output based on the session turns
        val actions = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        val turnCount = session.turns.size
        actions.add("Parsed $turnCount dialogue exchanges from source '${session.sourceId}'")
        
        val uniqueModels = session.turns.mapNotNull { it.extraData["model"] }.distinct()
        if (uniqueModels.isNotEmpty()) {
            actions.add("Identified active agent models: ${uniqueModels.joinToString(", ")}")
        }
        
        var compactionCount = 0
        var totalComputeMs = 0L
        for (turn in session.turns) {
            val userMsg = turn.userMessage.lowercase()
            val asstMsg = turn.assistantMessage.lowercase()
            
            if (turn.extraData["isCompaction"] == "true") {
                compactionCount++
            }
            val ms = turn.extraData["computeTimeMs"]?.toLongOrNull() ?: 0L
            totalComputeMs += ms
            
            if (userMsg.contains("error") || asstMsg.contains("error") || asstMsg.contains("exception") || asstMsg.contains("fail")) {
                val excerpt = if (turn.userMessage.length > 40) turn.userMessage.take(40) + "..." else turn.userMessage
                errors.add("Detected execution failure or warning in turn user query: '$excerpt'")
            }
        }
        
        if (compactionCount > 0) {
            actions.add("Analyzed $compactionCount context compaction events during session execution")
        }
        if (totalComputeMs > 0) {
            actions.add("Measured total active compute work duration: ${totalComputeMs}ms")
        }
        
        if (errors.isEmpty()) {
            errors.add("No critical runtime or execution exceptions detected in this thread.")
        }
        
        val lexicalSpeed = (300.0 + (turnCount * 12.5)).coerceAtMost(600.0)
        val semanticSpeed = (100.0 + (turnCount * 5.2)).coerceAtMost(250.0)
        val watcherLatency = (10.0 + (turnCount * 0.8)).coerceAtMost(50.0)

        val jsonObject = buildJsonObject {
            put("keyActions", buildJsonArray {
                actions.forEach { add(it) }
            })
            put("errors", buildJsonArray {
                errors.forEach { add(it) }
            })
            put("performanceCharts", buildJsonArray {
                add(buildJsonObject {
                    put("label", "Lexical Search Speed")
                    put("value", lexicalSpeed)
                })
                add(buildJsonObject {
                    put("label", "Semantic Search Speed")
                    put("value", semanticSpeed)
                })
                add(buildJsonObject {
                    put("label", "Directory Watcher Latency")
                    put("value", watcherLatency)
                })
            })
        }
        return jsonObject.toString()
    }
}
