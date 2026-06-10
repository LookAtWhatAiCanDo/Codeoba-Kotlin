# Plan: Firebase-Proxied Auto-Updates with Client Telemetry

This document details the architecture and implementation of the auto-update subsystem for Codeoba. The system uses a server-proxied checking model where the desktop client queries a Firebase Cloud Function endpoint on `Codeoba-Backend`, which caches GitHub releases and logs client telemetry.

## Architecture & Benefits

The proxied update architecture provides the following:
1. **GitHub API Protection:** GitHub limits unauthenticated requests to 60/hour. Our Firebase function caches the GitHub API response for 15 minutes, allowing unlimited clients to check for updates without getting rate-limited.
2. **Cloud Logging Telemetry:**
   - **Console Ingestion:** Client check information (GUID, app version, OS, architecture, and IP) is printed directly to standard output as a telemetry log line (`console.info`).
   - **No Database Writes:** GCP Cloud Logging automatically ingests and indexes these logs. This avoids all Firestore database writes/deletes, eliminates database storage growth and write billing costs, and remains fully secure and public.
3. **Dynamic Controls:** The server can return custom variables (e.g. `uiDelayMillis` or custom checking intervals) without requiring a new client build.
4. **Resilience & Security:** Enforces client-side sanitization of changelog markdown to prevent script injection and URL protocol abuse.

---

## Proposed Changes

### 1. Codeoba Backend (`Codeoba-Backend`)

#### [MODIFY] [firebase.json](https://github.com/LookAtWhatAiCanDo/Codeoba-Backend/blob/main/firebase.json)
- Add a URL rewrite under `hosting`:
  - Map `/api/update` to the `checkLatestRelease` Cloud Function to provide a clean endpoint.

#### [MODIFY] [index.ts](https://github.com/LookAtWhatAiCanDo/Codeoba-Backend/blob/main/functions/src/index.ts)
- Implement a Gen 2 HTTPS onRequest function `checkLatestRelease`:
  - **In-Memory Cache:** Cache the GitHub Releases API response (`https://api.github.com/repos/LookAtWhatAiCanDo/Codeoba/releases/latest`) for 15 minutes.
  - **User-Agent Parsing:** Parse the client's custom User-Agent to extract:
    - Application Version
    - OS name/version
    - CPU architecture
    - Client Installation GUID
  - **Telemetry Logging:** Output log lines via `console.info` prefixed with `[TELEMETRY]` containing the client's sanitized and truncated GUID (up to 128 characters), IP, version, OS, and CPU architecture.
  - **No Database writes:** Bypasses Firestore database writes entirely to prevent resource exhaustion abuse.
  - **Response Payload:** Return a structured JSON response (matching GitHub's release shape plus custom throttle/UI fields):
    - `tag_name`: latest release tag from GitHub (e.g. `"v1.10.0"`)
    - `html_url`: latest release page URL
    - `body`: Markdown changelog/release notes
    - `assets`: array of release asset objects:
      - `name`: name of the asset (e.g., `"codeoba-1.10.0.pkg"`)
      - `browser_download_url`: direct download URL for the asset
    - `uiDelayMillis`: delay before showing the dialog in milliseconds (default: `1000`)
    - `minAutoUpdateCheckIntervalSeconds`: checking interval (default: `97200`)

---

### 2. Codeoba Client (`Codeoba`)

#### [MODIFY] [SettingsManager.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/SettingsManager.kt)
- Add properties for updating rate-limiting and identity:
  - `getLastUpdateCheck(): Long` and `setLastUpdateCheck(Long)`
  - `getInstallGuid(): String`: retrieves a cached UUID. If it does not exist, generates a new random UUID, persists it, and returns it.

#### [MODIFY] [UpdateManager.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/UpdateManager.kt)
- Declare three developer overrides:
  - `@Volatile var ignoreUpdateThrottling = false`
  - `@Volatile var forceUpdateAvailable = false`
  - `@Volatile var mockUpdateNotes = false`
- Update `checkLatestRelease()`:
  - If `mockUpdateNotes` is active, immediately return a synthetic `GitHubRelease` object containing hostile XSS injection payloads (to verify client sanitization).
  - Otherwise, send a POST request to the Firebase proxy endpoint (`http://localhost:5000/api/update` in dev/emulator mode, or production Cloud Function URL).
  - Include the custom formatted User-Agent:
    `Codeoba/{version} ({OS}; {arch}; GUID-{guid})`
  - Decode the JSON response into a `GitHubRelease` model.
- Update `isUpdateAvailable()`:
  - Return `true` if `forceUpdateAvailable` is active. Otherwise, parse and compare versions using `SemVer`.

#### [MODIFY] [Main.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Main.kt)
- Parse command-line developer flags inside `main(args)` and set the corresponding properties on `UpdateManager`.
- Implement a helper `isSafeLocalFileLink(url: String): Boolean` to strictly validate local link protocols (allow `file://`, relative paths, and Windows drives; reject remote schemes like `javascript:` or `data:`).
- Update `openUrl(url: String)`:
  - Enforce that the URL strictly starts with `http://` or `https://` (case-insensitive). Block and log all other schemes.
- Update `onUrlClick` blocks (in main view and file preview):
  - Delegate safe web links to `openUrl` and validated local references to `FileViewerDialog`. Block all other remote protocols.
- Update startup `LaunchedEffect(Unit)`:
  - Check the 27-hour interval (`minAutoUpdateCheckIntervalSeconds` returned by the server or a fallback 27 hours).
  - Bypass throttling if `ignoreUpdateThrottling` is active.
  - Record the timestamp of successful update checks to `SettingsManager`.

#### [MODIFY] [SettingsDialog.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/SettingsDialog.kt)
- Ensure the manual check button bypasses the 27-hour rate limit and updates the last successful check timestamp in `SettingsManager`.

---

## Verification Plan

### Automated Tests
- Run `cd functions && npm run test` to verify Cloud Functions compilation and logic (mock test cases).
- Run `./gradlew :core:desktopTest` to verify version parsing and comparison.

### Manual Verification
1. **Telemetry Logging Test:**
   - Run the backend emulator using `firebase emulators:start`.
   - Run the desktop application in dev mode.
   - Inspect the terminal / console output of the local Firebase emulator to confirm that a telemetry log line with prefix `[TELEMETRY]` is printed containing the client's sanitized GUID, version, OS, architecture, and IP, and that no Firestore writes are executed.
2. **API Caching Test:**
   - Verify that subsequent update requests within 15 minutes reuse the cached GitHub response and do not trigger new external network requests to GitHub.
3. **URI Sanitization & Diagnostic Overrides:**
   - Run the client with `--update-mock-notes --update-force`.
   - Confirm the update dialog is shown on launch.
   - Click malicious link protocols (`javascript:`, `data:`) in the release notes and verify they are blocked and logged.
   - Verify `<script>` and `<img>` tags are printed as plain text.
