package com.whataicando.codeoba.core.domain.parser

import com.whataicando.codeoba.core.domain.model.Session

interface Summarizer {
    fun summarize(session: Session, parserConfigJson: String?): SummaryResult
}

sealed interface SummaryResult {
    data class Ok(val summary: SessionSummary) : SummaryResult
    data class Unavailable(val reason: String) : SummaryResult
    data class Failed(val reason: String, val cause: String? = null) : SummaryResult
}
