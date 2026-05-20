package llc.lookatwhataicando.codeoba.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Turn(
    val turnId: String,
    val userMessage: String,
    val assistantMessage: String,
    val timestamp: Long = 0L,
    val extraData: Map<String, String> = emptyMap()
)
