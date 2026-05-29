package llc.lookatwhataicando.codeoba.core.util

object PlatformUtils {
    private val osName = System.getProperty("os.name").lowercase()

    fun isMac(): Boolean = osName.contains("mac")
    fun isWindows(): Boolean = osName.contains("win")
    fun isLinux(): Boolean = osName.contains("nux") || osName.contains("nix") || osName.contains("aix")
}
