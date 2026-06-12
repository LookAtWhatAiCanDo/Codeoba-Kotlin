# Development Guide — Codeoba

## 🛠️ Environment Setup
- **Prerequisites**: JDK 17+ and IntelliJ IDEA with **Kotlin** and **Compose Multiplatform** plugins.
- **Root Directory**: `LookAtWhatAiCanDo/Codeoba`

```
Codeoba/
├── app-desktop/      # Desktop UI
├── core/             # Models, parsers, search logic
└── docs/             # Documentation
```

---

## 💻 Commands
Run in monorepo root:
- **Build**: `./gradlew :app-desktop:compileKotlinDesktop`
- **Test**: `./gradlew :core:desktopTest`
- **Run**: `./gradlew :app-desktop:run`

---

## 🔌 Adding a Log Source Adapter

For a detailed step-by-step walkthrough, refer to the [Adding a New Source Adapter Guide](file:///Users/pv/Dev/GitHub/LookAtWhatAiCanDo/Codeoba/docs/ADD_NEW_SOURCE.md).
1. **Implement `SourceAdapter`**: Create a parser class under `core/src/desktopMain/kotlin/com/whataicando/codeoba/core/source/` subclassing `DesktopSourceAdapter`.
2. **Register**: Add it to the main sources list and register it in `SourceRegistry` in `Main.kt`.
3. **Configure UI mapping**: Add custom brand accent colors, formatted name mapping, and the product URL in [FormatUtils.kt](../app-desktop/src/desktopMain/kotlin/com/whataicando/codeoba/desktop/FormatUtils.kt).
4. **Test**: Add tests in `core/src/desktopTest/.../SourceCapabilitiesTest.kt` and `SourceParsersTest.kt`.

---

## 🎨 Customizing Theme & UI

Views reside in `:app-desktop` desktop main:
- `Main.kt`: Main window state coordinates, breadcrumbs toolbar, detail header card, and overlay dialogs.
- `Sidebar.kt`: Log lists, sorting filters, status chips, search query input, and context menus.
- `DetailPane.kt`: Rendered message panels, expandable tool work blocks, and file viewer overlays.
- `Components.kt`: Drag-to-scroll wrappers, scrollbar templates, and orphaned alert indicators.

### Dynamic Theme System:
Theme parameters are resolved dynamically based on the user's active settings selection. The 8 premium theme color definitions (Obsidian, Nordic Frost, Emerald Forest, Dracula, Dracula, Cyberpunk Neon, etc.) reside in `Theme.kt`. Standard color properties include:
- `ObsidianBg` (Background) | `SlateSurface` (Surface panels) | `CardSurface` (Cards/Items)
- `BorderColor` (Borders/Dividers) | `AccentCyan` (Primary Accents/Pins) | `AccentPurple` (Secondary Accents/Tags)
- `TextPrimary` (Primary labels) | `TextSecondary` (Sub-text descriptions)


### UI Rule:
- **Mixed-Case Casing:** Actor labels must be formatted as "User" and "Assistant" (never uppercase-only like "USER" or "ASSISTANT").

---

## 🧪 Testing Guidelines
- Use temporary folders for test environments (`File.createTempFile` + `tempFile.deleteOnExit()`).
- Keep SQLite tests lightweight using memory or temporary database instances.
