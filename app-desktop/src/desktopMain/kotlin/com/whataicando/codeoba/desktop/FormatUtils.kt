package com.whataicando.codeoba.desktop

import androidx.compose.ui.graphics.Color
import com.whataicando.codeoba.core.domain.model.Session
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.text.NumberFormat
import kotlin.math.round

fun copyToClipboard(text: String) {
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    } catch (_: Exception) {
        // Ignore clipboard operation failures
    }
}

// Colors helper for premium source badges
fun getSourceBadgeColors(sourceId: String): Pair<Color, Color> {
    return when (sourceId) {
        "claude" -> Pair(Color(0xFF00E676), Color(0x1F00E676)) // Green
        "antigravity" -> Pair(Color(0xFF2979FF), Color(0x1F2979FF)) // Blue
        "cursor" -> Pair(Color(0xFFD500F9), Color(0x1FD500F9)) // Purple
        "codex" -> Pair(Color(0xFF00BCD4), Color(0x1F00BCD4)) // Teal
        "aider" -> Pair(Color(0xFFFF3D00), Color(0x1FFF3D00)) // Red-Orange
        "copilot" -> Pair(Color(0xFF8F5FE8), Color(0x1F8F5FE8)) // Purple-Violet
        else -> Pair(AccentCyan, Color(0x1F00E5FF))
    }
}

fun formatNumber(number: Long): String {
    return NumberFormat.getNumberInstance().format(number)
}

fun formatSourceDisplayName(sourceId: String): String {
    return when (sourceId.lowercase()) {
        "claude" -> "Claude Code"
        "antigravity" -> "Google Antigravity"
        "cursor" -> "Cursor"
        "codex" -> "OpenAI Codex"
        "aider" -> "Aider"
        "copilot" -> "GitHub Copilot"
        else -> sourceId.substring(0, 1).uppercase() + sourceId.substring(1)
    }
}

fun getProductUrl(sourceId: String): String? {
    return when (sourceId.lowercase()) {
        "claude" -> "https://code.claude.com"
        "antigravity" -> "https://deepmind.google"
        "cursor" -> "https://cursor.com"
        "codex" -> "https://developers.openai.com"
        "aider" -> "https://aider.chat"
        "copilot" -> "https://github.com/features/ai/github-app"
        else -> null
    }
}

fun formatLanguageName(lang: String): String {
    if (lang.isEmpty()) return ""
    return when (lang.lowercase()) {
        "html" -> "HTML"
        "css" -> "CSS"
        "json" -> "JSON"
        "xml" -> "XML"
        "sql" -> "SQL"
        "js" -> "JavaScript"
        "ts" -> "TypeScript"
        else -> lang.substring(0, 1).uppercase() + lang.substring(1).lowercase()
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0s"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

fun getSessionComputeTimeMs(session: Session): Long {
    var totalMs = 0L
    for (turn in session.turns) {
        val ms = turn.extraData["computeTimeMs"]?.toLongOrNull()
        if (ms != null && ms > 0) {
            totalMs += ms.coerceAtMost(900_000L)
        } else if (turn.assistantMessage.isNotEmpty()) {
            val estMs = (turn.assistantMessage.length / 120.0 * 1000.0).toLong()
            totalMs += estMs.coerceIn(2000L, 60000L)
        }
    }
    return totalMs
}

fun formatSpeed(tokens: Long, ms: Long): String {
    if (ms <= 0) return "0.0 t/s"
    val tps = (tokens.toDouble() * 1000.0) / ms
    val rounded = round(tps * 10.0) / 10.0
    return "$rounded t/s"
}

fun formatTurnTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val date = java.util.Date(timestamp)
    val now = java.util.Date()
    
    val fmtYear = java.text.SimpleDateFormat("yyyy")
    val fmtDay = java.text.SimpleDateFormat("yyyyMMdd")
    
    return when {
        fmtDay.format(date) == fmtDay.format(now) -> {
            java.text.SimpleDateFormat("HH:mm:ss").format(date)
        }
        fmtYear.format(date) == fmtYear.format(now) -> {
            java.text.SimpleDateFormat("MMM d, HH:mm:ss").format(date)
        }
        else -> {
            java.text.SimpleDateFormat("MMM d, yyyy HH:mm").format(date)
        }
    }
}

