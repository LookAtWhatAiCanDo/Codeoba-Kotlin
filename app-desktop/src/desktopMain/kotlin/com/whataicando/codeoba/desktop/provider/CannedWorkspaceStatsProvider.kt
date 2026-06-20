package com.whataicando.codeoba.desktop.provider

import com.whataicando.codeoba.core.domain.model.Turn
import com.whataicando.codeoba.core.domain.search.SearchResult
import java.util.Random

class CannedWorkspaceStatsProvider(baseResults: List<SearchResult>) : WorkspaceStatsProvider {
    private val delegate = RealWorkspaceStatsProvider(rememberGeneratedResults(baseResults))

    override val totalConversations: Int get() = delegate.totalConversations
    override val totalTurns: Int get() = delegate.totalTurns
    override val promptTokens: Long get() = delegate.promptTokens
    override val responseTokens: Long get() = delegate.responseTokens
    override val totalEstTokens: Long get() = delegate.totalEstTokens
    override val avgTurns: Float get() = delegate.avgTurns
    override val totalDurationMs: Long get() = delegate.totalDurationMs
    override val avgDurationMs: Long get() = delegate.avgDurationMs
    override val avgSpeedText: String get() = delegate.avgSpeedText
    override val totalCompactions: Int get() = delegate.totalCompactions
    override val totalCompactionTimeMs: Long get() = delegate.totalCompactionTimeMs
    override val modelStatsList: List<ModelItemStats> get() = delegate.modelStatsList
    override val sourceGroups: List<Pair<String, Int>> get() = delegate.sourceGroups

    override fun getGroupSessionCount(groupName: String, defaultValue: Int): Int {
        return when (groupName.lowercase()) {
            "_none_" -> 130
            "backend service" -> 115
            "frontend spa" -> 55
            "mobile clients" -> 42
            "devops & deploy" -> 28
            "documentation" -> 12
            else -> defaultValue
        }
    }
}

private fun rememberGeneratedResults(baseResults: List<SearchResult>): List<SearchResult> {
    if (baseResults.isEmpty()) return emptyList()
    val random = Random(42)
    val result = mutableListOf<SearchResult>()
    val totalCount = 382
    
    for (i in 0 until totalCount) {
        val base = baseResults[i % baseResults.size]
        val sourceId = when {
            i < 132 -> "claude"
            i < 132 + 110 -> "cursor"
            i < 132 + 110 + 85 -> "antigravity"
            else -> "copilot"
        }
        
        val turnsCount = when (sourceId) {
            "claude" -> random.nextInt(10) + 12
            "cursor" -> random.nextInt(8) + 10
            "antigravity" -> random.nextInt(6) + 8
            else -> random.nextInt(4) + 4
        }
        
        val turns = mutableListOf<Turn>()
        var currentTimestamp = base.session.timestamp
        var firstTurnTimestamp: Long? = null
        for (j in 0 until turnsCount) {
            val modelName = when (sourceId) {
                "claude" -> "Claude 3.5 Sonnet"
                "cursor" -> if (j % 3 == 0) "GPT-4o" else "Claude 3.5 Sonnet"
                "antigravity" -> "Gemini 1.5 Pro"
                else -> "Claude 3.5 Haiku"
            }
            
            val promptLength = random.nextInt(2000) + 500
            val assistantLength = random.nextInt(4000) + 1000
            
            val isCompaction = (random.nextFloat() < 0.05).toString()
            val compactionTime = if (isCompaction == "true") (random.nextInt(165000) + 15000).toString() else "0"
            
            val promptTokens = (promptLength + 3) / 4
            val responseTokens = (assistantLength + 3) / 4
            val totalTokens = promptTokens + responseTokens
            
            val speed = when (modelName) {
                "Claude 3.5 Sonnet" -> 38.5
                "GPT-4o" -> 46.2
                "Gemini 1.5 Pro" -> 48.5
                else -> 62.4
            }
            val computeTime = ((totalTokens.toDouble() / speed) * 1000).toLong()
            
            currentTimestamp += random.nextInt(15 * 60000) + 5 * 60000L
            if (firstTurnTimestamp == null) {
                firstTurnTimestamp = currentTimestamp
            }
            turns.add(
                Turn(
                    turnId = "t$j",
                    userMessage = "a".repeat(promptLength),
                    assistantMessage = "b".repeat(assistantLength),
                    timestamp = currentTimestamp,
                    extraData = mapOf(
                        "model" to modelName,
                        "computeTimeMs" to computeTime.toString(),
                        "isCompaction" to isCompaction,
                        "compactionTimeMs" to compactionTime
                    )
                )
            )
        }
        
        val mockSession = base.session.copy(
            id = "mock-session-$i",
            sourceId = sourceId,
            timestamp = firstTurnTimestamp ?: base.session.timestamp,
            updatedAt = currentTimestamp,
            turns = turns
        )
        
        result.add(
            SearchResult(
                session = mockSession,
                matchedTurnIndexes = emptyList(),
                score = 1.0f
            )
        )
    }
    return result
}
