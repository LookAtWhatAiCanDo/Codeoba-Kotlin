package com.whataicando.codeoba.core.domain.search

import java.io.File

class WordPieceTokenizer(vocabFile: File) {
    private val vocab = mutableMapOf<String, Int>()
    val unkId: Int
    val clsId: Int
    val sepId: Int
    val padId: Int

    init {
        vocabFile.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                vocab[trimmed] = vocab.size
            }
        }
        unkId = vocab["[UNK]"] ?: 100
        clsId = vocab["[CLS]"] ?: 101
        sepId = vocab["[SEP]"] ?: 102
        padId = vocab["[PAD]"] ?: 0
    }

    fun tokenizeToIds(text: String, maxLen: Int = 256): TokenizedInput {
        val words = tokenizeToWords(text.lowercase())
        val inputIds = mutableListOf<Int>()
        
        inputIds.add(clsId)
        
        for (word in words) {
            if (word.isEmpty()) continue
            val wordTokens = mutableListOf<Int>()
            var start = 0
            var isBad = false
            while (start < word.length) {
                var end = word.length
                var curSubstrId = -1
                while (start < end) {
                    val substr = if (start == 0) word.substring(start, end) else "##" + word.substring(start, end)
                    val id = vocab[substr]
                    if (id != null) {
                        curSubstrId = id
                        break
                    }
                    end--
                }
                if (curSubstrId == -1) {
                    isBad = true
                    break
                }
                wordTokens.add(curSubstrId)
                start = end
            }
            if (isBad) {
                inputIds.add(unkId)
            } else {
                inputIds.addAll(wordTokens)
            }
        }
        
        // Truncate if too long (leave space for [SEP])
        val finalInputIds = if (inputIds.size > maxLen - 1) {
            inputIds.subList(0, maxLen - 1).toMutableList()
        } else {
            inputIds.toMutableList()
        }
        finalInputIds.add(sepId)
        
        val attentionMask = MutableList(finalInputIds.size) { 1L }
        val idsLong = finalInputIds.map { it.toLong() }.toMutableList()
        
        // Padding to maxLen
        while (idsLong.size < maxLen) {
            idsLong.add(padId.toLong())
            attentionMask.add(0L)
        }
        
        val tokenTypeIds = LongArray(maxLen) { 0L }
        
        return TokenizedInput(
            inputIds = idsLong.toLongArray(),
            attentionMask = attentionMask.toLongArray(),
            tokenTypeIds = tokenTypeIds
        )
    }

    private fun tokenizeToWords(text: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val char = text[i]
            if (char.isWhitespace()) {
                i++
            } else if (char.isLetterOrDigit()) {
                val start = i
                while (i < text.length && text[i].isLetterOrDigit()) {
                    i++
                }
                result.add(text.substring(start, i))
            } else {
                result.add(char.toString())
                i++
            }
        }
        return result
    }
}

data class TokenizedInput(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray
)
