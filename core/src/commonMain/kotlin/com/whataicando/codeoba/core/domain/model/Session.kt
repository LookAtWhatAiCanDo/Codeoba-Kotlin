package com.whataicando.codeoba.core.domain.model

import kotlinx.serialization.Serializable
import com.whataicando.codeoba.core.domain.parser.SessionSummary

@Serializable
data class Session(
    val id: String,
    val sourceId: String,       // e.g. "claude", "cursor", "antigravity", "coder", "aider"
    val filePath: String,       // path to the original file/db on disk
    val timestamp: Long,        // session creation time in milliseconds
    val updatedAt: Long,        // last updated time in milliseconds
    val cwd: String?,           // detected project workspace directory
    val threadName: String?,    // thread name or title
    val turns: List<Turn>,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val summary: SessionSummary? = null
)
