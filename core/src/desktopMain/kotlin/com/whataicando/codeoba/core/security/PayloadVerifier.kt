package com.whataicando.codeoba.core.security

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object PayloadVerifier {
    // Pinned Ed25519 Public Key (corresponds to developer private key used to sign premium payloads)
    private const val PINNED_PUBLIC_KEY = "MCowBQYDK2VwAyEAODI39ZiclCUrQ8b4r+Euel+vXKJ5HReZgUuo5oa82h8="

    @Volatile
    private var testPublicKey: java.security.PublicKey? = null

    fun setTestPublicKey(key: java.security.PublicKey?) {
        testPublicKey = key
    }

    private val publicKey by lazy {
        val keyBytes = Base64.getDecoder().decode(PINNED_PUBLIC_KEY)
        val spec = X509EncodedKeySpec(keyBytes)
        KeyFactory.getInstance("Ed25519").generatePublic(spec)
    }

    fun verify(data: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(testPublicKey ?: publicKey)
            sig.update(data)
            sig.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }
}
