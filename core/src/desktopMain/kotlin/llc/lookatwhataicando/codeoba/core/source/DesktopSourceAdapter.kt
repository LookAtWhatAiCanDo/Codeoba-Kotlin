package llc.lookatwhataicando.codeoba.core.source

import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter
import llc.lookatwhataicando.codeoba.core.manager.SessionCacheManager
import llc.lookatwhataicando.codeoba.core.util.PlatformUtils
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
        val isWindows = PlatformUtils.isWindows()

        if (!isWindows) {
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
        }

        // Scan PATH directories
        val pathEnv = System.getenv("PATH") ?: ""
        val pathSeparator = File.pathSeparator
        val extensions = if (isWindows) {
            listOf("", ".exe", ".cmd", ".bat")
        } else {
            listOf("")
        }
        for (dir in pathEnv.split(pathSeparator)) {
            val dirFile = File(dir)
            if (dirFile.exists() && dirFile.isDirectory) {
                for (ext in extensions) {
                    if (File(dirFile, "$binaryName$ext").exists()) return true
                }
            }
        }

        // Fallback to executing command finder
        try {
            val cmd = if (isWindows) listOf("where", binaryName) else listOf("which", binaryName)
            val process = ProcessBuilder(cmd)
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

    /**
     * Allows subclasses to dynamically refresh metadata on a cached session
     * (e.g. thread name or archival status) from companion config files.
     */
    open fun refreshCachedSession(session: Session): Session {
        return session
    }

    override suspend fun parseSession(filePath: String): Session? {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return null

        val parser = llc.lookatwhataicando.codeoba.core.domain.parser.LogParserFactory.getParser()
        return parser.parse(file) {
            val cached = SessionCacheManager.getCachedSessionForFile(id, filePath, file.lastModified(), file.length())
            if (cached != null) {
                val refreshed = refreshCachedSession(cached)
                if (refreshed != cached) {
                    val hash = SessionCacheManager.calculateMd5(file)
                    SessionCacheManager.putCachedSession(id, filePath, file.lastModified(), file.length(), hash, refreshed)
                }
                refreshed
            } else {
                val session = parseSessionContent(file)
                if (session != null) {
                    val hash = SessionCacheManager.calculateMd5(file)
                    SessionCacheManager.putCachedSession(id, filePath, file.lastModified(), file.length(), hash, session)
                }
                session
            }
        }
    }
}
