package com.whataicando.codeoba.core.domain.parser

import java.util.concurrent.atomic.AtomicReference

object SummarizerProvider {
    private val currentRef = AtomicReference<Summarizer>(StubSummarizer())
    
    fun current(): Summarizer = currentRef.get()
    
    fun install(real: Summarizer) {
        currentRef.set(real)
    }
    
    fun revertToStub() {
        currentRef.set(StubSummarizer())
    }
}
