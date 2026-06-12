package com.whataicando.codeoba.core.domain.search

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.whataicando.codeoba.core.domain.model.Session
import kotlin.math.sqrt

class SemanticSearchEngine(
    private val embedder: SemanticEmbedder,
    private val cache: EmbeddingCache? = null,
    var similarityThreshold: Float = 0.35f
) : SearchEngine {
    private val sessionsMap = mutableMapOf<String, Session>()
    private val sessionEmbeddings = mutableMapOf<String, SessionVectorIndex>()

    private class SessionVectorIndex(
        val threadNameEmbedding: FloatArray,
        val turnEmbeddings: List<FloatArray>
    )

    override suspend fun updateIndex(sessions: List<Session>, onProgress: ((Int, Int) -> Unit)?) {
        synchronized(sessionsMap) {
            sessionsMap.clear()
            sessionEmbeddings.clear()
        }

        val total = sessions.size
        var processed = 0
        val progressLock = Any()

        // Limit concurrency for CPU-heavy embedding calls
        val semaphore = Semaphore(4)
        coroutineScope {
            for (session in sessions) {
                launch {
                    semaphore.withPermit {
                        updateSession(session)
                    }
                    val current = synchronized(progressLock) {
                        processed++
                        processed
                    }
                    onProgress?.invoke(current, total)
                }
            }
        }
    }

    override suspend fun updateSession(session: Session) {
        val threadName = session.threadName ?: "Untitled Session"
        val threadNameEmb = try {
            val cached = cache?.get(threadName)
            if (cached != null) {
                cached
            } else {
                val emb = embedder.getEmbeddings(threadName)
                cache?.put(threadName, emb)
                emb
            }
        } catch (e: Throwable) {
            FloatArray(0)
        }

        val turnEmbs = mutableListOf<FloatArray>()
        for (turn in session.turns) {
            val turnText = "${turn.userMessage}\n${turn.assistantMessage}"
            val turnEmb = try {
                val cached = cache?.get(turnText)
                if (cached != null) {
                    cached
                } else {
                    val emb = embedder.getEmbeddings(turnText)
                    cache?.put(turnText, emb)
                    emb
                }
            } catch (e: Throwable) {
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
        if (query.isBlank()) {
            val allSessions = synchronized(sessionsMap) { sessionsMap.values.toList() }
            return allSessions
                .filter { filter.matches(it) }
                .map { SearchResult(it, emptyList(), score = 1.0f) }
                .sortedByDescending { it.session.updatedAt }
        }

        val queryEmbedding = try {
            embedder.getEmbeddings(query)
        } catch (e: Throwable) {
            return emptyList()
        }

        val results = mutableListOf<SearchResult>()
        val currentSessions = synchronized(sessionsMap) { sessionsMap.values.toList() }

        for (session in currentSessions) {
            if (!filter.matches(session)) continue

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

}
