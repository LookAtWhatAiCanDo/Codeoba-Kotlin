package com.whataicando.codeoba.core.security

object SecretsScanner {
    private val regexes = listOf(
        // GitHub Personal Access Tokens
        Regex("ghp_[a-zA-Z0-9]{36}"),
        Regex("github_pat_[a-zA-Z0-9]{82}"),
        
        // OpenAI API Keys
        Regex("sk-[a-zA-Z0-9]{32,}"),
        
        // Anthropic API Keys
        Regex("sk-ant-sid01-[a-zA-Z0-9\\-_]{86}"),
        Regex("sk-ant-api03-[a-zA-Z0-9\\-_]{95}"),
        
        // AWS Access Key ID
        Regex("AKIA[0-9A-Z]{16}"),
        
        // Private Key blocks (RSA, EC, etc.)
        Regex("-----BEGIN [A-Z ]+ PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]+ PRIVATE KEY-----"),
        
        // Generic Password/API Key environment variables (e.g. API_KEY=abc123xyz)
        Regex("(?i)(api_key|password|secret|passwd|token|auth_token|client_secret)\\s*=\\s*['\"]?[a-zA-Z0-9\\-_]{12,}['\"]?")
    )

    fun scrub(input: String): String {
        var result = input
        for (regex in regexes) {
            result = regex.replace(result) { match ->
                val matchedText = match.value
                if (matchedText.startsWith("-----BEGIN")) {
                    "[SCRUBBED PRIVATE KEY]"
                } else if (matchedText.contains("=")) {
                    // Retain the key name but scrub the value assigned to it
                    val parts = matchedText.split("=", limit = 2)
                    "${parts[0]}=[SCRUBBED SECRET]"
                } else {
                    "[SCRUBBED KEY]"
                }
            }
        }
        return result
    }
}
