package llc.lookatwhataicando.codeoba.core.util

object AppConfig {
    /**
     * Determines the active environment host/base URL.
     * Can be specified via JVM system property `-Dcodeoba.base_url=...`.
     * If not specified, defaults to "codeoba.com".
     */
    fun getBaseUrl(): String {
        val raw = System.getProperty("codeoba.base_url")
        if (raw.isNullOrBlank()) {
            return "codeoba.com"
        }
        // Strip protocols and trailing slashes to normalize
        return raw.replace(Regex("^https?://"), "").trimEnd('/')
    }

    /**
     * Determines if the application is running in the local emulator environment.
     * This is true if the base URL points to localhost or 127.0.0.1.
     */
    fun useEmulator(): Boolean {
        val host = getBaseUrl()
        return host.contains("localhost") || host.contains("127.0.0.1")
    }

    /**
     * Resolves the web console URL based on the specified base URL.
     */
    fun getWebConsoleUrl(): String {
        val host = getBaseUrl()
        return if (useEmulator()) {
            "http://localhost:5000" // Emulator hosting port
        } else {
            "https://$host"
        }
    }

    /**
     * Resolves the Firebase Project ID based on the specified base URL.
     */
    fun getFirebaseProjectId(): String {
        val host = getBaseUrl()
        return if (host.contains("localhost") || host.contains("127.0.0.1") || host.contains("codeoba-dev")) {
            "codeoba-dev"
        } else {
            "codeoba-prod"
        }
    }
}
