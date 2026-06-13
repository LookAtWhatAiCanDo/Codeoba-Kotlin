package com.whataicando.codeoba.core.util

object AppConfig {
    /**
     * Determines the active environment host/base URL.
     * Can be specified via JVM system property `-Dcodeoba.base_url=...`.
     * If not specified, defaults to "codeoba.com".
     */
    fun getBaseUrl(): String {
        val raw = System.getProperty("codeoba.base_url")?.trim().orEmpty()
        if (raw.isBlank()) return "codeoba.com"

        val normalized = raw.trimEnd('/')
        return try {
            val uri = if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                java.net.URI(normalized)
            } else {
                // Add a scheme purely for parsing; we only return host[:port]
                java.net.URI("https://$normalized")
            }
            val host = uri.host ?: normalized.replace(Regex("^https?://"), "")
            val portPart = if (uri.port != -1) ":${uri.port}" else ""
            (host + portPart).trimEnd('/')
        } catch (_: Exception) {
            normalized.replace(Regex("^https?://"), "").trimEnd('/')
        }
    }

    /**
     * Determines if a given host points to a local environment (localhost or 127.0.0.1, with or without a port).
     */
    fun isLocalHost(host: String): Boolean {
        return isRawLocalHost(host) || host.startsWith("localhost:") ||
            host.startsWith("127.0.0.1:")
    }

    /**
     * Determines if a given host is exactly "localhost" or "127.0.0.1" without an explicit port.
     */
    fun isRawLocalHost(host: String): Boolean {
        return host == "localhost" || host == "127.0.0.1"
    }

    /**
     * Determines if the application is running in the local emulator environment.
     * This is true if the base URL points to localhost or 127.0.0.1.
     */
    fun useEmulator(): Boolean {
        return isLocalHost(getBaseUrl())
    }

    /**
     * Resolves the web console URL based on the specified base URL.
     */
    fun getWebConsoleUrl(): String {
        val host = getBaseUrl()
        return if (useEmulator()) {
            val emulatorHost = if (isRawLocalHost(host)) "$host:5000" else host
            "http://$emulatorHost"
        } else {
            "https://$host"
        }
    }

    /**
     * Resolves the Firebase Project ID based on the specified base URL.
     */
    fun getFirebaseProjectId(): String {
        val host = getBaseUrl()
        val domain = host.substringBefore(':')
        val isDev = useEmulator() || 
            Regex("(^|\\.)codeoba-dev(\\.|$)").containsMatchIn(domain) ||
            domain == "dev.codeoba.com"
        return if (isDev) {
            "codeoba-dev"
        } else {
            "codeoba-prod"
        }
    }
}
