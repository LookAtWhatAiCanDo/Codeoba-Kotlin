package llc.lookatwhataicando.codeoba.core.domain.search

import llc.lookatwhataicando.codeoba.core.domain.model.Session
import kotlin.math.sqrt

class SemanticSearchEngine(
    private val embedder: SemanticEmbedder,
    private val similarityThreshold: Float = 0.35f
) : SearchEngine {
    private val sessionsMap = mutableMapOf<String, Session>()
    private val sessionEmbeddings = mutableMapOf<String, SessionVectorIndex>()

    private class SessionVectorIndex(
        val threadNameEmbedding: FloatArray,
        val turnEmbeddings: List<FloatArray>
    )

    override suspend fun updateIndex(sessions: List<Session>) {
        synchronized(sessionsMap) {
            sessionsMap.clear()
            sessionEmbeddings.clear()
        }
        for (session in sessions) {
            updateSession(session)
        }
    }

    override suspend fun updateSession(session: Session) {
        val threadNameEmb = try {
            embedder.getEmbeddings(session.threadName ?: "Untitled Session")
        } catch (e: Exception) {
            FloatArray(0)
        }

        val turnEmbs = mutableListOf<FloatArray>()
        for (turn in session.turns) {
            val turnText = "${turn.userMessage}\n${turn.assistantMessage}"
            val turnEmb = try {
                embedder.getEmbeddings(turnText)
            } catch (e: Exception) {
                FloatArray(0)
            }
            turnEmbs.add(turnEmb)
        }

        synchronized(sessionsMap) {
            sessionsMap[session.id] = session
            sessionEmbeddings[session.id] = SessionVectorIndex(threadNameEmb, turnEmbs)
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
                sessionEmbeddings.remove(id)
            }
        }
    }

    override suspend fun removeSessionsBySource(sourceId: String) {
        synchronized(sessionsMap) {
            val toRemove = sessionsMap.values.filter { it.sourceId == sourceId }.map { it.id }
            for (id in toRemove) {
                sessionsMap.remove(id)
                sessionEmbeddings.remove(id)
            }
        }
    }

    override suspend fun search(query: String, filter: SearchFilter): List<SearchResult> {
        val queryEmbedding = try {
            embedder.getEmbeddings(query)
        } catch (e: Exception) {
            return emptyList()
        }

        val results = mutableListOf<SearchResult>()
        val currentSessions = synchronized(sessionsMap) { sessionsMap.values.toList() }

        for (session in currentSessions) {
            if (!matchesFilter(session, filter)) continue

            val index = synchronized(sessionsMap) { sessionEmbeddings[session.id] } ?: continue
            val matchedTurnIndexes = mutableListOf<Int>()
            var maxSimilarity = -1.0f

            if (index.threadNameEmbedding.isNotEmpty() && queryEmbedding.isNotEmpty()) {
                val threadSimilarity = cosineSimilarity(queryEmbedding, index.threadNameEmbedding)
                if (threadSimilarity > maxSimilarity) {
                    maxSimilarity = threadSimilarity
                }
            }

            for ((idx, turnEmb) in index.turnEmbeddings.withIndex()) {
                if (turnEmb.isEmpty() || queryEmbedding.isEmpty()) continue
                val similarity = cosineSimilarity(queryEmbedding, turnEmb)
                if (similarity >= similarityThreshold) {
                    matchedTurnIndexes.add(idx)
                }
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity
                }
            }

            if (maxSimilarity >= similarityThreshold) {
                results.add(SearchResult(session, matchedTurnIndexes, maxSimilarity))
            }
        }

        return results.sortedWith(
            compareByDescending<SearchResult> { it.score }
                .thenByDescending { it.session.updatedAt }
        )
    }

    private fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size || vectorA.isEmpty()) return 0.0f
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        if (normA == 0.0 || normB == 0.0) return 0.0f
        return (dotProduct / (sqrt(normA) * sqrt(normB))).toFloat()
    }

    private fun matchesFilter(session: Session, filter: SearchFilter): Boolean {
        if (filter.sourceIds.isNotEmpty() && !filter.sourceIds.contains(session.sourceId)) {
            return false
        }
        if (session.updatedAt < filter.minTimestamp || session.updatedAt > filter.maxTimestamp) {
            return false
        }
        if (filter.cwdFilter != null) {
            val cwd = session.cwd ?: return false
            if (!cwd.lowercase().contains(filter.cwdFilter.lowercase())) {
                return false
            }
        }
        when (filter.archivalFilter) {
            ArchivalFilter.ACTIVE -> if (session.isArchived) return false
            ArchivalFilter.ARCHIVED -> if (!session.isArchived) return false
            ArchivalFilter.ALL -> {}
        }
        return true
    }
}
