# Architecture Guide — Codeoba

Platform-agnostic, zero-dependency local indexing & search engine for AI coding logs (Claude Code, Antigravity, Cursor, Codex, Aider).

---

## 🏗️ Module Structure

```mermaid
graph TD
    AppDesktop[":app-desktop (Compose UI)"] --> Core[":core (Domain, Search, Parsers)"]
    Core --> Common[commonMain: Models, Interfaces, Search Logic]
    Core --> Desktop[desktopMain: NIO watchers, SQLite WAL, Parsers]
```

### 1. `:core`
- **`commonMain`**: Contains models (`Session`, `Turn`), search engine interfaces, and lexical/semantic search algorithms.
- **`desktopMain`**: JVM SQLite JDBC adapter and NIO `WatchService` directory monitoring.

### 2. `:app-desktop`
Jetpack Compose presentation layer split into:
- `Main.kt` (State coordinator), `Sidebar.kt` (Search filter lists), `DetailPane.kt` (Thread viewer), `Components.kt` (Overlay dialogs), `FormatUtils.kt` (Formatting & Clipboard), `MarkdownParser.kt` (Highlighting), `SettingsDialog.kt` (Preferences UI), `StatsComponents.kt` (Metrics charts), `UpdateManager.kt` (Update manager), and `UpdateDialog.kt` (Update prompt UI).

---

## 📊 Standardized Data Model

Defined in `commonMain`:

```kotlin
data class Session(
    val id: String,
    val sourceId: String, // "claude", "cursor", "antigravity", "aider", "codex"
    val filePath: String,
    val timestamp: Long,
    val updatedAt: Long,
    val cwd: String?,
    val threadName: String?,
    val turns: List<Turn>,
    val isArchived: Boolean,
    val isPinned: Boolean
)

data class Turn(
    val turnId: String,
    val userMessage: String,
    val assistantMessage: String,
    val timestamp: Long,
    val extraData: Map<String, String> // e.g. "model", "computeTimeMs"
)
```

---

## 🔌 Source Adapters

Adapters implement the `SourceAdapter` interface to parse transcripts:

```kotlin
interface SourceAdapter {
    val id: String
    val displayName: String
    fun isAvailable(): Boolean
    fun getDefaultLogPaths(): List<String>
    fun getWatchPaths(): List<String>
    fun getWatchFileFilter(): (String) -> Boolean = { true }
    suspend fun parseSession(filePath: String): Session?
    suspend fun parseAllSessions(): List<Session>
}
```

### Implementations:
All desktop source implementations inherit from the abstract base class [DesktopSourceAdapter](../core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/source/DesktopSourceAdapter.kt) under `desktopMain`, which centralizes common caching, folder validation (`getBaseDir()`), directory deletion, and command execution availability checking.

1. **`DesktopCursorSource`**: Parses Cursor's global SQLite (`state.vscdb`). Filters sessions against each workspace's local `allComposers` list to exclude deleted sessions. Overrides the base `parseSession` to handle row-level database hashing.
2. **`DesktopClaudeSource`**: Parses JSONL log files under `~/.claude/projects/`. Implements `parseSessionContent(File)`.
3. **`DesktopAntigravitySource`**: Parses Antigravity's JSONL files, extracting settings changes (e.g., `<USER_SETTINGS_CHANGE>`), system messages (e.g., background compile logs), and tool execution error logs. Implements `parseSessionContent(File)`.
4. **`DesktopCodexSource`**: Indexes JSONL logs under `~/.codex/`. Implements `parseSessionContent(File)`.
5. **`DesktopAiderSource`**: Reads and tokenizes Markdown logs (`.aider.chat.history.md`). Implements `parseSessionContent(File)`.

---

## 🔍 Search & Indexing

- **`LexicalSearchEngine`**: Keyword query match using term frequency ranking.
- **`SemanticSearchEngine`**: Concept similarity ranking using local hashing-based embeddings and cosine distance.
- **`SearchFilter`**: Restricts search results by source IDs, timestamp ranges, workspace paths, and session archival status (`ArchivalFilter`).
- **`IndexManager`**: Coordinates scanning, index updates, directory watchers, and timing instrumentation.
- **`SessionCacheManager`**: Persistently caches pre-parsed `Session` models under `~/.codeoba/cache/cache_<sourceId>.json` using `kotlinx.serialization` to avoid expensive startup JSON/Markdown parsing and semantic embedding computation:
  - File-based sources: Check file metadata (path, size, modification timestamp) to hit cache instantly.
  - Database-based sources (Cursor): Compute string MD5 hashes on SQLite values in memory to hit cache without JSON parsing.
  - Pruning: Automatically deletes orphaned cache entries for sessions that are no longer present on disk during scans.
- **`Startup Profiler`**: Measures and formats log reports of exact scanning times and percentages for all registered active source adapters. Together with the persistent caching layer, this reduces the average startup log scanning/load time from ~2.5s down to ~0.25s (a ~90% optimization).
- **SQLite WAL**: Uses `mode=ro` (read-only) without `immutable` to safely query Cursor’s database concurrently in real time.

---

## ⚙️ Settings Management

Persisted via `java.util.prefs.Preferences`:
- **States**: `Monitor` (scan even if not installed), `Ignore` (skip folder), `Default` (prompt on orphaned logs).
- **Settings UI**: Two-pane dialog in `:app-desktop` for settings toggles and recursive path purges.
- **Search Filters**: Restores the last selected "Filter by Source" and "Filter by Status" states on application launch.
- **Auto-Updates**: Configures automatic startup update checks (enabled/disabled) and stores user-skipped version tags, providing manual check hooks and native platform installer deployment.
