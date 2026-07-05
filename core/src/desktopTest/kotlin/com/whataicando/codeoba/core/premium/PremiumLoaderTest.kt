package com.whataicando.codeoba.core.premium

import com.whataicando.codeoba.core.domain.model.PremiumManifest
import com.whataicando.codeoba.core.domain.model.Session
import com.whataicando.codeoba.core.domain.parser.SummarizerProvider
import com.whataicando.codeoba.core.domain.parser.SummaryResult
import com.whataicando.codeoba.core.security.PayloadVerifier
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class PremiumLoaderTest {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    private val classBase64 = "yv66vgAAAEEAKwoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWBwAIAQA5Y29tL3doYXRhaWNhbmRvL2NvZGVvYmEvY29yZS9kb21haW4vcGFyc2VyL1Nlc3Npb25TdW1tYXJ5CAAKAQAfRHluYW1pYyBwcmVtaXVtIHN1bW1hcnkgc3VjY2VzcwoADAANBwAODAAPABABABVqYXZhL3V0aWwvQ29sbGVjdGlvbnMBAA1zaW5nbGV0b25MaXN0AQAkKExqYXZhL2xhbmcvT2JqZWN0OylMamF2YS91dGlsL0xpc3Q7CgAMABIMABMAFAEACWVtcHR5TGlzdAEAEigpTGphdmEvdXRpbC9MaXN0OwoABwAWDAAFABcBADMoTGphdmEvdXRpbC9MaXN0O0xqYXZhL3V0aWwvTGlzdDtMamF2YS91dGlsL0xpc3Q7KVYHABkBADtjb20vd2hhdGFpY2FuZG8vY29kZW9iYS9jb3JlL2RvbWFpbi9wYXJzZXIvU3VtbWFyeVJlc3VsdCRPawoAGAAbDAAFABwBAD4oTGNvbS93aGF0YWljYW5kby9jb2Rlb2JhL2NvcmUvZG9tYWluL3BhcnNlci9TZXNzaW9uU3VtbWFyeTspVgcAHgEAQGNvbS93aGF0YWljYW5kby9jb2Rlb2JhL2NvcmUvcHJlbWl1bS9maXh0dXJlL0R1bW15UmVhbFN1bW1hcml6ZXIHACABADVjb20vd2hhdGFpY2FuZG8vY29kZW9iYS9jb3JlL2RvbWFpbi9wYXJzZXIvU3VtbWFyaXplcgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAAlzdW1tYXJpemUBAIEoTGNvbS93aGF0YWljYW5kby9jb2Rlb2JhL2NvcmUvZG9tYWluL21vZGVsL1Nlc3Npb247TGphdmEvbGFuZy9TdHJpbmc7KUxjb20vd2hhdGFpY2FuZG8vY29kZW9iYS9jb3JlL2RvbWFpbi9wYXJzZXIvU3VtbWFyeVJlc3VsdDsBAApTb3VyY2VGaWxlAQAYRHVtbXlSZWFsU3VtbWFyaXplci5qYXZhAQAMSW5uZXJDbGFzc2VzBwApAQA4Y29tL3doYXRhaWNhbmRvL2NvZGVvYmEvY29yZS9kb21haW4vcGFyc2VyL1N1bW1hcnlSZXN1bHQBAAJPawAhAB0AAgABAB8AAAACAAEABQAGAAEAIQAAAB0AAQABAAAABSq3AAGxAAAAAQAiAAAABgABAAAACQABACMAJAABACEAAABEAAUABAAAABy7AAdZEgm4AAu4ABG4ABG3ABVOuwAYWS23ABqwAAAAAQAiAAAAFgAFAAAADAAGAA0ACQAOAAwADwATABEAAgAlAAAAAgAmACcAAAAKAAEAGAAoACoAGQ=="

    private lateinit var tempJar: File
    private lateinit var tempManifestFile: File
    private lateinit var tempDir: File
    
    private val keyPair by lazy {
        val g = KeyPairGenerator.getInstance("Ed25519")
        g.generateKeyPair()
    }

    @BeforeTest
    fun setUp() {
        tempDir = File.createTempFile("premium_test_", "_dir")
        tempDir.delete()
        tempDir.mkdirs()
        
        tempJar = File(tempDir, "premium.jar")
        tempManifestFile = File(tempDir, "premium-manifest.json")

        // Set test public key override in verifier
        PayloadVerifier.setTestPublicKey(keyPair.public)
    }

    @AfterTest
    fun tearDown() {
        PayloadVerifier.setTestPublicKey(null)
        tempDir.deleteRecursively()
        SummarizerProvider.revertToStub()
    }

    @Test
    fun testPayloadVerifierCorrectSignature() {
        val data = "Hello World".toByteArray()
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(keyPair.private)
        sig.update(data)
        val signatureBytes = sig.sign()

        assertTrue(PayloadVerifier.verify(data, signatureBytes))
    }

    @Test
    fun testPayloadVerifierMismatchedSignature() {
        val data = "Hello World".toByteArray()
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(keyPair.private)
        sig.update(data)
        val signatureBytes = sig.sign()

        // Tamper data
        val tampered = "Hello World!".toByteArray()
        assertFalse(PayloadVerifier.verify(tampered, signatureBytes))
    }

    @Test
    fun testClassLoaderInstallerAndVerification() {
        // Build the dynamic mock premium JAR
        val classBytes = Base64.getDecoder().decode(classBase64)
        ZipOutputStream(FileOutputStream(tempJar)).use { zos ->
            zos.putNextEntry(ZipEntry("com/whataicando/codeoba/core/premium/fixture/DummyRealSummarizer.class"))
            zos.write(classBytes)
            zos.closeEntry()
        }

        // Verify ClassLoaderInstaller can successfully classload it
        val summarizer = ClassLoaderInstaller.install(
            tempJar,
            "com.whataicando.codeoba.core.premium.fixture.DummyRealSummarizer"
        )
        assertNotNull(summarizer)

        val dummySession = Session(
            id = "test",
            sourceId = "test-src",
            filePath = "path",
            timestamp = 0L,
            updatedAt = 0L,
            cwd = null,
            threadName = null,
            turns = emptyList(),
            isArchived = false,
            isPinned = false,
            summary = null
        )

        val result = summarizer.summarize(dummySession, null)
        assertTrue(result is SummaryResult.Ok)
        assertEquals("Dynamic premium summary success", result.summary.keyActions.first())
    }

    @Test
    fun testClassLoaderInstallerSecurityViolation() {
        assertFailsWith<IllegalArgumentException> {
            ClassLoaderInstaller.install(
                tempJar,
                "java.lang.System"
            )
        }
    }

    @Test
    fun testPremiumLoaderLocalOverrideSync() = runBlocking {
        // 1. Build and Sign JAR
        val classBytes = Base64.getDecoder().decode(classBase64)
        ZipOutputStream(FileOutputStream(tempJar)).use { zos ->
            zos.putNextEntry(ZipEntry("com/whataicando/codeoba/core/premium/fixture/DummyRealSummarizer.class"))
            zos.write(classBytes)
            zos.closeEntry()
        }

        val jarBytes = tempJar.readBytes()
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(keyPair.private)
        sig.update(jarBytes)
        val signatureBase64 = Base64.getEncoder().encodeToString(sig.sign())
        val jarHash = PremiumCache.sha256(jarBytes)

        val manifestContent = """
            {
              "version": "1.0.0",
              "jarHash": "$jarHash",
              "signature": "$signatureBase64",
              "entrypointClass": "com.whataicando.codeoba.core.premium.fixture.DummyRealSummarizer"
            }
        """.trimIndent()
        tempManifestFile.writeText(manifestContent)

        // Clean cache first
        PremiumCache.clearCache()

        // 2. Set system property local override directory
        System.setProperty("codeoba.premium.local.dir", tempDir.absolutePath)

        try {
            // Sync loader with active subscription
            PremiumLoader.sync(isSubscribed = true)

            // Confirm custom loader was loaded
            val activeSummarizer = SummarizerProvider.current()
            assertNotEquals("StubSummarizer", activeSummarizer::class.java.simpleName)

            val dummySession = Session(
                id = "test",
                sourceId = "test-src",
                filePath = "path",
                timestamp = 0L,
                updatedAt = 0L,
                cwd = null,
                threadName = null,
                turns = emptyList(),
                isArchived = false,
                isPinned = false,
                summary = null
            )
            val result = activeSummarizer.summarize(dummySession, null)
            assertTrue(result is SummaryResult.Ok)
            assertEquals("Dynamic premium summary success", result.summary.keyActions.first())

            // Sync loader with INACTIVE subscription
            PremiumLoader.sync(isSubscribed = false)
            assertEquals("StubSummarizer", SummarizerProvider.current()::class.java.simpleName)

        } finally {
            System.clearProperty("codeoba.premium.local.dir")
            PremiumCache.clearCache()
        }
    }

    @Test
    fun testPremiumLoaderGracePeriodExpiry() = runBlocking {
        // 1. Build and Sign JAR
        val classBytes = Base64.getDecoder().decode(classBase64)
        ZipOutputStream(FileOutputStream(tempJar)).use { zos ->
            zos.putNextEntry(ZipEntry("com/whataicando/codeoba/core/premium/fixture/DummyRealSummarizer.class"))
            zos.write(classBytes)
            zos.closeEntry()
        }

        val jarBytes = tempJar.readBytes()
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(keyPair.private)
        sig.update(jarBytes)
        val signatureBase64 = Base64.getEncoder().encodeToString(sig.sign())
        val jarHash = PremiumCache.sha256(jarBytes)

        val manifestContent = """
            {
              "version": "1.0.0",
              "jarHash": "$jarHash",
              "signature": "$signatureBase64",
              "entrypointClass": "com.whataicando.codeoba.core.premium.fixture.DummyRealSummarizer"
            }
        """.trimIndent()
        
        // Write directly to cache
        PremiumCache.getJarFile().writeBytes(jarBytes)
        PremiumCache.saveManifest(json.decodeFromString<PremiumManifest>(manifestContent))

        // Seed last sync timestamp to be expired (> 24 hours ago)
        val expiredTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        com.whataicando.codeoba.core.util.SecureStorage.put("premium_last_sync_ms", expiredTime.toString())

        // Set console url to a non-existent URL so online fetch fails
        System.setProperty("codeoba.base_url", "invalid-domain-should-fail-12345.com")

        try {
            // Confirm the cached payload is valid but grace period is expired
            assertTrue(PremiumCache.verifyCachedPayload())
            assertFalse(PremiumCache.isWithinGracePeriod())

            // Sync loader - should fail fetch and fall back to stub because grace period is expired
            PremiumLoader.sync(isSubscribed = true)

            // Confirm active summarizer is reverted to StubSummarizer
            assertEquals("StubSummarizer", SummarizerProvider.current()::class.java.simpleName)
        } finally {
            System.clearProperty("codeoba.base_url")
            PremiumCache.clearCache()
            com.whataicando.codeoba.core.util.SecureStorage.delete("premium_last_sync_ms")
        }
    }

    @Test
    fun testPremiumLoaderClockRollback() = runBlocking {
        // 1. Build and Sign JAR
        val classBytes = Base64.getDecoder().decode(classBase64)
        ZipOutputStream(FileOutputStream(tempJar)).use { zos ->
            zos.putNextEntry(ZipEntry("com/whataicando/codeoba/core/premium/fixture/DummyRealSummarizer.class"))
            zos.write(classBytes)
            zos.closeEntry()
        }

        val jarBytes = tempJar.readBytes()
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(keyPair.private)
        sig.update(jarBytes)
        val signatureBase64 = Base64.getEncoder().encodeToString(sig.sign())
        val jarHash = PremiumCache.sha256(jarBytes)

        val manifestContent = """
            {
              "version": "1.0.0",
              "jarHash": "$jarHash",
              "signature": "$signatureBase64",
              "entrypointClass": "com.whataicando.codeoba.core.premium.fixture.DummyRealSummarizer"
            }
        """.trimIndent()
        
        // Write directly to cache
        PremiumCache.getJarFile().writeBytes(jarBytes)
        PremiumCache.saveManifest(json.decodeFromString<PremiumManifest>(manifestContent))

        // Seed last sync timestamp to be in the future (clock rollback scenario)
        val futureTime = System.currentTimeMillis() + (10 * 60 * 1000L) // 10 minutes in the future
        com.whataicando.codeoba.core.util.SecureStorage.put("premium_last_sync_ms", futureTime.toString())

        // Set console url to a non-existent URL so online fetch fails
        System.setProperty("codeoba.base_url", "invalid-domain-should-fail-12345.com")

        try {
            // Confirm the cached payload is valid but grace period is expired due to clock rollback
            assertTrue(PremiumCache.verifyCachedPayload())
            assertFalse(PremiumCache.isWithinGracePeriod())

            // Sync loader - should fail fetch and fall back to stub because clock was rolled back
            PremiumLoader.sync(isSubscribed = true)

            // Confirm active summarizer is reverted to StubSummarizer
            assertEquals("StubSummarizer", SummarizerProvider.current()::class.java.simpleName)
        } finally {
            System.clearProperty("codeoba.base_url")
            PremiumCache.clearCache()
            com.whataicando.codeoba.core.util.SecureStorage.delete("premium_last_sync_ms")
        }
    }

    @Test
    fun testClassLoaderIsolation() {
        // Build the dynamic mock premium JAR
        val classBytes = Base64.getDecoder().decode(classBase64)
        ZipOutputStream(FileOutputStream(tempJar)).use { zos ->
            zos.putNextEntry(ZipEntry("com/whataicando/codeoba/core/premium/fixture/DummyRealSummarizer.class"))
            zos.write(classBytes)
            zos.closeEntry()
        }

        // Verify ClassLoaderInstaller can successfully classload it
        val summarizer = ClassLoaderInstaller.install(
            tempJar,
            "com.whataicando.codeoba.core.premium.fixture.DummyRealSummarizer"
        )
        assertNotNull(summarizer)

        // Confirm the classloader is our URLClassLoader with FilteredParentClassLoader
        val classLoader = summarizer.javaClass.classLoader
        assertNotNull(classLoader)

        // Try loading a core class that is NOT in the whitelisted packages (e.g. SecureStorage)
        // It should fail with ClassNotFoundException due to the FilteredParentClassLoader
        assertFailsWith<ClassNotFoundException> {
            classLoader.loadClass("com.whataicando.codeoba.core.util.SecureStorage")
        }
        
        // Also try loading app-desktop classes (e.g. SettingsManager) which should fail too
        assertFailsWith<ClassNotFoundException> {
            classLoader.loadClass("com.whataicando.codeoba.desktop.SettingsManager")
        }

        // Whitelisted classes like java.lang.String or domain contract classes should succeed
        assertNotNull(classLoader.loadClass("java.lang.String"))
        assertNotNull(classLoader.loadClass("com.whataicando.codeoba.core.domain.parser.Summarizer"))
    }
}
