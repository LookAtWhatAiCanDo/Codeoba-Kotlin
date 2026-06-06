package llc.lookatwhataicando.codeoba.core.auth

import com.sun.net.httpserver.HttpServer
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import java.net.InetSocketAddress
import java.net.URLDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import llc.lookatwhataicando.codeoba.core.util.AppConfig

object LocalAuthServer {
    private var server: HttpServer? = null
    var expectedState: String? = null
        private set

    private val allowedOrigins = setOf(
        "http://localhost:5000",
        "http://127.0.0.1:5000",
        "https://codeoba-dev.web.app",
        "https://codeoba-prod.web.app",
        "https://codeoba.com",
        "https://codeoba.firebaseapp.com",
        "https://codeoba-dev.firebaseapp.com"
    )

    fun start(onSuccess: (idToken: String, refreshToken: String, email: String, uid: String) -> Unit): Int = synchronized(this) {
        // Stop any running instance first
        stop()

        val activeServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val port = activeServer.address.port
        val state = java.util.UUID.randomUUID().toString()
        expectedState = state
        log("LocalAuthServer: Started on port $port")

        // Schedule an absolute safety timeout of 5 minutes to close this listener
        Thread {
            try {
                Thread.sleep(5 * 60 * 1000L)
                synchronized(LocalAuthServer) {
                    if (server === activeServer) {
                        log("LocalAuthServer: Stopping due to 5-minute inactivity timeout")
                        stop()
                    }
                }
            } catch (_: Exception) {}
        }.apply { isDaemon = true }.start()

        activeServer.createContext("/callback") { exchange ->
            var wasSuccessful = false
            try {
                val isPost = exchange.requestMethod.equals("POST", ignoreCase = true)
                val origin = exchange.requestHeaders.getFirst("Origin")
                val isAllowed = origin != null && (
                    allowedOrigins.contains(origin.trimEnd('/')) ||
                    origin.trimEnd('/') == AppConfig.getWebConsoleUrl().trimEnd('/')
                )

                if ((origin != null && !isAllowed) || (isPost && origin == null)) {
                    log("LocalAuthServer: Rejected request from unauthorized or missing origin: $origin")
                    val errorResponse = "Unauthorized origin."
                    exchange.sendResponseHeaders(403, errorResponse.toByteArray().size.toLong())
                    exchange.responseBody.write(errorResponse.toByteArray())
                    exchange.close()
                    return@createContext
                }

                // Set CORS headers
                if (origin != null) {
                    exchange.responseHeaders.set("Access-Control-Allow-Origin", origin)
                }
                exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type, Authorization")

                if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                    exchange.sendResponseHeaders(204, -1)
                    exchange.close()
                    return@createContext
                }

                val params = mutableMapOf<String, String>()

                if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                    val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: ""
                    val bodyText = exchange.requestBody.bufferedReader().use { it.readText() }
                    if (contentType.contains("application/json", ignoreCase = true)) {
                        try {
                            val jsonElement = Json.parseToJsonElement(bodyText)
                            jsonElement.jsonObject.forEach { (key, value) ->
                                params[key] = value.jsonPrimitive.content
                            }
                        } catch (e: Exception) {
                            log("LocalAuthServer: Error parsing JSON body: ${e.message}")
                        }
                    } else {
                        // Form urlencoded fallback
                        if (bodyText.isNotEmpty()) {
                            bodyText.split("&").forEach {
                                val parts = it.split("=", limit = 2)
                                if (parts.size == 2) {
                                    params[parts[0]] = URLDecoder.decode(parts[1], "UTF-8")
                                } else if (parts.isNotEmpty()) {
                                    params[parts[0]] = ""
                                }
                            }
                        }
                    }
                } else if (exchange.requestMethod.equals("GET", ignoreCase = true)) {
                    val query = exchange.requestURI.query ?: ""
                    if (query.isNotEmpty()) {
                        query.split("&").forEach {
                            val parts = it.split("=", limit = 2)
                            if (parts.size == 2) {
                                params[parts[0]] = URLDecoder.decode(parts[1], "UTF-8")
                            } else if (parts.isNotEmpty()) {
                                params[parts[0]] = ""
                            }
                        }
                    }
                }

                val state = params["state"]
                if (state == null || state != expectedState) {
                    log("LocalAuthServer: Rejected request due to invalid or missing state parameter: $state")
                    val errorResponse = "Invalid or missing state parameter."
                    val responseText = if (isPost) {
                        """
                        {
                            "status": "error",
                            "message": "Invalid or missing state parameter"
                        }
                        """.trimIndent()
                    } else {
                        errorResponse
                    }
                    val contentType = if (isPost) "application/json" else "text/plain"
                    exchange.responseHeaders.set("Content-Type", "$contentType; charset=UTF-8")
                    exchange.sendResponseHeaders(403, responseText.toByteArray(Charsets.UTF_8).size.toLong())
                    exchange.responseBody.write(responseText.toByteArray(Charsets.UTF_8))
                    exchange.close()
                    return@createContext
                }

                val idToken = if (isPost) params["idToken"] else null
                val refreshToken = if (isPost) params["refreshToken"] else null
                val email = if (isPost) (params["email"] ?: "") else ""
                val uid = if (isPost) (params["uid"] ?: "") else ""
                val responseBody = if (!idToken.isNullOrBlank() && !refreshToken.isNullOrBlank() && uid.isNotBlank()) {
                    onSuccess(idToken, refreshToken, email, uid)
                    wasSuccessful = true
                    if (isPost) {
                        """
                        {
                            "status": "success",
                            "message": "Successfully authenticated"
                        }
                        """.trimIndent()
                    } else {
                        """
                        <html>
                        <head>
                            <title>Success</title>
                            <style>
                                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background-color: #1a1a1a; color: #ffffff; text-align: center; padding-top: 100px; }
                                h1 { color: #00E5FF; }
                            </style>
                        </head>
                        <body>
                            <h1>Successfully Authenticated!</h1>
                            <p>You can close this browser tab and return to the Codeoba app.</p>
                        </body>
                        </html>
                        """.trimIndent()
                    }
                } else {
                    if (isPost) {
                        """
                        {
                            "status": "error",
                            "message": "Missing tokens"
                        }
                        """.trimIndent()
                    } else {
                        """
                        <html>
                        <head>
                            <title>Failed</title>
                            <style>
                                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background-color: #1a1a1a; color: #ffffff; text-align: center; padding-top: 100px; }
                                h1 { color: #EF5350; }
                            </style>
                        </head>
                        <body>
                            <h1>Authentication Failed</h1>
                            <p>No tokens were received. Please try signing in again.</p>
                        </body>
                        </html>
                        """.trimIndent()
                    }
                }

                val contentType = if (isPost) "application/json" else "text/html"
                exchange.responseHeaders.set("Content-Type", "$contentType; charset=UTF-8")
                val hasRequiredFields = !idToken.isNullOrBlank() && !refreshToken.isNullOrBlank() && uid.isNotBlank()
                exchange.sendResponseHeaders(if (hasRequiredFields) 200 else 400, responseBody.toByteArray(Charsets.UTF_8).size.toLong())
                exchange.responseBody.write(responseBody.toByteArray(Charsets.UTF_8))
                exchange.close()
            } catch (e: Exception) {
                log("LocalAuthServer: Error handling request: ${e.message}")
                try {
                    val errorResponse = "Error handling authentication callback."
                    exchange.sendResponseHeaders(500, errorResponse.toByteArray().size.toLong())
                    exchange.responseBody.write(errorResponse.toByteArray())
                    exchange.close()
                } catch (_: Exception) {}
            } finally {
                if (wasSuccessful) {
                    // Terminate server asynchronously shortly after
                    Thread {
                        try {
                            Thread.sleep(1000)
                            synchronized(LocalAuthServer) {
                                if (server === activeServer) {
                                    stop()
                                }
                            }
                        } catch (_: Exception) {}
                    }.apply { isDaemon = true }.start()
                }
            }
        }

        activeServer.start()
        server = activeServer
        return port
    }

    fun stop() = synchronized(this) {
        expectedState = null
        server?.let {
            try {
                it.stop(0)
                log("LocalAuthServer: Stopped")
            } catch (e: Exception) {
                log("LocalAuthServer: Error stopping: ${e.message}")
            }
            server = null
        }
    }
}
