package com.whataicando.codeoba.core.domain.search

import com.whataicando.codeoba.core.domain.model.Session

enum class ArchivalFilter {
    ALL, ACTIVE, ARCHIVED
}

data class SearchFilter(
    val sourceIds: Set<String> = emptySet(),
    val minTimestamp: Long = 0L,
    val maxTimestamp: Long = Long.MAX_VALUE,
    val cwdFilter: String? = null,
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val useRegex: Boolean = false,
    val archivalFilter: ArchivalFilter = ArchivalFilter.ALL,
    val sessionIds: Set<String>? = null
)

fun SearchFilter.matches(session: Session): Boolean {
    if (sourceIds.isNotEmpty() && !sourceIds.contains(session.sourceId)) {
        return false
    }
    if (session.updatedAt < minTimestamp || session.updatedAt > maxTimestamp) {
        return false
    }
    if (cwdFilter != null) {
        val cwd = session.cwd ?: return false
        if (!cwd.lowercase().contains(cwdFilter.lowercase())) {
            return false
        }
    }
    when (archivalFilter) {
        ArchivalFilter.ACTIVE -> if (session.isArchived) return false
        ArchivalFilter.ARCHIVED -> if (!session.isArchived) return false
        ArchivalFilter.ALL -> {}
    }
    if (sessionIds != null && !sessionIds.contains(session.id)) {
        return false
    }
    return true
}

fun buildFindRegex(query: String, matchCase: Boolean, wholeWord: Boolean, useRegex: Boolean): Regex? {
    if (query.isEmpty()) return null
    return try {
        val pattern = if (useRegex) {
            query
        } else {
            Regex.escape(query)
        }
        val finalPattern = if (wholeWord) {
            "\\b$pattern\\b"
        } else {
            pattern
        }
        val options = mutableSetOf<RegexOption>()
        if (!matchCase) {
            options.add(RegexOption.IGNORE_CASE)
        }
        Regex(finalPattern, options)
    } catch (e: Exception) {
        null
    }
}

data class SearchResult(
    val session: Session,
    val matchedTurnIndexes: List<Int>, // Indices of turns that matched the query
    val score: Float
)

interface SearchEngine {
    /**
     * Executes a text search against the index.
     */
    suspend fun search(query: String, filter: SearchFilter = SearchFilter()): List<SearchResult>

    /**
     * Overwrites or populates the index with a fresh list of sessions.
     */
    suspend fun updateIndex(sessions: List<Session>, onProgress: ((processed: Int, total: Int) -> Unit)? = null)

    /**
     * Incremental update for a single session when it gets modified or added.
     */
    suspend fun updateSession(session: Session)

    /**
     * Retrieves a session by its ID directly.
     */
    suspend fun getSession(id: String): Session?

    /**
     * Removes a session by its file path.
     */
    suspend fun removeSessionByPath(filePath: String)

    /**
     * Removes all sessions associated with a specific source ID.
     */
    suspend fun removeSessionsBySource(sourceId: String)
}
