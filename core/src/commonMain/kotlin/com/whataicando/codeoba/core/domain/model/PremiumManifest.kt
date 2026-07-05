package com.whataicando.codeoba.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PremiumManifest(
    val version: String,
    val jarHash: String, // SHA-256
    val signature: String, // Base64 encoded Ed25519 signature
    val entrypointClass: String,
    val watermarkId: String? = null
)
