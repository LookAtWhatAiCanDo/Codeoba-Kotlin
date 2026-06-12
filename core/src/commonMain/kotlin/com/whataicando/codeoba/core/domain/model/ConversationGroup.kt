package com.whataicando.codeoba.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupTask(
    val id: String, // UUID string
    val title: String,
    val isCompleted: Boolean,
    val associatedSessionId: String? = null
)

@Serializable
data class ConversationGroup(
    val name: String, // slash-separated, e.g. "Project A/Subtask 1"
    val description: String = "",
    val status: String = "Active", // "Active", "Completed", "Paused"
    val sessionIds: Set<String> = emptySet(),
    val tasks: List<GroupTask> = emptyList(),
    val pastWorkSummary: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
