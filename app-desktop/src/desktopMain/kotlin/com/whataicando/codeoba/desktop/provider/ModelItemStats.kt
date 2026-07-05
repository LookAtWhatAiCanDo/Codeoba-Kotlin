package com.whataicando.codeoba.desktop.provider

data class ModelItemStats(
    val modelName: String,
    val turnCount: Int,
    val promptChars: Long,
    val responseChars: Long,
    val computeTimeMs: Long,
    val totalTokens: Long,
    val speedTps: Double
)
