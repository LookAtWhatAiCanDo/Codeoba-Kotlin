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
            assertEquals("EC", loadedKeyPair.public.algorithm)
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

    @Test
    fun testRSAMigrationToEC() {
        if (System.getProperty("codeoba.run.keyring.integration.tests") != "true") return

        // 1. Backup any existing real production keys
        val backupPrivate = SecureStorage.get("device_private_key")
        val backupPublic = SecureStorage.get("device_public_key")

        try {
            // 2. Generate and store RSA keys using raw Java crypto
            val kpg = java.security.KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048)
            val rsaKp = kpg.generateKeyPair()
            
            SecureStorage.put("device_private_key", java.util.Base64.getEncoder().encodeToString(rsaKp.private.encoded))
            SecureStorage.put("device_public_key", java.util.Base64.getEncoder().encodeToString(rsaKp.public.encoded))

            // 3. Load KeyPair — should detect RSA, catch error, delete them, and generate fresh EC keys
            val loadedKeyPair = DeviceKeyManager.getOrGenerateKeyPair()
            assertNotNull(loadedKeyPair)
            assertEquals("EC", loadedKeyPair.public.algorithm)

            // Confirm they are saved back as EC
            val newPubBase64 = SecureStorage.get("device_public_key")
            assertNotNull(newPubBase64)
            val newPubBytes = java.util.Base64.getDecoder().decode(newPubBase64)
            val kf = java.security.KeyFactory.getInstance("EC")
            val parsedPub = kf.generatePublic(java.security.spec.X509EncodedKeySpec(newPubBytes))
            assertEquals("EC", parsedPub.algorithm)
        } finally {
            // 4. Clean up test keys and restore backup
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
