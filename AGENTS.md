# Codeoba Agent Instructions

Welcome! You are an AI coding assistant working on **Codeoba**—a platform-agnostic, zero-external-dependency, 100% local search application that indexes, monitors, and searches conversation transcripts across Claude Code, Google Antigravity, Cursor, OpenAI Codex, and Aider.

This file acts as the primary repository context and instructions guide. Read this first to align with the codebase.

---

## 📖 Documentation Maintenance Guidelines

To ensure the project context remains accurate:
1. **Synchronized Updates:** When code structures, design decisions, source adapters, or file paths change, you must update the relevant codebase documentation (including this file `AGENTS.md`, the root `README.md`, and architectural files under `docs/`).
2. **Definition of Done:** A task, refactoring, or feature implementation is not complete until all corresponding documentation has been updated to reflect the new state of the codebase.
3. **No Automatic Git Staging/Commits:** By default, never stage (`git add`) or commit (`git commit`) changes unless explicitly requested or prompted by the user.
4. **Relative Pathing Requirement:** Always write file paths relative to the folder they are in (e.g., `./README.md` or `../core/`). Never document absolute file paths or paths outside of the repository, with the important exception of data paths for the source agents this application monitors.

---

## 🏗️ Codebase Directory Map

- **[core](./core)**: Kotlin Multiplatform (KMP) search engine, parsers, and watchers.
  - `commonMain`: Unified data models (`Session`, `Turn`), indexer interfaces, lexical/semantic search engine algorithms.
  - `desktopMain`: JVM-specific log directory watchers (Java NIO) and SQLite JDBC adapters (with read-only WAL support).
  - `desktopTest`: Parser, watch-filter, and semantic similarity unit tests.
- **[app-desktop](./app-desktop)**: Desktop UI front-end built with Jetpack Compose Multiplatform.
  - `Main.kt`: App entry point, lifecycle, window state, and navigation stack.
  - `Sidebar.kt`: Displays search query inputs, lexical vs. semantic search toggles, and results list.
  - `DetailPane.kt`: Dialogue turns viewer, metadata card, toolbar, find-bar, and markdown renderer.
  - `Components.kt`: Shared UI elements ( orphaned warning overlays, canvas toggle icons).
  - `FormatUtils.kt`: Clipboard utilities, execution speed calculations, and color mapping.
  - `MarkdownParser.kt`: Tokenizes and syntax-highlights raw conversation transcripts.
  - `SettingsDialog.kt`: App-level settings pane (general preferences, source monitoring controls).
  - `StatsComponents.kt`: Statistics overview and metrics charts.
- **[docs](./docs)**: System architecture and developer walkthroughs.

---

## 🎨 UI Style Guidelines & Constraints

When modifying the Compose UI under `app-desktop`, adhere to these style guidelines:

1. **Obsidian / Slate Styling Palette**:
   - Background Color: `ObsidianBg` = `Color(0xFF0C0C0E)`
   - Surface Color: `SlateSurface` = `Color(0xFF14141A)`
   - Item Container Color: `CardSurface` = `Color(0xFF1E1E28)`
   - Border Color: `BorderColor` = `Color(0xFF2C2C3A)`
   - Highlights / Primary Accent: `AccentCyan` = `Color(0xFF00E5FF)`
   - Secondary Accent: `AccentPurple` = `Color(0xFFAB47BC)`
   - Text Colors: `TextPrimary` = `Color(0xFFF5F5F7)` and `TextSecondary` = `Color(0xFF9E9EAE)`

2. **Mixed-Case Casing Constraint**:
   - Never display uppercase-only labels like "USER" or "ASSISTANT". Use capitalized words (e.g. "User", "Assistant").

3. **macOS Window Layout & Navigation**:
   - Clear a `76.dp` top-left spacer to avoid overlapping the macOS transparent titlebar window controls.
   - **Navigation History Stack & Breadcrumbs Toolbar**:
     - `Main.kt` maintains `navigationStack` (selected session IDs) and `navigationIndex` for Back/Forward navigation.
     - Breadcrumbs display `Workspace Name / Active Session Title`. Clickable workspace root clears selection.
     - Far-right actions (`...`) dropdown menu provides copy actions and local file opening.
     - Session metadata is rendered as a scrollable `DetailHeaderCard` at the top of the message `LazyColumn`.

4. **Text Vertical Alignment in Small Containers**:
   - Small buttons, badges, status indicators, and toggle labels (especially with heights `< 32.dp`) containing capital letters or glyphs/symbols without descenders (e.g. "Cc", "W", ".*", "\n", "OK") often appear vertically shifted upwards in Skia / JVM Compose due to reserved descender font metrics.
   - Proactively compensate for this by applying `Modifier.offset(y = 1.dp)` (or custom vertical padding/offsets) to the `Text` composable inside centering containers (such as `Box(contentAlignment = Alignment.Center)`).
   - **Important**: When small texts are nested inside an `OutlinedTextField` (e.g., in a `trailingIcon`), they inherit the parent's `LocalTextStyle` which can have a large `lineHeight` that pushes the text upwards. Break this inheritance by passing `style = TextStyle.Default` (from `androidx.compose.ui.text.TextStyle`) and setting `lineHeight` equal to `fontSize` (e.g. `lineHeight = 11.sp` for `fontSize = 11.sp`).

---

## ⚙️ Core Architecture Patterns

1. **Cursor Orphaned Session Filter**:
   - Skip global Cursor database (`state.vscdb`) rows not present in the workspace's local `allComposers` list to hide deleted sessions.
2. **Cursor Read-Only WAL Synchronization**:
   - SQLite connections to Cursor use read-only mode (`mode=ro`) without `immutable` for real-time querying without write locks.
3. **Targeted Watcher Filters**:
   - NIO directory watchers must be filtered to specific target extensions (e.g., only `.jsonl` or `.aider.chat.history.md`) to avoid loops on build/source files.
4. **FlowRow Statistic Wrapping**:
   - Wrap conversation stats inside a `FlowRow` to prevent vertical layout clipping.
5. **Window & Sidebar Splitter Layout Persistence**:
   - Persist and restore the window bounds (x, y, width, height), maximized state, active screen, and sidebar width/collapsed state via `SettingsManager` (Java Preferences API).
   - Use standard AWT `ComponentListener` and `WindowStateListener` with a 500ms debounce to track changes in the normal/floating window states, plus an immediate save on application close.
   - Validate saved coordinates on startup against the active display configuration (`GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices`) to prevent off-screen window layouts.
6. **Conversation Archival Patterns & UI Styling**:
   - Indicate archived sessions in the sidebar list view (`Sidebar.kt`) by applying `Modifier.alpha(0.5f)` to the container and rendering a dedicated "Archived" badge next to the source badge.
   - For Google Antigravity, parse the archived status from the companion state annotation files (`~/.gemini/antigravity/annotations/<sessionId>.pbtxt`) by checking if they contain `"archived:true"`. Enable real-time updates by adding `.pbtxt` files to the watcher filter.
   - For OpenAI Codex, detect archived status by verifying if the parent directory name of the session `.jsonl` file is `"archived_sessions"`.
7. **Exhaustive Settings & Dynamic Sidebar Filtering**:
    - The settings dialog shows an exhaustive list of all supported source adapters, regardless of whether they are active or ignored, so the user can easily re-enable them.
    - A modern toggle switch allows enabling or disabling a source provider by moving its state to `IGNORE` or `MONITOR`/`UNDECIDED`. When disabled, its configuration segments are hidden.
    - The sidebar displays filters for all active (non-ignored) sources, regardless of whether they have any sessions in the index database. Selecting an enabled source with zero sessions displays an empty list, allowing natural discoverability.
    - Added direct product page URLs to settings so users can quickly visit the respective agent products.
8. **Background Auto-Detection & Auto-Enabling**:
    - Undetected or uninstalled sources default to a soft-disabled (off) state when their decision is `UNDECIDED`.
    - A background polling loop in `Main.kt` runs every 5 seconds, checking if any adapter's effective enabled status changes (e.g. when a previously undetected agent tool gets installed or starts generating logs). If a status change is detected, it automatically schedules a full index re-scan.
9. **Active & Archived Search Status Filtering**:
    - The search filters sidebar displays a "Filter by Status" section with two chips: "Active" (highlighted in `AccentCyan` when selected) and "Archived" (highlighted in `AccentPurple` when selected).
    - Toggling a status chip restricts results to sessions matching that state (`isArchived == false` for Active, `isArchived == true` for Archived).
    - If neither or both status chips are selected, the application displays all sessions (equivalent to "Show All").
10. **Model Performance & Usage Breakdown Sorting**:
    - The Model Performance & Usage Breakdown card on the main dashboard displays statistics per model.
    - It allows sorting by Turns, Tokens, Speed (tokens/second), Duration (compute time), and Model Name via interactive sorting chips wrapped in a `FlowRow` to prevent layout clipping.
    - Sorting defaults prioritize user interest: numeric metrics (Turns, Tokens, Speed, Duration) default to descending order on first click, whereas lexical metrics (Model Name) default to ascending (A-Z) order.
    - The active sort selection is indicated by an arrow icon reflecting the current direction (Upward for ascending, Downward for descending), toggling direction on subsequent clicks.
11. **Session Item Context Menu & Toast Notification**:
    - Right-clicking a session item in the sidebar list view (`./app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Sidebar.kt`) opens a context menu via `ContextMenuArea` allowing the user to "Copy Source File Path".
    - Clicking the copy menu action invokes the platform clipboard utilities to copy `session.filePath` and triggers a bottom-aligned floating toast notification (`AnimatedVisibility`) overlay inside the sidebar layout with standard premium accents (`AccentCyan`).
12. **Search Filter State Persistence**:
    - Persist and restore the last used "Filter by Source" and "Filter by Status" settings via `SettingsManager` (Java Preferences API) to maintain selected search filters across application launches.
13. **Multi-Level Expandable Conversation Details**:
    - Group assistant conversation message parts up to the last tool execution of a turn into a top-level collapsible `WorkedForBlock` Composable ("Worked for X").
    - Renders as a clickable chevron-toggled header at Level 1, which reveals nested Level 2 (individual tool headers) and Level 3 (raw tool content/outputs) details inside a layout indented with a left vertical connector line (thread line) styled with 50% opacity border color.
    - If search results match inside any inner tool or text block of the collapsed work block, auto-expand the `WorkedForBlock` via `LaunchedEffect`.
14. **Antigravity Tool Tag Escaping**:
    - The Antigravity log parser uses `[[[TOOL:` and `[[[/TOOL]]]` tags in the assistant message string to represent tool execution boundaries for the UI.
    - If log text or tool inputs/outputs contain these tags literally (e.g. from search queries or file content discussions), they must be escaped as `\\[\\[\\[TOOL:` and `\\[\\[\\[/TOOL\\]\\]\\]` during parser extraction in `./core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/source/DesktopAntigravitySource.kt` to prevent malformed parsing.
    - The UI's `parseAssistantMessage` in `./app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/MarkdownParser.kt` skips escaped tags during parsing and unescapes them back to their original form for presentation.
15. **Persistent Startup Cache & Index Profiler**:
    - A thread-safe file caching system in `SessionCacheManager` serializes parsed `Session` objects to `~/.codeoba/cache/cache_<sourceId>.json` using `kotlinx.serialization`.
    - File-based sources check if `filePath`, `lastModified` timestamp, and `size` match the cached entry before reading and parsing. Database-based sources (Cursor) query the SQLite DB and match the row value's size and MD5 hash to bypass JSON parsing.
    - Orphaned cache entries are automatically deleted during the scan process via an in-memory tracking list (`seenPaths`).
    - Startup scanning timings are measured at millisecond precision for each active adapter and overall total. A detailed and formatted profiling summary is printed in the logs, sorting the sources by their duration in descending order.
    - Caching reduces the average startup log scanning/load time from ~2.5s down to ~0.25s (a ~90% performance improvement).
    - Cache features can be toggled in the General settings panel in the UI or overridden using command line arguments (`--cache` / `--no-cache`).
16. **Desktop Source Adapter Consolidation**:
    - Centralizes common parsing caching logic, directory checking (`getBaseDir()`), availability/installation checks (`isAvailable()`, `isExecutableInstalled(binaryName)`), and session data cleanup (`deleteDataPaths()`, `getDataPathsToDelete()`) into the shared base class `./core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/source/DesktopSourceAdapter.kt`.
    - File-based sources (Claude, Codex, Antigravity, Aider) implement `parseSessionContent(File)` and benefit from automated file metadata check/cache write on miss.
    - Database-based sources (Cursor) override `parseSession(String)` directly to bypass file-based caching and implement custom SQLite row hashing.
17. **Conversations Sidebar List Sorting**:
    - The left-side conversations sidebar displays a "Sort by" section with interactive horizontal chips: Relevance (only when a search query is active), Updated, Tokens, Speed, Turns, and Duration.
    - Selecting a chip sorts the indexed conversation sessions dynamically. Numeric/temporal metrics default to descending order on first click. Toggling the active chip flips the sorting direction.
    - If the search query becomes empty, the active sort option falls back from Relevance to Updated automatically.
    - The sorting preference (sortBy and sortAscending) is persistently stored via `SettingsManager` (Java Preferences API) to remain persistent across application launches.

---

## 🛠️ Common Gradle Development Commands

- Compile Desktop Client: `./gradlew :app-desktop:compileKotlinDesktop`
- Run Unit Tests: `./gradlew :core:desktopTest`
- Launch Application in Dev Mode: `./gradlew :app-desktop:run`
