package llc.lookatwhataicando.codeoba.core.manager

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import llc.lookatwhataicando.codeoba.core.domain.model.ConversationGroup
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import java.io.File

@Serializable
data class GroupsContainer(
    val groups: List<ConversationGroup> = emptyList()
)

object GroupManager {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private var inMemoryGroups = mutableListOf<ConversationGroup>()
    private val lock = Any()

    private fun getGroupsFile(): File {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".codeoba")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "groups.json")
    }

    fun loadGroups(): List<ConversationGroup> = synchronized(lock) {
        val file = getGroupsFile()
        if (!file.exists()) {
            inMemoryGroups = mutableListOf()
            return emptyList()
        }
        try {
            val text = file.readText()
            if (text.isBlank()) {
                inMemoryGroups = mutableListOf()
                return emptyList()
            }
            val container = json.decodeFromString<GroupsContainer>(text)
            inMemoryGroups = container.groups.toMutableList()
            return inMemoryGroups.toList()
        } catch (e: Exception) {
            log("GroupManager: Failed to load groups.json, returning empty list.", e)
            inMemoryGroups = mutableListOf()
            return emptyList()
        }
    }

    fun saveGroups() = synchronized(lock) {
        val file = getGroupsFile()
        try {
            val text = json.encodeToString(GroupsContainer.serializer(), GroupsContainer(inMemoryGroups.toList()))
            file.writeText(text)
        } catch (e: Exception) {
            log("GroupManager: Failed to save groups.json.", e)
        }
    }

    fun getGroups(): List<ConversationGroup> = synchronized(lock) {
        if (inMemoryGroups.isEmpty()) {
            loadGroups()
        }
        return inMemoryGroups.toList()
    }

    fun addOrUpdateGroup(group: ConversationGroup) = synchronized(lock) {
        val existingIndex = inMemoryGroups.indexOfFirst { it.name.lowercase() == group.name.lowercase() }
        val now = System.currentTimeMillis()
        val updatedGroup = group.copy(
            updatedAt = now,
            createdAt = if (existingIndex >= 0) inMemoryGroups[existingIndex].createdAt else now
        )
        if (existingIndex >= 0) {
            inMemoryGroups[existingIndex] = updatedGroup
        } else {
            inMemoryGroups.add(updatedGroup)
        }
        saveGroups()
    }

    fun addGroup(name: String): Boolean = synchronized(lock) {
        if (name.isBlank()) return false
        val exists = inMemoryGroups.any { it.name.lowercase() == name.lowercase() }
        if (exists) return false
        val now = System.currentTimeMillis()
        inMemoryGroups.add(
            ConversationGroup(
                name = name,
                createdAt = now,
                updatedAt = now
            )
        )
        saveGroups()
        return true
    }

    fun deleteGroup(name: String) = synchronized(lock) {
        inMemoryGroups.removeAll { it.name.lowercase() == name.lowercase() }
        saveGroups()
    }

    fun renameGroup(oldName: String, newName: String): Boolean = synchronized(lock) {
        if (newName.isBlank() || oldName.lowercase() == newName.lowercase()) return false
        if (inMemoryGroups.any { it.name.lowercase() == newName.lowercase() }) return false
        
        val existingIndex = inMemoryGroups.indexOfFirst { it.name.lowercase() == oldName.lowercase() }
        if (existingIndex >= 0) {
            val oldGroup = inMemoryGroups[existingIndex]
            inMemoryGroups[existingIndex] = oldGroup.copy(
                name = newName,
                updatedAt = System.currentTimeMillis()
            )
            // Rename descendants if nested
            val prefix = "$oldName/"
            for (i in inMemoryGroups.indices) {
                val g = inMemoryGroups[i]
                if (g.name.startsWith(prefix)) {
                    val remaining = g.name.substring(prefix.length)
                    inMemoryGroups[i] = g.copy(
                        name = "$newName/$remaining",
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
            saveGroups()
            return true
        }
        return false
    }

    fun assignSessionToGroup(sessionId: String, groupName: String) = synchronized(lock) {
        val index = inMemoryGroups.indexOfFirst { it.name.lowercase() == groupName.lowercase() }
        if (index >= 0) {
            val group = inMemoryGroups[index]
            if (!group.sessionIds.contains(sessionId)) {
                inMemoryGroups[index] = group.copy(
                    sessionIds = group.sessionIds + sessionId,
                    updatedAt = System.currentTimeMillis()
                )
                saveGroups()
            }
        } else {
            // Group doesn't exist, create it and add the session
            val now = System.currentTimeMillis()
            inMemoryGroups.add(
                ConversationGroup(
                    name = groupName,
                    sessionIds = setOf(sessionId),
                    createdAt = now,
                    updatedAt = now
                )
            )
            saveGroups()
        }
    }

    fun removeSessionFromGroup(sessionId: String, groupName: String) = synchronized(lock) {
        val index = inMemoryGroups.indexOfFirst { it.name.lowercase() == groupName.lowercase() }
        if (index >= 0) {
            val group = inMemoryGroups[index]
            if (group.sessionIds.contains(sessionId)) {
                inMemoryGroups[index] = group.copy(
                    sessionIds = group.sessionIds - sessionId,
                    updatedAt = System.currentTimeMillis()
                )
                saveGroups()
            }
        }
    }

    fun getGroupsForSession(sessionId: String): List<ConversationGroup> = synchronized(lock) {
        return getGroups().filter { it.sessionIds.contains(sessionId) }
    }

    fun setGroupPinned(name: String, pinned: Boolean) = synchronized(lock) {
        val index = inMemoryGroups.indexOfFirst { it.name.lowercase() == name.lowercase() }
        if (index >= 0) {
            val group = inMemoryGroups[index]
            inMemoryGroups[index] = group.copy(
                isPinned = pinned,
                updatedAt = System.currentTimeMillis()
            )
            saveGroups()
        }
    }

    fun cleanOrphanedSessions(allValidSessionIds: Set<String>) = synchronized(lock) {
        var changed = false
        for (i in inMemoryGroups.indices) {
            val group = inMemoryGroups[i]
            val cleanedIds = group.sessionIds.filter { allValidSessionIds.contains(it) }.toSet()
            
            // Clean tasks that reference orphaned sessions
            val cleanedTasks = group.tasks.map { task ->
                if (task.associatedSessionId != null && !allValidSessionIds.contains(task.associatedSessionId)) {
                    task.copy(associatedSessionId = null)
                } else {
                    task
                }
            }

            if (cleanedIds.size != group.sessionIds.size || cleanedTasks != group.tasks) {
                inMemoryGroups[i] = group.copy(
                    sessionIds = cleanedIds,
                    tasks = cleanedTasks,
                    updatedAt = System.currentTimeMillis()
                )
                changed = true
            }
        }
        if (changed) {
            saveGroups()
        }
    }
}
