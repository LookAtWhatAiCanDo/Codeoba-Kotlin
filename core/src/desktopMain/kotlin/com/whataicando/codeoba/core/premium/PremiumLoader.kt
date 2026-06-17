package com.whataicando.codeoba.core.premium

import com.whataicando.codeoba.core.domain.model.PremiumManifest
import com.whataicando.codeoba.core.domain.parser.SummarizerProvider
import com.whataicando.codeoba.core.util.AppConfig
import com.whataicando.codeoba.core.util.Logger.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI

object PremiumLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Synchronizes the premium module. If subscription is active, it checks/downloads/loads the JAR.
     * If not active, it reverts to the StubSummarizer and cleans up.
     */
    suspend fun sync(isSubscribed: Boolean) {
        withContext(Dispatchers.IO) {
            if (!isSubscribed) {
                if (SummarizerProvider.current().javaClass.simpleName != "StubSummarizer") {
                    log("PremiumLoader: Reverting to StubSummarizer (Ecosystem Sync disabled).")
                }
                SummarizerProvider.revertToStub()
                return@withContext
            }

            try {
                // 1. Check for local developer override first (extremely useful for tests/dev)
                val localOverrideDir = System.getProperty("codeoba.premium.local.dir")
                if (!localOverrideDir.isNullOrBlank()) {
                    val dir = File(localOverrideDir)
                    val jar = File(dir, "premium.jar")
                    val manifestFile = File(dir, "premium-manifest.json")
                    if (jar.exists() && manifestFile.exists()) {
                        val manifest = json.decodeFromString<PremiumManifest>(manifestFile.readText())
                        
                        // Copy/Save to cache and verify
                        val jarBytes = jar.readBytes()
                        if (PremiumCache.sha256(jarBytes) == manifest.jarHash) {
                            PremiumCache.getJarFile().writeBytes(jarBytes)
                            PremiumCache.saveManifest(manifest)
                            if (PremiumCache.verifyCachedPayload()) {
                                val instance = ClassLoaderInstaller.install(
                                    PremiumCache.getJarFile(),
                                    manifest.entrypointClass
                                )
                                SummarizerProvider.install(instance)
                                log("PremiumLoader: Successfully loaded local developer override module: ${manifest.entrypointClass}")
                                return@withContext
                            }
                        }
                    }
                }

                // 2. Authenticated download path from Cloud Function via rewrite endpoint
                val consoleUrl = AppConfig.getWebConsoleUrl()
                
                // Get ID token and Device ID
                val idToken = com.whataicando.codeoba.core.util.SecureStorage.get("firebase_auth_id_token")
                val deviceId = try {
                    java.util.prefs.Preferences.userRoot().node("com/whataicando/codeoba/desktop").get("device_id", null)
                } catch (_: Exception) {
                    null
                }

                val deviceQuery = if (deviceId != null) "&deviceId=${java.net.URLEncoder.encode(deviceId, "UTF-8")}" else ""
                val manifestUrl = "$consoleUrl/api/premium-download?file=manifest$deviceQuery"
                val jarUrl = "$consoleUrl/api/premium-download?file=jar"

                log("PremiumLoader: Fetching manifest from $manifestUrl")
                val manifestBytes = downloadBytes(manifestUrl, idToken)
                val serverManifest = json.decodeFromString<PremiumManifest>(manifestBytes.decodeToString())

                val cachedManifest = PremiumCache.getCachedManifest()
                val cacheValid = PremiumCache.verifyCachedPayload()

                if (cacheValid && cachedManifest != null && cachedManifest.jarHash == serverManifest.jarHash) {
                    // Cache is up to date and valid, update the manifest in cache (keeps watermark fresh)
                    PremiumCache.saveManifest(serverManifest)
                    val instance = ClassLoaderInstaller.install(
                        PremiumCache.getJarFile(),
                        serverManifest.entrypointClass
                    )
                    SummarizerProvider.install(instance)
                    PremiumCache.saveLastSyncTimestamp()
                    log("PremiumLoader: Successfully loaded premium module from cache: ${serverManifest.entrypointClass}")
                } else {
                    // Cache is invalid or outdated, download new jar
                    log("PremiumLoader: Cache missing or outdated. Syncing module from $jarUrl...")
                    val jarBytes = downloadBytes(jarUrl, idToken)
                    
                    // Verify hash and signature before writing to disk
                    if (PremiumCache.sha256(jarBytes) != serverManifest.jarHash) {
                        throw Exception("Downloaded premium JAR hash mismatch.")
                    }

                    val sigBytes = java.util.Base64.getDecoder().decode(serverManifest.signature)
                    if (!com.whataicando.codeoba.core.security.PayloadVerifier.verify(jarBytes, sigBytes)) {
                        throw Exception("Downloaded premium JAR signature verification failed.")
                    }

                    // Write to cache
                    PremiumCache.getJarFile().writeBytes(jarBytes)
                    PremiumCache.saveManifest(serverManifest)

                    val instance = ClassLoaderInstaller.install(
                        PremiumCache.getJarFile(),
                        serverManifest.entrypointClass
                    )
                    SummarizerProvider.install(instance)
                    PremiumCache.saveLastSyncTimestamp()
                    log("PremiumLoader: Successfully downloaded, verified, and loaded premium module: ${serverManifest.entrypointClass}")
                }
            } catch (e: Exception) {
                log("PremiumLoader: Sync failed: ${e.message}")
                // Fall back to stub or try loading cached version as offline grace period if valid
                if (PremiumCache.verifyCachedPayload() && PremiumCache.isWithinGracePeriod()) {
                    try {
                        val cachedManifest = PremiumCache.getCachedManifest()!!
                        val instance = ClassLoaderInstaller.install(
                            PremiumCache.getJarFile(),
                            cachedManifest.entrypointClass
                        )
                        SummarizerProvider.install(instance)
                        log("PremiumLoader: Offline fallback to cached premium module succeeded: ${cachedManifest.entrypointClass}")
                    } catch (inner: Exception) {
                        log("PremiumLoader: Reverting to StubSummarizer after fallback exception.")
                        SummarizerProvider.revertToStub()
                    }
                } else {
                    log("PremiumLoader: Cache invalid or offline grace period expired. Reverting to StubSummarizer.")
                    SummarizerProvider.revertToStub()
                }
            }
        }
    }

    private fun downloadBytes(urlStr: String, idToken: String?): ByteArray {
        val url = java.net.URI(urlStr).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.setRequestProperty("X-App-Signature", com.whataicando.codeoba.core.util.BuildConfig.APP_SIGNATURE)
        if (idToken != null) {
            connection.setRequestProperty("Authorization", "Bearer $idToken")
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw Exception("HTTP Error $responseCode: ${connection.responseMessage} when fetching $urlStr")
        }

        return connection.inputStream.use { it.readBytes() }
    }
}
