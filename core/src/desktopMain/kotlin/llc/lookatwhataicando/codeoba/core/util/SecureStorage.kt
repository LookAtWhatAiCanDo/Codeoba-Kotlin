package llc.lookatwhataicando.codeoba.core.util

import com.github.javakeyring.Keyring
import java.util.prefs.Preferences

object SecureStorage {
    private const val SERVICE_NAME = "Codeoba"

    private val keyring: Keyring? = try {
        if (System.getProperty("codeoba.no.keyring") == "true") {
            Logger.log("SecureStorage: Native Keyring disabled via system property. Falling back to Java Preferences.")
            null
        } else {
            Keyring.create()
        }
    } catch (e: Throwable) {
        Logger.log("SecureStorage: Native Keyring initialization failed. Falling back to Java Preferences. Error: ${e.message}")
        null
    }

    private val fallbackPrefs by lazy {
        Preferences.userNodeForPackage(SecureStorage::class.java)
    }

    /**
     * Retrieves a password or token.
     */
    fun get(key: String): String? {
        if (keyring != null) {
            try {
                return keyring.getPassword(SERVICE_NAME, key)
            } catch (e: Throwable) {
                // Keyring throws an exception if the password is not found or inaccessible
            }
        }
        return fallbackPrefs.get(key, null)
    }

    /**
     * Stores a password or token. If the value is null, deletes it.
     */
    fun put(key: String, value: String?) {
        if (value == null) {
            delete(key)
            return
        }
        if (keyring != null) {
            try {
                keyring.setPassword(SERVICE_NAME, key, value)
                fallbackPrefs.remove(key)
                return
            } catch (e: Throwable) {
                Logger.log("SecureStorage: Failed to write to native keyring for key $key, falling back to Preferences. Error: ${e.message}")
            }
        }
        fallbackPrefs.put(key, value)
    }

    /**
     * Deletes a password or token from secure storage and fallback preferences.
     */
    fun delete(key: String) {
        if (keyring != null) {
            try {
                keyring.deletePassword(SERVICE_NAME, key)
            } catch (e: Throwable) {
                // Ignore errors if deleting a non-existent password
            }
        }
        fallbackPrefs.remove(key)
    }
}
