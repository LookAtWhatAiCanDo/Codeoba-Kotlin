package llc.lookatwhataicando.codeoba.core.domain.auth

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FirebaseAuthClientTest {

    @Test
    fun testProductionApiKeyValidation() {
        // Save current system properties so we can restore them
        val originalBaseUrl = System.getProperty("codeoba.base_url")
        val originalApiKey = System.getProperty("codeoba.firebase.api_key")

        try {
            // Force production mode (non-emulator) by setting base_url to production host
            System.setProperty("codeoba.base_url", "codeoba.com")
            
            // Clear API key system property to trigger validation check on fallback
            System.clearProperty("codeoba.firebase.api_key")
            
            // Assert that calling refreshIdToken throws IllegalArgumentException
            val exception = assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    FirebaseAuthClient.refreshIdToken("dummy_token")
                }
            }
            assertTrue(exception.message!!.contains("Firebase API key is not configured"), "Expected validation failure message, got: ${exception.message}")
        } finally {
            // Restore original state
            if (originalBaseUrl != null) {
                System.setProperty("codeoba.base_url", originalBaseUrl)
            } else {
                System.clearProperty("codeoba.base_url")
            }

            if (originalApiKey != null) {
                System.setProperty("codeoba.firebase.api_key", originalApiKey)
            } else {
                System.clearProperty("codeoba.firebase.api_key")
            }
        }
    }

    @Test
    fun testConfiguredApiKeyPassedToUrl() {
        val originalBaseUrl = System.getProperty("codeoba.base_url")
        val originalApiKey = System.getProperty("codeoba.firebase.api_key")

        try {
            // Force production mode (non-emulator)
            System.setProperty("codeoba.base_url", "codeoba.com")
            
            // Set a custom API key
            System.setProperty("codeoba.firebase.api_key", "my-test-api-key")

            // Calling refreshIdToken should bypass the check (it might fail due to network/dummy token, 
            // but it should NOT fail with API Key not configured check)
            val exception = assertFailsWith<Exception> {
                runBlocking {
                    FirebaseAuthClient.refreshIdToken("dummy_token")
                }
            }
            // Ensure the failure is not the API Key validation check
            assertTrue(!exception.message!!.contains("Firebase API key is not configured"), "Expected network or authentication failure, got: ${exception.message}")
        } finally {
            // Restore original state
            if (originalBaseUrl != null) {
                System.setProperty("codeoba.base_url", originalBaseUrl)
            } else {
                System.clearProperty("codeoba.base_url")
            }

            if (originalApiKey != null) {
                System.setProperty("codeoba.firebase.api_key", originalApiKey)
            } else {
                System.clearProperty("codeoba.firebase.api_key")
            }
        }
    }
}
