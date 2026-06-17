package com.whataicando.codeoba.core.util

/**
 * Centralized configuration utility for the Codeoba application environment.
 *
 * Resolves environmental variables, host domains, base URLs, and determines
 * target Firebase project environments and local emulator states.
 */
object AppConfig {
    /**
     * Determines the active base URL / host domain of the environment (e.g. `"codeoba.com"`, `"localhost:5000"`, `"dev.codeoba.com"`).
     * Resolves from the JVM system property `-Dcodeoba.base_url=...`.
     * Defaults to `"codeoba.com"` if the property is missing or empty.
     *
     * @return The parsed base URL.
     */
    fun getBaseUrl(): String {
        val raw = System.getProperty("codeoba.base_url")?.trim().orEmpty()
        if (raw.isBlank()) return "codeoba.com"

        val normalized = raw.trimEnd('/')
        return try {
            val uri = java.net.URI(if (normalized.startsWith("http://") || normalized.startsWith("https://")) normalized else "https://$normalized")
            val port = if (uri.port != -1) ":${uri.port}" else ""
            ((uri.host ?: normalized.replace(Regex("^https?://"), "")) + port).trimEnd('/')
        } catch (_: Exception) {
            normalized.replace(Regex("^https?://"), "").trimEnd('/')
        }
    }

    /**
     * Checks if the given [host] represents a local loopback domain (e.g. `"localhost"` or `"127.0.0.1"`),
     * with or without an explicit port configuration.
     *
     * @param host The host string to evaluate.
     * @return `true` if the host represents a local address.
     */
    fun isLocalHost(host: String) =
        host == "localhost" || host == "127.0.0.1" ||
        host.startsWith("localhost:") || host.startsWith("127.0.0.1:")

    /**
     * Checks if the given [host] is exactly `"localhost"` or `"127.0.0.1"` without an explicit port suffix.
     *
     * @param host The host string to evaluate.
     * @return `true` if the host matches exactly.
     */
    fun isRawLocalHost(host: String) = host == "localhost" || host == "127.0.0.1"

    /**
     * Determines if the application is configured to run against the local Firebase emulator suite.
     *
     * @return `true` if the active base URL points to a local address.
     */
    fun useEmulator() = isLocalHost(getBaseUrl())

    /**
     * Resolves the URL for the web console / dashboard based on the active environment configuration.
     * Maps local hostnames to emulator port 5000 and secure HTTPS schemes for remote domains.
     *
     * @return The fully formatted web console URL string.
     */
    fun getWebConsoleUrl(): String {
        val baseUrl = getBaseUrl()
        if (!isLocalHost(baseUrl)) return "https://$baseUrl"
        val emulatorHost = if (isRawLocalHost(baseUrl)) "$baseUrl:5000" else baseUrl
        return "http://$emulatorHost"
    }

    /**
     * Determines whether the active environment is local development (emulator) or a staging environment.
     * This environment targets the `"codeoba-dev"` Firebase project.
     *
     * @return `true` if the active environment is an emulator or development configuration.
     */
    fun isEmulatorOrDev() =
        useEmulator() ||
        getBaseUrl().substringBefore(':').let { domain ->
            domain == "dev.codeoba.com" ||
            Regex("(^|\\.)codeoba-dev(\\.|$)").containsMatchIn(domain)
        }

    /**
     * Resolves the active Firebase Project ID target string based on [isEmulatorOrDev].
     *
     * @return `"codeoba-dev"` for developer/staging environments, otherwise `"codeoba-prod"`.
     */
    fun getFirebaseProjectId() = if (isEmulatorOrDev()) "codeoba-dev" else "codeoba-prod"
}
