package llc.lookatwhataicando.codeoba.desktop

import java.util.prefs.Preferences
import llc.lookatwhataicando.codeoba.core.domain.search.ArchivalFilter
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter

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
}

fun SourceAdapter.isEffectiveEnabled(): Boolean {
    val decision = SettingsManager.getUserDecision(this.id)
    return when (decision) {
        SettingsManager.Decision.MONITOR -> true
        SettingsManager.Decision.IGNORE -> false
        SettingsManager.Decision.UNDECIDED -> this.isAppInstalled() || this.isAvailable()
    }
}


