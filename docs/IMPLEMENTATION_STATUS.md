# Implementation Status

Current feature status and roadmap for Codeoba.

## 📈 Feature Matrix

| Phase | Feature | Status | Notes |
|---|---|---|---|
| 1 | KMP monorepo restructuring | 100% (✅) | Core module with commonMain/desktopMain split |
| 2 | Pluggable Source Adapters | 100% (✅) | Claude, Cursor, Aider, Codex, Antigravity, GitHub Copilot |
| 3 | Local ONNX Semantic Search | 100% (✅) | Neural text embeddings using local `all-MiniLM-L6-v2` model and cosine distance |
| 4 | Jetpack Compose Desktop UI | 100% (✅) | 8 premium dark themes, dynamic colors, scrollbars |
| 5 | Navigation History Stack | 100% (✅) | Back/forward stack & breadcrumbs navigation bar |
| 6 | Scroll-Lock & Tail Scrolling | 100% (✅) | Thread loading list with auto-scroll anchors |
| 7 | Uninstalled App Overlays | 100% (✅) | Alerts for orphaned directories with user Preference persistence |
| 8 | Two-Pane Settings Dialog | 100% (✅) | Alphabetical source management, dynamic soft-disables, data purges |
| 9 | Deletion Path Verification | 100% (✅) | Explicit path listings for file deletions |
| 10 | Window & Splitter Layout Persistence | 100% (✅) | Restores last bounds, screen, maximized, & sidebar state |
| 11 | Search Filter State Persistence | 100% (✅) | Persists and restores last used "Filter by Source" and "Filter by Status" |
| 12 | Persistent Startup Cache | 100% (✅) | Session metadata caching, MD5 database hashing, scanning time profiler |
| 13 | Context Compaction Tracking | 100% (✅) | Auto-identifies Antigravity compaction events and calculates durations |
| 14 | Conversation Pinning | 100% (✅) | Pin/unpin conversations, persistent settings, sidebar float-to-top |
| 15 | In-App File Preview Viewer | 100% (✅) | Markdown link clicking, secure path resolver, granular permissions |

## 🚀 Future Roadmap

### Phase 16: Mobile (Android / iOS) Build Configurations
- Native compilation configs for Android and iOS launchers.
- Adapt common main UI views for responsive mobile displays.

### Phase 17: Real-time Database Remote Sync
- Implement remote synchronization of indexed sessions across peer Codeoba installs.
