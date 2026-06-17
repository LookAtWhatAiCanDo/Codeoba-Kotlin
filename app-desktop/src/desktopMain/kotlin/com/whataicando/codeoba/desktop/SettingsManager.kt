package com.whataicando.codeoba.desktop

import com.whataicando.codeoba.core.domain.parser.LogParserFactory
import com.whataicando.codeoba.core.domain.parser.ParserMode
import com.whataicando.codeoba.core.domain.search.ArchivalFilter
import com.whataicando.codeoba.core.domain.source.SourceAdapter
import com.whataicando.codeoba.core.util.AppConfig
import com.whataicando.codeoba.core.util.JsonUtils
import com.whataicando.codeoba.core.util.SecureStorage
import java.util.prefs.Preferences

/**
 * Annotates settings, properties, or functions in [SettingsManager] whose stored values
 * are keyed off and isolated by the active target server base URL.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ServerDependent

object SettingsManager {
    private val prefs: Preferences = Preferences.userNodeForPackage(SettingsManager::class.java)

    enum class Decision {
        UNDECIDED,
        MONITOR,
        IGNORE
    }

    fun getUserDecision(sourceId: String): Decision {
        val value = prefs.get("source_decision_$sourceId", Decision.UNDECIDED.name)
        return try {
            Decision.valueOf(value)
        } catch (e: Exception) {
            Decision.UNDECIDED
        }
    }

    fun setUserDecision(sourceId: String, decision: Decision) {
        prefs.put("source_decision_$sourceId", decision.name)
    }

    fun getWindowX(): Int? {
        val value = prefs.get("window_x", null) ?: return null
        return value.toIntOrNull()
    }
    fun setWindowX(value: Int) {
        prefs.put("window_x", value.toString())
    }

    fun getWindowY(): Int? {
        val value = prefs.get("window_y", null) ?: return null
        return value.toIntOrNull()
    }
    fun setWindowY(value: Int) {
        prefs.put("window_y", value.toString())
    }

    fun getWindowWidth(): Int? {
        val value = prefs.get("window_width", null) ?: return null
        return value.toIntOrNull()
    }
    fun setWindowWidth(value: Int) {
        prefs.put("window_width", value.toString())
    }

    fun getWindowHeight(): Int? {
        val value = prefs.get("window_height", null) ?: return null
        return value.toIntOrNull()
    }
    fun setWindowHeight(value: Int) {
        prefs.put("window_height", value.toString())
    }

    fun getWindowMaximized(): Boolean? {
        val value = prefs.get("window_maximized", null) ?: return null
        return value.toBoolean()
    }
    fun setWindowMaximized(value: Boolean) {
        prefs.put("window_maximized", value.toString())
    }

    fun getWindowScreen(): String? {
        val value = prefs.get("window_screen", null)
        return if (value.isNullOrEmpty()) null else value
    }
    fun setWindowScreen(value: String) {
        prefs.put("window_screen", value)
    }

    fun getSidebarWidth(): Float? {
        val value = prefs.get("sidebar_width", null) ?: return null
        return value.toFloatOrNull()
    }
    fun setSidebarWidth(value: Float) {
        prefs.put("sidebar_width", value.toString())
    }

    fun getSidebarCollapsed(): Boolean? {
        val value = prefs.get("sidebar_collapsed", null) ?: return null
        return value.toBoolean()
    }
    fun setSidebarCollapsed(value: Boolean) {
        prefs.put("sidebar_collapsed", value.toString())
    }

    fun getLastActiveFilters(): List<String> {
        val value = prefs.get("last_active_filters", "")
        if (value.isEmpty()) return emptyList()
        return value.split(",")
    }
    fun setLastActiveFilters(filters: List<String>) {
        prefs.put("last_active_filters", filters.joinToString(","))
    }

    fun getLastStatusFilters(): Set<ArchivalFilter> {
        val value = prefs.get("last_status_filters", "")
        if (value.isEmpty()) return emptySet()
        return value.split(",").mapNotNull {
            try {
                ArchivalFilter.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }
    fun setLastStatusFilters(filters: Set<ArchivalFilter>) {
        prefs.put("last_status_filters", filters.map { it.name }.joinToString(","))
    }

    fun getCacheEnabled(): Boolean {
        return prefs.getBoolean("cache_enabled", true)
    }

    fun setCacheEnabled(value: Boolean) {
        prefs.putBoolean("cache_enabled", value)
    }

    fun getSimilarityThreshold(): Float {
        return prefs.getFloat("similarity_threshold", 0.30f)
    }

    fun setSimilarityThreshold(value: Float) {
        prefs.putFloat("similarity_threshold", value)
    }

    fun getSidebarSortBy(): SidebarSortDimension {
        val name = prefs.get("sidebar_sort_by", SidebarSortDimension.UPDATED.name)
        return try {
            SidebarSortDimension.valueOf(name)
        } catch (e: Exception) {
            SidebarSortDimension.UPDATED
        }
    }

    fun setSidebarSortBy(value: SidebarSortDimension) {
        prefs.put("sidebar_sort_by", value.name)
    }

    fun getSidebarSortAscending(): Boolean {
        return prefs.getBoolean("sidebar_sort_ascending", false)
    }

    fun setSidebarSortAscending(value: Boolean) {
        prefs.putBoolean("sidebar_sort_ascending", value)
    }

    //region Server Dependent settings

    private fun serverKey(key: String): String {
        return "${AppConfig.getBaseUrl()}:$key"
    }

    @ServerDependent
    fun getFirebaseUserEmail(): String? {
        val sKey = serverKey("firebase_user_email")
        var value = prefs.get(sKey, null)
        if (value == null && AppConfig.getBaseUrl() == "codeoba.com") {
            value = prefs.get("firebase_user_email", null)
            if (value != null) {
                prefs.put(sKey, value)
                prefs.remove("firebase_user_email")
            }
        }
        return value
    }

    @ServerDependent
    fun setFirebaseUserEmail(value: String?) {
        val sKey = serverKey("firebase_user_email")
        putOrRemove(sKey, value)
        if (AppConfig.getBaseUrl() == "codeoba.com") {
            prefs.remove("firebase_user_email")
        }
    }

    @ServerDependent
    fun getFirebaseUserUid(): String? {
        val sKey = serverKey("firebase_user_uid")
        var value = prefs.get(sKey, null)
        if (value == null && AppConfig.getBaseUrl() == "codeoba.com") {
            value = prefs.get("firebase_user_uid", null)
            if (value != null) {
                prefs.put(sKey, value)
                prefs.remove("firebase_user_uid")
            }
        }
        return value
    }

    @ServerDependent
    fun setFirebaseUserUid(value: String?) {
        val sKey = serverKey("firebase_user_uid")
        putOrRemove(sKey, value)
        if (AppConfig.getBaseUrl() == "codeoba.com") {
            prefs.remove("firebase_user_uid")
        }
    }

    @ServerDependent
    fun getFirebaseAuthIdToken(): String? {
        val sKey = serverKey("firebase_auth_id_token")
        var value = SecureStorage.get(sKey)
        if (value == null && AppConfig.getBaseUrl() == "codeoba.com") {
            value = SecureStorage.get("firebase_auth_id_token")
            if (value != null) {
                SecureStorage.put(sKey, value)
                SecureStorage.delete("firebase_auth_id_token")
            }
        }
        return value
    }

    @ServerDependent
    fun setFirebaseAuthIdToken(value: String?) {
        val sKey = serverKey("firebase_auth_id_token")
        SecureStorage.put(sKey, value)
        if (AppConfig.getBaseUrl() == "codeoba.com") {
            SecureStorage.delete("firebase_auth_id_token")
        }
    }

    @ServerDependent
    fun getFirebaseAuthRefreshToken(): String? {
        val sKey = serverKey("firebase_auth_refresh_token")
        var value = SecureStorage.get(sKey)
        if (value == null && AppConfig.getBaseUrl() == "codeoba.com") {
            value = SecureStorage.get("firebase_auth_refresh_token")
            if (value != null) {
                SecureStorage.put(sKey, value)
                SecureStorage.delete("firebase_auth_refresh_token")
            }
        }
        return value
    }

    @ServerDependent
    fun setFirebaseAuthRefreshToken(value: String?) {
        val sKey = serverKey("firebase_auth_refresh_token")
        SecureStorage.put(sKey, value)
        if (AppConfig.getBaseUrl() == "codeoba.com") {
            SecureStorage.delete("firebase_auth_refresh_token")
        }
    }

    @ServerDependent
    fun getDeviceId(): String {
        val sKey = serverKey("device_id")
        var deviceId = prefs.get(sKey, null)
        if (deviceId.isNullOrEmpty()) {
            if (AppConfig.getBaseUrl() == "codeoba.com") {
                val legacyId = prefs.get("device_id", null)
                if (!legacyId.isNullOrEmpty()) {
                    prefs.put(sKey, legacyId)
                    prefs.remove("device_id")
                    return legacyId
                }
            }
            val hasLegacyAccount = !getFirebaseUserUid().isNullOrEmpty() ||
                    !getFirebaseUserEmail().isNullOrEmpty()
            if (hasLegacyAccount) {
                val os = System.getProperty("os.name") ?: "Unknown"
                val uid = getFirebaseUserUid()
                val email = getFirebaseUserEmail()
                val stableAccountId = uid ?: email ?: "Unknown"
                val rawId = "$os:$stableAccountId"
                deviceId = java.util.UUID.nameUUIDFromBytes(rawId.toByteArray()).toString()
            } else {
                deviceId = java.util.UUID.randomUUID().toString()
            }
            prefs.put(sKey, deviceId)
        }
        return deviceId
    }

    enum class SyncMode {
        LOCAL_ONLY,
        METADATA_ONLY,
        SUMMARIES_ONLY,
        FULL_SYNC
    }

    @ServerDependent
    fun getSyncMode(): SyncMode {
        val sKey = serverKey("ecosystem_sync_mode")
        val hasKey = prefs.get(sKey, null) != null
        if (!hasKey && AppConfig.getBaseUrl() == "codeoba.com") {
            val legacyVal = prefs.get("ecosystem_sync_mode", null)
            if (legacyVal != null) {
                prefs.put(sKey, legacyVal)
                prefs.remove("ecosystem_sync_mode")
            }
        }
        val value = prefs.get(sKey, SyncMode.METADATA_ONLY.name)
        return try {
            SyncMode.valueOf(value)
        } catch (e: Exception) {
            SyncMode.METADATA_ONLY
        }
    }

    @ServerDependent
    fun setSyncMode(mode: SyncMode) {
        val sKey = serverKey("ecosystem_sync_mode")
        prefs.put(sKey, mode.name)
        if (AppConfig.getBaseUrl() == "codeoba.com") {
            prefs.remove("ecosystem_sync_mode")
        }
    }

    @ServerDependent
    fun getEcosystemActive(): Boolean {
        val sKey = serverKey("ecosystem_active")
        val hasKey = prefs.get(sKey, null) != null
        if (!hasKey && AppConfig.getBaseUrl() == "codeoba.com") {
            val legacyVal = prefs.get("ecosystem_active", null)
            if (legacyVal != null) {
                prefs.putBoolean(sKey, legacyVal.toBoolean())
                prefs.remove("ecosystem_active")
            }
        }
        return prefs.getBoolean(sKey, false)
    }

    @ServerDependent
    fun setEcosystemActive(value: Boolean) {
        val sKey = serverKey("ecosystem_active")
        prefs.putBoolean(sKey, value)
        if (AppConfig.getBaseUrl() == "codeoba.com") {
            prefs.remove("ecosystem_active")
        }
    }

    //endregion

    fun getPreferredParserMode(): ParserMode {
        val name = prefs.get("preferred_parser_mode", ParserMode.SUMMARIZING.name)
        return try {
            ParserMode.valueOf(name)
        } catch (e: Exception) {
            ParserMode.SUMMARIZING
        }
    }

    fun setPreferredParserMode(mode: ParserMode) {
        prefs.put("preferred_parser_mode", mode.name)
    }

    fun getEffectiveParserMode(): ParserMode {
        return if (getEcosystemActive()) {
            getPreferredParserMode()
        } else {
            ParserMode.STANDARD
        }
    }

    fun signOut() {
        setFirebaseUserEmail(null)
        setFirebaseUserUid(null)
        setFirebaseAuthIdToken(null)
        setFirebaseAuthRefreshToken(null)
        setEcosystemActive(false)
        LogParserFactory.setParserMode(getEffectiveParserMode())
    }

    enum class RemoteControlPolicy {
        ALLOW_ALL,
        ALLOW_PAIRED_ONLY,
        BLOCK_ALL
    }

    fun getRemoteControlPolicy(): RemoteControlPolicy {
        val value = prefs.get("remote_control_policy", RemoteControlPolicy.ALLOW_PAIRED_ONLY.name)
        return try {
            RemoteControlPolicy.valueOf(value)
        } catch (e: Exception) {
            RemoteControlPolicy.ALLOW_PAIRED_ONLY
        }
    }

    fun setRemoteControlPolicy(policy: RemoteControlPolicy) {
        prefs.put("remote_control_policy", policy.name)
    }

    fun getExcludedPaths(): List<String> {
        val value = prefs.get("excluded_paths", "")
        return JsonUtils.deserializeList(value)
    }

    fun setExcludedPaths(paths: List<String>) {
        val nonBlank = paths.filter { it.isNotBlank() }
        val jsonStr = JsonUtils.serializeList(nonBlank)
        prefs.put("excluded_paths", jsonStr)
    }

    private fun putOrRemove(key: String, value: String?) {
        if (value == null) {
            prefs.remove(key)
        } else {
            prefs.put(key, value)
        }
    }

    fun getPinnedSessionIds(): Set<String> {
        val value = prefs.get("pinned_session_ids", "")
        if (value.isEmpty()) return emptySet()
        return value.split(",").toSet()
    }

    fun setPinnedSessionIds(ids: Set<String>) {
        prefs.put("pinned_session_ids", ids.joinToString(","))
    }

    fun toggleSessionPinned(sessionId: String) {
        val current = getPinnedSessionIds().toMutableSet()
        if (current.contains(sessionId)) {
            current.remove(sessionId)
        } else {
            current.add(sessionId)
        }
        setPinnedSessionIds(current)
    }

    fun isSessionPinned(sessionId: String): Boolean {
        return getPinnedSessionIds().contains(sessionId)
    }

    fun getSelectedGroupFilter(): String? {
        val value = prefs.get("selected_group_filter", "")
        return if (value.isEmpty()) null else value
    }

    fun setSelectedGroupFilter(value: String?) {
        prefs.put("selected_group_filter", value ?: "")
    }

    fun getThemeCode(): String {
        return prefs.get("theme_code", "obsidian")
    }

    fun setThemeCode(code: String) {
        prefs.put("theme_code", code)
    }

    fun getCustomBg(): Int = prefs.getInt("custom_bg", 0xFF0C0C0E.toInt())
    fun setCustomBg(value: Int) = prefs.putInt("custom_bg", value)

    fun getCustomSurface(): Int = prefs.getInt("custom_surface", 0xFF14141A.toInt())
    fun setCustomSurface(value: Int) = prefs.putInt("custom_surface", value)

    fun getCustomAccent1(): Int = prefs.getInt("custom_accent1", 0xFF00E5FF.toInt())
    fun setCustomAccent1(value: Int) = prefs.putInt("custom_accent1", value)

    fun getCustomAccent2(): Int = prefs.getInt("custom_accent2", 0xFFAB47BC.toInt())
    fun setCustomAccent2(value: Int) = prefs.putInt("custom_accent2", value)

    fun getAutoUpdateEnabled(): Boolean {
        return prefs.getBoolean("auto_update_enabled", true)
    }

    fun setAutoUpdateEnabled(value: Boolean) {
        prefs.putBoolean("auto_update_enabled", value)
    }

    fun getSkippedVersion(): String {
        return prefs.get("skipped_version", "")
    }

    fun setSkippedVersion(value: String) {
        prefs.put("skipped_version", value)
    }

    fun getLastUpdateCheck(): Long {
        return prefs.getLong("last_update_check", 0L)
    }

    fun setLastUpdateCheck(value: Long) {
        prefs.putLong("last_update_check", value)
    }

    fun getInstallGuid(): String {
        val guid = prefs.get("install_guid", "")
        if (guid.isEmpty()) {
            val newGuid = java.util.UUID.randomUUID().toString()
            prefs.put("install_guid", newGuid)
            return newGuid
        }
        return guid
    }

    fun getMinUpdateCheckIntervalSeconds(): Long {
        return prefs.getLong("min_update_check_interval_seconds", UpdateManager.DEFAULT_MIN_UPDATE_CHECK_INTERVAL_SECONDS)
    }

    fun setMinUpdateCheckIntervalSeconds(value: Long) {
        prefs.putLong("min_update_check_interval_seconds", value.coerceAtLeast(0L))
    }
}

fun SourceAdapter.isEffectiveEnabled(): Boolean {
    val decision = SettingsManager.getUserDecision(this.id)
    return when (decision) {
        SettingsManager.Decision.MONITOR -> true
        SettingsManager.Decision.IGNORE -> false
        SettingsManager.Decision.UNDECIDED -> this.isAppInstalled() || this.isAvailable()
    }
}
