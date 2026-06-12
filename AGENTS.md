# Codeoba Agent Instructions

Welcome! You are an AI coding assistant working on **Codeoba**—a platform-agnostic, zero-external-dependency, 100% local search application that indexes, monitors, and searches conversation transcripts across Claude Code, Google Antigravity, Cursor, OpenAI Codex, Aider, and GitHub Copilot.

This file acts as the primary repository context and instructions guide. Read this first to align with the codebase.

---

## 📖 Documentation Maintenance Guidelines

To ensure the project context remains accurate:
1. **Synchronized Updates:** When code structures, design decisions, source adapters, or file paths change, you must update the relevant codebase documentation (including this file `AGENTS.md`, the root `README.md`, and architectural files under `docs/`).
2. **Definition of Done:** A task, refactoring, or feature implementation is not complete until all corresponding documentation has been updated to reflect the new state of the codebase.
3. **No Automatic Git Staging/Commits:** By default, never stage (`git add`) or commit (`git commit`) changes unless explicitly requested or prompted by the user.
4. **Relative Pathing Requirement:** Always write file paths relative to the folder they are in (e.g., `./README.md` or `../core/`). Never document absolute file paths or paths outside of the repository, with the important exception of data paths for the source agents this application monitors.
5. **Plan Synchronization:** Any time a CLI command, parameter, file path, or configuration flag changes or is corrected during implementation, you must immediately propagate that change to the local `implementation_plan.md` in the system app data directory, as well as any architectural plan files under `docs/`.

---

## 🏗️ Codebase Directory Map

- **[core](./core)**: Kotlin Multiplatform (KMP) search engine, parsers, and watchers.
  - `commonMain`: Unified data models (`Session`, `Turn`), indexer interfaces, lexical/semantic search engine algorithms, and shared `SemVer.kt` version parsing.
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
  - `UpdateManager.kt`: Asynchronous release checking, redirect-following download streams, and native installer execution.
  - `UpdateDialog.kt`: Dark-themed update overlay prompting version jumps, markdown release notes, and progress.
- **[docs](./docs)**: System architecture and developer walkthroughs.
  - **[ECOSYSTEM_GUIDE.md](./docs/ECOSYSTEM_GUIDE.md)**: User-facing guide explaining device pairing, sync modes, and wearable features.

---

## 🎨 UI Style Guidelines & Constraints

When modifying the Compose UI under `app-desktop`, adhere to these style guidelines:

1. **Dynamic Color Theme Styling Palette**:
   - Background Color `ObsidianBg`, Surface Color `SlateSurface`, Item Container Color `CardSurface`, Border Color `BorderColor`, Highlights/Primary Accent `AccentCyan`, Secondary Accent `AccentPurple`, Text Primary `TextPrimary`, and Text Secondary `TextSecondary` are dynamic package-level properties defined in `./app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Theme.kt`.
   - The values resolve dynamically based on the current theme loaded from `SettingsManager.getThemeCode()` into `ThemeManager.currentTheme`.
   - Users can select from 8 handsome themes (Obsidian, Nordic Frost, Emerald Forest, Sunset Copper, Royal Amethyst, Dracula, Cyberpunk Neon, Monochrome Slate) in the General settings panel.

2. **Mixed-Case Casing Constraint**:
   - Never display uppercase-only labels like "USER" or "ASSISTANT". Use capitalized words (e.g. "User", "Assistant").

3. **macOS Window Layout & Navigation**:
   - Clear a `76.dp` top-left spacer on macOS to avoid overlapping the macOS transparent titlebar window controls.
   - **Navigation History Stack & Breadcrumbs Toolbar**:
     - `Main.kt` maintains `navigationStack` (selected session IDs) and `navigationIndex` for Back/Forward navigation.
     - Breadcrumbs display `Workspace Name / Active Session Title`. Clickable workspace root clears selection.
     - Far-right actions (`...`) dropdown menu provides copy actions and local file opening.
     - Session metadata is rendered as a scrollable `DetailHeaderCard` at the top of the message `LazyColumn`.

4. **Text Vertical Alignment in Small Containers**:
   - Small buttons, badges, status indicators, and toggle labels (especially with heights `< 32.dp`) containing capital letters or glyphs/symbols without descenders (e.g. "Cc", "W", ".*", "\n", "OK") often appear vertically shifted upwards in Skia / JVM Compose due to reserved descender font metrics.
   - Proactively compensate for this by applying `Modifier.offset(y = 1.dp)` (or custom vertical padding/offsets) to the `Text` composable inside centering containers (such as `Box(contentAlignment = Alignment.Center)`).
   - **Important**: When small texts are nested inside an `OutlinedTextField` (e.g., in a `trailingIcon`), they inherit the parent's `LocalTextStyle` which can have a large `lineHeight` that pushes the text upwards. Break this inheritance by passing `style = TextStyle.Default` (from `androidx.compose.ui.text.TextStyle`) and setting `lineHeight` equal to `fontSize` (e.g. `lineHeight = 11.sp` for `fontSize = 11.sp`).

5. **Scrollbar Controls & Touch/Drag Scrolling**:
   - Every scrollable container (e.g. `LazyColumn`, `Column` with `verticalScroll`, `Row` with `horizontalScroll`) must support touch-screen and mouse-drag scrolling and display a visual scrollbar.
   - Use the custom extension `.dragToScroll(scrollState)` or `.dragToScroll(lazyListState)` defined in `./app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Components.kt` on the scrollable container.
   - Wrap the scrollable container in a `Box` and add a sibling `VerticalScrollbar` (or `HorizontalScrollbar`) aligned to the edge (e.g. `Alignment.CenterEnd`).
   - Use `themedScrollbarStyle()` to style the scrollbar to match the dynamic color theme's cyan accents (`AccentCyan`). Apply a small padding (e.g., `end = 12.dp`) to the scrollable container so its content doesn't overlap the scrollbar.

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
    - The settings dialog shows an exhaustive list of all supported source adapters, regardless of whether they are active or ignored, so the user can easily re-enable them. The sources list is sorted alphabetically by their display name, but bisected into two groups: installed adapters are displayed at the top, followed by uninstalled/not-detected adapters at the bottom.
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
    - Right-clicking a session item in the sidebar list view (`./app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Sidebar.kt`) opens a custom `DropdownMenu` at the click cursor location, matching the design and actions of the detail pane actions/tags overflow menu.
    - It provides options to toggle pin status, edit groups/tags (via an inline filterable sub-menu checklist), open the session file, and copy the session ID or file path (which triggers the bottom-aligned floating toast notification (`AnimatedVisibility`) overlay with standard premium accents (`AccentCyan`)).
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
    - When `ParserMode.SUMMARIZING` is enabled, the cache is queried outside of the parser delegate block (before parsing and summarization run). On cache hits, the fully cached session (with its generated AI summaries) is returned immediately, completely bypassing local model inference and making log re-scans extremely efficient.
    - Orphaned cache entries are automatically deleted during the scan process via an in-memory tracking list (`seenPaths`).
    - Startup scanning timings are measured at millisecond precision for each active adapter and overall total. A detailed and formatted profiling summary is printed in the logs, sorting the sources by their duration in descending order.
    - Caching reduces the average startup log scanning/load time from ~2.5s down to ~0.25s (a ~90% performance improvement).
    - Cache features can be toggled in the General settings panel in the UI or overridden using command line arguments (`--cache` / `--no-cache`).
16. **Desktop Source Adapter Consolidation**:
    - Centralizes common parsing caching logic, directory checking (`getBaseDir()`), availability/installation checks (`isAvailable()`, `isExecutableInstalled(binaryName)`), and session data cleanup (`deleteDataPaths()`, `getDataPathsToDelete()`) into the shared base class `./core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/source/DesktopSourceAdapter.kt`.
    - File-based sources (Claude, Codex, Antigravity, Aider, Copilot) implement `parseSessionContent(File)` and benefit from automated file metadata check/cache write on miss.
    - Database-based sources (Cursor) override `parseSession(String)` directly to bypass file-based caching and implement custom SQLite row hashing.
17. **Conversations Sidebar List Sorting**:
    - The left-side conversations sidebar displays a "Sort by" section with interactive horizontal chips: Relevance (only when a search query is active), Updated, Tokens, Speed, Turns, and Duration.
    - Selecting a chip sorts the indexed conversation sessions dynamically. Numeric/temporal metrics default to descending order on first click. Toggling the active chip flips the sorting direction.
    - If the search query becomes empty, the active sort option falls back from Relevance to Updated automatically.
    - The sorting preference (sortBy and sortAscending) is persistently stored via `SettingsManager` (Java Preferences API) to remain persistent across application launches.

18. **Context Compaction Tracking**:
    - Identifies context compaction checkpoints (`"type":"CHECKPOINT"`) in Google Antigravity logs (`transcript.jsonl`).
    - Calculates the estimated duration of each compaction event based on the timestamp gap relative to the preceding user input event.
    - Persists statistics within `Turn.extraData` as `isCompaction` and `compactionTimeMs`.
    - Exposes stats in the UI as a dedicated "Compactions: N" badge on active sessions and "Context Compactions" and "Est. Compaction Time" cards in the main workspace statistics dashboard.

19. **Conversation Pinning**:
    - Allows users to pin/unpin conversation sessions via a context menu option in the sidebar list or an actions dropdown option in the detail toolbar.
    - Persists the set of pinned session IDs locally using Java Preferences API (`SettingsManager`).
    - Integrates with the local sidebar list sorting (`sortedSearchResults` remember block) by stable-sorting pinned items to float them to the top: `.sortedByDescending { it.session.isPinned }`. This keeps pinned sessions at the top while preserving sorting dimensions inside both pinned and unpinned groups.
    - Renders a custom cyan "Pinned" badge in the sidebar items, and displays a small cyan pin icon next to the session title in the detail toolbar.

20. **Markdown Link Resolution & In-App File Viewer**:
    - Parses inline markdown links `[link text](url)` inside `MarkdownParser.kt` and formats them with an underline and standard premium accent color (`AccentCyan`) while adding a `"URL"` string annotation to the `AnnotatedString`.
    - Handles pointer hover icon swaps and tap gestures inside a custom `ClickableMarkdownText` composable, ensuring click bounds match the exact character bounds to avoid false positive clicks on blank line ends.
    - Resolves local file references and URIs securely via the centralized utility `LocalFileResolver.resolveLocalFileLink` in the `:core` module.
    - **Security & Path Traversal Controls**: Implements strict boundary checking. Only the session workspace directory (`session.cwd`) is implicitly trusted (`Allowed`). Files outside this workspace, or directories/scripts, trigger a `ConfirmationRequired` state.
    - **Action-Specific Permission Store**: Consent choices (`ALLOW` / `DENY`) are persisted in Java Preferences under the child node `file_permissions` via `PermissionManager` and are split by action scopes (`PREVIEW` vs. `EXTERNAL_OPEN`) associated with the MD5 hash and canonical path of the target file to prevent hash collisions. Users can review and revoke these rules in a dedicated "Permissions" category in the settings panel.
    - **Bounded File Reading**: Restricts file reader allocation by loading a maximum of 5MB + 1 byte using `readNBytes()` to prevent memory exhaustion.
    - Renders markdown files (`.md`) recursively using the app's rich `MarkdownView`, and other source code files as scrollable monospace text with line numbers.
    - Provides a fallback button in the file preview dialog to launch the file in the default OS handler after validating external opening permissions.

21. **Sidebar Session Item Tag Display**:
    - Each conversation item in the sidebar list view (`Sidebar.kt`) displays its assigned tags/groups using a `FlowRow` of badges styled in `AccentPurple` (12% alpha background, 40% alpha border, 4.dp rounded corners) positioned below the last message snippet.
    - To prevent Skia/JVM text vertical alignment issues, apply `Modifier.offset(y = 0.5.dp)` to the badge text.

22. **Entire Sidebar Cell Draggable Hit-Testing**:
    - When rendering draggable list items (such as `SessionItem`), place the `.clickable` modifier *before* (outer to) the `.pointerInput` modifiers in the chain. Placing `clickable` outer makes the entire layout shape hit-testable, which forces Compose to propagate pointer events down to the inner `pointerInput` modifiers even over empty transparent spacing and gaps in the cell.
    - Always place custom `.pointerHoverIcon(PointerIcon.Hand)` modifiers *after* `.clickable` to prevent hover gesture filters from intercepting or consuming long-press pointer events meant for drag gesture detectors.
    - To prevent vertical drag conflicts inside scrollable containers (like `verticalScroll` or `LazyColumn` lists on Desktop), use immediate `detectDragGestures` instead of `detectDragGesturesAfterLongPress`. Because scrolling on desktop with a mouse uses the mouse wheel, dragging inside list items can start dragging immediately (past touch slop) to optimize responsiveness and success rates across all directions. Clicks still propagate normally since they do not exceed touch slop.

23. **Consolidated OS Detection**:
    - Avoid direct system property checks for operating system detection (e.g., `System.getProperty("os.name")`). Use centralized helpers in `./core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/util/PlatformUtils.kt`: `PlatformUtils.isMac()`, `PlatformUtils.isWindows()`, `PlatformUtils.isLinux()`.

24. **Local ONNX-based Semantic Search**:
    - Replaces pseudo-random word hashing with a real local neural text embedding pipeline using the quantized `all-MiniLM-L6-v2` transformer model.
    - Pre-trained models are downloaded on demand to `~/.codeoba/models/` and loaded dynamically using ONNX Runtime JVM bindings.
    - The search similarity threshold is dynamically adjustable via an interactive slider in a dedicated "Semantic" tab in the settings dialog and persisted via `SettingsManager.getSimilarityThreshold()`.
    - Leverages a local `EmbeddingCache` (implemented via `EmbeddingCacheManager`) to store text embedding vectors in `~/.codeoba/cache/embeddings_cache.json`, preventing redundant ONNX calls.
    - Indexes sessions in parallel using Kotlin coroutines and a `Semaphore` (concurrency limit of 4) in `SemanticSearchEngine.updateIndex` to utilize multicore CPUs efficiently and prevent UI thread blockage.

25. **Auto-Update Mechanism**:
    - Exposes the app version at runtime by generating a `version.txt` resource via a custom Gradle build task during compilation.
    - Compares versions numerically using a shared `SemVer` class (major, minor, patch parsing) to ensure that `1.10.0` is recognized as newer than `1.9.0`.
    - Queries the Firebase-proxied update endpoint (`http://localhost:5000/api/update` in dev/emulator mode, or production Cloud Function URL) on startup using a custom User-Agent (`Codeoba/{version} ({OS}; {arch}; GUID-{guid})`) to avoid GitHub API rate-limiting while logging anonymous telemetry.
    - Downloads matching installer packages (`.msi`, `.pkg`, or `.deb`) to the directory `~/.codeoba/updates/` with real-time progress indicators.
    - Launches native installers via `ProcessBuilder` (e.g. passive MSI `/passive` on Windows for seamless background installation, PKG launcher `open` on macOS, or `xdg-open` on Linux) and exits the JVM with `System.exit(0)` to release write locks.

26. **Multi-Device Ecosystem Sync & Subscription Redesign**:
    - Free local lexical and semantic searches are enabled by default for all users, while AI-powered summarization is a premium subscription feature.
    - Paid subscription entitlements are enforced strictly on the backend to gate access to the Device Sync Hub and remote command relay APIs.
    - Implements secure, browser-delegated OAuth flow utilizing a temporary JDK-native HTTP loopback server (listening on a random port for `/callback` parameters) and a unified Web Console SPA (running on Firebase Hosting) to prevent in-app credential handling.
    - Stores all sensitive authentication credentials (ID and refresh tokens, licensing JWT, decryption key) in the OS-native keyring (Keychain on macOS, Credential Manager on Windows, Secret Service on Linux) using a secure utility with automatic self-healing migration and a Java Preferences fallback. Keychain/Keyring prompts can be bypassed completely during development or troubleshooting by setting the JVM system property `codeoba.no.keyring=true`.
    - Billing webhooks query subscriptions using the user's immutable Firebase `uid` mapped in checkout custom metadata rather than mutable emails to support profile email updates.
    - Implements challenge-response authentication utilizing 90-second single-use nonces and device public/private key pairs (stored securely in the OS-native keyring via `SecureStorage` with a fallback to Preferences).
    - Integrates a best-effort local regex secrets scanner to redact sensitive credentials on the client side before synchronizing data.
    - Allows configuring Sync Modes (Local, Metadata, Summaries, Full Sync), target machine Remote Control Policies, and workspace Path Exclusions inside the Settings panel.

26. **Firebase API Key Configuration**:
    - The Firebase Web API key used for token refresh operations is resolved dynamically via the JVM system property `codeoba.firebase.api_key` or environment variable `CODEOBA_FIREBASE_API_KEY`.
    - If running in production (non-emulator) mode and the API key matches the default placeholder or is blank, a fail-fast validation check throws an `IllegalArgumentException` with clear configuration instructions.

---

## 🛠️ Common Gradle Development Commands

- Compile Desktop Client: `./gradlew :app-desktop:compileKotlinDesktop`
- Run Unit Tests: `./gradlew :core:desktopTest`
- Launch Application in Dev Mode: `./gradlew :app-desktop:run`
