package com.whataicando.codeoba.core.source

import com.whataicando.codeoba.core.domain.model.Session
import com.whataicando.codeoba.core.domain.model.Turn
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class DesktopAiderSource : DesktopSourceAdapter() {
    override val id: String = "aider"
    override val displayName: String = "Aider"

    @Volatile
    private var activeAiderPaths = emptyList<String>()

    override fun getBaseDir(): File {
        throw UnsupportedOperationException("Aider does not have a single base directory")
    }

    private fun getBaseDirs(): List<File> {
        val userHome = System.getProperty("user.home")
        val dirs = mutableListOf<File>()
        val devDir = File(userHome, "Dev")
        if (devDir.exists() && devDir.isDirectory) {
            dirs.add(devDir)
        }
        val githubDir = File(userHome, "GitHub")
        if (githubDir.exists() && githubDir.isDirectory) {
            dirs.add(githubDir)
        }
        return dirs
    }

    override fun isAvailable(): Boolean {
        return isAppInstalled() || activeAiderPaths.isNotEmpty()
    }

    override fun getDefaultLogPaths(): List<String> {
        return getBaseDirs().map { it.absolutePath }
    }

    override fun getWatchPaths(): List<String> {
        return activeAiderPaths
    }

    override fun isAppInstalled(): Boolean {
        if (activeAiderPaths.isNotEmpty()) return true
        return isExecutableInstalled("aider")
    }

    override fun deleteDataPaths(): Boolean {
        // Delete all .aider.chat.history.md files found in the active watch paths
        var success = true
        for (path in activeAiderPaths) {
            val file = File(path, ".aider.chat.history.md")
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) success = false
            }
        }
        activeAiderPaths = emptyList()
        return success
    }

    override fun getDataPathsToDelete(): List<String> {
        return activeAiderPaths.map { File(it, ".aider.chat.history.md").absolutePath }
    }

    override suspend fun parseSessionContent(file: File): Session? {
        val filePath = file.absolutePath

        val text = try {
            file.readText()
        } catch (e: Exception) {
            return null
        }

        val rawTurns = mutableListOf<RawTurn>()
        var createdTime = file.lastModified()
        val updatedTime = file.lastModified()

        val timeRegex = Regex("Aider chat started at (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})", RegexOption.IGNORE_CASE)
        val timeMatch = timeRegex.find(text)
        if (timeMatch != null) {
            try {
                val timeStr = timeMatch.groupValues[1]
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val localDateTime = LocalDateTime.parse(timeStr, formatter)
                createdTime = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {}
        }

        val pattern = Regex("(?:^|\\n)#### (User|Assistant|Aider|Bot):", RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(text).toList()
        for (i in matches.indices) {
            val match = matches[i]
            val role = match.groupValues[1].lowercase()
            val startContent = match.range.last + 1
            val endContent = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val content = text.substring(startContent, endContent).trim()
            if (content.isEmpty()) continue

            val isUser = role == "user"
            val isAssistant = role in setOf("assistant", "aider", "bot")

            if (isUser) {
                rawTurns.add(RawTurn(isUser = true, text = content, timestamp = createdTime))
            } else if (isAssistant) {
                rawTurns.add(RawTurn(isUser = false, text = content, timestamp = createdTime))
            }
        }

        if (rawTurns.isEmpty()) return null

        val turns = mutableListOf<Turn>()
        var currentIdx = 0
        var turnCount = 0
        val sessionId = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString()

        while (currentIdx < rawTurns.size) {
            val userRaw = rawTurns[currentIdx]
            if (userRaw.isUser) {
                var assistantText = ""
                var computeTimeMs = 0L
                if (currentIdx + 1 < rawTurns.size && !rawTurns[currentIdx + 1].isUser) {
                    val assistantRaw = rawTurns[currentIdx + 1]
                    assistantText = assistantRaw.text
                    computeTimeMs = (assistantRaw.timestamp - userRaw.timestamp).coerceAtLeast(0L)
                    currentIdx += 2
                } else {
                    currentIdx += 1
                }
                turns.add(
                    Turn(
                        turnId = "${sessionId}_${turnCount++}",
                        userMessage = userRaw.text,
                        assistantMessage = assistantText,
                        timestamp = userRaw.timestamp,
                        extraData = mapOf(
                            "computeTimeMs" to computeTimeMs.toString(),
                            "model" to "Unknown"
                        )
                    )
                )
            } else {
                turns.add(
                    Turn(
                        turnId = "${sessionId}_${turnCount++}",
                        userMessage = "",
                        assistantMessage = userRaw.text,
                        timestamp = userRaw.timestamp,
                        extraData = mapOf(
                            "computeTimeMs" to "0",
                            "model" to "Unknown"
                        )
                    )
                )
                currentIdx += 1
            }
        }

        val cwd = file.parentFile?.absolutePath
        val projectName = file.parentFile?.name ?: "Project"
        val threadName = "$projectName (Aider)"

        val session = Session(
            id = sessionId,
            sourceId = id,
            filePath = filePath,
            timestamp = createdTime,
            updatedAt = updatedTime,
            cwd = cwd,
            threadName = threadName,
            turns = turns
        )
        return session
    }

    override suspend fun parseAllSessions(): List<Session> {
        val baseDirs = getBaseDirs()
        val sessions = mutableListOf<Session>()
        val activeDirs = mutableSetOf<String>()
        baseDirs.forEach { dir ->
            dir.walkTopDown()
                .maxDepth(5)
                .onEnter { subDir ->
                    val name = subDir.name
                    name != "node_modules" && name != ".git" && name != "build" && name != ".gradle" &&
                    name != ".idea" && name != "target" && name != "bin" && name != "out" &&
                    name != "dist" && name != "vendor"
                }
                .forEach { file ->
                    if (file.isFile && file.name == ".aider.chat.history.md") {
                        val session = parseSession(file.absolutePath)
                        if (session != null) {
                            sessions.add(session)
                            file.parentFile?.absolutePath?.let { activeDirs.add(it) }
                        }
                    }
                }
        }
        activeAiderPaths = activeDirs.toList()
        return sessions
    }

    override fun getWatchFileFilter(): ((String) -> Boolean) = { path ->
        path.endsWith(".aider.chat.history.md")
    }

    private data class RawTurn(val isUser: Boolean, val text: String, val timestamp: Long)
}
