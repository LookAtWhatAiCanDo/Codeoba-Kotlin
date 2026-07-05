# Marketing Screenshot Generator

Codeoba contains a built-in marketing screenshot configuration tool **for debug builds only**. This tool automatically sizes the application window to store-compliant dimensions, centers the window, loads high-quality canned mock data, and bypasses local folder scanning.

---

## 🚀 Configuration Options

The screenshot generator uses a clean separation between **developer-only debug hooks** (configured via JVM System Properties) and **user-facing layout configurations** (configured via command-line arguments). They are ignored entirely in production/release builds:

### 1. Developer Debug configurations (JVM System Properties `-D`)
*   `-Dcodeoba.store=apple|microsoft`: Activates screenshot mock mode and loads the corresponding canned dataset (`store/canned_apple.json` or `store/canned_microsoft.json`).
*   `-Dcodeoba.canned_data=PATH`: Loads mock session data from a custom JSON file path instead of the store defaults.

### 2. User/layout configurations (Command-Line Arguments `--`)
*   `--size=WIDTHxHEIGHT`: Sets/overrides the startup window dimensions (e.g. `--size=2880x1800` or `--size=3840x2160`).
*   `-h`, `--help`: Prints all available command-line options and description details to the console standard output and exits immediately without launching the application GUI.

---

## 💻 Example Usage

To compile, load macOS mock data, and launch centered at `2880x1800` resolution:
```bash
./gradlew :app-desktop:run -Dcodeoba.store=apple --args="--size=2880x1800"
```

To load Windows mock data and launch centered at `3840x2160` (4k) resolution:
```bash
./gradlew :app-desktop:run -Dcodeoba.store=microsoft --args="--size=3840x2160"
```

---

## 🛠️ Run/Launch Configurations in Android Studio

To create a dedicated launch configuration in Android Studio for generating screenshots:

### Option A: Gradle Run Configuration
1.  Open **Edit Configurations...**
2.  Click **+** and select **Gradle**.
3.  Configure the settings:
    *   **Name**: `Run App (Store - Apple)`
    *   **Tasks and Arguments** (or **Command line**): `:app-desktop:run -Dcodeoba.store=apple --args="--size=2880x1800"`
4.  Click **Apply** and then **Run**!

### Option B: Native JVM Application Run Configuration (Bypasses Gradle)
1.  Open **Edit Configurations...**
2.  Click **+** and select **Application**.
3.  Configure the settings:
    *   **Name**: `Codeoba App (Store - Microsoft)`
    *   **Main Class**: `com.whataicando.codeoba.desktop.MainKt`
    *   **VM Options**: `-Dcodeoba.store=microsoft`
    *   **Program Arguments**: `--size=3840x2160`
    *   **Use classpath of module**: `Codeoba.app-desktop.desktopMain`
4.  Click **Apply** and then **Run**!

---

## 📂 Customizing Mock Data

Mock data is loaded from `store/canned_apple.json` and `store/canned_microsoft.json` relative to the root directory. You can edit these files directly to customize the lists, summaries, performance chart points, and conversation turns shown in the screenshot.
