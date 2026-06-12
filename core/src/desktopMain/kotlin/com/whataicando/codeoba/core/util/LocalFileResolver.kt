package com.whataicando.codeoba.core.util

import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

sealed interface LocalFileResolution {
    data class Allowed(val path: Path) : LocalFileResolution
    data class ConfirmationRequired(val path: Path, val reason: String) : LocalFileResolution
    data class Rejected(val reason: String) : LocalFileResolution
}

object LocalFileResolver {

    fun resolveLocalFileLink(
        rawLink: String,
        baseDirectory: Path?,
        trustedRoot: Path?
    ): LocalFileResolution {
        val trimmed = rawLink.trim()
        if (trimmed.isEmpty()) return LocalFileResolution.Rejected("Empty link")

        // Reject web scheme early
        val lower = trimmed.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return LocalFileResolution.Rejected("Web URLs not supported for local resolution")
        }

        val path: Path = try {
            if (lower.startsWith("file:")) {
                val uri = URI(trimmed)
                // Reject opaque URIs and URIs with non-empty authorities/hosts (like UNC shares)
                if (uri.isOpaque) {
                    return LocalFileResolution.Rejected("Opaque file URIs are not supported")
                }
                if (!uri.authority.isNullOrBlank()) {
                    return LocalFileResolution.Rejected("Remote authorities/UNC shares are not supported")
                }
                Paths.get(uri)
            } else {
                // Plain paths: handle tilde (~) indicators and relative paths
                val expanded = expandTilde(trimmed)
                if (expanded != null) {
                    Paths.get(expanded)
                } else if (baseDirectory != null && !isAbsolutePath(trimmed)) {
                    baseDirectory.resolve(trimmed)
                } else {
                    Paths.get(trimmed)
                }
            }
        } catch (e: Exception) {
            return LocalFileResolution.Rejected("Invalid path format: ${e.message}")
        }

        // Normalization & Symlink Resolution
        val normalized = path.toAbsolutePath().normalize()
        val realPath = try {
            if (normalized.exists()) normalized.toRealPath() else normalized
        } catch (e: Exception) {
            normalized
        }

        // Validate target path constraints
        if (!realPath.exists()) {
            return LocalFileResolution.Rejected("File does not exist: $realPath")
        }
        if (realPath.isDirectory()) {
            return LocalFileResolution.Rejected("Target path is a directory: $realPath")
        }
        if (!realPath.isRegularFile()) {
            return LocalFileResolution.Rejected("Target path is not a regular file: $realPath")
        }

        // Compare against trusted root
        if (trustedRoot != null) {
            val isTrusted = try {
                val realRoot = if (trustedRoot.exists()) trustedRoot.toRealPath() else trustedRoot.toAbsolutePath().normalize()
                realPath.startsWith(realRoot)
            } catch (e: Exception) {
                false
            }
            if (isTrusted) {
                return LocalFileResolution.Allowed(realPath)
            }
        }

        return LocalFileResolution.ConfirmationRequired(realPath, "The path lies outside your workspace.")
    }

    private fun isAbsolutePath(path: String): Boolean {
        if (path.startsWith("/")) return true
        if (path.length > 2 && path[1] == ':' && (path[2] == '/' || path[2] == '\\')) return true
        return false
    }

    private fun expandTilde(path: String): String? {
        if (path == "~") return getHomeDirectory()
        if (path.startsWith("~/") || path.startsWith("~\\")) {
            val home = getHomeDirectory() ?: return null
            val sub = path.drop(2).replace('\\', File.separatorChar).replace('/', File.separatorChar)
            return File(home, sub).absolutePath
        }
        return null
    }

    private fun getHomeDirectory(): String? {
        val javaHome = System.getProperty("user.home")
        if (!javaHome.isNullOrBlank()) return javaHome

        val envHome = System.getenv("HOME")
        if (!envHome.isNullOrBlank()) return envHome

        val envUserProfile = System.getenv("USERPROFILE")
        if (!envUserProfile.isNullOrBlank()) return envUserProfile

        val homeDrive = System.getenv("HOMEDRIVE")
        val homePath = System.getenv("HOMEPATH")
        if (!homeDrive.isNullOrBlank() && !homePath.isNullOrBlank()) {
            return homeDrive + homePath
        }

        return null
    }
}
