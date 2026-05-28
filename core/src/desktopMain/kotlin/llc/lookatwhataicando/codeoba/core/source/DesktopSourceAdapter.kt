package llc.lookatwhataicando.codeoba.core.source

import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter
import llc.lookatwhataicando.codeoba.core.manager.SessionCacheManager
import java.io.File

abstract class DesktopSourceAdapter : SourceAdapter {
    /**
     * Gets the base log directory for this source.
     */
    abstract fun getBaseDir(): File

    override fun isAvailable(): Boolean {
        return try {
            val dir = getBaseDir()
            dir.exists() && dir.isDirectory
        } catch (e: UnsupportedOperationException) {
            false
        }
    }

    override fun getDefaultLogPaths(): List<String> {
        return try {
            listOf(getBaseDir().absolutePath)
        } catch (e: UnsupportedOperationException) {
            emptyList()
        }
    }

    override fun getWatchPaths(): List<String> = getDefaultLogPaths()

    override fun deleteDataPaths(): Boolean {
        return try {
            val dir = getBaseDir()
            if (dir.exists()) dir.deleteRecursively() else true
        } catch (e: UnsupportedOperationException) {
            false
        }
    }

    override fun getDataPathsToDelete(): List<String> {
        return try {
            listOf(getBaseDir().absolutePath)
        } catch (e: UnsupportedOperationException) {
            emptyList()
        }
    }

    /**
     * Helper check to verify if a command line executable is installed.
     */
    protected fun isExecutableInstalled(binaryName: String): Boolean {
        val userHome = System.getProperty("user.home")
        val commonPaths = listOf(
            "/opt/homebrew/bin/$binaryName",
            "/usr/local/bin/$binaryName",
            "/usr/bin/$binaryName",
            "$userHome/.local/bin/$binaryName",
            "$userHome/.npm-global/bin/$binaryName"
        )
        for (path in commonPaths) {
            if (File(path).exists()) return true
        }

        try {
            val process = ProcessBuilder("which", binaryName)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            val exitCode = process.waitFor()
            if (exitCode == 0) return true
        } catch (_: Exception) {}

        return false
    }

    /**
     * Subclasses implement this method to perform the actual parsing of file content.
     */
    open suspend fun parseSessionContent(file: File): Session? {
        throw UnsupportedOperationException("File-based parsing is not supported by this source.")
    }

    override suspend fun parseSession(filePath: String): Session? {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return null

        val cached = SessionCacheManager.getCachedSessionForFile(id, filePath, file.lastModified(), file.length())
        if (cached != null) return cached

        val session = parseSessionContent(file) ?: return null

        val hash = SessionCacheManager.calculateMd5(file)
        SessionCacheManager.putCachedSession(id, filePath, file.lastModified(), file.length(), hash, session)

        return session
    }
}
