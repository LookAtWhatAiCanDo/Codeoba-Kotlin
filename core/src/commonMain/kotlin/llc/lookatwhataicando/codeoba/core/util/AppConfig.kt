package llc.lookatwhataicando.codeoba.core.util

object AppConfig {
    /**
     * Centralized utility to determine if the application is running in the local emulator environment.
     * Enabled via the JVM system property `-Dcodeoba.use_emulator=true`.
     */
    fun useEmulator(): Boolean {
        return System.getProperty("codeoba.use_emulator") == "true"
    }
}
