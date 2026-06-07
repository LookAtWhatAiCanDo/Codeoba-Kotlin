# Plan: Firebase-Proxied Auto-Updates with Client Telemetry

This document details the architecture and implementation of the auto-update subsystem for Codeoba. The system uses a server-proxied checking model where the desktop client queries a Firebase Cloud Function endpoint on `Codeoba-Backend`, which caches GitHub releases and logs client telemetry.

## Architecture & Benefits

The proxied update architecture provides the following:
1. **GitHub API Protection:** GitHub limits unauthenticated requests to 60/hour. Our Firebase function caches the GitHub API response for 15 minutes, allowing unlimited clients to check for updates without getting rate-limited.
2. **Hybrid Telemetry (Profile + 90-day TTL History):**
   - **Client Profile:** Maintains exactly one permanent document per installation GUID (`/updates/{guid}`) containing the latest `lastSeen` timestamp, application version, OS name, and CPU architecture. This allows fast, low-cost active user and environment queries.
   - **Request History:** Appends every check event to `/updates/{guid}/requests/{requestTime}` with a `ttl` field set to 90 days in the future. This provides lossless, short-term history for tracking upgrade paths and debugging, while Firestore automatically deletes expired logs in the background for free.
3. **Dynamic Controls:** The server can return custom variables (e.g. `uiDelayMillis` or custom checking intervals) without requiring a new client build.
4. **Resilience & Security:** Enforces client-side sanitization of changelog markdown to prevent script injection and URL protocol abuse.

---

## Proposed Changes

### 1. Codeoba Backend (`Codeoba-Backend`)

#### [MODIFY] [firebase.json](../Codeoba-Backend/firebase.json)
- Add a URL rewrite under `hosting`:
  - Map `/api/update` to the `checkLatestRelease` Cloud Function to provide a clean endpoint.

#### [MODIFY] [index.ts](../Codeoba-Backend/functions/src/index.ts)
- Implement a Gen 2 HTTPS onRequest function `checkLatestRelease`:
  - **In-Memory Cache:** Cache the GitHub Releases API response (`https://api.github.com/repos/LookAtWhatAiCanDo/Codeoba/releases/latest`) for 15 minutes.
  - **User-Agent Parsing:** Parse the client's custom User-Agent to extract:
    - Application Version
    - OS name/version
    - CPU architecture
    - Client Installation GUID
  - **Telemetry Logging:** Perform a batch write to:
    1. **Client Profile:** `/updates/{guid}` (or `/updates-dev/{guid}`)
       - Fields: `lastSeen` (server timestamp), `appVersion`, `osName`, `cpuArch`.
    2. **Request History Log:** `/updates/{guid}/requests/{requestTime}` (or `/updates-dev/{guid}/requests/{requestTime}`)
       - Fields: `timestamp` (server timestamp), `appVersion`, `osName`, `cpuArch`, `ttl` (current time + 90 days).
  - **Response Payload:** Return a structured JSON response:
    - `releaseTag`: latest release tag from GitHub
    - `releaseUrl`: latest release download/details page URL
    - `releaseNotes`: Markdown changelog body
    - `uiDelayMillis`: delay before showing the dialog (default: `1000`)
    - `minAutoUpdateCheckIntervalSeconds`: checking interval (default: `86400`)

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
  - Otherwise, send a GET request to the Firebase proxy endpoint (`http://localhost:5000/api/update` in dev/emulator mode, or production Cloud Function URL).
  - Include the custom formatted User-Agent:
    `Codeoba/{version} ({OS}; {arch}; GUID-{guid})`
  - Parse the structured JSON response into a `GitHubRelease` model.
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
  - Check the 24-hour interval (`minAutoUpdateCheckIntervalSeconds` returned by the server or a fallback 24 hours).
  - Bypass throttling if `ignoreUpdateThrottling` is active.
  - Record the timestamp of successful update checks to `SettingsManager`.

#### [MODIFY] [SettingsDialog.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/SettingsDialog.kt)
- Ensure the manual check button bypasses the 24-hour rate limit and updates the last successful check timestamp in `SettingsManager`.

---

## Verification Plan

### Automated Tests
- Run `cd functions && npm run test` to verify Cloud Functions compilation and logic (mock test cases).
- Run `./gradlew :core:desktopTest` to verify version parsing and comparison.

### Manual Verification
1. **Telemetry Logging & Hybrid Storage Test:**
   - Run the backend emulator using `firebase emulators:start`.
   - Run the desktop application in dev mode.
   - Inspect the local Firestore Emulator UI to confirm:
     - A profile document is created/updated at `/updates-dev/{guid}`.
     - A log document is created at `/updates-dev/{guid}/requests/{timestamp}` containing a `ttl` timestamp set to 90 days in the future.
2. **API Caching Test:**
   - Verify that subsequent update requests within 15 minutes reuse the cached GitHub response and do not trigger new external network requests to GitHub.
3. **URI Sanitization & Diagnostic Overrides:**
   - Run the client with `--update-mock-notes --update-force`.
   - Confirm the update dialog is shown on launch.
   - Click malicious link protocols (`javascript:`, `data:`) in the release notes and verify they are blocked and logged.
   - Verify `<script>` and `<img>` tags are printed as plain text.
