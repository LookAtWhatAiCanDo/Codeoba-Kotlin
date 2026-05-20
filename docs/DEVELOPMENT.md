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

1. **Implement `SourceAdapter`**: Create a parser class under `core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/source/`.
2. **Register**: Add it to `SourceRegistry` in `Main.kt`.
3. **Configure UI**: Add styling, badges, and name conversion in [FormatUtils.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/FormatUtils.kt).
4. **Test**: Write parser and watch verification tests in `core/src/desktopTest/.../SourceParsersTest.kt`.

---

## 🎨 Customizing Theme & UI

Views reside in `:app-desktop` desktop main:
- `Main.kt`: Root windows and layout bounds.
- `Sidebar.kt`: List filters and search inputs.
- `DetailPane.kt`: Message scroll panels and toolbar.
- `Components.kt`: Shared layouts and custom dialog overlays.

### Theme Palette (Obsidian-Dark):
- Background: `ObsidianBg` (`#0A0E17`) | Surface: `SlateSurface` (`#161F30`) | Cards: `CardSurface` (`#1E293B`)
- Border: `BorderColor` (`#2C3B54`) | Accents: `AccentCyan` (`#00E5FF`) & `AccentPurple` (`#D500F9`)

### UI Rule:
- **Mixed-Case Casing:** Actor labels must be formatted as "User" and "Assistant" (never uppercase-only like "USER" or "ASSISTANT").

---

## 🧪 Testing Guidelines
- Use temporary folders for test environments (`File.createTempFile` + `tempFile.deleteOnExit()`).
- Keep SQLite tests lightweight using memory or temporary database instances.
