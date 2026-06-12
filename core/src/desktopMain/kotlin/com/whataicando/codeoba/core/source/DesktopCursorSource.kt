package com.whataicando.codeoba.core.source

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import com.whataicando.codeoba.core.domain.model.Session
import com.whataicando.codeoba.core.domain.model.Turn
import com.whataicando.codeoba.core.manager.SessionCacheManager
import com.whataicando.codeoba.core.util.Logger.log
import com.whataicando.codeoba.core.util.PlatformUtils
import java.io.File
import java.sql.DriverManager

class DesktopCursorSource : DesktopSourceAdapter() {
    override val id: String = "cursor"
    override val displayName: String = "Cursor"

    private val json = Json { ignoreUnknownKeys = true }

    init {
        // Ensure the JDBC driver is registered
        Class.forName("org.sqlite.JDBC")
    }

    override fun getBaseDir(): File {
        val userHome = System.getProperty("user.home")
        return when {
            PlatformUtils.isMac() -> {
                File(userHome, "Library/Application Support/Cursor/User")
            }
            PlatformUtils.isWindows() -> {
                val appData = System.getenv("APPDATA")
                if (appData != null) File(appData, "Cursor/User") else File(userHome, "AppData/Roaming/Cursor/User")
            }
            else -> {
                File(userHome, ".config/Cursor/User")
            }
        }
    }

    private fun getGlobalDbFile(): File {
        return File(getBaseDir(), "globalStorage/state.vscdb")
    }

    private fun getWorkspaceStorageDir(): File {
        return File(getBaseDir(), "workspaceStorage")
    }

    override fun isAvailable(): Boolean {
        return getGlobalDbFile().exists()
    }

    override fun getDefaultLogPaths(): List<String> {
        val paths = mutableListOf(getGlobalDbFile().absolutePath)
        val wsDir = getWorkspaceStorageDir()
        if (wsDir.exists() && wsDir.isDirectory) paths.add(wsDir.absolutePath)
        return paths
    }

    override fun getWatchPaths(): List<String> = getDefaultLogPaths()

    override fun isAppInstalled(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> {
                File("/Applications/Cursor.app").exists()
            }
            os.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA") ?: ""
                File(localAppData, "Programs\\cursor\\Cursor.exe").exists()
            }
            else -> {
                File("/usr/share/cursor/cursor").exists() || File("/opt/Cursor").exists()
            }
        }
    }

    override fun deleteDataPaths(): Boolean {
        var success = true
        val dbFile = getGlobalDbFile()
        if (dbFile.exists()) {
            if (!dbFile.delete()) success = false
        }
        val wsDir = getWorkspaceStorageDir()
        if (wsDir.exists()) {
            if (!wsDir.deleteRecursively()) success = false
        }
        return success
    }

    override fun getDataPathsToDelete(): List<String> {
        return listOf(getGlobalDbFile().absolutePath, getWorkspaceStorageDir().absolutePath)
    }

    /**
     * Open the SQLite database in read-only WAL mode and execute a single
     * SELECT, returning all rows as a list of String-keyed maps.
     * Returns an empty list on any error (locked file, missing table, etc.).
     */
    private fun queryDb(dbPath: String, sql: String): List<Map<String, String>> {
        val results = mutableListOf<Map<String, String>>()
        val url = "jdbc:sqlite:file:${dbPath}?mode=ro"
        return try {
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.queryTimeout = 5   // seconds
                    stmt.executeQuery(sql).use { rs ->
                        val meta = rs.metaData
                        val colCount = meta.columnCount
                        while (rs.next()) {
                            val row = mutableMapOf<String, String>()
                            for (i in 1..colCount) {
                                row[meta.getColumnName(i)] = rs.getString(i) ?: ""
                            }
                            results.add(row)
                        }
                    }
                }
            }
            results
        } catch (e: Exception) {
            // Locked, missing table, corrupt — just return empty
            emptyList()
        }
    }



    private fun parseSessionFromJson(composerId: String, valueStr: String): Session? {
        val filePath = "composerData:$composerId"
        val size = valueStr.length.toLong()
        val hash = SessionCacheManager.calculateStringMd5(valueStr)
        val cached = SessionCacheManager.getCachedSessionForDb(id, filePath, hash, size)
        if (cached != null) return cached

        return try {
            val valObj = json.parseToJsonElement(valueStr).jsonObject
            val name       = valObj["name"]?.jsonPrimitive?.content ?: "Cursor Session"
            val createdAt  = valObj["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L
            val updatedAt  = valObj["lastUpdatedAt"]?.jsonPrimitive?.longOrNull ?: createdAt
            val conversation = valObj["conversation"]?.jsonArray ?: return null

            val turns = mutableListOf<Turn>()
            var idx = 0; var turnCount = 0
            while (idx < conversation.size) {
                val item = conversation[idx].jsonObject
                val type = item["type"]?.jsonPrimitive?.intOrNull ?: 1
                val text = item["text"]?.jsonPrimitive?.content ?: ""
                val modelName = item["model"]?.jsonPrimitive?.content
                    ?: valObj["model"]?.jsonPrimitive?.content
                    ?: valObj["modelName"]?.jsonPrimitive?.content
                    ?: "Unknown"
                val extraData = mapOf(
                    "model" to modelName,
                    "computeTimeMs" to "0"
                )
                if (type == 1) {
                    var assistantText = ""
                    if (idx + 1 < conversation.size) {
                        val next = conversation[idx + 1].jsonObject
                        if ((next["type"]?.jsonPrimitive?.intOrNull ?: 1) == 2) {
                            assistantText = next["text"]?.jsonPrimitive?.content ?: ""
                            idx += 2
                        } else idx++
                    } else idx++
                    turns.add(Turn("${composerId}_${turnCount++}", text, assistantText, createdAt, extraData))
                } else {
                    turns.add(Turn("${composerId}_${turnCount++}", "", text, createdAt, extraData))
                    idx++
                }
            }

            if (turns.isEmpty()) return null

            val cwd = composerToWorkspaceMap?.get(composerId)
            val session = Session(
                id          = composerId,
                sourceId    = id,
                filePath    = filePath,
                timestamp   = createdAt,
                updatedAt   = updatedAt,
                cwd         = cwd,
                threadName  = name,
                turns       = turns
            )
            SessionCacheManager.putCachedSession(id, filePath, lastModified = 0L, size = size, hash = hash, session = session)
            session
        } catch (e: Exception) { null }
    }

    // Workspace → folder mapping, built once per scan.
    // Only composerIds present in a workspace's allComposers list are considered active;
    // the global DB retains orphaned rows for deleted sessions indefinitely.
    @Volatile private var composerToWorkspaceMap: Map<String, String>? = null

    // The set of composer IDs that are actually visible/active in at least one workspace.
    // Used to filter out sessions that Cursor deleted from its UI but left in the global DB.
    @Volatile private var activeComposerIds: Set<String> = emptySet()

    private fun buildWorkspaceMap() {
        val map = mutableMapOf<String, String>()
        val wsDir = getWorkspaceStorageDir()
        if (!wsDir.exists()) {
            composerToWorkspaceMap = emptyMap()
            activeComposerIds = emptySet()
            return
        }

        val activeDirs = (wsDir.listFiles()?.filter { it.isDirectory } ?: emptyList())
            .filter { File(it, "workspace.json").exists() && File(it, "state.vscdb").exists() }
            .sortedByDescending { File(it, "state.vscdb").lastModified() }
            .take(100)

        log("CursorSource: Mapping ${activeDirs.size} workspaces via JDBC...")
        for (subDir in activeDirs) {
            try {
                val wsObj = json.parseToJsonElement(File(subDir, "workspace.json").readText()).jsonObject
                val folderUrl = wsObj["folder"]?.jsonPrimitive?.content ?: continue
                var folderPath = if (folderUrl.startsWith("file://"))
                    folderUrl.substringAfter("file://") else folderUrl
                if (folderPath.startsWith("/") && folderPath.length > 2 && folderPath[2] == ':') {
                    folderPath = folderPath.substring(1)
                }

                val rows = queryDb(
                    File(subDir, "state.vscdb").absolutePath,
                    "SELECT value FROM ItemTable WHERE key = 'composer.composerData' LIMIT 1;"
                )
                val valueStr = rows.firstOrNull()?.get("value") ?: continue
                val dataObj = json.parseToJsonElement(valueStr).jsonObject
                dataObj["allComposers"]?.jsonArray?.forEach { ci ->
                    val compId = ci.jsonObject["composerId"]?.jsonPrimitive?.content ?: return@forEach
                    map[compId] = folderPath
                }
            } catch (_: Exception) {}
        }
        composerToWorkspaceMap = map
        activeComposerIds = map.keys.toSet()
        log("CursorSource: Workspace map built — ${map.size} active composer IDs across all workspaces.")
    }

    override suspend fun parseSession(filePath: String): Session? {
        val globalDb = getGlobalDbFile()
        if (!globalDb.exists()) return null
        val composerId = if (filePath.startsWith("composerData:"))
            filePath.substringAfter("composerData:") else filePath

        // If the workspace map is populated, respect it as an allowlist.
        // A composerId not in any workspace's allComposers has been deleted from Cursor's UI.
        val knownIds = activeComposerIds
        if (knownIds.isNotEmpty() && composerId !in knownIds) return null

        val rows = queryDb(
            globalDb.absolutePath,
            "SELECT value FROM cursorDiskKV WHERE key = 'composerData:$composerId' LIMIT 1;"
        )
        val valueStr = rows.firstOrNull()?.get("value") ?: return null
        return parseSessionFromJson(composerId, valueStr)
    }

    override suspend fun parseAllSessions(): List<Session> {
        val globalDb = getGlobalDbFile()
        if (!globalDb.exists()) {
            log("CursorSource: Global DB not found, skipping.")
            return emptyList()
        }

        log("CursorSource: Querying global DB via JDBC...")
        val rows = queryDb(
            globalDb.absolutePath,
            "SELECT key, value FROM cursorDiskKV WHERE key LIKE 'composerData:%';"
        )
        log("CursorSource: Found ${rows.size} composer rows in global DB.")
        if (rows.isEmpty()) return emptyList()

        buildWorkspaceMap()
        val allowedIds = activeComposerIds
        log("CursorSource: ${allowedIds.size} active composer IDs in workspace allowlist.")

        val sessions = mutableListOf<Session>()
        var skipped = 0
        for (row in rows) {
            val key        = row["key"] ?: continue
            val composerId = key.substringAfter("composerData:")
            val valueStr   = row["value"] ?: continue

            // Skip orphaned sessions not in any workspace's active composer list.
            if (allowedIds.isNotEmpty() && composerId !in allowedIds) {
                skipped++
                continue
            }

            parseSessionFromJson(composerId, valueStr)?.let { sessions.add(it) }
        }
        if (skipped > 0) log("CursorSource: Skipped $skipped orphaned/deleted composer sessions.")
        log("CursorSource: Parsed ${sessions.size} Cursor sessions.")
        return sessions
    }

    override fun getWatchFileFilter(): ((String) -> Boolean) = { path ->
        path.endsWith("state.vscdb") ||
        path.endsWith("workspace.json") ||
        path.endsWith("-wal") ||
        path.endsWith("-shm")
    }
}
