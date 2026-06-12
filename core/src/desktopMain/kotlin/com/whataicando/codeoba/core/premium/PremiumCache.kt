package com.whataicando.codeoba.core.premium

import com.whataicando.codeoba.core.security.PayloadVerifier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.Base64

@Serializable
data class PremiumManifest(
    val version: String,
    val jarHash: String, // SHA-256
    val signature: String, // Base64 encoded Ed25519 signature
    val entrypointClass: String
)

object PremiumCache {
    private val json = Json { ignoreUnknownKeys = true }

    fun getPremiumDir(): File {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".codeoba/premium")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getJarFile(): File = File(getPremiumDir(), "premium.jar")
    fun getManifestFile(): File = File(getPremiumDir(), "premium-manifest.json")

    fun getCachedManifest(): PremiumManifest? {
        val file = getManifestFile()
        if (!file.exists()) return null
        return try {
            json.decodeFromString<PremiumManifest>(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun saveManifest(manifest: PremiumManifest) {
        getManifestFile().writeText(json.encodeToString(PremiumManifest.serializer(), manifest))
    }

    fun verifyCachedPayload(): Boolean {
        val jar = getJarFile()
        val manifest = getCachedManifest() ?: return false
        if (!jar.exists()) return false

        val jarBytes = jar.readBytes()

        // 1. Verify Hash
        val computedHash = sha256(jarBytes)
        if (computedHash != manifest.jarHash) {
            return false
        }

        // 2. Verify Signature
        val signatureBytes = Base64.getDecoder().decode(manifest.signature)
        return PayloadVerifier.verify(jarBytes, signatureBytes)
    }

    fun clearCache() {
        val jar = getJarFile()
        val manifest = getManifestFile()
        if (jar.exists()) jar.delete()
        if (manifest.exists()) manifest.delete()
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
