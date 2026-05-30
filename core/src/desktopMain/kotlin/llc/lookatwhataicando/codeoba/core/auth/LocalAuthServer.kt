package llc.lookatwhataicando.codeoba.core.auth

import com.sun.net.httpserver.HttpServer
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import java.net.InetSocketAddress
import java.net.URLDecoder

object LocalAuthServer {
    private var server: HttpServer? = null

    fun start(onSuccess: (idToken: String, refreshToken: String, email: String, uid: String) -> Unit): Int {
        // Stop any running instance first
        stop()

        val activeServer = HttpServer.create(InetSocketAddress(0), 0)
        val port = activeServer.address.port
        log("LocalAuthServer: Started on port $port")

        activeServer.createContext("/callback") { exchange ->
            try {
                val query = exchange.requestURI.query ?: ""
                val params = query.split("&").associate {
                    val parts = it.split("=")
                    if (parts.size == 2) {
                        parts[0] to URLDecoder.decode(parts[1], "UTF-8")
                    } else {
                        parts[0] to ""
                    }
                }

                val idToken = params["idToken"]
                val refreshToken = params["refreshToken"]
                val email = params["email"] ?: ""
                val uid = params["uid"] ?: ""

                val responseBody = if (idToken != null && refreshToken != null) {
                    onSuccess(idToken, refreshToken, email, uid)
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

                exchange.sendResponseHeaders(200, responseBody.toByteArray(Charsets.UTF_8).size.toLong())
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
                // Terminate server asynchronously shortly after
                Thread {
                    try {
                        Thread.sleep(1000)
                        stop()
                    } catch (_: Exception) {}
                }.start()
            }
        }

        activeServer.start()
        server = activeServer
        return port
    }

    fun stop() {
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
