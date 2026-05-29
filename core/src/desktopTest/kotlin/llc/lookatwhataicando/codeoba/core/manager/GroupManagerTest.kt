package llc.lookatwhataicando.codeoba.core.manager

import kotlinx.coroutines.runBlocking
import llc.lookatwhataicando.codeoba.core.domain.model.ConversationGroup
import llc.lookatwhataicando.codeoba.core.domain.model.GroupTask
import java.io.File
import kotlin.test.*

class GroupManagerTest {

    private lateinit var tempHomeDir: File
    private var originalUserHome: String? = null

    @BeforeTest
    fun setUp() {
        originalUserHome = System.getProperty("user.home")
        tempHomeDir = File.createTempFile("codeoba_test_home_", "")
        tempHomeDir.delete()
        tempHomeDir.mkdirs()
        System.setProperty("user.home", tempHomeDir.absolutePath)
        
        // Reset singleton memory state by loading (will load empty list from the empty directory)
        GroupManager.loadGroups()
    }

    @AfterTest
    fun tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome!!)
        }
        tempHomeDir.deleteRecursively()
    }

    @Test
    fun testAddLoadSaveGroups() {
        val groups = GroupManager.getGroups()
        assertTrue(groups.isEmpty())

        val added = GroupManager.addGroup("Project Alpha")
        assertTrue(added)
        
        val groupsAfterAdd = GroupManager.getGroups()
        assertEquals(1, groupsAfterAdd.size)
        assertEquals("Project Alpha", groupsAfterAdd[0].name)

        // Attempting to add duplicate should fail
        val addedDuplicate = GroupManager.addGroup("Project Alpha")
        assertFalse(addedDuplicate)

        // Verify file was written
        val codeobaDir = File(tempHomeDir, ".codeoba")
        val groupsFile = File(codeobaDir, "groups.json")
        assertTrue(groupsFile.exists())

        // Load groups in a clean list to simulate app restart
        // (will read from file)
        val reloadedGroups = GroupManager.loadGroups()
        assertEquals(1, reloadedGroups.size)
        assertEquals("Project Alpha", reloadedGroups[0].name)
    }

    @Test
    fun testAssignAndRemoveSessions() {
        GroupManager.addGroup("Project Alpha")
        
        GroupManager.assignSessionToGroup("session_123", "Project Alpha")
        var group = GroupManager.getGroups().first()
        assertTrue(group.sessionIds.contains("session_123"))

        // Assigning same session again shouldn't duplicate
        GroupManager.assignSessionToGroup("session_123", "Project Alpha")
        group = GroupManager.getGroups().first()
        assertEquals(1, group.sessionIds.size)

        // Remove session
        GroupManager.removeSessionFromGroup("session_123", "Project Alpha")
        group = GroupManager.getGroups().first()
        assertFalse(group.sessionIds.contains("session_123"))
    }

    @Test
    fun testRenameNestedGroups() {
        GroupManager.addGroup("Project Alpha")
        GroupManager.addGroup("Project Alpha/Backend")
        GroupManager.addGroup("Project Alpha/Frontend")
        GroupManager.addGroup("Other Project")

        // Rename Project Alpha -> Project Beta
        val renamed = GroupManager.renameGroup("Project Alpha", "Project Beta")
        assertTrue(renamed)

        val groups = GroupManager.getGroups().map { it.name }.toSet()
        assertTrue(groups.contains("Project Beta"))
        assertTrue(groups.contains("Project Beta/Backend"))
        assertTrue(groups.contains("Project Beta/Frontend"))
        assertTrue(groups.contains("Other Project"))
        assertFalse(groups.contains("Project Alpha"))
        assertFalse(groups.contains("Project Alpha/Backend"))
        assertFalse(groups.contains("Project Alpha/Frontend"))
    }

    @Test
    fun testPinning() {
        GroupManager.addGroup("Project Alpha")
        var group = GroupManager.getGroups().first()
        assertFalse(group.isPinned)

        GroupManager.setGroupPinned("Project Alpha", true)
        group = GroupManager.getGroups().first()
        assertTrue(group.isPinned)

        GroupManager.setGroupPinned("Project Alpha", false)
        group = GroupManager.getGroups().first()
        assertFalse(group.isPinned)
    }

    @Test
    fun testCleanOrphanedSessions() {
        GroupManager.addGroup("Project Alpha")
        GroupManager.assignSessionToGroup("valid_session", "Project Alpha")
        GroupManager.assignSessionToGroup("orphaned_session", "Project Alpha")

        // Set up tasks: one linked to valid, one to orphaned
        val group = GroupManager.getGroups().first()
        val tasks = listOf(
            GroupTask("task_1", "Task 1", false, "valid_session"),
            GroupTask("task_2", "Task 2", false, "orphaned_session")
        )
        GroupManager.addOrUpdateGroup(group.copy(tasks = tasks))

        // Clean orphaned sessions
        GroupManager.cleanOrphanedSessions(setOf("valid_session"))

        val updatedGroup = GroupManager.getGroups().first()
        assertEquals(setOf("valid_session"), updatedGroup.sessionIds)
        
        val updatedTasks = updatedGroup.tasks
        assertEquals(2, updatedTasks.size)
        
        val task1 = updatedTasks.find { it.id == "task_1" }
        assertNotNull(task1)
        assertEquals("valid_session", task1.associatedSessionId)

        val task2 = updatedTasks.find { it.id == "task_2" }
        assertNotNull(task2)
        assertNull(task2.associatedSessionId)
    }
}
