package llc.lookatwhataicando.codeoba.core.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.options
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalAuthServerTest {

    @Test
    fun testCallbackFormParsingWithEqualsSigns() {
        runBlocking {
            var receivedIdToken: String? = null
            var receivedRefreshToken: String? = null
            var receivedEmail: String? = null
            var receivedUid: String? = null

            val port = LocalAuthServer.start { idToken, refreshToken, email, uid ->
                receivedIdToken = idToken
                receivedRefreshToken = refreshToken
                receivedEmail = email
                receivedUid = uid
            }

            assertTrue(port > 0, "Server port should be greater than 0")

            val client = HttpClient(CIO)
            try {
                // Request callback with base64/JWT-like tokens that contain equals signs in a POST form request
                val state = LocalAuthServer.expectedState
                val formBody = "idToken=eyJhbGciOi=MyToken=&refreshToken=ref=token=&email=test@example.com&uid=user_123&state=$state"
                val response = client.post("http://127.0.0.1:$port/callback") {
                    header("Origin", "https://codeoba.com")
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(formBody)
                }
                val jsonResponse = response.bodyAsText()

                assertTrue(jsonResponse.contains("Successfully authenticated"), "Response should indicate success")
                assertEquals("eyJhbGciOi=MyToken=", receivedIdToken)
                assertEquals("ref=token=", receivedRefreshToken)
                assertEquals("test@example.com", receivedEmail)
                assertEquals("user_123", receivedUid)
            } finally {
                client.close()
                LocalAuthServer.stop()
            }
        }
    }

    @Test
    fun testCallbackPostJson() {
        runBlocking {
            var receivedIdToken: String? = null
            var receivedRefreshToken: String? = null
            var receivedEmail: String? = null
            var receivedUid: String? = null

            val port = LocalAuthServer.start { idToken, refreshToken, email, uid ->
                receivedIdToken = idToken
                receivedRefreshToken = refreshToken
                receivedEmail = email
                receivedUid = uid
            }

            val client = HttpClient(CIO)
            try {
                val jsonBody = """
                {
                    "idToken": "token=123",
                    "refreshToken": "refresh=456",
                    "email": "user@gmail.com",
                    "uid": "uid_789",
                    "state": "${LocalAuthServer.expectedState}"
                }
                """.trimIndent()

                val response = client.post("http://127.0.0.1:$port/callback") {
                    header("Origin", "https://codeoba.com")
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }

                val jsonResponse = response.bodyAsText()
                assertTrue(jsonResponse.contains("Successfully authenticated"), "Response should indicate success")
                assertEquals("token=123", receivedIdToken)
                assertEquals("refresh=456", receivedRefreshToken)
                assertEquals("user@gmail.com", receivedEmail)
                assertEquals("uid_789", receivedUid)
                assertEquals("application/json; charset=UTF-8", response.headers["Content-Type"])
            } finally {
                client.close()
                LocalAuthServer.stop()
            }
        }
    }

    @Test
    fun testCallbackPostForm() {
        runBlocking {
            var receivedIdToken: String? = null
            var receivedRefreshToken: String? = null
            var receivedEmail: String? = null
            var receivedUid: String? = null

            val port = LocalAuthServer.start { idToken, refreshToken, email, uid ->
                receivedIdToken = idToken
                receivedRefreshToken = refreshToken
                receivedEmail = email
                receivedUid = uid
            }

            val client = HttpClient(CIO)
            try {
                val formBody = "idToken=formToken=1&refreshToken=formRefresh=2&email=form@gmail.com&uid=form_uid&state=${LocalAuthServer.expectedState}"

                val response = client.post("http://127.0.0.1:$port/callback") {
                    header("Origin", "https://codeoba.com")
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(formBody)
                }

                val jsonResponse = response.bodyAsText()
                assertTrue(jsonResponse.contains("Successfully authenticated"), "Response should indicate success")
                assertEquals("formToken=1", receivedIdToken)
                assertEquals("formRefresh=2", receivedRefreshToken)
                assertEquals("form@gmail.com", receivedEmail)
                assertEquals("form_uid", receivedUid)
            } finally {
                client.close()
                LocalAuthServer.stop()
            }
        }
    }

    @Test
    fun testCallbackOptionsCors() {
        runBlocking {
            val port = LocalAuthServer.start { _, _, _, _ -> }

            val client = HttpClient(CIO)
            try {
                val response = client.options("http://127.0.0.1:$port/callback") {
                    header("Origin", "https://codeoba.firebaseapp.com")
                    header("Access-Control-Request-Method", "POST")
                }

                assertEquals(204, response.status.value)
                assertEquals("https://codeoba.firebaseapp.com", response.headers["Access-Control-Allow-Origin"])
                assertEquals("POST, GET, OPTIONS", response.headers["Access-Control-Allow-Methods"])
                assertTrue(response.headers["Access-Control-Allow-Headers"]!!.contains("Content-Type"))
            } finally {
                client.close()
                LocalAuthServer.stop()
            }
        }
    }

    @Test
    fun testCallbackUnauthorizedOrigin() {
        runBlocking {
            val port = LocalAuthServer.start { _, _, _, _ -> }

            val client = HttpClient(CIO)
            try {
                val response = client.get("http://127.0.0.1:$port/callback") {
                    header("Origin", "https://malicious-origin.com")
                }

                assertEquals(403, response.status.value)
                assertEquals("Unauthorized origin.", response.bodyAsText())
            } finally {
                client.close()
                LocalAuthServer.stop()
            }
        }
    }

    @Test
    fun testCallbackMissingOriginOnPost() {
        runBlocking {
            val port = LocalAuthServer.start { _, _, _, _ -> }

            val client = HttpClient(CIO)
            try {
                val response = client.post("http://127.0.0.1:$port/callback") {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

                assertEquals(403, response.status.value)
                assertEquals("Unauthorized origin.", response.bodyAsText())
            } finally {
                client.close()
                LocalAuthServer.stop()
            }
        }
    }

    @Test
    fun testCallbackInvalidState() {
        runBlocking {
            val port = LocalAuthServer.start { _, _, _, _ -> }

            val client = HttpClient(CIO)
            try {
                val response = client.get("http://127.0.0.1:$port/callback?state=invalid_state")

                assertEquals(403, response.status.value)
                assertTrue(response.bodyAsText().contains("Invalid or missing state parameter"))
            } finally {
                client.close()
                LocalAuthServer.stop()
            }
        }
    }
}
