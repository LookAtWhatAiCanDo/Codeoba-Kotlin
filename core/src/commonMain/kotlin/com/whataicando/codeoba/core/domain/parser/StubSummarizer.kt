package com.whataicando.codeoba.core.domain.parser

import com.whataicando.codeoba.core.domain.model.Session

class StubSummarizer : Summarizer {
    override fun summarize(session: Session, parserConfigJson: String?): SummaryResult {
        return SummaryResult.Unavailable("AI-powered summarization requires an active Codeoba subscription.")
    }
}
