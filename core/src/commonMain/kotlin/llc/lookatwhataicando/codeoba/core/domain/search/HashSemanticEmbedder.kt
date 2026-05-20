package llc.lookatwhataicando.codeoba.core.domain.search

import kotlin.math.sqrt

class HashSemanticEmbedder(private val dimensions: Int = 384) : SemanticEmbedder {
    override suspend fun getEmbeddings(text: String): FloatArray {
        val vector = FloatArray(dimensions)
        val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return vector

        for (word in words) {
            val random = kotlin.random.Random(word.hashCode())
            for (d in 0 until dimensions) {
                val weight = random.nextFloat() * 2.0f - 1.0f
                vector[d] += weight
            }
        }

        var sumSquares = 0.0
        for (i in 0 until dimensions) {
            sumSquares += vector[i] * vector[i]
        }
        val magnitude = sqrt(sumSquares)
        if (magnitude > 0) {
            for (i in 0 until dimensions) {
                vector[i] = (vector[i] / magnitude).toFloat()
            }
        }

        return vector
    }
}
