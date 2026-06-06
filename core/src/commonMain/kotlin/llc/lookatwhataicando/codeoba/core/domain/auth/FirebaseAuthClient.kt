package llc.lookatwhataicando.codeoba.core.domain.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import llc.lookatwhataicando.codeoba.core.util.AppConfig

object FirebaseAuthClient {
    // Firebase Web API key is required for securetoken.googleapis.com calls.
    // Allow overriding via JVM system property or environment variable to keep environment config out of source.
    private const val DEFAULT_FIREBASE_API_KEY = "EMULATOR_ONLY"

    private fun getFirebaseApiKey(): String {
        val key = try {
            System.getProperty("codeoba.firebase.api_key") ?: System.getenv("CODEOBA_FIREBASE_API_KEY")
        } catch (_: Throwable) {
            null
        } ?: DEFAULT_FIREBASE_API_KEY

        if (!useEmulator) {
            require(key != DEFAULT_FIREBASE_API_KEY && key.isNotBlank()) {
                "Firebase API key is not configured. Set -Dcodeoba.firebase.api_key=... or CODEOBA_FIREBASE_API_KEY=..."
            }
        }
        return key
    }

    private val firebaseProjectId: String
        get() = AppConfig.getFirebaseProjectId()
    private const val CLOUD_FUNCTION_REGION = "us-central1"

    private val json = Json { ignoreUnknownKeys = true }
    internal var client = HttpClient()
    
    private val useEmulator: Boolean
        get() = AppConfig.useEmulator()



    suspend fun refreshIdToken(refreshToken: String): AuthResponse {
        return withContext(Dispatchers.IO) {
            val apiKey = getFirebaseApiKey()
            val url = if (useEmulator) {
                "http://127.0.0.1:9099/securetoken.googleapis.com/v1/token?key=$apiKey"
            } else {
                "https://securetoken.googleapis.com/v1/token?key=$apiKey"
            }
            val bodyStr = "grant_type=refresh_token&refresh_token=${java.net.URLEncoder.encode(refreshToken, "UTF-8")}"
            
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(bodyStr)
            }
            
            val responseText = response.bodyAsText()
            if (response.status.value != 200) {
                throw Exception("Failed to refresh token. Status: ${response.status.value}. Body: $responseText")
            }
            
            val obj = json.parseToJsonElement(responseText).jsonObject
            val idToken = obj["id_token"]?.jsonPrimitive?.content
            val userId = obj["user_id"]?.jsonPrimitive?.content
            
            if (idToken.isNullOrBlank() || userId.isNullOrBlank()) {
                throw Exception("Invalid token refresh response from server")
            }
            
            val returnedRefreshToken = obj["refresh_token"]?.jsonPrimitive?.content
            val finalRefreshToken = if (returnedRefreshToken.isNullOrBlank()) {
                refreshToken
            } else {
                returnedRefreshToken
            }
            
            AuthResponse(
                idToken = idToken,
                refreshToken = finalRefreshToken,
                uid = userId,
                email = "" // Email should be persisted by client, not returned on refresh
            )
        }
    }

    suspend fun getRegistrationChallenge(idToken: String, deviceId: String): String {
        return withContext(Dispatchers.IO) {
            val url = if (useEmulator) {
                "http://127.0.0.1:5001/$firebaseProjectId/$CLOUD_FUNCTION_REGION/getRegistrationChallenge"
            } else {
                "https://$CLOUD_FUNCTION_REGION-$firebaseProjectId.cloudfunctions.net/getRegistrationChallenge"
            }
            val bodyStr = buildJsonObject {
                put("data", buildJsonObject {
                    put("deviceId", deviceId)
                })
            }.toString()
            
            val response: HttpResponse = client.post(url) {
                header("Authorization", "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(bodyStr)
            }
            
            val responseText = response.bodyAsText()
            if (response.status.value != 200) {
                throw Exception("Failed to get registration challenge. Status: ${response.status.value}")
            }
            
            val root = json.parseToJsonElement(responseText).jsonObject
            val result = root["result"]?.jsonObject ?: throw Exception("Invalid challenge response format")
            result["nonce"]?.jsonPrimitive?.content ?: throw Exception("Nonce missing in response")
        }
    }

    suspend fun registerEcosystemDevice(idToken: String, deviceId: String, deviceName: String, publicKeyPem: String, nonce: String, signature: String): Boolean {
        return withContext(Dispatchers.IO) {
            val url = if (useEmulator) {
                "http://127.0.0.1:5001/$firebaseProjectId/$CLOUD_FUNCTION_REGION/registerEcosystemDevice"
            } else {
                "https://$CLOUD_FUNCTION_REGION-$firebaseProjectId.cloudfunctions.net/registerEcosystemDevice"
            }
            val bodyStr = buildJsonObject {
                put("data", buildJsonObject {
                    put("deviceId", deviceId)
                    put("deviceName", deviceName)
                    put("publicKey", publicKeyPem)
                    put("nonce", nonce)
                    put("signature", signature)
                })
            }.toString()
            
            val response: HttpResponse = client.post(url) {
                header("Authorization", "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(bodyStr)
            }
            
            val responseText = response.bodyAsText()
            if (response.status.value != 200) {
                throw Exception("Failed to register device in Sync Hub. Status: ${response.status.value}")
            }
            
            val root = json.parseToJsonElement(responseText).jsonObject
            val result = root["result"]?.jsonObject ?: throw Exception("Invalid server response format")
            result["success"]?.jsonPrimitive?.content == "true"
        }
    }

    suspend fun getCustomerPortalUrl(idToken: String): String {
        return withContext(Dispatchers.IO) {
            val url = if (useEmulator) {
                "http://127.0.0.1:5001/$firebaseProjectId/$CLOUD_FUNCTION_REGION/getCustomerPortalUrl"
            } else {
                "https://$CLOUD_FUNCTION_REGION-$firebaseProjectId.cloudfunctions.net/getCustomerPortalUrl"
            }
            val bodyStr = buildJsonObject {
                put("data", buildJsonObject {})
            }.toString()
            
            val response: HttpResponse = client.post(url) {
                header("Authorization", "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(bodyStr)
            }
            
            val responseText = response.bodyAsText()
            if (response.status.value != 200) {
                val errorMsg = try {
                    val root = json.parseToJsonElement(responseText).jsonObject
                    root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                } catch (_: Exception) { null }
                throw Exception(errorMsg ?: "Failed to get customer portal URL. Status: ${response.status.value}")
            }
            
            val root = json.parseToJsonElement(responseText).jsonObject
            val result = root["result"]?.jsonObject ?: throw Exception("Invalid customer portal response format")
            result["url"]?.jsonPrimitive?.content ?: throw Exception("URL missing in response")
        }
    }
}

data class AuthResponse(
    val idToken: String,
    val refreshToken: String,
    val uid: String,
    val email: String
)
