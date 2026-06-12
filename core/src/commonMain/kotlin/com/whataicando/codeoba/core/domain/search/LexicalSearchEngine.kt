package com.whataicando.codeoba.core.domain.search

import com.whataicando.codeoba.core.domain.model.Session

class LexicalSearchEngine : SearchEngine {
    private val sessionsMap = mutableMapOf<String, Session>()

    override suspend fun updateIndex(sessions: List<Session>, onProgress: ((Int, Int) -> Unit)?) {
        synchronized(sessionsMap) {
            sessionsMap.clear()
            for (session in sessions) {
                sessionsMap[session.id] = session
            }
        }
    }

    override suspend fun updateSession(session: Session) {
        synchronized(sessionsMap) {
            sessionsMap[session.id] = session
        }
    }

    override suspend fun getSession(id: String): Session? {
        return synchronized(sessionsMap) { sessionsMap[id] }
    }

    override suspend fun removeSessionByPath(filePath: String) {
        synchronized(sessionsMap) {
            val toRemove = sessionsMap.values.filter { it.filePath == filePath }.map { it.id }
            for (id in toRemove) {
                sessionsMap.remove(id)
            }
        }
    }

    override suspend fun removeSessionsBySource(sourceId: String) {
        synchronized(sessionsMap) {
            val toRemove = sessionsMap.values.filter { it.sourceId == sourceId }.map { it.id }
            for (id in toRemove) {
                sessionsMap.remove(id)
            }
        }
    }

    override suspend fun search(query: String, filter: SearchFilter): List<SearchResult> {
        if (query.isBlank()) {
            val allSessions = synchronized(sessionsMap) { sessionsMap.values.toList() }
            return allSessions
                .filter { filter.matches(it) }
                .map { SearchResult(it, emptyList(), score = 1.0f) }
                .sortedByDescending { it.session.updatedAt }
        }

        val isSinglePattern = filter.useRegex || query.contains("\n")
        val regexes = if (isSinglePattern) {
            val r = buildFindRegex(query, filter.matchCase, filter.wholeWord, filter.useRegex)
            if (r != null) listOf(r) else emptyList()
        } else {
            val terms = query.split(Regex("\\s+")).filter { it.isNotEmpty() }
            terms.mapNotNull { buildFindRegex(it, filter.matchCase, filter.wholeWord, useRegex = false) }
        }

        if (regexes.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<SearchResult>()
        val currentSessions = synchronized(sessionsMap) { sessionsMap.values.toList() }

        for (session in currentSessions) {
            if (!filter.matches(session)) continue

            var score = 0.0f
            val matchedTurnIndexes = mutableListOf<Int>()

            val threadName = session.threadName ?: ""
            var threadNameMatches = 0
            for (regex in regexes) {
                threadNameMatches += regex.findAll(threadName).count()
            }
            if (threadNameMatches > 0) {
                score += threadNameMatches * 5.0f
            }

            val cwd = session.cwd ?: ""
            if (cwd.isNotEmpty()) {
                var cwdMatches = 0
                for (regex in regexes) {
                    cwdMatches += regex.findAll(cwd).count()
                }
                if (cwdMatches > 0) {
                    score += cwdMatches * 2.0f
                }
            }

            for ((index, turn) in session.turns.withIndex()) {
                var userMatches = 0
                var assistantMatches = 0
                for (regex in regexes) {
                    userMatches += regex.findAll(turn.userMessage).count()
                    assistantMatches += regex.findAll(turn.assistantMessage).count()
                }
                val turnMatches = userMatches * 2 + assistantMatches * 1
                if (turnMatches > 0) {
                    score += turnMatches * 1.0f
                    matchedTurnIndexes.add(index)
                }
            }

            if (score > 0.0f) {
                results.add(SearchResult(session, matchedTurnIndexes, score))
            }
        }

        return results.sortedWith(
            compareByDescending<SearchResult> { it.score }
                .thenByDescending { it.session.updatedAt }
        )
    }

}
