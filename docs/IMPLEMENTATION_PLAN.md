# Implementation Plan: Codeoba KMP Cross-Tool Session Search

Codeoba is a unified local indexing and search app designed to search coding assistant session histories (Claude Code, OpenAI Codex, Cursor, Google Antigravity, Aider, and others). 

To achieve a platform-holistic solution (Desktop, Mobile, Wearables) and embrace the personal challenge of a Kotlin Multiplatform stack, we will build a native **Kotlin Multiplatform (KMP) monorepo** in the repository root.

---

## User Review Required

> [!IMPORTANT]
> **Branding & Repository Guidelines Alignment**
> 1. **Phase Numbering:** Whole integers only (Phase 1, 2, 3, etc. No decimals).
> 2. **Doc Hierarchy:** Maintain a dedicated `docs/` folder in `Codeoba` containing `IMPLEMENTATION_STATUS.md`, `ARCHITECTURE.md`, `DEVELOPMENT.md`, and `ISSUE_TRACKING.md`.
> 3. **Commit Messages:** Standard prefix headers (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`, `chore:`).

---

## Open Questions

> [!IMPORTANT]
> 1. **Kotlin UI Framework:** We propose using **Compose Multiplatform** for the Desktop UI (split-pane layout) and shared core views. Does this match your design preferences?
> 2. **Target Platforms for v1 Build:** Should we configure the Gradle project for **Desktop (JVM) only** in Phase 1 to get the search engine and UI running, and stub out Android/iOS build targets for Phase 2? (Recommended, as it accelerates initial validation).

---

## Proposed Changes

### Monorepo Restructuring (KMP)

We will initialize the following Gradle module structure in `Codeoba`:
- **`:core` (Kotlin Multiplatform)**
  - `commonMain` — Shared indexing pipelines, search engine interfaces, data models, and Compose UI views.
  - `desktopMain` — Desktop-specific file-watching hooks (JVM WatchService) and native SQLite JDBC database drivers.
- **`:app-desktop` (Compose Multiplatform Desktop)**
  - Desktop launcher and packaging scripts (DMG/MSI/DEB) for running the local search engine and UI.

---

### `:core` Component Design

#### Directory Structure
```
packages/core/src/commonMain/kotlin/llc/lookatwhataicando/codeoba/
├── domain/
│   ├── model/
│   │   ├── Session.kt         # Normalized session structure
│   │   └── Turn.kt            # Message/action turn details
│   ├── search/
│   │   ├── SearchEngine.kt    # Dual-Engine search interface (Lexical + Vector)
│   │   └── SemanticEmbedder.kt# Semantic embeddings interface
│   └── source/
│       ├── SourceAdapter.kt   # Plug-and-play adapter interface
│       └── SourceRegistry.kt  # Registry of active adapters
├── data/
│   ├── search/
│   │   └── InMemorySearchEngine.kt # FTS5/Lexical and pluggable Vector search
│   └── source/
│       ├── ClaudeSource.kt    # Claude Code JSONL parser & path watch
│       ├── CursorSource.kt    # Cursor SQLite db parser
│       └── AntigravitySource.kt # Antigravity JSONL log parser
└── ui/
    ├── AppTheme.kt            # Visual theme (dark mode, high aesthetics)
    └── MainSearchScreen.kt    # Sidebar + Split-Pane search interface
```

#### Unified Models
```kotlin
data class Session(
    val id: String,
    val sourceId: String,       // "claude", "cursor", "antigravity", "aider", "codex"
    val filePath: String,
    val timestamp: Long,
    val updatedAt: Long,
    val cwd: String?,
    val threadName: String?,
    val turns: List<Turn>
)

data class Turn(
    val turnId: String,
    val userMessage: String,
    val assistantMessage: String,
    val extraData: Map<String, String> // e.g. tool execution details, git branch
)
```

#### Pluggable `SourceAdapter` Interface
```kotlin
interface SourceAdapter {
    val id: String
    val displayName: String
    
    fun isAvailable(): Boolean
    fun getDefaultLogPaths(): List<String>
    fun getWatchPaths(): List<String>
    suspend fun parseSession(filePath: String): Session?
    suspend fun parseAllSessions(): List<Session>
}
```

---

### In-Memory Indexing & Lightweight Watching

To keep searches **instant (<5ms)** and fully up-to-date with **live head data**, the `:app-desktop` runner will:
1. Initialize the indexer on startup, calling `parseAllSessions()` across all registered `SourceAdapter`s.
2. Store the resulting `Session` list in an in-memory thread-safe cache.
3. Set up a JVM `WatchService` monitoring *only* the specific log and database paths returned by `getWatchPaths()`.
4. When a file modification is detected, re-parse only that session file and update the in-memory cache dynamically.

---

### Dual-Engine Search Interface (Lexical + Semantic)

```kotlin
interface SearchEngine {
    suspend fun search(query: String, filter: SearchFilter): List<SearchResult>
}

interface SemanticEmbedder {
    suspend fun getEmbeddings(text: String): FloatArray
}
```
* **v1:** The `InMemorySearchEngine` will token-match the query against user messages and assistant transcripts.
* **v2:** A local `SemanticEmbedder` (e.g., via ONNX Java bindings running MiniLM locally) will be plugged in, running cosine-similarity math against cached embeddings for natural language questions.

---

### Verification Plan

#### Automated Tests
- JUnit tests in `:core` to mock JSONL files and verify parser accuracy.
- Tests to check that file watch triggers correctly update the in-memory cache.

#### Manual Verification
- Launch the Compose Desktop UI (`./gradlew :app-desktop:run`).
- Verify immediate filtering by source (Claude, Cursor, Antigravity) and workspace path.
- Type in a query and check that results update instantly.
- Ask a question in Claude Code and verify that the session updates live in the Codeoba search UI within 1 second.
