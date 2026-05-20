package llc.lookatwhataicando.codeoba.core.domain.source

import llc.lookatwhataicando.codeoba.core.domain.model.Session

interface SourceAdapter {
    val id: String
    val displayName: String

    /**
     * Checks whether this source is active or available on the current machine
     * (e.g. if the default folder paths exist or are readable).
     */
    fun isAvailable(): Boolean

    /**
     * Returns default paths where this source typically stores log/db files.
     */
    fun getDefaultLogPaths(): List<String>

    /**
     * Returns the specific directories or file paths that Codeoba should watch for live updates.
     */
    fun getWatchPaths(): List<String>

    /**
     * Optional file filter for live-update watching. Return a predicate (file path -> Boolean)
     * to restrict which files trigger re-indexing, or null to watch all files.
     */
    fun getWatchFileFilter(): ((String) -> Boolean)? = null

    /**
     * Parses a specific file path into a unified Session object.
     */
    suspend fun parseSession(filePath: String): Session?

    /**
     * Performs a full scan of all default paths and parses all found sessions.
     */
    suspend fun parseAllSessions(): List<Session>

    /**
     * Checks whether the underlying application itself is currently installed
     * (e.g. app package in Applications directory or executable on path).
     * Defaults to true if unknown.
     */
    fun isAppInstalled(): Boolean = true

    /**
     * Deletes the local data paths/databases stored by this application
     * to clean up disk space if requested by the user.
     * Returns true if cleanup was successful, false otherwise.
     */
    fun deleteDataPaths(): Boolean = false

    /**
     * Returns the list of exact file or directory paths that will be deleted
     * when deleteDataPaths() is called.
     */
    fun getDataPathsToDelete(): List<String> = emptyList()
}

