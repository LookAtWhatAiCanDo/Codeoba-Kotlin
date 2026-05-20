package llc.lookatwhataicando.codeoba.core.domain.search

import kotlinx.coroutines.runBlocking
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class SemanticEmbedderTest {

    @Test
    fun testHashSemanticEmbedder() = runBlocking {
        val embedder = HashSemanticEmbedder()

        val text1 = "how to build a project with kotlin"
        val text2 = "how do I build kotlin projects"
        val text3 = "apples grow on trees in the autumn"

        val emb1 = embedder.getEmbeddings(text1)
        val emb2 = embedder.getEmbeddings(text2)
        val emb3 = embedder.getEmbeddings(text3)

        // Print individual word similarities
        val uniqueWords = (text1.lowercase().split(Regex("\\s+")) + text2.lowercase().split(Regex("\\s+")) + text3.lowercase().split(Regex("\\s+")))
            .filter { it.isNotEmpty() }
            .distinct()

        val wordEmbeddings = uniqueWords.associateWith { embedder.getEmbeddings(it) }

        println("--- Word-to-Word Cosine Similarities ---")
        for (i in uniqueWords.indices) {
            for (j in i until uniqueWords.size) {
                val w1 = uniqueWords[i]
                val w2 = uniqueWords[j]
                val sim = cosineSimilarity(wordEmbeddings[w1]!!, wordEmbeddings[w2]!!)
                if (w1 == w2 || sim > 0.1f || sim < -0.1f) {
                    println("  $w1 <-> $w2 = $sim")
                }
            }
        }

        println("--- Text Similarities ---")
        val simSimilar = cosineSimilarity(emb1, emb2)
        val simDisjoint = cosineSimilarity(emb1, emb3)
        println("simSimilar: $simSimilar, simDisjoint: $simDisjoint")

        // Similar should be strictly greater than disjoint
        assertTrue(simSimilar > simDisjoint, "Similar similarity ($simSimilar) should be greater than disjoint similarity ($simDisjoint)")
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
