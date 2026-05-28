package llc.lookatwhataicando.codeoba.core.source

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter
import java.io.File
import java.time.Instant
import llc.lookatwhataicando.codeoba.core.manager.SessionCacheManager

class DesktopAntigravitySource : DesktopSourceAdapter() {
    override val id: String = "antigravity"
    override val displayName: String = "Google Antigravity"

    private val json = Json { ignoreUnknownKeys = true }

    // Strip <truncated N bytes> markers left by the logging system
    private val truncationRegex = Regex("<truncated \\d+ bytes>\\s*")

    @Volatile
    private var antigravityTitleMap: Map<String, String>? = null

    @Volatile
    private var lastPbFileModified: Long = 0L

    private fun getSessionTitle(sessionId: String): String {
        val userHome = System.getProperty("user.home")
        val pbFile = File(userHome, ".gemini/antigravity/agyhub_summaries_proto.pb")
        val currentModified = if (pbFile.exists() && pbFile.isFile) pbFile.lastModified() else 0L

        var map = antigravityTitleMap
        if (map == null || currentModified > lastPbFileModified) {
            map = buildAntigravityTitleMap()
            antigravityTitleMap = map
            lastPbFileModified = currentModified
        }
        return map[sessionId] ?: "Antigravity Session"
    }

    private fun buildAntigravityTitleMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val userHome = System.getProperty("user.home")
        val pbFile = File(userHome, ".gemini/antigravity/agyhub_summaries_proto.pb")
        if (pbFile.exists() && pbFile.isFile) {
            try {
                val bytes = pbFile.readBytes()
                val offset = intArrayOf(0)
                while (offset[0] < bytes.size) {
                    val tag = readVarint(bytes, offset)
                    val wireType = (tag and 0x07).toInt()
                    val fieldNumber = (tag ushr 3).toInt()
                    if (fieldNumber == 1 && wireType == 2) {
                        val entryLen = readVarint(bytes, offset).toInt()
                        val entryEnd = offset[0] + entryLen
                        if (entryEnd > bytes.size) break

                        var uuid: String? = null
                        var title: String? = null

                        while (offset[0] < entryEnd) {
                            val entryTag = readVarint(bytes, offset)
                            val entryWireType = (entryTag and 0x07).toInt()
                            val entryFieldNumber = (entryTag ushr 3).toInt()
                            if (entryFieldNumber == 1 && entryWireType == 2) {
                                val uuidLen = readVarint(bytes, offset).toInt()
                                if (offset[0] + uuidLen <= entryEnd) {
                                    uuid = String(bytes, offset[0], uuidLen, kotlin.text.Charsets.UTF_8)
                                    offset[0] += uuidLen
                                } else {
                                    offset[0] = entryEnd
                                }
                            } else if (entryFieldNumber == 2 && entryWireType == 2) {
                                val infoLen = readVarint(bytes, offset).toInt()
                                val infoEnd = offset[0] + infoLen
                                if (infoEnd <= entryEnd) {
                                    while (offset[0] < infoEnd) {
                                        val infoTag = readVarint(bytes, offset)
                                        val infoWireType = (infoTag and 0x07).toInt()
                                        val infoFieldNumber = (infoTag ushr 3).toInt()
                                        if (infoFieldNumber == 1 && infoWireType == 2) {
                                            val strLen = readVarint(bytes, offset).toInt()
                                            if (offset[0] + strLen <= infoEnd) {
                                                val str = String(bytes, offset[0], strLen, kotlin.text.Charsets.UTF_8)
                                                offset[0] += strLen
                                                if (title == null && !str.startsWith("\n") && !str.startsWith("file://")) {
                                                    title = str
                                                }
                                            } else {
                                                offset[0] = infoEnd
                                            }
                                        } else {
                                            skipField(bytes, offset, infoWireType, infoEnd)
                                        }
                                    }
                                } else {
                                    offset[0] = entryEnd
                                }
                            } else {
                                skipField(bytes, offset, entryWireType, entryEnd)
                            }
                        }
                        if (uuid != null && title != null) {
                            map[uuid] = title
                        }
                        offset[0] = entryEnd
                    } else {
                        skipField(bytes, offset, wireType, bytes.size)
                    }
                }
            } catch (_: Exception) {}
        }
        return map
    }

    private fun readVarint(bytes: ByteArray, offset: IntArray): Long {
        var result = 0L
        var shift = 0
        while (offset[0] < bytes.size) {
            val b = bytes[offset[0]++].toLong() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0L) {
                return result
            }
            shift += 7
            if (shift >= 64) throw IllegalStateException("Varint too long")
        }
        return result
    }

    private fun skipField(bytes: ByteArray, offset: IntArray, wireType: Int, limit: Int) {
        when (wireType) {
            0 -> {
                readVarint(bytes, offset)
            }
            1 -> {
                offset[0] = (offset[0] + 8).coerceAtMost(limit)
            }
            2 -> {
                val len = readVarint(bytes, offset).toInt()
                offset[0] = (offset[0] + len).coerceAtMost(limit)
            }
            5 -> {
                offset[0] = (offset[0] + 4).coerceAtMost(limit)
            }
            else -> {
                offset[0] = limit
            }
        }
    }

    override fun getBaseDir(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, ".gemini/antigravity/brain")
    }

    override fun getWatchPaths(): List<String> {
        val userHome = System.getProperty("user.home")
        return listOf(File(userHome, ".gemini/antigravity").absolutePath)
    }

    override fun isAppInstalled(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> {
                File("/Applications/Antigravity.app").exists() || File("/Applications/Gemini.app").exists()
            }
            else -> {
                val userHome = System.getProperty("user.home")
                File(userHome, ".gemini/antigravity").exists()
            }
        }
    }

    // Only re-index when transcript.jsonl itself changes — not log/task files
    override fun getWatchFileFilter(): ((String) -> Boolean) = { path ->
        path.endsWith("transcript.jsonl") ||
                path.endsWith("agyhub_summaries_proto.pb") ||
                (path.contains("annotations") && path.endsWith(".pbtxt"))
    }

    private fun clean(text: String): String {
        return text.replace(Regex("<truncated (\\d+) bytes>\\s*")) { match ->
            val bytes = match.groupValues[1]
            "\n\n[⚠️ SYSTEM LIMIT: Truncated $bytes bytes of log output here]\n\n"
        }.trim()
    }

    private fun String.escapeToolTags(): String {
        return this.replace("[[[TOOL", "\\[\\[\\[TOOL")
            .replace("[[[/TOOL", "\\[\\[\\[/TOOL")
    }

    /**
     * Format a tool call entry into a human-readable block for the assistant message.
     */
    private fun formatToolEntry(type: String, content: String, toolCalls: JsonArray?, timestamp: Long): String {
        val label = when (type) {
            "VIEW_FILE" -> "📄 View File"
            "RUN_COMMAND" -> "⚡ Run Command"
            "CODE_ACTION" -> "✏️ Code Edit"
            "GREP_SEARCH" -> "🔍 Search"
            "LIST_DIRECTORY" -> "📂 List Directory"
            "SEARCH_WEB" -> "🌐 Web Search"
            "GENERIC" -> "🔧 Tool"
            "SYSTEM_MESSAGE" -> "⚙️ System Message"
            "ERROR_MESSAGE" -> "❌ Error"
            else -> "🔧 $type"
        }

        val headerParts = mutableListOf<String>()
        if (toolCalls != null && toolCalls.isNotEmpty()) {
            try {
                for (tc in toolCalls) {
                    val tcObj = tc.jsonObject
                    val name = tcObj["name"]?.jsonPrimitive?.content ?: ""
                    val args = tcObj["args"]?.jsonObject
                    if (args != null) {
                        val summary = when (name) {
                            "view_file" -> args["AbsolutePath"]?.jsonPrimitive?.content?.removeSurrounding("\"")?.let { clean(it) }
                            "run_command" -> args["CommandLine"]?.jsonPrimitive?.content?.removeSurrounding("\"")?.let { clean(it) }
                            "grep_search" -> {
                                val query = args["Query"]?.jsonPrimitive?.content?.removeSurrounding("\"")?.let { clean(it) }
                                val path = args["SearchPath"]?.jsonPrimitive?.content?.removeSurrounding("\"")?.let { clean(it) }
                                if (query != null) "Query: $query" + (if (path != null) " in $path" else "") else null
                            }
                            "list_dir" -> args["DirectoryPath"]?.jsonPrimitive?.content?.removeSurrounding("\"")?.let { clean(it) }
                            "replace_file_content", "multi_replace_file_content" ->
                                args["TargetFile"]?.jsonPrimitive?.content?.removeSurrounding("\"")?.let { clean(it) }
                            "write_to_file" -> args["TargetFile"]?.jsonPrimitive?.content?.removeSurrounding("\"")?.let { clean(it) }
                            else -> null
                        }
                        if (summary != null) headerParts.add(summary)
                    }
                }
            } catch (_: Exception) {
            }
        }

        val header = if (headerParts.isNotEmpty()) {
            "$label: ${headerParts.joinToString(", ")}"
        } else {
            label
        }.escapeToolTags()

        val cleaned = clean(content).escapeToolTags()
        return "[[[TOOL:$type|$header|$timestamp]]]\n$cleaned\n[[[/TOOL]]]"
    }

    override suspend fun parseSessionContent(file: File): Session? {
        val filePath = file.absolutePath

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            return null
        }

        // Get sessionId from parent directory of .system_generated
        val sessionId = file.parentFile?.parentFile?.parentFile?.name ?: file.nameWithoutExtension

        // We'll build a chronological list of "events" and then group them into turns
        data class Event(val isUser: Boolean, val text: String, val timestamp: Long, val model: String?)
        val events = mutableListOf<Event>()
        var cwd: String? = null
        var currentModel: String? = null

        for (line in lines) {
            if (line.trim().isEmpty()) continue
            try {
                val element = json.parseToJsonElement(line).jsonObject
                val type = element["type"]?.jsonPrimitive?.content ?: continue
                val source = element["source"]?.jsonPrimitive?.content ?: ""
                val createdAtStr = element["created_at"]?.jsonPrimitive?.content
                val timestamp = createdAtStr?.let {
                    try { Instant.parse(it).toEpochMilli() } catch (_: Exception) { null }
                } ?: 0L
                val content = element["content"]?.jsonPrimitive?.content ?: ""
                val toolCalls = element["tool_calls"]?.jsonArray

                // Track user settings changes for model selection
                val userSettingsChange = element["user_settings_change"]?.jsonObject
                if (userSettingsChange != null) {
                    val modelSel = userSettingsChange["Model Selection"]?.jsonPrimitive?.content
                    if (modelSel != null) {
                        currentModel = modelSel
                    }
                }
                if (content.contains("<USER_SETTINGS_CHANGE>")) {
                    val settingsContent = content.substringAfter("<USER_SETTINGS_CHANGE>").substringBefore("</USER_SETTINGS_CHANGE>")
                    val lineWithModel = settingsContent.lines().firstOrNull { it.contains("`Model Selection`") }
                    if (lineWithModel != null) {
                        val afterTo = lineWithModel.substringAfter(" to ").trim()
                        // The model name ends at the first ". " separator before the boilerplate instructions.
                        // e.g. "Claude Sonnet 4.6 (Thinking). No need to comment..." → "Claude Sonnet 4.6 (Thinking)"
                        val modelName = afterTo.substringBefore(". ").trimEnd('.')
                        if (modelName.isNotEmpty()) {
                            currentModel = modelName.trim()
                        }
                    }
                }

                // Extract Cwd from run_command or other tool call args if available
                if (toolCalls != null && toolCalls.isNotEmpty()) {
                    for (tc in toolCalls) {
                        try {
                            val tcObj = tc.jsonObject
                            val args = tcObj["args"]?.jsonObject
                            if (args != null) {
                                val argCwd = args["Cwd"]?.jsonPrimitive?.content ?: args["cwd"]?.jsonPrimitive?.content
                                if (argCwd != null) {
                                    cwd = argCwd.removeSurrounding("\"")
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                when {
                    // User messages
                    type == "USER_INPUT" && source == "USER_EXPLICIT" -> {
                        var cleanContent = content.trim()
                        val match = Regex("^\\s*<USER_REQUEST>([\\s\\S]*?)</USER_REQUEST>\\s*(?:<ADDITIONAL_METADATA>|<USER_SETTINGS_CHANGE>|$)", RegexOption.IGNORE_CASE).find(content)
                        if (match != null) {
                            cleanContent = match.groupValues[1].trim()
                        }
                        cleanContent = clean(cleanContent)
                        if (cleanContent.isNotEmpty()) {
                            events.add(Event(isUser = true, text = cleanContent, timestamp = timestamp, model = currentModel))
                        }
                    }

                    // Model text responses
                    (type == "PLANNER_RESPONSE" || type == "ASK_QUESTION") && source == "MODEL" -> {
                        val cleanContent = clean(content).escapeToolTags()
                        if (cleanContent.isNotEmpty()) {
                            events.add(Event(isUser = false, text = cleanContent, timestamp = timestamp, model = currentModel))
                        }
                    }

                    // Tool outputs — these are actions the assistant took
                    type in setOf("VIEW_FILE", "RUN_COMMAND", "CODE_ACTION", "GREP_SEARCH",
                                  "LIST_DIRECTORY", "SEARCH_WEB", "GENERIC") && source == "MODEL" -> {
                        val formatted = formatToolEntry(type, content, toolCalls, timestamp)
                        if (formatted.isNotBlank()) {
                            events.add(Event(isUser = false, text = formatted, timestamp = timestamp, model = currentModel))
                        }
                    }

                    // System messages
                    type == "SYSTEM_MESSAGE" && source == "SYSTEM" -> {
                        var cleanContent = content.trim()
                        val match = Regex("^\\s*<SYSTEM_MESSAGE>([\\s\\S]*?)</SYSTEM_MESSAGE>\\s*$", RegexOption.IGNORE_CASE).find(cleanContent)
                        if (match != null) {
                            cleanContent = match.groupValues[1].trim()
                        } else {
                            val intro = "The following is a <SYSTEM_MESSAGE> not actually sent by the user. It is provided by the system as important information to pay attention to."
                            if (cleanContent.startsWith(intro)) {
                                cleanContent = cleanContent.substring(intro.length).trim()
                            }
                        }
                        val formatted = formatToolEntry(type, cleanContent, toolCalls, timestamp)
                        if (formatted.isNotBlank()) {
                            events.add(Event(isUser = false, text = formatted, timestamp = timestamp, model = currentModel))
                        }
                    }

                    // Error messages
                    type == "ERROR_MESSAGE" && source == "SYSTEM" -> {
                        val formatted = formatToolEntry(type, content, toolCalls, timestamp)
                        if (formatted.isNotBlank()) {
                            events.add(Event(isUser = false, text = formatted, timestamp = timestamp, model = currentModel))
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (events.isEmpty()) return null

        // Group events into turns: each user message starts a new turn,
        // and all following assistant events are concatenated into the assistant message.
        val turns = mutableListOf<Turn>()
        var turnCount = 0
        var idx = 0
        while (idx < events.size) {
            val ev = events[idx]
            if (ev.isUser) {
                // Gather all following non-user events into the assistant response
                val assistantParts = mutableListOf<String>()
                var nextIdx = idx + 1
                var turnModel = ev.model
                var activeTimeMs = 0L
                var currentTimestamp = ev.timestamp
                while (nextIdx < events.size && !events[nextIdx].isUser) {
                    val nextEv = events[nextIdx]
                    assistantParts.add(nextEv.text)
                    val gap = (nextEv.timestamp - currentTimestamp).coerceAtLeast(0L)
                    // Cap gaps at 2 minutes (120,000 ms), assuming > 2 min means waiting for user input/approval.
                    // If a gap is capped, we estimate 15 seconds of active work for that step.
                    activeTimeMs += if (gap > 120_000L) 15_000L else gap
                    currentTimestamp = nextEv.timestamp
                    if (nextEv.model != null) {
                        turnModel = nextEv.model
                    }
                    nextIdx++
                }
                val assistantMessage = assistantParts.joinToString("\n\n")
                val extraData = mutableMapOf("computeTimeMs" to activeTimeMs.toString())
                val finalModel = turnModel ?: "Unknown"
                extraData["model"] = finalModel
                turns.add(
                    Turn(
                        turnId = "${sessionId}_${turnCount++}",
                        userMessage = ev.text,
                        assistantMessage = assistantMessage,
                        timestamp = ev.timestamp,
                        extraData = extraData
                    )
                )
                idx = nextIdx
            } else {
                // Orphaned assistant events (before first user message)
                val extraData = mutableMapOf("computeTimeMs" to "0")
                val finalModel = ev.model ?: "Unknown"
                extraData["model"] = finalModel
                turns.add(
                    Turn(
                        turnId = "${sessionId}_${turnCount++}",
                        userMessage = "",
                        assistantMessage = ev.text,
                        timestamp = ev.timestamp,
                        extraData = extraData
                    )
                )
                idx++
            }
        }

        val firstTime = events.firstOrNull()?.timestamp ?: file.lastModified()
        val lastTime = events.lastOrNull()?.timestamp ?: file.lastModified()

        val userHome = System.getProperty("user.home")
        val annotationFile = File(userHome, ".gemini/antigravity/annotations/$sessionId.pbtxt")
        val isArchived = if (annotationFile.exists() && annotationFile.isFile) {
            try {
                val text = annotationFile.readText()
                val normalized = text.replace(Regex("\\s+"), "")
                normalized.contains("archived:true")
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }

        val session = Session(
            id = sessionId,
            sourceId = id,
            filePath = filePath,
            timestamp = firstTime,
            updatedAt = lastTime,
            cwd = cwd,
            threadName = getSessionTitle(sessionId),
            turns = turns,
            isArchived = isArchived
        )
        return session
    }

    override suspend fun parseAllSessions(): List<Session> {
        val baseDir = getBaseDir()
        if (!baseDir.exists() || !baseDir.isDirectory) return emptyList()

        val userHome = System.getProperty("user.home")
        val pbFile = File(userHome, ".gemini/antigravity/agyhub_summaries_proto.pb")
        lastPbFileModified = if (pbFile.exists() && pbFile.isFile) pbFile.lastModified() else 0L

        // Force rebuild/refresh the title map for this run
        antigravityTitleMap = buildAntigravityTitleMap()

        val sessions = mutableListOf<Session>()
        baseDir.walkTopDown().forEach { file ->
            if (file.isFile && file.name == "transcript.jsonl") {
                val session = parseSession(file.absolutePath)
                if (session != null) sessions.add(session)
            }
        }
        return sessions
    }
}
