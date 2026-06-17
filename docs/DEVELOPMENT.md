# Developer Setup & Development Guide — Codeoba Client

Welcome to the **Codeoba Client** development team! This guide will walk you through setting up your local environment, building the application, and developing new features.

---

## 🛠️ Environment Setup

Follow these steps to set up your local machine for developing Codeoba:

### 1. Install Java Development Kit (JDK)
Codeoba compiles against JDK 17 (or newer JVM version).
* **Mac (Homebrew)**: 
  ```bash
  brew install openjdk@17
  ```
* Ensure your `JAVA_HOME` environment variable is set and points to your JDK 17 installation:
  ```bash
  java -version # Verify it prints 17.x.x
  ```

### 2. Import into IntelliJ IDEA
We recommend using **IntelliJ IDEA (Community or Ultimate Edition)**:
1. Open IntelliJ.
2. Select **Open** and target the root of this repository: `LookAtWhatAiCanDo/Codeoba`.
3. Wait for IntelliJ to sync the Gradle build files automatically.
4. Install the following plugins from the Marketplace:
   * **Kotlin** (bundled by default)
   * **Compose Multiplatform IDE Support** (from JetBrains)

### 3. Repository Directory Layout
```
Codeoba/
├── build.gradle.kts     # Root Gradle settings and plugin configuration
├── settings.gradle.kts  # Module project linkage (:core, :app-desktop)
├── local.properties     # Git-ignored local developer overrides
├── core/                # Kotlin Multiplatform library module
│   └── src/
│       ├── commonMain/  # Platform-agnostic data models, interface definitions
│       └── desktopMain/ # JVM-specific SQLite database, NIO directory watchers
├── app-desktop/         # Jetpack Compose Multiplatform UI module
│   └── src/
│       └── desktopMain/ # Compose UI views, entrypoint, settings, theme
└── docs/                # Feature guides and architecture documentation
```

---

## ⚙️ Build-Time Properties (`local.properties`) vs. Runtime JVM Arguments

When building and running the client app, configuration properties are split into two categories:

### 1. Build-Time Properties (in `local.properties` or environment variables)
These properties are evaluated by Gradle during compilation to generate `BuildConfig.kt` and are baked directly into the binary.
* **`codeoba.enable_subscription`**: Set to `true` to compile paid subscription features, device sync views, and remote control options into the UI. *(Note: This is a temporary developer toggle to A/B test the application with and without subscription capabilities. It will eventually be removed and permanently enabled once the subscription features are officially released).*
* **`codeoba.premium.public_key`**: The Ed25519 public key used to verify the premium summarizer JAR signature. If `codeoba.enable_subscription=true`, this key is **required** and the build will fail if it is missing.
* **`codeoba.firebase.api_key`**: Overrides the Firebase Web API Key for auth token refreshes in staging or production. Defaults to `"EMULATOR_ONLY"`.
* **`codeoba.app_signature_hash`**: Overrides the client app attestation token sent in HTTP headers. Defaults to `"DEVELOPMENT_ONLY"`.

> [!WARNING]
> `codeoba.no.keyring` is **not** a build-time property and has no effect if specified in `local.properties`. It is evaluated at runtime and must be passed as a JVM system property.

### 2. Runtime JVM Arguments (passed as `-Dargument=value` at execution time)
These arguments configure the compiled binary dynamically during execution.
* **`-Dcodeoba.base_url=<host>`**: Configures the base URL/domain for API requests. Defaults to `codeoba.com` (Production). Set to `localhost:5000` to target the local emulator, or `dev.codeoba.com` to target staging.
* **`-Dcodeoba.no.keyring=<true|false>`**: Defaults to `true` in dev/emulator environments (to automatically bypass native Keychain/Credential Manager access prompts on unsigned developer builds) and is ignored/forced to `false` in production. Set to `false` in staging/dev if you want to explicitly test native OS keyring integration.

---

## 🔑 Zero-Configuration Emulator Onboarding

To run the application locally in the default Free/Local mode, you do not need to configure any keys or `local.properties` files. 

If you want to test the full subscription and premium summarizer flow using the local Firebase emulator suite:
1. In the companion premium module workspace, run `./gradlew generateDevKeys`. This dynamically generates a developer Ed25519 keypair and writes:
   * The private key (`codeoba.premium.private_key`) to its local properties file.
   * The public key (`codeoba.premium.public_key`) directly to `Codeoba/local.properties`.
2. Open `Codeoba/local.properties` and add:
   ```properties
   codeoba.enable_subscription=true
   ```
3. Run the client pointing to the emulator:
   ```bash
   ./gradlew :app-desktop:run -Dcodeoba.base_url=localhost:5000
   ```

---

## 💻 Common Terminal Commands

Always run these commands from the root directory of the repository:

| Command | Action |
|---|---|
| `./gradlew :app-desktop:compileKotlinDesktop` | Compiles the desktop application code. |
| `./gradlew :core:desktopTest` | Runs all unit and parser tests in the core library. |
| `./gradlew :app-desktop:run` | Launches the desktop client interface locally in **Free/Local Mode**. |
| `./gradlew :app-desktop:run -Dcodeoba.base_url=localhost:5000` | Launches the app pointing to the local emulator. |

---

## 🔌 Developing Log Source Adapters

If you need to add support for indexing logs from a new coding agent, follow [Adding a New Source Adapter Guide](file:///Users/pv/Dev/GitHub/LookAtWhatAiCanDo/Codeoba/docs/ADD_NEW_SOURCE.md):
1. **Implement Adapter**: Subclass `DesktopSourceAdapter` under `core/src/desktopMain/kotlin/com/whataicando/codeoba/core/source/`.
2. **Register Adapter**: Add your adapter instance inside `SourceRegistry` in `Main.kt`.
3. **Accent Colors & UI Formatting**: Add brand colors, formatted name mapping, and the developer/product URL in `FormatUtils.kt`.
4. **Test**: Write parser and watch-filtering assertions inside `SourceCapabilitiesTest.kt` and `SourceParsersTest.kt`.

---

## 🎨 UI & Theme Customization

The user interface resides entirely inside the `:app-desktop` module under `desktopMain`:
* **`Main.kt`**: Coordinates window state lifecycle, breadcrumbs, detail cards, and modal dialogs.
* **`Sidebar.kt`**: Handles search query inputs, relevance sorting, status/source filters, and context menus.
* **`DetailPane.kt`**: Renders message dialogue panels, collapsible tool blocks, and markdown text.
* **`Components.kt`**: Provides reusable composables such as drag-to-scroll lists and theme scrollbars.

### Theming System (`Theme.kt`)
The app UI loads colors dynamically based on the active theme selected in Settings (e.g. Obsidian, Nordic Frost, Dracula, Emerald Forest). Ensure all UI components reference the dynamic theme properties rather than static values:
* `ObsidianBg` (Background) | `SlateSurface` (Panels) | `CardSurface` (Cards)
* `BorderColor` (Dividers) | `AccentCyan` (Pins/Primary) | `AccentPurple` (Tags/Secondary)
* `TextPrimary` (Primary Labels) | `TextSecondary` (Details)

### UI Typography / Casing Constraints
* **Casing Constraint**: Never display capitalized actor labels like "USER" or "ASSISTANT". Always format them as **"User"** and **"Assistant"**.

---

## 🧪 Testing Guidelines

* **Platform Isolation**: Ensure unit tests run offline and do not rely on local developer files.
* **Temporary Storage**: Always allocate test storage dynamically using JUnit's temporary folder rules or standard Java temp file creation (`File.createTempFile` + `deleteOnExit()`).
* **Database Tests**: Use in-memory SQLite instances (`jdbc:sqlite::memory:`) or write to temporary databases to prevent side-effects.

