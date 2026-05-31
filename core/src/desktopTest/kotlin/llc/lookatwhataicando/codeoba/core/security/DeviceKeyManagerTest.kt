package llc.lookatwhataicando.codeoba.core.security

import llc.lookatwhataicando.codeoba.core.util.SecureStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeviceKeyManagerTest {

    @Test
    fun testKeyGenerationAndRetrieval() {
        // 1. Reset state
        SecureStorage.delete("device_private_key")
        SecureStorage.delete("device_public_key")

        // 2. Load KeyPair — should generate a new one
        val generatedKeyPair = DeviceKeyManager.getOrGenerateKeyPair()
        assertNotNull(generatedKeyPair)

        // Verify they now reside in SecureStorage
        assertNotNull(SecureStorage.get("device_private_key"))
        assertNotNull(SecureStorage.get("device_public_key"))

        // 3. Load KeyPair again — should return the same generated one
        val loadedKeyPair = DeviceKeyManager.getOrGenerateKeyPair()
        assertNotNull(loadedKeyPair)
        assertEquals(
            generatedKeyPair.public.encoded.toList(),
            loadedKeyPair.public.encoded.toList()
        )

        // Clean up
        SecureStorage.delete("device_private_key")
        SecureStorage.delete("device_public_key")
    }
}
