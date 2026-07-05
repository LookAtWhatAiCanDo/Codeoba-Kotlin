package com.whataicando.codeoba.desktop.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.whataicando.codeoba.core.domain.search.SearchResult
import com.whataicando.codeoba.core.util.DebugStoreConfig

interface WorkspaceStatsProvider {
    val totalConversations: Int
    val totalTurns: Int
    val promptTokens: Long
    val responseTokens: Long
    val totalEstTokens: Long
    val avgTurns: Float
    val totalDurationMs: Long
    val avgDurationMs: Long
    val avgSpeedText: String
    val totalCompactions: Int
    val totalCompactionTimeMs: Long
    val modelStatsList: List<ModelItemStats>
    val sourceGroups: List<Pair<String, Int>>

    fun getGroupSessionCount(groupName: String, defaultValue: Int): Int
}

@Composable
fun rememberWorkspaceStatsProvider(searchResults: List<SearchResult>): WorkspaceStatsProvider {
    return remember(searchResults) {
        if (DebugStoreConfig.isStoreMode) CannedWorkspaceStatsProvider(searchResults) else RealWorkspaceStatsProvider(searchResults)
    }
}
