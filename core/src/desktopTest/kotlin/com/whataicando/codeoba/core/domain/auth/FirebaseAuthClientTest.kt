package com.whataicando.codeoba.core.domain.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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
            System.setProperty("codeoba.firebase.api_key", "")
            
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
        val originalClient = FirebaseAuthClient.client

        try {
            // Force production mode (non-emulator)
            System.setProperty("codeoba.base_url", "codeoba.com")
            
            // Set a custom API key
            System.setProperty("codeoba.firebase.api_key", "my-test-api-key")

            var capturedUrl = ""
            val mockEngine = MockEngine { request ->
                capturedUrl = request.url.toString()
                respond(
                    content = """{"id_token": "mock-id", "user_id": "mock-user", "refresh_token": "mock-refresh"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            }
            FirebaseAuthClient.client = HttpClient(mockEngine)

            val result = runBlocking {
                FirebaseAuthClient.refreshIdToken("dummy_token")
            }

            // Verify the URL contained our test API key
            assertTrue(capturedUrl.contains("key=my-test-api-key"), "Expected URL to contain API key, got: $capturedUrl")
            // Verify the result is parsed correctly
            kotlin.test.assertEquals("mock-id", result.idToken)
            kotlin.test.assertEquals("mock-user", result.uid)
            kotlin.test.assertEquals("mock-refresh", result.refreshToken)
        } finally {
            // Restore original state
            FirebaseAuthClient.client = originalClient
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
