package com.whataicando.codeoba.core.util

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection

object ModelDownloader {
    private const val MODEL_URL = "https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/onnx/model_quantized.onnx"
    private const val VOCAB_URL = "https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/vocab.txt"

    fun getModelDir(): File {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".codeoba/models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getModelFile(): File = File(getModelDir(), "model_quantized.onnx")
    fun getVocabFile(): File = File(getModelDir(), "vocab.txt")

    fun isModelDownloaded(): Boolean {
        return getModelFile().exists() && getModelFile().length() > 10_000_000 &&
                getVocabFile().exists() && getVocabFile().length() > 100_000
    }

    fun deleteModelFiles() {
        val model = getModelFile()
        val vocab = getVocabFile()
        if (model.exists()) model.delete()
        if (vocab.exists()) vocab.delete()
    }

    fun downloadModel(onProgress: (Float) -> Unit) {
        val modelFile = getModelFile()
        val vocabFile = getVocabFile()

        val tempModel = File(getModelDir(), "model_quantized.onnx.tmp")
        val tempVocab = File(getModelDir(), "vocab.txt.tmp")

        try {
            // 1. Download vocab (smaller, ~232 KB)
            downloadFile(VOCAB_URL, tempVocab) { progress ->
                // Vocab is only ~1% of the total size, let's map it to 0-1% of overall progress
                onProgress(progress * 0.01f)
            }
            // 2. Download model (~22.9 MB)
            downloadFile(MODEL_URL, tempModel) { progress ->
                onProgress(0.01f + progress * 0.99f)
            }

            // Rename temp files to final files
            if (tempVocab.renameTo(vocabFile) && tempModel.renameTo(modelFile)) {
                // Success!
            } else {
                throw Exception("Failed to rename temporary download files.")
            }
        } catch (e: Exception) {
            if (tempModel.exists()) tempModel.delete()
            if (tempVocab.exists()) tempVocab.delete()
            throw e
        }
    }

    private fun downloadFile(urlStr: String, destFile: File, onProgress: (Float) -> Unit) {
        val url = java.net.URI(urlStr).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.requestMethod = "GET"
        
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw Exception("HTTP Error $responseCode: ${connection.responseMessage} when downloading $urlStr")
        }

        val contentLength = connection.contentLengthLong
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(destFile).use { output ->
                val data = ByteArray(8192)
                var totalBytesRead = 0L
                var bytesRead: Int
                while (input.read(data).also { bytesRead = it } != -1) {
                    output.write(data, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength
                        onProgress(progress)
                    }
                }
            }
        }
    }
}
