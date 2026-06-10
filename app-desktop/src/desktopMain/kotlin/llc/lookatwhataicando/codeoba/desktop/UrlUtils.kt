package llc.lookatwhataicando.codeoba.desktop

import java.awt.Desktop
import java.net.URI
import llc.lookatwhataicando.codeoba.core.util.Logger.log

fun isWebUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.startsWith("http://") || lower.startsWith("https://")
}

fun isLocalUrl(url: String): Boolean {
    val trimmed = url.trim()
    val host = try {
        val uri = if (isWebUrl(trimmed)) URI(trimmed) else URI("http://$trimmed")
        uri.host?.lowercase()
    } catch (_: Exception) {
        null
    }
    return host == "localhost" || host == "127.0.0.1"
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
