# Implementation Status

Current feature status and roadmap for Codeoba.

## 📈 Feature Matrix

| Phase | Feature | Status | Notes |
|---|---|---|---|
| 1 | KMP monorepo restructuring | 100% (✅) | Core module with commonMain/desktopMain split |
| 2 | Pluggable Source Adapters | 100% (✅) | Claude, Cursor, Aider, Codex, Antigravity |
| 3 | Dual-Engine Search Interface | 100% (✅) | Keyword search + high-dimensional local semantic matcher |
| 4 | Jetpack Compose Desktop UI | 100% (✅) | Premium Obsidian dark theme, resizable split-panes |
| 5 | Navigation History Stack | 100% (✅) | Back/forward stack & breadcrumbs navigation bar |
| 6 | Scroll-Lock & Tail Scrolling | 100% (✅) | Thread loading list with auto-scroll anchors |
| 7 | Uninstalled App Overlays | 100% (✅) | Alerts for orphaned directories with user Preference persistence |
| 8 | Two-Pane Settings Dialog | 100% (✅) | Per-source monitoring, status checks, and data purges |
| 9 | Deletion Path Verification | 100% (✅) | Explicit path listings for file deletions |
| 10 | Window & Splitter Layout Persistence | 100% (✅) | Restores last bounds, screen, maximized, & sidebar state |
| 11 | Search Filter State Persistence | 100% (✅) | Persists and restores last used "Filter by Source" and "Filter by Status" |

## 🚀 Future Roadmap

### Phase 12: Mobile (Android / iOS) Build Configurations
- Native compilation configs for Android and iOS launchers.
- Adapt common main UI views for responsive mobile displays.

### Phase 13: Real-time Database Remote Sync
- Implement remote synchronization of indexed sessions across peer Codeoba installs.
