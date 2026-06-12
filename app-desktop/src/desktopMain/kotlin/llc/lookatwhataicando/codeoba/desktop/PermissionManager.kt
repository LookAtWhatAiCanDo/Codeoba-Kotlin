package llc.lookatwhataicando.codeoba.desktop

import java.security.MessageDigest
import java.util.prefs.Preferences

object PermissionManager {
    private val prefs: Preferences = Preferences.userNodeForPackage(SettingsManager::class.java).node("file_permissions")

    enum class Action {
        PREVIEW,
        EXTERNAL_OPEN
    }

    enum class Decision {
        ALLOW,
        DENY,
        ASK
    }

    private fun md5(str: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(str.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getDecision(canonicalPath: String, action: Action): Decision {
        val hash = md5(canonicalPath)
        val value = prefs.get(hash, null) ?: return Decision.ASK

        val parts = value.split("|").associate {
            val idx = it.indexOf("=")
            if (idx != -1) {
                it.substring(0, idx) to it.substring(idx + 1)
            } else {
                "" to ""
            }
        }

        val storedPath = parts["path"]
        if (storedPath != canonicalPath) {
            return Decision.ASK
        }

        val decisionStr = if (action == Action.PREVIEW) parts["preview"] else parts["external"]
        return try {
            Decision.valueOf(decisionStr ?: "")
        } catch (_: Exception) {
            Decision.ASK
        }
    }

    fun setDecision(canonicalPath: String, action: Action, decision: Decision) {
        val hash = md5(canonicalPath)
        val value = prefs.get(hash, null)

        var preview = Decision.ASK.name
        var external = Decision.ASK.name

        if (value != null) {
            val parts = value.split("|").associate {
                val idx = it.indexOf("=")
                if (idx != -1) {
                    it.substring(0, idx) to it.substring(idx + 1)
                } else {
                    "" to ""
                }
            }
            if (parts["path"] == canonicalPath) {
                preview = parts["preview"] ?: Decision.ASK.name
                external = parts["external"] ?: Decision.ASK.name
            }
        }

        if (action == Action.PREVIEW) {
            preview = decision.name
        } else {
            external = decision.name
        }

        val newValue = "path=$canonicalPath|preview=$preview|external=$external"
        prefs.put(hash, newValue)
        prefs.flush()
    }

    fun removeDecision(canonicalPath: String) {
        val hash = md5(canonicalPath)
        prefs.remove(hash)
        prefs.flush()
    }

    fun removeActionDecision(canonicalPath: String, action: Action) {
        setDecision(canonicalPath, action, Decision.ASK)
        if (getDecision(canonicalPath, Action.PREVIEW) == Decision.ASK &&
            getDecision(canonicalPath, Action.EXTERNAL_OPEN) == Decision.ASK
        ) {
            removeDecision(canonicalPath)
        }
    }

    fun getAllDecisions(): List<PermissionEntry> {
        val entries = mutableListOf<PermissionEntry>()
        val keys = prefs.keys()
        for (key in keys) {
            val value = prefs.get(key, null) ?: continue
            val parts = value.split("|").associate {
                val idx = it.indexOf("=")
                if (idx != -1) {
                    it.substring(0, idx) to it.substring(idx + 1)
                } else {
                    "" to ""
                }
            }
            val path = parts["path"] ?: continue
            val previewStr = parts["preview"] ?: Decision.ASK.name
            val externalStr = parts["external"] ?: Decision.ASK.name
            entries.add(
                PermissionEntry(
                    path = path,
                    previewDecision = try { Decision.valueOf(previewStr) } catch (_: Exception) { Decision.ASK },
                    externalDecision = try { Decision.valueOf(externalStr) } catch (_: Exception) { Decision.ASK }
                )
            )
        }
        return entries
    }
}

data class PermissionEntry(
    val path: String,
    val previewDecision: PermissionManager.Decision,
    val externalDecision: PermissionManager.Decision
)
