package com.whataicando.codeoba.core.security

import com.whataicando.codeoba.core.util.Logger.log
import com.whataicando.codeoba.core.util.SecureStorage
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Manages device identity keys.
 * 
 * TODO: Implement non-exportable/TPM/Secure Enclave-backed keys (CNG on Windows, Secure Enclave on macOS).
 * TODO: Implement a fallback to software-protected key file with restricted permissions (0600) on headless servers where keyring is unavailable.
 */
object DeviceKeyManager {

    fun getOrGenerateKeyPair(): KeyPair {
        // 1. Try loading from SecureStorage first (OS-native secure keyring)
        val securePriv = SecureStorage.get("device_private_key")
        val securePub = SecureStorage.get("device_public_key")
        if (securePriv != null && securePub != null) {
            try {
                val privBytes = Base64.getDecoder().decode(securePriv)
                val pubBytes = Base64.getDecoder().decode(securePub)
                
                val kf = KeyFactory.getInstance("RSA")
                val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                val publicKey = kf.generatePublic(X509EncodedKeySpec(pubBytes))
                
                return KeyPair(publicKey, privateKey)
            } catch (e: Exception) {
                log("DeviceKeyManager: Failed to load keys from SecureStorage: ${e.message}")
            }
        }

        // 2. Generate a new key pair
        return generateAndSaveKeyPair()
    }

    private fun generateAndSaveKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.genKeyPair()

        val privBase64 = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        val pubBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        
        // Write to SecureStorage
        SecureStorage.put("device_private_key", privBase64)
        SecureStorage.put("device_public_key", pubBase64)

        return keyPair
    }

    fun getPublicKeyPem(): String {
        val kp = getOrGenerateKeyPair()
        val base64 = Base64.getEncoder().encodeToString(kp.public.encoded)
        return "-----BEGIN PUBLIC KEY-----\n$base64\n-----END PUBLIC KEY-----"
    }

    fun signPayload(payload: String): String {
        val kp = getOrGenerateKeyPair()
        val sig = java.security.Signature.getInstance("SHA256withRSA")
        sig.initSign(kp.private)
        sig.update(payload.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(sig.sign())
    }
}
