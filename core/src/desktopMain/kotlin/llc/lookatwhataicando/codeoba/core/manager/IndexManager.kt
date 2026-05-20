package llc.lookatwhataicando.codeoba.core.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.search.SearchEngine
import llc.lookatwhataicando.codeoba.core.domain.source.SourceRegistry
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import llc.lookatwhataicando.codeoba.core.watcher.DirectoryWatcher
import java.io.File

class IndexManager(
    private val sourceRegistry: SourceRegistry,
    private val searchEngine: SearchEngine,
    private val scope: CoroutineScope
) {
    private val watchers = mutableListOf<DirectoryWatcher>()
    private var isScanning = false

    private val onIndexUpdatedListeners = mutableListOf<() -> Unit>()

    fun addIndexUpdatedListener(listener: () -> Unit) {
        onIndexUpdatedListeners.add(listener)
    }

    suspend fun initialScanAndWatch() = withContext(Dispatchers.IO) {
        if (isScanning) {
            log("IndexManager: Already scanning, ignoring request.")
            return@withContext
        }
        isScanning = true
        log("IndexManager: Beginning initial scan and watch...")

        try {
            val allSessions = mutableListOf<Session>()
            val activeAdapters = sourceRegistry.getActiveAdapters()
            log("IndexManager: Found ${activeAdapters.size} active adapters out of ${sourceRegistry.getAllAdapters().size} registered.")

            for (adapter in activeAdapters) {
                log("IndexManager: Scanning ${adapter.displayName}...")
                try {
                    val sessions = adapter.parseAllSessions()
                    log("IndexManager: Finished ${adapter.displayName} scan, found ${sessions.size} sessions.")
                    allSessions.addAll(sessions)
                } catch (e: Exception) {
                    log("IndexManager: Error scanning ${adapter.displayName}:", e)
                }
            }

            log("IndexManager: Updating search index with ${allSessions.size} total sessions...")
            searchEngine.updateIndex(allSessions)
            log("IndexManager: Index updated successfully. Notifying listeners...")
            notifyListeners()

            log("IndexManager: Setting up directory watchers...")
            for (adapter in activeAdapters) {
                val watchPaths = adapter.getWatchPaths()
                if (watchPaths.isEmpty()) {
                    log("IndexManager: No watch paths for ${adapter.displayName}")
                    continue
                }

                log("IndexManager: Watching paths for ${adapter.displayName}: $watchPaths")
                val watcher = DirectoryWatcher(
                    pathsToWatch = watchPaths,
                    fileFilter = adapter.getWatchFileFilter()
                ) { filePath ->
                    log("IndexManager: Watcher triggered for filePath: $filePath (exists: ${File(filePath).exists()})")
                    scope.launch(Dispatchers.IO) {
                        val file = File(filePath)
                        val isDatabaseOrDir = filePath.endsWith(".sqlite") ||
                                filePath.endsWith(".vscdb") ||
                                filePath.endsWith("-wal") ||
                                filePath.endsWith("-shm") ||
                                filePath.endsWith(".pb") ||
                                filePath.endsWith(".pbtxt") ||
                                file.isDirectory

                        if (isDatabaseOrDir) {
                            try {
                                log("IndexManager: Database or directory changed. Reloading all sessions for ${adapter.displayName}...")
                                val sessions = adapter.parseAllSessions()
                                searchEngine.removeSessionsBySource(adapter.id)
                                for (session in sessions) {
                                    searchEngine.updateSession(session)
                                }
                                log("IndexManager: Reloaded ${sessions.size} sessions from database.")
                                notifyListeners()
                            } catch (e: Exception) {
                                log("Error reloading ${adapter.displayName} after DB change: ${e.message ?: ""}", e)
                            }
                        } else {
                            try {
                                if (!file.exists()) {
                                    log("IndexManager: File deleted. Removing session: $filePath")
                                    searchEngine.removeSessionByPath(filePath)
                                } else {
                                    log("IndexManager: File updated. Parsing session: $filePath")
                                    val session = adapter.parseSession(filePath)
                                    if (session != null) {
                                        searchEngine.updateSession(session)
                                        log("IndexManager: Successfully updated session: ${session.id} (turns: ${session.turns.size})")
                                    } else {
                                        log("IndexManager: Parsing failed or empty session. Removing session: $filePath")
                                        searchEngine.removeSessionByPath(filePath)
                                    }
                                }
                                notifyListeners()
                            } catch (e: Exception) {
                                log("Error reloading session $filePath: ${e.message ?: ""}", e)
                            }
                        }
                    }
                }
                watcher.start(scope)
                watchers.add(watcher)
            }
            log("IndexManager: All directory watchers started successfully.")
        } catch (e: Exception) {
            log("IndexManager: Critical error during initialScanAndWatch:", e)
            throw e
        } finally {
            isScanning = false
            log("IndexManager: Scanning state cleared.")
        }
    }

    fun stopWatchers() {
        watchers.forEach { it.stop() }
        watchers.clear()
    }

    private fun notifyListeners() {
        scope.launch(Dispatchers.Main) {
            onIndexUpdatedListeners.forEach { it() }
        }
    }
}
