package llc.lookatwhataicando.codeoba.core.domain.search

interface SemanticEmbedder {
    /**
     * Generates a float array vector representation of the given text segment.
     */
    suspend fun getEmbeddings(text: String): FloatArray
}
