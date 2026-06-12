package com.whataicando.codeoba.core.domain.search

interface EmbeddingCache {
    fun get(text: String): FloatArray?
    fun put(text: String, vector: FloatArray)
}
