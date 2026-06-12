package com.whataicando.codeoba.core.security

import com.whataicando.codeoba.core.util.SecureStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeviceKeyManagerTest {

    @Test
    fun testKeyGenerationAndRetrieval() {
        if (System.getProperty("codeoba.run.keyring.integration.tests") != "true") return

        // 1. Backup any existing real production keys
        val backupPrivate = SecureStorage.get("device_private_key")
        val backupPublic = SecureStorage.get("device_public_key")

        try {
            // 2. Reset state for test
            SecureStorage.delete("device_private_key")
            SecureStorage.delete("device_public_key")

            // 3. Load KeyPair — should generate a new one
            val generatedKeyPair = DeviceKeyManager.getOrGenerateKeyPair()
            assertNotNull(generatedKeyPair)

            // Verify they now reside in SecureStorage
            assertNotNull(SecureStorage.get("device_private_key"))
            assertNotNull(SecureStorage.get("device_public_key"))

            // 4. Load KeyPair again — should return the same generated one
            val loadedKeyPair = DeviceKeyManager.getOrGenerateKeyPair()
            assertNotNull(loadedKeyPair)
            assertEquals(
                generatedKeyPair.public.encoded.toList(),
                loadedKeyPair.public.encoded.toList()
            )
        } finally {
            // 5. Clean up test keys and restore backup
            SecureStorage.delete("device_private_key")
            SecureStorage.delete("device_public_key")

            if (backupPrivate != null) {
                SecureStorage.put("device_private_key", backupPrivate)
            }
            if (backupPublic != null) {
                SecureStorage.put("device_public_key", backupPublic)
            }
        }
    }
}
