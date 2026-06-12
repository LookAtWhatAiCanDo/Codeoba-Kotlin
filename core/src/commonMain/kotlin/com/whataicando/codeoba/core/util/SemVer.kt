package com.whataicando.codeoba.core.util

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    companion object {
        fun parse(versionStr: String): SemVer {
            val clean = versionStr.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
            val parts = clean.split('.')
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            return SemVer(major, minor, patch)
        }
    }

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"
}
