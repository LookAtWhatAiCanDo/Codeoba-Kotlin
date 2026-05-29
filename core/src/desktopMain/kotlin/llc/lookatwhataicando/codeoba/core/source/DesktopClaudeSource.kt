package llc.lookatwhataicando.codeoba.core.source

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter
import java.io.File
import java.time.Instant
import llc.lookatwhataicando.codeoba.core.manager.SessionCacheManager

class DesktopClaudeSource : DesktopSourceAdapter() {
    override val id: String = "claude"
    override val displayName: String = "Claude Code"

    private val json = Json { ignoreUnknownKeys = true }

    override fun getBaseDir(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, ".claude/projects")
    }

    override fun isAppInstalled(): Boolean {
        // First check if ~/.claude/projects exists and has files (as a fallback)
        val baseDir = getBaseDir()
        if (baseDir.exists() && baseDir.isDirectory) {
            val files = baseDir.listFiles()
            if (files != null && files.any { it.isFile && it.extension == "jsonl" }) {
                return true
            }
        }
        return isExecutableInstalled("claude")
    }

    override suspend fun parseSessionContent(file: File): Session? {
        val filePath = file.absolutePath

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            return null
        }

        val rawTurns = mutableListOf<RawTurn>()
        var sessionId: String = file.nameWithoutExtension
        var cwd: String? = null
        var gitBranch: String? = null
        var slug: String? = null
        val createdTime: Long = file.lastModified()
        val updatedTime: Long = file.lastModified()

        for (line in lines) {
            if (line.trim().isEmpty()) continue
            try {
                val element = json.parseToJsonElement(line).jsonObject
                val type = element["type"]?.jsonPrimitive?.content ?: continue
                val timestampStr = element["timestamp"]?.jsonPrimitive?.content
                val timestamp = timestampStr?.let {
                    try {
                        Instant.parse(it).toEpochMilli()
                    } catch (e: Exception) {
                        null
                    }
                } ?: 0L

                // Extract metadata if available
                element["sessionId"]?.jsonPrimitive?.content?.let { sessionId = it }
                element["cwd"]?.jsonPrimitive?.content?.let { cwd = it }
                element["gitBranch"]?.jsonPrimitive?.content?.let { gitBranch = it }
                element["slug"]?.jsonPrimitive?.content?.let { slug = it }

                if (type == "user") {
                    val msgObj = element["message"]?.jsonObject ?: continue
                    val content = msgObj["content"]?.jsonPrimitive?.content ?: ""
                    rawTurns.add(RawTurn(isUser = true, text = content, timestamp = timestamp))
                } else if (type == "assistant") {
                    val msgObj = element["message"]?.jsonObject ?: continue
                    val modelName = msgObj["model"]?.jsonPrimitive?.content
                    val contentArray = msgObj["content"]?.jsonArray
                    val textBuilder = StringBuilder()
                    if (contentArray != null) {
                        for (item in contentArray) {
                            val itemObj = item.jsonObject
                            val itemType = itemObj["type"]?.jsonPrimitive?.content
                            if (itemType == "text") {
                                val text = itemObj["text"]?.jsonPrimitive?.content ?: ""
                                textBuilder.append(text).append("\n")
                            }
                        }
                    }
                    val text = textBuilder.toString().trim()
                    if (text.isNotEmpty()) {
                        rawTurns.add(RawTurn(isUser = false, text = text, timestamp = timestamp, model = modelName))
                    }
                } else if (type == "system") {
                    val subtype = element["subtype"]?.jsonPrimitive?.content
                    if (subtype == "compact_boundary") {
                        val compactMetadata = element["compactMetadata"]?.jsonObject
                        val durationMs = compactMetadata?.get("durationMs")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                        rawTurns.add(RawTurn(
                            isUser = false,
                            text = "",
                            timestamp = timestamp,
                            isCompaction = true,
                            compactionTimeMs = durationMs
                        ))
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors for individual lines
            }
        }

        if (rawTurns.isEmpty()) return null

        // Pair raw turns into Turns
        val turns = mutableListOf<Turn>()
        var currentIdx = 0
        var turnCount = 0
        while (currentIdx < rawTurns.size) {
            val userRaw = rawTurns[currentIdx]
            if (userRaw.isUser) {
                var assistantText = ""
                var computeTimeMs = 0L
                var modelName: String? = null
                var hasCompaction = false
                var compactionTimeMs = 0L
                var nextIdx = currentIdx + 1
                val assistantParts = mutableListOf<String>()
                var lastTimestamp = userRaw.timestamp
                while (nextIdx < rawTurns.size && !rawTurns[nextIdx].isUser) {
                    val nextRaw = rawTurns[nextIdx]
                    if (nextRaw.isCompaction) {
                        hasCompaction = true
                        compactionTimeMs += nextRaw.compactionTimeMs
                    } else {
                        if (nextRaw.text.isNotEmpty()) {
                            assistantParts.add(nextRaw.text)
                        }
                    }
                    lastTimestamp = nextRaw.timestamp
                    if (nextRaw.model != null) {
                        modelName = nextRaw.model
                    }
                    nextIdx++
                }
                assistantText = assistantParts.joinToString("\n\n")
                computeTimeMs = (lastTimestamp - userRaw.timestamp).coerceAtLeast(0L)
                currentIdx = nextIdx

                val extraData = mutableMapOf("computeTimeMs" to computeTimeMs.toString())
                extraData["model"] = modelName ?: "Unknown"
                if (hasCompaction) {
                    extraData["isCompaction"] = "true"
                    extraData["compactionTimeMs"] = compactionTimeMs.toString()
                }
                turns.add(
                    Turn(
                        turnId = "${sessionId}_${turnCount++}",
                        userMessage = userRaw.text,
                        assistantMessage = assistantText,
                        timestamp = userRaw.timestamp,
                        extraData = extraData
                    )
                )
            } else {
                val extraData = mutableMapOf("computeTimeMs" to "0")
                extraData["model"] = userRaw.model ?: "Unknown"
                if (userRaw.isCompaction) {
                    extraData["isCompaction"] = "true"
                    extraData["compactionTimeMs"] = userRaw.compactionTimeMs.toString()
                }
                turns.add(
                    Turn(
                        turnId = "${sessionId}_${turnCount++}",
                        userMessage = "",
                        assistantMessage = userRaw.text,
                        timestamp = userRaw.timestamp,
                        extraData = extraData
                    )
                )
                currentIdx += 1
            }
        }

        val firstTime = rawTurns.firstOrNull()?.timestamp ?: createdTime
        val lastTime = rawTurns.lastOrNull()?.timestamp ?: updatedTime

        val cleanThreadName = if (!slug.isNullOrBlank()) {
            val userHome = System.getProperty("user.home")
            val planFile = File(userHome, ".claude/plans/$slug.md")
            if (planFile.exists() && planFile.isFile) {
                try {
                    val firstLine = planFile.useLines { it.firstOrNull() }?.trim()
                    if (firstLine != null && firstLine.startsWith("#")) {
                        val rawTitle = firstLine.removePrefix("#").trim()
                        when {
                            rawTitle.startsWith("Plan:", ignoreCase = true) -> rawTitle.substring(5).trim()
                            rawTitle.startsWith("Goal:", ignoreCase = true) -> rawTitle.substring(5).trim()
                            else -> rawTitle
                        }
                    } else {
                        slug.replace("-", " ").lowercase().replaceFirstChar { it.uppercase() }
                    }
                } catch (_: Exception) {
                    slug.replace("-", " ").lowercase().replaceFirstChar { it.uppercase() }
                }
            } else {
                slug.replace("-", " ").lowercase().replaceFirstChar { it.uppercase() }
            }
        } else {
            "Claude Session"
        }

        val session = Session(
            id = sessionId,
            sourceId = id,
            filePath = filePath,
            timestamp = firstTime,
            updatedAt = lastTime,
            cwd = cwd,
            threadName = cleanThreadName,
            turns = turns
        )
        return session
    }

    override suspend fun parseAllSessions(): List<Session> {
        val baseDir = getBaseDir()
        if (!baseDir.exists() || !baseDir.isDirectory) return emptyList()

        val sessions = mutableListOf<Session>()
        baseDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "jsonl") {
                val session = parseSession(file.absolutePath)
                if (session != null) {
                    sessions.add(session)
                }
            }
        }
        return sessions
    }

    override fun getWatchFileFilter(): ((String) -> Boolean) = { path ->
        path.endsWith(".jsonl")
    }

    private data class RawTurn(
        val isUser: Boolean,
        val text: String,
        val timestamp: Long,
        val model: String? = null,
        val isCompaction: Boolean = false,
        val compactionTimeMs: Long = 0L
    )
}
