package llc.lookatwhataicando.codeoba.desktop

import java.awt.Desktop
import java.net.URI
import llc.lookatwhataicando.codeoba.core.util.PlatformUtils
import llc.lookatwhataicando.codeoba.core.util.Logger.log

fun isWebUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.startsWith("http://") || lower.startsWith("https://")
}

fun isLocalUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.contains("localhost") || lower.contains("127.0.0.1")
}

fun isSafeLocalFileLink(url: String): Boolean {
    if (isWebUrl(url)) return false
    val lower = url.trim().lowercase()
    val colonIdx = lower.indexOf(':')
    if (colonIdx != -1) {
        val scheme = lower.substring(0, colonIdx)
        if (scheme == "file") return true
        if (scheme.length == 1 && scheme[0] in 'a'..'z') {
            val after = lower.substring(colonIdx + 1)
            if (after.startsWith("/") || after.startsWith("\\")) {
                return true
            }
        }
        return false
    }
    return true
}

fun parseLocalFilePath(url: String): String {
    val cleanedUrl = url.substringBefore('#').substringBefore('?').trim()
    val lower = cleanedUrl.lowercase()
    if (!lower.startsWith("file:")) {
        return java.net.URLDecoder.decode(cleanedUrl, "UTF-8")
    }

    return try {
        val uri = URI(cleanedUrl)
        val authority = uri.authority
        val path = uri.path ?: ""
        val combined = if (!authority.isNullOrBlank()) "//$authority$path" else path
        if (PlatformUtils.isWindows() && combined.startsWith("/") && combined.length > 2 && combined[2] == ':') {
            combined.drop(1)
        } else {
            combined
        }
    } catch (_: Exception) {
        var decoded = java.net.URLDecoder.decode(cleanedUrl, "UTF-8")
        val decLower = decoded.lowercase()
        decoded = when {
            decLower.startsWith("file:///") -> "/" + decoded.drop(8)
            decLower.startsWith("file://") -> decoded.drop(7)
            decLower.startsWith("file:/") -> "/" + decoded.drop(6)
            else -> decoded
        }
        while (decoded.contains("//")) {
            decoded = decoded.replace("//", "/")
        }
        if (PlatformUtils.isWindows() && decoded.startsWith("/") && decoded.length > 2 && decoded[2] == ':') {
            decoded = decoded.substring(1)
        }
        decoded
    }
}

internal fun openUrl(url: String) {
    if (!isWebUrl(url)) {
        log("openUrl: Blocked unsafe URL: $url")
        return
    }
    val trimmed = url.trim()
    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(trimmed))
            }
        }
    } catch (e: Exception) {
        log("Failed to open URL $url: ${e.message}")
    }
}
