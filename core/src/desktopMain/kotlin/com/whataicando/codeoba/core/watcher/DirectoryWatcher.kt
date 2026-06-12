package com.whataicando.codeoba.core.watcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private val PRUNED_DIRS = setOf(
    "node_modules", ".git", "build", ".gradle", ".idea",
    "target", "bin", "out", "dist", "vendor"
)

class DirectoryWatcher(
    private val pathsToWatch: List<String>,
    private val intervalMs: Long = 1000L,
    private val fileFilter: ((String) -> Boolean)? = null,   // null = accept all files
    private val debounceMs: Long = 500L,                     // trailing edge debounce window
    private val onFileChanged: suspend (String) -> Unit
) {
    private var job: Job? = null
    private var watcherScope: CoroutineScope? = null
    private val fileStates = mutableMapOf<String, Long>()    // path -> lastModified
    private val debounceJobs = mutableMapOf<String, Job>()   // path -> Job

    fun start(scope: CoroutineScope) {
        if (job != null) return
        watcherScope = scope
        job = scope.launch(Dispatchers.IO) {
            scan(triggerCallbacks = false)
            while (isActive) {
                delay(intervalMs)
                scan(triggerCallbacks = true)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        debounceJobs.values.forEach { it.cancel() }
        debounceJobs.clear()
        watcherScope = null
    }

    private suspend fun scan(triggerCallbacks: Boolean) {
        val visitedPaths = mutableSetOf<String>()
        for (path in pathsToWatch) {
            val root = File(path)
            if (!root.exists()) continue
            if (root.isFile) {
                if (fileFilter == null || fileFilter.invoke(root.absolutePath)) {
                    visitedPaths.add(root.absolutePath)
                    checkFile(root, triggerCallbacks)
                }
            } else if (root.isDirectory) {
                try {
                    root.walkTopDown()
                        .onEnter { dir -> dir.name !in PRUNED_DIRS }
                        .forEach { child ->
                            if (child.isFile && (fileFilter == null || fileFilter.invoke(child.absolutePath))) {
                                visitedPaths.add(child.absolutePath)
                                checkFile(child, triggerCallbacks)
                            }
                        }
                } catch (_: Exception) {}
            }
        }

        // Deletion detection
        val deletedPaths = mutableListOf<String>()
        for (path in fileStates.keys) {
            if (path !in visitedPaths) {
                deletedPaths.add(path)
            }
        }

        for (path in deletedPaths) {
            fileStates.remove(path)
            debounceJobs[path]?.cancel()
            debounceJobs.remove(path)
            if (triggerCallbacks) {
                val scope = watcherScope
                if (scope != null && scope.isActive) {
                    scope.launch(Dispatchers.IO) {
                        onFileChanged(path)
                    }
                }
            }
        }
    }

    private suspend fun checkFile(file: File, triggerCallbacks: Boolean) {
        val path = file.absolutePath
        val lastMod = file.lastModified()
        val prevMod = fileStates[path]

        val changed = prevMod == null || lastMod > prevMod
        if (changed) fileStates[path] = lastMod

        if (triggerCallbacks && changed) {
            val scope = watcherScope
            if (scope != null && scope.isActive) {
                debounceJobs[path]?.cancel()
                debounceJobs[path] = scope.launch(Dispatchers.IO) {
                    delay(debounceMs)
                    onFileChanged(path)
                }
            }
        }
    }
}
