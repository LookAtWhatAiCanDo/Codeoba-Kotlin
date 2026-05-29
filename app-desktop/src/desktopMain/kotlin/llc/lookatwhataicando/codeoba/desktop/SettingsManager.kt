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
}

fun SourceAdapter.isEffectiveEnabled(): Boolean {
    val decision = SettingsManager.getUserDecision(this.id)
    return when (decision) {
        SettingsManager.Decision.MONITOR -> true
        SettingsManager.Decision.IGNORE -> false
        SettingsManager.Decision.UNDECIDED -> this.isAppInstalled() || this.isAvailable()
    }
}


