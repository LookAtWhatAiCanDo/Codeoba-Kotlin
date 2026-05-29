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

class DesktopCodexSource : DesktopSourceAdapter() {
    override val id: String = "codex"
    override val displayName: String = "OpenAI Codex"

    private val json = Json { ignoreUnknownKeys = true }

    override fun getBaseDir(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, ".codex")
    }

    override fun getDefaultLogPaths(): List<String> {
        val baseDir = getBaseDir()
        return listOf(
            File(baseDir, "sessions").absolutePath,
            File(baseDir, "archived_sessions").absolutePath
        )
    }

    override fun getWatchPaths(): List<String> {
        return getDefaultLogPaths()
    }

    override fun isAppInstalled(): Boolean {
        // First check if ~/.codex/sessions exists and has files (as a fallback)
        val sessionsDir = File(getBaseDir(), "sessions")
        if (sessionsDir.exists() && sessionsDir.isDirectory) {
            val files = sessionsDir.listFiles()
            if (files != null && files.any { it.isFile && it.extension == "jsonl" }) {
                return true
            }
        }
        return isExecutableInstalled("codex")
    }

    @Volatile
    private var sessionTitleMap: Map<String, String>? = null

    @Volatile
    private var lastIndexFileModified: Long = 0L

    private fun getSessionTitle(sessionId: String): String {
        val indexFile = File(getBaseDir(), "session_index.jsonl")
        val currentModified = if (indexFile.exists() && indexFile.isFile) indexFile.lastModified() else 0L

        var map = sessionTitleMap
        if (map == null || currentModified > lastIndexFileModified) {
            buildSessionTitleMap()
            map = sessionTitleMap
            lastIndexFileModified = currentModified
        }
        return map?.get(sessionId) ?: "Codex Session"
    }

    private fun buildSessionTitleMap() {
        val map = mutableMapOf<String, String>()
        val indexFile = File(getBaseDir(), "session_index.jsonl")
        if (indexFile.exists() && indexFile.isFile) {
            try {
                indexFile.readLines().forEach { line ->
                    if (line.trim().isNotEmpty()) {
                        val obj = json.parseToJsonElement(line).jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content
                        val name = obj["thread_name"]?.jsonPrimitive?.content
                        if (id != null && name != null) {
                            map[id] = name
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore index parsing errors
            }
        }
        sessionTitleMap = map
    }

    override suspend fun parseSessionContent(file: File): Session? {
        val filePath = file.absolutePath

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            return null
        }

        var sessionId = file.nameWithoutExtension.substringAfter("rollout-")
        var cwd: String? = null
        var createdTime: Long = file.lastModified()
        var updatedTime: Long = file.lastModified()
        val rawTurns = mutableListOf<RawTurn>()

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

                val payload = element["payload"]?.jsonObject ?: continue

                if (type == "session_meta") {
                    payload["id"]?.jsonPrimitive?.content?.let { sessionId = it }
                    payload["cwd"]?.jsonPrimitive?.content?.let { cwd = it }
                    val timeStr = payload["timestamp"]?.jsonPrimitive?.content
                    timeStr?.let {
                        try {
                            createdTime = Instant.parse(it).toEpochMilli()
                        } catch (e: Exception) {}
                    }
                } else if (type == "response_item") {
                    val role = payload["role"]?.jsonPrimitive?.content ?: continue
                    val modelName = payload["model"]?.jsonPrimitive?.content
                    val contentArray = payload["content"]?.jsonArray
                    val textBuilder = StringBuilder()
                    contentArray?.forEach { item ->
                        val itemObj = item.jsonObject
                        val text = itemObj["text"]?.jsonPrimitive?.content
                        if (text != null) {
                            textBuilder.append(text).append("\n")
                        }
                    }
                    val text = textBuilder.toString().trim()
                    if (text.isNotEmpty()) {
                        rawTurns.add(RawTurn(isUser = (role == "user"), text = text, timestamp = timestamp, model = modelName))
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors for individual lines
            }
        }

        if (rawTurns.isEmpty()) return null

        val turns = mutableListOf<Turn>()
        var currentIdx = 0
        var turnCount = 0
        while (currentIdx < rawTurns.size) {
            val userRaw = rawTurns[currentIdx]
            if (userRaw.isUser) {
                var assistantText = ""
                var computeTimeMs = 0L
                var modelName: String? = null
                if (currentIdx + 1 < rawTurns.size && !rawTurns[currentIdx + 1].isUser) {
                    val assistantRaw = rawTurns[currentIdx + 1]
                    assistantText = assistantRaw.text
                    computeTimeMs = (assistantRaw.timestamp - userRaw.timestamp).coerceAtLeast(0L)
                    modelName = assistantRaw.model
                    currentIdx += 2
                } else {
                    currentIdx += 1
                }
                 val extraData = mutableMapOf("computeTimeMs" to computeTimeMs.toString())
                extraData["model"] = modelName ?: "Unknown"
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
        val threadName = getSessionTitle(sessionId)
        val isArchived = File(filePath).parentFile?.name == "archived_sessions"

        val session = Session(
            id = sessionId,
            sourceId = id,
            filePath = filePath,
            timestamp = firstTime,
            updatedAt = lastTime,
            cwd = cwd,
            threadName = threadName,
            turns = turns,
            isArchived = isArchived
        )
        return session
    }

    override suspend fun parseAllSessions(): List<Session> {
        val baseDir = getBaseDir()
        if (!baseDir.exists() || !baseDir.isDirectory) return emptyList()

        buildSessionTitleMap()

        val sessions = mutableListOf<Session>()
        val defaultPaths = getDefaultLogPaths()
        for (path in defaultPaths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && file.extension == "jsonl" && file.name.startsWith("rollout-")) {
                        val session = parseSession(file.absolutePath)
                        if (session != null) {
                            sessions.add(session)
                        }
                    }
                }
            }
        }
        return sessions
    }

    override fun getWatchFileFilter(): ((String) -> Boolean) = { path ->
        val file = File(path)
        (file.isFile && file.extension == "jsonl" && file.name.startsWith("rollout-")) ||
                file.name == "session_index.jsonl"
    }

    override fun refreshCachedSession(session: Session): Session {
        val currentTitle = getSessionTitle(session.id)
        if (session.threadName != currentTitle) {
            return session.copy(threadName = currentTitle)
        }
        return session
    }

    private data class RawTurn(val isUser: Boolean, val text: String, val timestamp: Long, val model: String? = null)
}
