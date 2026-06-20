package com.whataicando.codeoba.desktop.provider

import com.whataicando.codeoba.core.domain.search.SearchResult
import com.whataicando.codeoba.desktop.formatSpeed
import com.whataicando.codeoba.desktop.getSessionComputeTimeMs

class RealWorkspaceStatsProvider(private val searchResults: List<SearchResult>) : WorkspaceStatsProvider {
    override val totalConversations: Int
        get() = searchResults.size

    override val totalTurns: Int
        get() = searchResults.sumOf { it.session.turns.size }

    private val totalUserChars = searchResults.sumOf { it.session.turns.sumOf { turn -> turn.userMessage.length } }
    private val totalAssistantChars = searchResults.sumOf { it.session.turns.sumOf { turn -> turn.assistantMessage.length } }

    override val promptTokens: Long
        get() = ((totalUserChars + 3) / 4).toLong()

    override val responseTokens: Long
        get() = ((totalAssistantChars + 3) / 4).toLong()

    override val totalEstTokens: Long
        get() = promptTokens + responseTokens

    override val avgTurns: Float
        get() = if (totalConversations > 0) totalTurns.toFloat() / totalConversations else 0f

    override val totalDurationMs: Long
        get() = searchResults.sumOf { getSessionComputeTimeMs(it.session) }

    override val avgDurationMs: Long
        get() {
            if (totalConversations <= 0) return 0L
            val totalElapsedMs = searchResults.sumOf {
                (it.session.updatedAt - it.session.timestamp).coerceAtLeast(0L)
            }
            return totalElapsedMs / totalConversations
        }

    override val avgSpeedText: String
        get() = formatSpeed(totalEstTokens, totalDurationMs)

    override val totalCompactions: Int
        get() = searchResults.sumOf { res ->
            res.session.turns.count { it.extraData["isCompaction"] == "true" }
        }

    override val totalCompactionTimeMs: Long
        get() = searchResults.sumOf { res ->
            res.session.turns.sumOf { it.extraData["compactionTimeMs"]?.toLongOrNull() ?: 0L }
        }

    override val modelStatsList: List<ModelItemStats>
        get() {
            class ModelStats(
                var turnCount: Int = 0,
                var promptChars: Long = 0,
                var responseChars: Long = 0,
                var computeTimeMs: Long = 0
            )
            val modelStatsMap = mutableMapOf<String, ModelStats>()
            for (res in searchResults) {
                for (turn in res.session.turns) {
                    val mName = turn.extraData["model"] ?: "Unknown Model"
                    val stats = modelStatsMap.getOrPut(mName) { ModelStats() }
                    stats.turnCount++
                    stats.promptChars += turn.userMessage.length
                    stats.responseChars += turn.assistantMessage.length
                    val ms = turn.extraData["computeTimeMs"]?.toLongOrNull()
                    if (ms != null && ms > 0) {
                        stats.computeTimeMs += ms.coerceAtMost(900_000L)
                    } else if (turn.assistantMessage.isNotEmpty()) {
                        val estMs = (turn.assistantMessage.length / 120.0 * 1000.0).toLong()
                        stats.computeTimeMs += estMs.coerceIn(2000L, 60000L)
                    }
                }
            }

            return modelStatsMap.entries.map { (modelName, stats) ->
                val modelPromptTokens = (stats.promptChars + 3) / 4
                val modelResponseTokens = (stats.responseChars + 3) / 4
                val modelTotalTokens = modelPromptTokens + modelResponseTokens
                val speedTps = if (stats.computeTimeMs > 0) {
                    (modelTotalTokens.toDouble() * 1000.0) / stats.computeTimeMs
                } else 0.0
                ModelItemStats(
                    modelName = modelName,
                    turnCount = stats.turnCount,
                    promptChars = stats.promptChars,
                    responseChars = stats.responseChars,
                    computeTimeMs = stats.computeTimeMs,
                    totalTokens = modelTotalTokens,
                    speedTps = speedTps
                )
            }
        }

    override val sourceGroups: List<Pair<String, Int>>
        get() = searchResults.groupBy { it.session.sourceId }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

    override fun getGroupSessionCount(groupName: String, defaultValue: Int): Int {
        return defaultValue
    }
}
