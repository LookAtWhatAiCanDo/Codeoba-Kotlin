package llc.lookatwhataicando.codeoba.core.security

import kotlin.test.Test
import kotlin.test.assertEquals

class SecretsScannerTest {

    @Test
    fun testScrubGitHubToken() {
        val input = "Here is my github token: ghp_1234567890abcdef1234567890abcdef1234 and some extra text."
        val expected = "Here is my github token: [SCRUBBED KEY] and some extra text."
        assertEquals(expected, SecretsScanner.scrub(input))
    }

    @Test
    fun testScrubOpenAiKey() {
        val input = "sk-7d8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f"
        val expected = "[SCRUBBED KEY]"
        assertEquals(expected, SecretsScanner.scrub(input))
    }

    @Test
    fun testScrubPrivateKey() {
        val input = """
            Some config header
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEA0Yh...
            -----END RSA PRIVATE KEY-----
            Some footer
        """.trimIndent()
        val expected = """
            Some config header
            [SCRUBBED PRIVATE KEY]
            Some footer
        """.trimIndent()
        assertEquals(expected, SecretsScanner.scrub(input))
    }

    @Test
    fun testScrubEnvAssignments() {
        val input = "export DB_PASSWORD=my_super_secret_password_123"
        val expected = "export DB_PASSWORD=[SCRUBBED SECRET]"
        assertEquals(expected, SecretsScanner.scrub(input))
    }
}
