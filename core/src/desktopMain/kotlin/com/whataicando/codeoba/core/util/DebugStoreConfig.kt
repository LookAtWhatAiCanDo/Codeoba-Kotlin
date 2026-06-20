package com.whataicando.codeoba.core.util

import com.whataicando.codeoba.core.domain.model.ConversationGroup

/**
 * Thread-safe configuration object to hold store screenshot options.
 * Honored only when BuildConfig.DEBUG is true.
 */
object DebugStoreConfig {
    @Volatile
    var storeMode: String? = null // "apple" or "microsoft"

    @Volatile
    var sizeOverride: Pair<Int, Int>? = null // parsed from --size=WIDTHxHEIGHT

    @Volatile
    var cannedDataPath: String? = null // parsed from --canned-data=PATH

    val isStoreMode: Boolean
        get() = BuildConfig.DEBUG && storeMode != null

    val isCannedDataMode: Boolean
        get() = BuildConfig.DEBUG && (storeMode != null || cannedDataPath != null)

    val cannedGroups: List<ConversationGroup> = listOf(
        ConversationGroup(name = "Backend Service", isPinned = true),
        ConversationGroup(name = "Frontend SPA"),
        ConversationGroup(name = "Mobile Clients"),
        ConversationGroup(name = "DevOps & Deploy"),
        ConversationGroup(name = "Documentation")
    )
}
