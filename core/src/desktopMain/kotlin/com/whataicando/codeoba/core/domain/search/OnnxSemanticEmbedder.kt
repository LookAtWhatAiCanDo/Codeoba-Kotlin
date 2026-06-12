package com.whataicando.codeoba.core.domain.search

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

class OnnxSemanticEmbedder(
    modelFile: File,
    vocabFile: File
) : SemanticEmbedder, AutoCloseable {

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val tokenizer = WordPieceTokenizer(vocabFile)
    private val embeddingDimension: Int

    init {
        val options = OrtSession.SessionOptions()
        // Optimize for low latency local CPU execution
        options.setIntraOpNumThreads(1)
        session = env.createSession(modelFile.absolutePath, options)

        // Discover embedding dimension from output shape metadata
        val outputMeta = session.outputInfo.values.firstOrNull()
        val shape = outputMeta?.info?.toString() // e.g. TensorInfo(NodeName=...,Type=...,Shape=[1, -1, 384])
        var dim = 384
        if (shape != null) {
            val parts = shape.split(",")
            if (parts.size >= 3) {
                val lastPart = parts.last().replace("]", "").replace(" ", "").trim().toIntOrNull()
                if (lastPart != null) {
                    dim = lastPart
                }
            }
        }
        embeddingDimension = dim
    }

    override suspend fun getEmbeddings(text: String): FloatArray {
        val tokenized = tokenizer.tokenizeToIds(text, maxLen = 256)
        
        val inputs = mutableMapOf<String, OnnxTensor>()
        try {
            if (session.inputNames.contains("input_ids")) {
                inputs["input_ids"] = OnnxTensor.createTensor(env, arrayOf(tokenized.inputIds))
            }
            if (session.inputNames.contains("attention_mask")) {
                inputs["attention_mask"] = OnnxTensor.createTensor(env, arrayOf(tokenized.attentionMask))
            }
            if (session.inputNames.contains("token_type_ids")) {
                inputs["token_type_ids"] = OnnxTensor.createTensor(env, arrayOf(tokenized.tokenTypeIds))
            }

            session.run(inputs).use { results ->
                val outputName = if (session.outputNames.contains("last_hidden_state")) "last_hidden_state" else session.outputNames.first()
                val outputTensor = results[outputName].get() as OnnxTensor
                @Suppress("UNCHECKED_CAST")
                val outputValue = outputTensor.value as Array<Array<FloatArray>> // [batch][seq_len][dim]
                
                // Mean Pooling
                val sentenceEmbedding = FloatArray(embeddingDimension)
                var validCount = 0
                val firstBatch = outputValue[0]
                val mask = tokenized.attentionMask
                for (i in firstBatch.indices) {
                    if (i < mask.size && mask[i] == 1L) {
                        val tokenEmb = firstBatch[i]
                        for (d in 0 until embeddingDimension) {
                            sentenceEmbedding[d] += tokenEmb[d]
                        }
                        validCount++
                    }
                }
                if (validCount > 0) {
                    for (d in 0 until embeddingDimension) {
                        sentenceEmbedding[d] /= validCount
                    }
                }

                // L2 Normalization
                var sumSquares = 0.0
                for (d in 0 until embeddingDimension) {
                    sumSquares += sentenceEmbedding[d] * sentenceEmbedding[d]
                }
                val magnitude = kotlin.math.sqrt(sumSquares)
                if (magnitude > 0) {
                    for (d in 0 until embeddingDimension) {
                        sentenceEmbedding[d] = (sentenceEmbedding[d] / magnitude).toFloat()
                    }
                }

                return sentenceEmbedding
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    override fun close() {
        try {
            session.close()
        } catch (_: Exception) {}
    }
}
