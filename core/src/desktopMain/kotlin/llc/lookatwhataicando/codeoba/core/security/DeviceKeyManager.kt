package llc.lookatwhataicando.codeoba.core.security

import llc.lookatwhataicando.codeoba.core.util.Logger.log
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object DeviceKeyManager {
    private val keysDir = File(System.getProperty("user.home"), ".codeoba/keys")
    private val privateKeyFile = File(keysDir, "device_private.der")
    private val publicKeyFile = File(keysDir, "device_public.der")

    init {
        keysDir.mkdirs()
    }

    fun getOrGenerateKeyPair(): KeyPair {
        return if (privateKeyFile.exists() && publicKeyFile.exists()) {
            try {
                loadKeyPair()
            } catch (e: Exception) {
                log("DeviceKeyManager: Failed to load existing keys, regenerating. Error: ${e.message}")
                generateAndSaveKeyPair()
            }
        } else {
            generateAndSaveKeyPair()
        }
    }

    private fun loadKeyPair(): KeyPair {
        val kf = KeyFactory.getInstance("RSA")
        
        val privBytes = privateKeyFile.readBytes()
        val privSpec = PKCS8EncodedKeySpec(privBytes)
        val privateKey = kf.generatePrivate(privSpec)

        val pubBytes = publicKeyFile.readBytes()
        val pubSpec = X509EncodedKeySpec(pubBytes)
        val publicKey = kf.generatePublic(pubSpec)

        return KeyPair(publicKey, privateKey)
    }

    private fun generateAndSaveKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.genKeyPair()

        privateKeyFile.writeBytes(keyPair.private.encoded)
        publicKeyFile.writeBytes(keyPair.public.encoded)

        // Set Unix permissions to 0600 (owner read-write only)
        try {
            val path = privateKeyFile.toPath()
            val perms = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
            Files.setPosixFilePermissions(path, perms)
            Files.setPosixFilePermissions(publicKeyFile.toPath(), perms)
        } catch (e: UnsupportedOperationException) {
            // Non-posix OS (Windows), standard file hiding/attributes can be applied, or standard permissions.
            privateKeyFile.setReadable(true, true)
            privateKeyFile.setWritable(true, true)
            publicKeyFile.setReadable(true, true)
            publicKeyFile.setWritable(true, true)
        } catch (e: Exception) {
            log("DeviceKeyManager: Error setting file permissions: ${e.message}")
        }

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
