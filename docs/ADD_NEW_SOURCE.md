# Developer Guide: Adding a New Log Source to Codeoba

This guide outlines the step-by-step process for developers to add a new AI coding agent or application as a supported log source inside Codeoba (e.g. Claude Code, Google Antigravity, Cursor, OpenAI Codex, Aider, GitHub Copilot).

---

## 🛠️ Step 1: Implement the Source Adapter in `:core`

All log parsers and directory scanning adapters reside in the `:core` module under:
`core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/source/`

Create a subclass of `DesktopSourceAdapter` (e.g., `DesktopMyAgentSource.kt`):

```kotlin
package llc.lookatwhataicando.codeoba.core.source

import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import llc.lookatwhataicando.codeoba.core.util.PlatformUtils
import java.io.File

class DesktopMyAgentSource : DesktopSourceAdapter() {
    override val id: String = "myagent"
    override val displayName: String = "My Agent Name"

    override fun getBaseDir(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, ".myagent/logs")
    }

    override fun isAppInstalled(): Boolean {
        // Return true if the agent binary or application folder is present
        return isExecutableInstalled("myagent") || getBaseDir().exists()
    }

    override fun getWatchFileFilter(): ((String) -> Boolean) = { path ->
        // Watch path filter matching log/session files
        path.endsWith(".jsonl") || path.endsWith(".log")
    }

    override suspend fun parseSessionContent(file: File): Session? {
        // Parse individual log files and map them to Session and Turn domains
        val filePath = file.absolutePath
        val turns = mutableListOf<Turn>()
        
        // ... read lines and populate turns ...
        
        return Session(
            id = file.nameWithoutExtension,
            sourceId = id,
            filePath = filePath,
            timestamp = file.lastModified(),
            updatedAt = file.lastModified(),
            cwd = "/detected/project/path",
            threadName = "Session Title",
            turns = turns
        )
    }

    override suspend fun parseAllSessions(): List<Session> {
        val baseDir = getBaseDir()
        if (!baseDir.exists() || !baseDir.isDirectory) return emptyList()

        val sessions = mutableListOf<Session>()
        baseDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "jsonl") {
                val session = parseSession(file.absolutePath)
                if (session != null) sessions.add(session)
            }
        }
        return sessions
    }
}
```

---

## 🎨 Step 2: Register in `:app-desktop` UI

To integrate the new source with the user interface, register it in the following files:

### 1. `Main.kt`
Path: `app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Main.kt`

*   **Import the class**:
    ```kotlin
    import llc.lookatwhataicando.codeoba.core.source.DesktopMyAgentSource
    ```
*   **Add to debug list** inside `main(args: Array<String>)`:
    ```kotlin
    val sources = listOf(
        // ...
        llc.lookatwhataicando.codeoba.core.source.DesktopMyAgentSource()
    )
    ```
*   **Register in `SourceRegistry`** inside `mainEntry()`:
    ```kotlin
    val sourceRegistry = remember {
        SourceRegistry().apply {
            // ...
            register(DesktopMyAgentSource())
        }
    }
    ```

### 2. `FormatUtils.kt`
Path: `app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/FormatUtils.kt`

*   **Assign Brand Colors** in `getSourceBadgeColors(sourceId: String)`:
    ```kotlin
    "myagent" -> Pair(Color(0xFFACC123), Color(0x1FACC123)) // Custom HSL-harmonious color
    ```
*   **Map Display Name** in `formatSourceDisplayName(sourceId: String)`:
    ```kotlin
    "myagent" -> "My Agent Name"
    ```
*   **Map Product Page Webpage** in `getProductUrl(sourceId: String)`:
    ```kotlin
    "myagent" -> "https://myagent.website"
    ```

---

## 🧪 Step 3: Write Unit Tests

Ensure capabilities and parsing logic are covered under the `:core` desktop test module:

### 1. `SourceCapabilitiesTest.kt`
Path: `core/src/desktopTest/kotlin/llc/lookatwhataicando/codeoba/core/source/SourceCapabilitiesTest.kt`

Add a test validating installation detection and data cleanup rules:
```kotlin
@Test
fun testMyAgentSourceAppInstalledAndCleanup() = withMockUserHome { home ->
    val source = DesktopMyAgentSource()
    assertEquals(listOf(File(home, ".myagent/logs").absolutePath), source.getDataPathsToDelete())

    val logsDir = File(home, ".myagent/logs")
    logsDir.mkdirs()

    assertTrue(source.isAvailable())
    assertTrue(source.deleteDataPaths())
    assertFalse(logsDir.exists())
}
```

### 2. `SourceParsersTest.kt`
Path: `core/src/desktopTest/kotlin/llc/lookatwhataicando/codeoba/core/source/SourceParsersTest.kt`

Add a test verifying that mock log logs parse correctly into turns:
```kotlin
@Test
fun testMyAgentSourceParsing() = runBlocking {
    val tempFile = File.createTempFile("myagent_test_", ".jsonl")
    tempFile.deleteOnExit()
    tempFile.writeText("{\"user\":\"hello\",\"bot\":\"hi\"}")

    val source = DesktopMyAgentSource()
    val session = source.parseSession(tempFile.absolutePath)
    
    assertNotNull(session)
    assertEquals(1, session.turns.size)
    assertEquals("hello", session.turns[0].userMessage)
    assertEquals("hi", session.turns[0].assistantMessage)
    
    tempFile.delete()
    Unit
}
```

---

## 🚀 Step 4: Build and Verify

1.  **Run unit tests**:
    ```bash
    ./gradlew :core:desktopTest
    ```
2.  **Launch application in Dev Mode**:
    ```bash
    ./gradlew :app-desktop:run
    ```
3.  **Confirm UI mapping**: Open **Settings -> Sources** inside the app to see the new source entry. Verify that session lists load and render styling correctly in the sidebar and main dashboards.
