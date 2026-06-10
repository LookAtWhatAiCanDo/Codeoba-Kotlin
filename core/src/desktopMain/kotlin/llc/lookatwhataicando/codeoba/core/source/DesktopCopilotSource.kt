package llc.lookatwhataicando.codeoba.core.source

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import llc.lookatwhataicando.codeoba.core.util.PlatformUtils
import java.io.File
import java.time.Instant

class DesktopCopilotSource : DesktopSourceAdapter() {
    override val id: String = "copilot"
    override val displayName: String = "GitHub Copilot"

    private val json = Json { ignoreUnknownKeys = true }

    override fun getBaseDir(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, ".copilot/session-state")
    }

    override fun isAppInstalled(): Boolean {
        return when {
            PlatformUtils.isMac() -> {
                File("/Applications/GitHub Copilot.app").exists()
            }
            else -> {
                val userHome = System.getProperty("user.home")
                File(userHome, ".copilot").exists()
            }
        }
    }

    override fun getWatchFileFilter(): ((String) -> Boolean) = { path ->
        path.endsWith("events.jsonl") || path.endsWith("workspace.yaml")
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

    private data class ToolStartInfo(
        val toolName: String,
        val arguments: String,
        val timestamp: Long
    )

    private data class ParsedEvent(
        val isUser: Boolean,
        val text: String,
        val timestamp: Long,
        val model: String?
    )

    override suspend fun parseSessionContent(file: File): Session? {
        val filePath = file.absolutePath
        val parentDir = file.parentFile ?: return null
        val workspaceYamlFile = File(parentDir, "workspace.yaml")

        // 1. Parse workspace.yaml for session metadata
        if (!workspaceYamlFile.exists() || !workspaceYamlFile.isFile) return null

        var sessionId = parentDir.name
        var threadName = "GitHub Copilot Session"
        var cwd: String? = null
        var gitBranch: String? = null
        var repository: String? = null
        var createdTime: Long = file.lastModified()
        var updatedTime: Long = file.lastModified()

        try {
            workspaceYamlFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                val key = trimmed.substringBefore(":").trim()
                val value = trimmed.substringAfter(":").trim().removeSurrounding("\"").removeSurrounding("'")
                when (key) {
                    "id" -> sessionId = value
                    "name" -> threadName = value
                    "cwd" -> cwd = value
                    "branch" -> gitBranch = value
                    "repository" -> repository = value
                    "created_at" -> {
                        try {
                            createdTime = Instant.parse(value).toEpochMilli()
                        } catch (_: Exception) {}
                    }
                    "updated_at" -> {
                        try {
                            updatedTime = Instant.parse(value).toEpochMilli()
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }

        // 2. Parse events.jsonl for conversation turns and tools
        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            return null
        }

        val eventsList = mutableListOf<ParsedEvent>()
        val activeToolCalls = mutableMapOf<String, ToolStartInfo>()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            try {
                val element = json.parseToJsonElement(trimmedLine).jsonObject
                val type = element["type"]?.jsonPrimitive?.content ?: continue
                val timestampStr = element["timestamp"]?.jsonPrimitive?.content
                val timestamp = timestampStr?.let {
                    try { Instant.parse(it).toEpochMilli() } catch (_: Exception) { null }
                } ?: file.lastModified()

                val data = element["data"]?.jsonObject ?: continue

                when (type) {
                    "user.message" -> {
                        val content = data["content"]?.jsonPrimitive?.content ?: ""
                        val cleanContent = clean(content)
                        if (cleanContent.isNotEmpty()) {
                            eventsList.add(
                                ParsedEvent(
                                    isUser = true,
                                    text = cleanContent,
                                    timestamp = timestamp,
                                    model = null
                                )
                            )
                        }
                    }
                    "assistant.message" -> {
                        val content = data["content"]?.jsonPrimitive?.content ?: ""
                        val reasoningText = data["reasoningText"]?.jsonPrimitive?.content ?: ""
                        val model = data["model"]?.jsonPrimitive?.content

                        val textBuilder = StringBuilder()
                        if (reasoningText.isNotBlank()) {
                            textBuilder.append("> [!NOTE]\n> **Reasoning:**\n> ")
                            textBuilder.append(reasoningText.trim().replace("\n", "\n> "))
                            textBuilder.append("\n\n")
                        }
                        if (content.isNotBlank() && content != "...") {
                            textBuilder.append(clean(content).escapeToolTags())
                        }

                        val text = textBuilder.toString().trim()
                        if (text.isNotEmpty()) {
                            eventsList.add(
                                ParsedEvent(
                                    isUser = false,
                                    text = text,
                                    timestamp = timestamp,
                                    model = model
                                )
                            )
                        }
                    }
                    "tool.execution_start" -> {
                        val toolCallId = data["toolCallId"]?.jsonPrimitive?.content ?: continue
                        val toolName = data["toolName"]?.jsonPrimitive?.content ?: ""
                        val argumentsObj = data["arguments"]?.jsonObject
                        val argumentsStr = argumentsObj?.toString() ?: ""
                        activeToolCalls[toolCallId] = ToolStartInfo(toolName, argumentsStr, timestamp)
                    }
                    "tool.execution_complete" -> {
                        val toolCallId = data["toolCallId"]?.jsonPrimitive?.content ?: continue
                        val success = data["success"]?.jsonPrimitive?.content?.toBoolean() ?: true
                        val startInfo = activeToolCalls.remove(toolCallId) ?: continue

                        val resultObj = data["result"]?.jsonObject
                        val detailedContent = resultObj?.get("detailedContent")?.jsonPrimitive?.content
                        val content = resultObj?.get("content")?.jsonPrimitive?.content ?: ""
                        val outputContent = if (!detailedContent.isNullOrBlank()) detailedContent else content

                        // Map toolName to standard Codeoba tool labels
                        val label = when (startInfo.toolName.lowercase()) {
                            "view_file" -> "📄 View File"
                            "run_command", "bash" -> "⚡ Run Command"
                            "replace_file_content", "multi_replace_file_content", "write_to_file" -> "✏️ Code Edit"
                            "grep_search" -> "🔍 Search"
                            "list_dir" -> "📂 List Directory"
                            "search_web" -> "🌐 Web Search"
                            else -> "🔧 Tool"
                        }

                        val toolCategory = when (startInfo.toolName.lowercase()) {
                            "view_file" -> "VIEW_FILE"
                            "run_command", "bash" -> "RUN_COMMAND"
                            "replace_file_content", "multi_replace_file_content", "write_to_file" -> "CODE_ACTION"
                            "grep_search" -> "GREP_SEARCH"
                            "list_dir" -> "LIST_DIRECTORY"
                            "search_web" -> "SEARCH_WEB"
                            else -> "GENERIC"
                        }

                        // Try to extract detailed summary from arguments
                        var summary = startInfo.toolName
                        try {
                            val argsObj = json.parseToJsonElement(startInfo.arguments).jsonObject
                            val extracted = when (startInfo.toolName.lowercase()) {
                                "view_file" -> argsObj["AbsolutePath"]?.jsonPrimitive?.content
                                "run_command" -> argsObj["CommandLine"]?.jsonPrimitive?.content
                                "bash" -> argsObj["command"]?.jsonPrimitive?.content
                                "grep_search" -> argsObj["Query"]?.jsonPrimitive?.content
                                "list_dir" -> argsObj["DirectoryPath"]?.jsonPrimitive?.content
                                "replace_file_content", "multi_replace_file_content", "write_to_file" -> argsObj["TargetFile"]?.jsonPrimitive?.content
                                else -> null
                            }
                            if (extracted != null) {
                                summary = extracted.removeSurrounding("\"")
                            }
                        } catch (_: Exception) {}

                        val header = "$label: $summary".escapeToolTags()
                        val cleanedOutput = clean(outputContent).escapeToolTags()
                        val formattedOutput = if (!success) {
                            "[[[TOOL:ERROR_MESSAGE|❌ Error: $summary|${startInfo.timestamp}]]]\n$cleanedOutput\n[[[/TOOL]]]"
                        } else {
                            "[[[TOOL:$toolCategory|$header|${startInfo.timestamp}]]]\n$cleanedOutput\n[[[/TOOL]]]"
                        }

                        eventsList.add(
                            ParsedEvent(
                                isUser = false,
                                text = formattedOutput,
                                timestamp = startInfo.timestamp,
                                model = data["model"]?.jsonPrimitive?.content
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        if (eventsList.isEmpty()) return null

        // Sort events chronologically by timestamp before grouping into turns
        eventsList.sortBy { it.timestamp }

        // 3. Group events into turns: each user message starts a new turn,
        // and all following assistant/tool events are concatenated into the assistantMessage.
        val turns = mutableListOf<Turn>()
        var turnCount = 0
        var idx = 0
        while (idx < eventsList.size) {
            val ev = eventsList[idx]
            if (ev.isUser) {
                val assistantParts = mutableListOf<String>()
                var nextIdx = idx + 1
                var turnModel = ev.model
                var activeTimeMs = 0L
                var currentTimestamp = ev.timestamp

                while (nextIdx < eventsList.size && !eventsList[nextIdx].isUser) {
                    val nextEv = eventsList[nextIdx]
                    if (nextEv.text.isNotEmpty()) {
                        assistantParts.add(nextEv.text)
                    }
                    val gap = (nextEv.timestamp - currentTimestamp).coerceAtLeast(0L)
                    // Cap gaps at 2 minutes, assuming > 2 min means waiting for user input/approval.
                    activeTimeMs += if (gap > 120_000L) 15_000L else gap
                    currentTimestamp = nextEv.timestamp
                    if (nextEv.model != null) {
                        turnModel = nextEv.model
                    }
                    nextIdx++
                }

                val assistantMessage = assistantParts.joinToString("\n\n")
                val extraData = mutableMapOf("computeTimeMs" to activeTimeMs.toString())
                extraData["model"] = turnModel ?: "Unknown"

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
                // Orphaned assistant events
                val extraData = mutableMapOf("computeTimeMs" to "0")
                extraData["model"] = ev.model ?: "Unknown"
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

        val firstTime = eventsList.firstOrNull()?.timestamp ?: createdTime
        val lastTime = eventsList.lastOrNull()?.timestamp ?: updatedTime

        return Session(
            id = sessionId,
            sourceId = id,
            filePath = filePath,
            timestamp = firstTime,
            updatedAt = lastTime,
            cwd = cwd,
            threadName = threadName,
            turns = turns,
            isArchived = false
        )
    }

    override suspend fun parseAllSessions(): List<Session> {
        val baseDir = getBaseDir()
        if (!baseDir.exists() || !baseDir.isDirectory) return emptyList()

        val sessions = mutableListOf<Session>()
        baseDir.walkTopDown().forEach { file ->
            if (file.isFile && file.name == "events.jsonl") {
                val session = parseSession(file.absolutePath)
                if (session != null) {
                    sessions.add(session)
                }
            }
        }
        return sessions
    }
}
