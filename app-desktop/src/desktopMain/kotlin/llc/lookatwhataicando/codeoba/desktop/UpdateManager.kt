package llc.lookatwhataicando.codeoba.desktop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import llc.lookatwhataicando.codeoba.core.util.SemVer
import llc.lookatwhataicando.codeoba.core.util.PlatformUtils
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList(),
    val uiDelayMillis: Long = 1000L,
    val minAutoUpdateCheckIntervalSeconds: Long = UpdateManager.DEFAULT_MIN_UPDATE_CHECK_INTERVAL_SECONDS
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

object UpdateManager {
    const val GITHUB_REPO = "LookAtWhatAiCanDo/Codeoba"
    const val DEFAULT_MIN_UPDATE_CHECK_INTERVAL_SECONDS = 97200L // 27 hours
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile var ignoreUpdateThrottling = isLocalUrl(getUpdateUrl())
    @Volatile var forceUpdateAvailable = false
    @Volatile var mockUpdateNotes = false
    @Volatile var lastCheckError: String? = null

    val currentVersion: String by lazy {
        UpdateManager::class.java.getResourceAsStream("/version.txt")
            ?.bufferedReader()
            ?.use { it.readText().trim() }
            ?: "1.0.0"
    }

    fun getUpdatesDir(): File {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".codeoba/updates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun cleanUpdatesDir() {
        try {
            val dir = getUpdatesDir()
            dir.listFiles()?.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            log("UpdateManager: Failed to clean updates directory: ${e.message}")
        }
    }

    fun getUpdateUrl(): String {
        val rawBase = System.getProperty("codeoba.base_url") ?: System.getenv("CODEOBA_BASE_URL")
        if (!rawBase.isNullOrBlank()) {
            var trimmed = rawBase.trim()
            val isLocal = isLocalUrl(trimmed)
            if (!isWebUrl(trimmed)) {
                trimmed = if (isLocal) {
                    "http://$trimmed"
                } else {
                    "https://$trimmed"
                }
            } else if (trimmed.lowercase().startsWith("http://") && !isLocal) {
                // Prevent accidental insecure update endpoints in production.
                trimmed = "https://${trimmed.removePrefix("http://")}"
            }
            return if (trimmed.endsWith("/api/update")) {
                trimmed
            } else {
                val separator = if (trimmed.endsWith("/")) "" else "/"
                "$trimmed${separator}api/update"
            }
        }

        return "https://codeoba.com/api/update"
    }

    fun checkLatestRelease(): GitHubRelease? {
        lastCheckError = null
        if (mockUpdateNotes) {
            log("UpdateManager: Generating mock release changelog for XSS/URI validation testing...")
            return GitHubRelease(
                tagName = "99.9.9",
                htmlUrl = "https://github.com/$GITHUB_REPO/releases/tag/v99.9.9",
                body = """
                    # Malicious Release Notes XSS Test
                    
                    This release contains multiple XSS and link injection payloads to verify sanitization:
                    
                    1. HTML Script tag (should be printed as text):
                    <script>alert('Inline XSS Executed!')</script>
                    <script src="https://evil.com/xss.js"></script>
                    
                    2. HTML Image with onerror handler (should be printed as text):
                    <img src="does-not-exist.jpg" onerror="alert('Img Onerror XSS Executed!')" />
                    
                    3. Unsafe link schemes (clicks should be blocked and logged):
                    - [Malicious JS Link](javascript:alert('Link XSS Executed!'))
                    - [Malicious JS Confirm Link](javascript:confirm('Clicking this executes JS!'))
                    - [Malicious Data URI Link](data:text/html,<script>alert('Data URI XSS!')</script>)
                    
                    4. Safe standard link (should open correctly):
                    - [Safe Web Link](https://github.com/LookAtWhatAiCanDo/Codeoba)
                """.trimIndent(),
                assets = listOf(
                    GitHubAsset(
                        name = "codeoba-99.9.9.pkg",
                        browserDownloadUrl = "https://github.com/$GITHUB_REPO/releases/download/v99.9.9/codeoba-99.9.9.pkg"
                    ),
                    GitHubAsset(
                        name = "codeoba-99.9.9.msi",
                        browserDownloadUrl = "https://github.com/$GITHUB_REPO/releases/download/v99.9.9/codeoba-99.9.9.msi"
                    ),
                    GitHubAsset(
                        name = "codeoba-99.9.9.deb",
                        browserDownloadUrl = "https://github.com/$GITHUB_REPO/releases/download/v99.9.9/codeoba-99.9.9.deb"
                    )
                ),
                uiDelayMillis = 0L,
                minAutoUpdateCheckIntervalSeconds = 0L
            )
        }

        val urlStr = getUpdateUrl()
        log("UpdateManager: Sending POST request to $urlStr")
        try {
            val url = URI(urlStr).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "POST"
            connection.setDoOutput(true)
            connection.setRequestProperty("Content-Length", "0")
            
            // Build custom User-Agent: Codeoba/{version} ({OS}; {arch}; GUID-{guid})
            val version = currentVersion
            val os = System.getProperty("os.name") ?: "Unknown OS"
            val arch = System.getProperty("os.arch") ?: "Unknown Arch"
            val guid = SettingsManager.getInstallGuid()
            val userAgent = "Codeoba/$version ($os; $arch; GUID-$guid)"
            
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "application/json")
            
            log("UpdateManager: Request Headers: User-Agent=$userAgent, Accept=application/json")
            log("UpdateManager: Writing empty request body (Content-Length: 0)")
            connection.outputStream.close()
 
            val responseCode = connection.responseCode
            log("UpdateManager: Received response code: $responseCode")
            if (responseCode !in 200..299) {
                val errorMsg = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(100)?.trim()
                } catch (e: Exception) { null }
                val errorDetails = if (!errorMsg.isNullOrBlank()) " ($errorMsg)" else ""
                log("UpdateManager: HTTP error checking updates: $responseCode$errorDetails")
                lastCheckError = "HTTP $responseCode$errorDetails"
                return null
            }
 
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            log("UpdateManager: Received response body (truncated to 2000 chars): ${text.take(2000)}")
            val release = json.decodeFromString<GitHubRelease>(text)
            log("UpdateManager: Update check successful. Latest Release Tag: ${release.tagName}, uiDelayMillis: ${release.uiDelayMillis}, minAutoUpdateCheckIntervalSeconds: ${release.minAutoUpdateCheckIntervalSeconds}")
            return release
        } catch (e: Exception) {
            log("UpdateManager: Exception checking updates: ${e.message}", e)
            val baseMsg = e.localizedMessage ?: e.message ?: e.toString()
            lastCheckError = when (e) {
                is java.net.ConnectException -> "Connection refused (is the update server running?)"
                is java.net.UnknownHostException -> "Host unreachable (check your internet connection)"
                is java.net.SocketTimeoutException -> "Connection timed out"
                else -> baseMsg
            }
            return null
        }
    }

    fun isUpdateAvailable(latestRelease: GitHubRelease): Boolean {
        if (forceUpdateAvailable) {
            log("UpdateManager: Forcing update availability via developer flag.")
            return true
        }
        val current = SemVer.parse(currentVersion)
        val latest = SemVer.parse(latestRelease.tagName)
        return latest > current
    }

    fun getMatchingAsset(release: GitHubRelease): GitHubAsset? {
        val suffix = when {
            PlatformUtils.isMac() -> ".pkg"
            PlatformUtils.isWindows() -> ".msi"
            PlatformUtils.isLinux() -> ".deb"
            else -> return null
        }
        // First try to find PKG for mac, fallback to DMG if not found
        if (PlatformUtils.isMac()) {
            val pkgAsset = release.assets.find { it.name.endsWith(".pkg") }
            if (pkgAsset != null) return pkgAsset
            return release.assets.find { it.name.endsWith(".dmg") }
        }
        return release.assets.find { it.name.endsWith(suffix) }
    }

    private fun createDownloadConnection(url: URL): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 60000
            requestMethod = "GET"
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Codeoba-Updater")
        }
    }

    fun downloadUpdate(
        asset: GitHubAsset,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File {
        val updatesDir = getUpdatesDir()
        cleanUpdatesDir() // Start fresh

        val destFile = File(updatesDir, asset.name)
        val tempFile = File(updatesDir, "${asset.name}.tmp")

        log("UpdateManager: Starting download for asset: ${asset.name} from URL: ${asset.browserDownloadUrl}")
        val url = URI(asset.browserDownloadUrl).toURL()
        val connection = createDownloadConnection(url)

        var conn = connection
        var status = conn.responseCode
        var redirectCount = 0
        while (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            if (redirectCount >= 5) throw Exception("Too many redirects")
            val location = conn.getHeaderField("Location") ?: throw Exception("Redirect missing Location header")
            val redirectedUri = conn.url.toURI().resolve(location)
            if (redirectedUri.scheme != "http" && redirectedUri.scheme != "https") {
                throw Exception("Unsafe redirect scheme: ${redirectedUri.scheme}")
            }
            log("UpdateManager: Redirecting ($status) to: $redirectedUri")
            conn.disconnect()
            conn = createDownloadConnection(redirectedUri.toURL())
            status = conn.responseCode
            redirectCount++
        }

        log("UpdateManager: Final download response status: $status")
        if (status !in 200..299) {
            log("UpdateManager: HTTP Error $status when downloading ${asset.name}")
            throw Exception("HTTP Error $status: ${conn.responseMessage} when downloading ${asset.name}")
        }

        val contentLength = conn.contentLengthLong
        log("UpdateManager: Content length to download: $contentLength bytes")
        BufferedInputStream(conn.inputStream).use { input ->
            FileOutputStream(tempFile).use { output ->
                val data = ByteArray(8192)
                var totalBytesRead = 0L
                var bytesRead: Int
                while (input.read(data).also { bytesRead = it } != -1) {
                    output.write(data, 0, bytesRead)
                    totalBytesRead += bytesRead
                    onProgress(
                        if (contentLength > 0) totalBytesRead.toFloat() / contentLength else 0f,
                        totalBytesRead,
                        contentLength
                    )
                }
                log("UpdateManager: Download completed successfully. Received $totalBytesRead bytes.")
            }
        }

        log("UpdateManager: Renaming temporary download file ${tempFile.name} to ${destFile.name}")
        if (tempFile.renameTo(destFile)) {
            log("UpdateManager: Update file ready at ${destFile.absolutePath}")
            return destFile
        } else {
            log("UpdateManager: Failed to rename temporary download file to ${destFile.name}")
            throw Exception("Failed to rename temporary download file to ${destFile.name}")
        }
    }

    fun installUpdate(file: File) {
        log("UpdateManager: Installing update from ${file.absolutePath}...")
        try {
            val pb = when {
                PlatformUtils.isWindows() -> {
                    ProcessBuilder("msiexec.exe", "/i", file.absolutePath, "/passive")
                }
                PlatformUtils.isMac() -> {
                    ProcessBuilder("open", file.absolutePath)
                }
                PlatformUtils.isLinux() -> {
                    ProcessBuilder("xdg-open", file.absolutePath)
                }
                else -> throw Exception("Unsupported operating system for auto-install")
            }
            log("UpdateManager: Running command: ${pb.command().joinToString(" ")}")
            pb.start()
            log("UpdateManager: Launcher process started successfully. Exiting app...")
        } catch (e: Exception) {
            log("UpdateManager: Error launching installer: ${e.message}", e)
            throw e
        }
    }
}
