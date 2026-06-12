package llc.lookatwhataicando.codeoba.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecureStorageTest {

    @Test
    fun testSecureStorageOperations() {
        if (System.getProperty("codeoba.run.keyring.integration.tests") != "true") return

        val testKey = "test_key_credential_storage"
        val testVal = "superSecretTokenValue123"

        // 1. Initial state: should be null or whatever was there
        SecureStorage.delete(testKey)
        assertNull(SecureStorage.get(testKey))

        // 2. Put value
        SecureStorage.put(testKey, testVal)
        assertEquals(testVal, SecureStorage.get(testKey))

        // 3. Update value
        val newVal = "anotherDifferentSecretToken456"
        SecureStorage.put(testKey, newVal)
        assertEquals(newVal, SecureStorage.get(testKey))

        // 4. Put null (should delete)
        SecureStorage.put(testKey, null)
        assertNull(SecureStorage.get(testKey))
    }
}
