# Walkthrough — Codeoba UI, Statistics, Navigation & Exit Refinements

Codeoba is now fully equipped with a modern, mixed-case, highly aesthetic UI, precise compute metrics, smart scroll controls, reliable window closing behavior, and a fully reactive database synchronization layer. Below is a detailed walkthrough of the changes implemented.

---

## 🎨 Visual Design & Casing Overhaul

To resolve the "yelling in all caps" issue and create a premium, IDE-like workspace feel, we standardized all labels, tags, headers, and UI messages:
1. **Turn Card Headers**:
   - `"USER"` changed to `"User"` (mixed case, HSL Cyan Accent).
   - `"ASSISTANT"` changed to `"Assistant"` (mixed case, HSL Purple Accent).
2. **Attribution Badge**: Shows the active AI model name and compute speed (e.g. `Assistant (Claude 3.5 Sonnet | 4s (24 t/s))`) directly in the message header.
3. **Dialogue Turn Timestamps**: Displays the high-precision timestamp (e.g., `10:45:26` if same-day, or `May 21, 10:45:26` if same-year, or full year date-time otherwise) next to the message sender's name (e.g., `User • 10:45:26` or `Assistant (Claude 3.5 Sonnet) • 10:45:26`), styled cleanly with a muted, subtle appearance.
4. **Workspace Stats Headers**: Changed from uppercase abbreviations to clean, readable titles (e.g., `Workspace Statistics`, `Model Performance & Usage Breakdown`).
5. **Sidebar Collapsed Indicator**: A subtle, elegant sidebar expander toggle replaces the previous arrow icons, matching modern editor designs.
6. **Granular Step-Level Timestamps**: Added detailed step-level timestamps next to every run command, view file, search web, system message, and error block header for Google Antigravity sessions, matching the turn-level timestamp format.

---

## 📊 Precise Compute Time & Model Performance Breakdown

Compute metrics are now computed based on actual agent work moments, preventing inaccurate time estimates (e.g., estimating 22 hours for a day-old idle session):
- **Turn-by-Turn Compute Time**: Sum of `assistantTimestamp - userTimestamp`. If not available, it defaults to a proportional estimate of `length / 120 * 1000ms`.
- **Session Duration**: The sum of each individual turn's actual compute time.
- **Token Speed (tokens/sec)**: Total number of tokens in the turn or session divided by the compute time.
- **Model Usage Breakdown (Per Workspace & Session)**:
  - If a session or workspace uses multiple models, Codeoba displays a detailed card breaking down turns, total tokens, active work duration, and percentage use time for each model (e.g., `gemini-2.5-pro`, `claude-3-5-sonnet`).
- **Interactive Sorting**:
  - The model usage list can be sorted dynamically by clicking chips for each dimension: Turns, Tokens, Speed, Duration, and Model Name.
  - The chips are arranged in a wrap-around `FlowRow` for responsive design.
  - Numeric columns (Turns, Tokens, Speed, Duration) default to descending order on first click, and Model Name defaults to ascending (A-Z) order. Subsequent clicks on the active sort toggle its direction, with an arrow icon indicating the state.

---

## 📜 Smart Scroll-Lock System & Reliable Tail Scrolling

To ensure smooth real-time reading during long-running tasks or log-tailing:
1. **Smart Bottom Lock**: The conversation detail panel automatically scrolls to the end when new turns are indexed.
2. **Scroll Lock Release**: If the user manually scrolls away from the bottom to inspect previous turns, the scroll-lock automatically releases, letting them read without being snapped back to the bottom.
3. **Bottom Anchor Button**: A centered, floating `"Scroll to End"` button appears above the bottom of the screen when scrolled away, letting the user snap back and re-engage scroll lock with a single click.
4. **Reliable 100% Scroll to End**:
   - Re-keyed the `lazyListState` on `session?.id` inside the `DetailPane` so that switching between sessions creates a fresh list state.
   - Initialized `LazyListState` to start layout at the bottom of the list using `firstVisibleItemIndex = session.turns.size + 1` (referencing the bottom spacer).
   - Appended a `bottom_spacer` item at the very bottom of the conversation's `LazyColumn` to represent the absolute end boundary, including the list's bottom content padding.
   - Updated scroll index targets to point to this bottom spacer (index `session.turns.size + 1`), ensuring that all scrolling actions (such as on load, on new turns, or clicking the "Scroll to End" button) scroll 100% past the top of the last message all the way to the end boundary.

---

## 🚪 Reliable Process Exit on Window Close & Menu Action

To ensure that closing the app window actually terminates the underlying JVM process immediately rather than leaving zombie background threads running:
1. **Window Close Request Handler**: Added `java.lang.System.exit(0)` after `exitApplication()` inside the window onCloseRequest block in `Main.kt`.
2. **Exit Menu Item Handler**: Added `java.lang.System.exit(0)` to the click handler of the File -> Exit menu item.
This terminates all non-daemon threads (such as AWT event loop and database/directory watcher connections) when closing the application.

---

## 🎨 Title Alignment & Titlebar Visibility

To solve the double-title overlapping and vertical text centering inside the floating pill toolbar:
1. **Hidden OS Title Text**: Added `putClientProperty("apple.awt.windowTitleVisible", false)` to the window rootPane `LaunchedEffect` in `Main.kt`. This completely hides the legacy OS window title text in the content area when `transparentTitleBar` is active, preventing it from rendering behind the custom pill toolbar.
2. **Vertical Text Centering**: Separated the static dashboard title (`Workspace Statistics & Insights` when `session == null`) so that it renders outside `AnimatedVisibility` as a direct child of the parent `Row`. For active session details (`session != null`), we removed `.fillMaxHeight()` from both `AnimatedVisibility` and the inner `Row`. Since standard Compose `Row` containers with `verticalAlignment = Alignment.CenterVertically` vertically center their children naturally, this allows the items to sit exactly in the vertical center of the `46.dp` high glassmorphic toolbar, resolving alignment anomalies.
3. **Locale Date & Time Formatting**: Configured the sidebar session items in the left list view to show system locale defined date **and time** using `java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)`. This formats the date-time correctly depending on user OS locale preferences (e.g. `5/21/26, 2:29 AM` instead of just a date).

---

## ⚡ Live SQLite Sync Layer Fix (Cursor)

Previously, updates to Cursor's database did not immediately propagate to Codeoba because the SQLite connection URL used `immutable=1`.
- **The Issue**: `immutable=1` tells SQLite that the database file will never change, disabling cache invalidations and file checking. Even if the file was modified on disk, SQLite served cached data.
- **The Fix**: Removed `immutable=1` and maintained read-only access with `mode=ro`. Now, when Cursor writes new sessions or deletes turns, Codeoba instantly invalidates its cache, reads the fresh data, and updates the search results and detail panes in real time.

---

## 🛠️ Collapsible Floating Toolbar & Home Button

To improve navigation UX and visual aesthetics, we implemented a collapsible, glassmorphic floating toolbar inside the detail pane, inspired by the Pieces app:
1. **Floating Pill Layout**: Styled as a floating card with `RoundedCornerShape(23.dp)`, a subtle border, and a translucent background (`SlateSurface` at 90% opacity).
2. **Dedicated Home Button**: Added a dedicated Home button (`Icons.Default.Home`) to reset the selected session and return to the statistics dashboard. We also removed the clickable go-to-home behavior on the workspace folder name in the breadcrumbs.
3. **Horizontal Scroll-Based Animations**:
   - Automatically collapses horizontally to the left when scrolling up in a conversation to maximize the readability space.
   - Automatically expands back when scrolling to the bottom of the conversation (reclaiming scroll lock).
4. **Manual Override Toggle**: Added a manual expand/collapse chevron button (`<` / `>`) on the right side of the pill that overrides the scroll-based auto-collapse.
5. **Title Alignment and Padding Refinements**: Corrected the vertical alignment of the static dashboard title (`Workspace Statistics & Insights`) and active session details inside the glassmorphic toolbar by making `AnimatedVisibility` and the inner `Row` occupy the full height (`fillMaxHeight()`) rather than relying on a nested `Box` layout. This places the text exactly at the widest horizontal part of the semi-circular end of the pill, solving both the off-center alignment and the cramped right margin.
6. **Optimized Vertical Content Spacing**: Standardized the top padding and spacers in both the dashboard and conversation views to a balanced `80.dp`. This leaves a clean `24.dp` gap between the bottom of the floating toolbar (at `56.dp`) and the first visible content, perfectly matching the horizontal page margins and inter-element spacing of the application.
7. **macOS Traffic Lights Clearance**: The toolbar dynamically shifts its start padding to `90.dp` from `16.dp` when the sidebar is collapsed, preventing any overlap with the macOS top-left window controls.
8. **Removed Duplicate Sidebar Controls**: Cleaned up duplicate layout collapse, back/forward navigation, and indexing refresh icon buttons from the sidebar header. Since all of these controls are centrally accessible in the floating Pill Toolbar, the sidebar header now functions as a clean placeholder clearing the top-left macOS window controls (traffic lights).

---

## ⚙️ Dynamic Source Filters & Settings Switches

To refine source management and ensure users are only presented with active, relevant source filters:
1. **Dynamic Filter List**: The sidebar's "Filter by Source" list displays all non-ignored sources (`ignoredSources`), serving as the single source of truth. All enabled/monitored source filter chips are fully interactive and selectable; filtering by a source with no indexed sessions simply yields zero results in the list view.
2. **Exhaustive Source List in Settings**: The Settings dialog displays an exhaustive list of all supported source adapters (unconditionally returning all adapters from `SourceRegistry.getAllAdapters()`), allowing users to easily discover and re-enable ignored/disabled adapters.
3. **Obsidian-themed Toggle Switches**: Replaced the "Ignore / Hide" segment item in settings with a brand-consistent, Obsidian-styled toggle `Switch`. When checked, the source is active (monitored/default). When unchecked, it sets the status to `Decision.IGNORE`, automatically hiding its configuration options and removing it from the sidebar filters list.
4. **Product Website Links**: Added a clean purple `"Visit Website"` link next to each source's name in settings (e.g. `https://code.claude.com`, `https://cursor.com`, `https://aider.chat`), which opens directly in the system's default browser.
5. **Soft-Disable for Undetected Sources**: Sources that are not installed/available default to OFF (soft-disabled) when their user decision is `UNDECIDED`. This avoids prompting the user or showing unnecessary filter buttons in the sidebar for tools they do not use.
6. **Background Auto-Detection & Auto-Enabling**: Added a background polling loop in `Main.kt` that checks every 5 seconds. If a soft-disabled source becomes active/available (e.g., when the tool is installed or starts saving history logs), the system automatically flips the switch to ON and triggers a full database re-scan.
7. **Relocated Delete Icon**: Placed the red delete (trash can) icon directly next to the `"Status: ... (Orphaned)"` text rather than the Switch. Added a tooltip that displays `"Clean up (delete) orphaned data"` on hover.

---

## 📁 Key File Changes

- **[SourceRegistry.kt](../core/src/commonMain/kotlin/llc/lookatwhataicando/codeoba/core/domain/source/SourceRegistry.kt)**: Changed `getAllAdapters()` to return all registered adapters unconditionally, keeping `getActiveAdapters()` filtered for background scanner synchronization.
- **[FormatUtils.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/FormatUtils.kt)**: Added `getProductUrl(sourceId: String)` mapping helper to retrieve official website links.
- **[Main.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Main.kt)**: 
  - Styled and built the floating glassmorphic `DetailPaneToolbar` pill card.
  - Lifted scroll states (`lazyListState`, `isScrollLocked`) in `DetailPane` to reactive level.
  - Synchronized `isHeaderExpanded` to `isScrollLocked` via `LaunchedEffect`.
  - Added top offsets and padded `LazyColumn` contents to flow cleanly under the toolbar.
  - Shifted search `FindBar` down to avoid overlapping the toolbar.
  - Added custom `openUrl` browser integration helper and `Help` menu to `MenuBar`.
  - Simplified sidebar source filters to use `ignoredSources` settings state as single source of truth.
- **[Sidebar.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Sidebar.kt)**: Filtered sidebar filter row using `ignoredSources`, adjusted SessionItem opacity/badge for archived state, added right-click context menu copying support, and implemented the bottom success toast overlay.
- **[SettingsDialog.kt](../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/SettingsDialog.kt)**: Integrated product links, custom brand-themed switches, and simplified segmented controls.
- **[DesktopCursorSource.kt](../core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/source/DesktopCursorSource.kt)**: Updated JDBC URL to remove `immutable=1` for live database refreshes.
- Icon Resources: Copied launch icon from CodexSearch:
  - [icon.png](../app-desktop/src/desktopMain/resources/icon.png)
  - [icon.icns](../app-desktop/src/desktopMain/resources/icon.icns)
  - [icon.ico](../app-desktop/src/desktopMain/resources/icon.ico)
  - [icon.svg](../app-desktop/src/desktopMain/resources/icon.svg)

---

## 🆘 Help Menu & Service Status Monitoring

To allow users to easily check the status of Google AI Studio, Anthropic, OpenAI, and Cursor APIs:
1. **Added Help Menu**: Added a new `"Help"` menu in the macOS / Windows application `MenuBar`.
2. **Service Status Submenu**: Grouped the monitoring pages under a `"Service Status"` submenu header.
3. **Status Links**:
   - `"Google AI Studio Status"` pointing to `https://aistudio.google.com/status`
   - `"Anthropic Status"` pointing to `https://status.anthropic.com/`
   - `"OpenAI Status"` pointing to `https://status.openai.com/`
   - `"Cursor Status"` pointing to `https://status.cursor.com`
4. **Desktop URI Opener**: Implemented a private `openUrl(url: String)` helper function that checks if AWT `Desktop` action `BROWSE` is supported on the user's OS, opening the link reactively in their default system browser.

---

## ⚙️ Premium App Settings Dialog
We replaced the basic settings dropdown menu in the sidebar header with a premium, Obsidian-styled two-pane configuration dialog:
1. **Category Sidebar**: Allows toggling between a **General** settings pane (placeholder screen: *"General settings are coming soon."*) and a **Sources** settings pane.
2. **Sources Pane**: Lists all registered source adapters with dynamic status badges (e.g. *Active & Installed*, *Ignored (Installed)*, *Monitored (Orphaned)*, *Orphaned Data*) and a segmented control selection row to toggle user monitoring preferences (**Default**, **Monitor**, or **Ignore / Hide**).
3. **Double-Confirmation Deletion Overlay**: Uninstalled adapters with historical data display a Trash icon. Clicking this opens a nested, red-accented confirmation overlay inside the settings dialog to recursively delete data files from disk and update the index immediately.

---

## 🔍 Targeted Deletion Paths, Antigravity Implementations & Aider Availability Fixes

To prevent accidental deletions and resolve false-positive prompts:
1. **Precise Deletion Warning**: The `SourceAdapter` interface now supports a `getDataPathsToDelete(): List<String>` method. Both the Startup Warning Overlay and the inner Settings Deletion Confirmation Dialog render these precise file and folder paths in a clean, scrollable inner list panel styled under the dark theme.
2. **Aider False-Positive Correction**: The Aider integration previously reported itself as active if generic directories like `~/Dev` or `~/GitHub` existed. We refined the detection logic so Aider's availability is based strictly on whether the Aider executable is installed or if active `.aider.chat.history.md` files are actually scanned and found on the system.
3. **Google Antigravity Lifecycle Support**: Implemented a proper `isAppInstalled()` check for Google Antigravity (detecting `/Applications/Antigravity.app`, `/Applications/Gemini.app`, or `~/.gemini/antigravity` configurations) and enabled complete data folder cleanup support (`deleteDataPaths()`) to securely wipe historical brain logs if desired by the user.



---

## 🖱️ Context Menu Path Copying & Premium Toast Notifications

To improve user workflow and data accessibility, we added support for copying a conversation's source data file path directly from the list item:
1. **Right-Click Context Menu**: Right-clicking any conversation item in the sidebar list view (`../app-desktop/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/desktop/Sidebar.kt`) triggers a standard desktop context menu via `ContextMenuArea` with a `"Copy Source File Path"` action.
2. **Clipboard Integration**: Selecting the action retrieves `session.filePath` and copies it to the system clipboard using the platform-integrated clipboard utilities.
3. **Premium Floating Toast Overlay**: Displays a bottom-aligned, Obsidian-themed, floating success toast notification with standard premium accents (`AccentCyan`) and a checkmark icon to confirm the path was copied to the clipboard. The toast automatically slides/fades away after a 2-second delay.

---

## ↕️ Conversations Sidebar List Sorting

To easily navigate and locate conversations based on size, speed, and time, we added sorting capabilities directly in the left-side sidebar:
1. **Interactive Sorting Row**: Placed a horizontal, scrollable row of chips (Relevance, Updated, Tokens, Speed, Turns, Duration) right above the conversations list.
2. **Relevance Option**: Automatically visible when a search query is typed, allowing sorting by score. If the query is empty, it hides and falls back to "Updated" sorting.
3. **Persisted Settings**: The sorting preferences are persisted via `SettingsManager` using Java Preferences API, so they remain persistent across application restarts.
4. **Intuitive Defaults**: On first click, sorting defaults to descending order for all numeric and chronological fields, and switches direction on subsequent clicks.

---

## 🧪 Verification Plan


1. **Verify Layout & Casing**:
   - Run `./gradlew :app-desktop:run`.
   - Inspect the detail view of a session. User messages are marked "User", assistant messages "Assistant".
2. **Verify Performance Dashboard**:
   - Look at the home workspace screen. The `Model Performance & Usage Breakdown` card shows the list of models, their turns, total tokens, speed, and time ratios.
3. **Verify Floating Toolbar & Navigation**:
   - Select a session, verify the floating toolbar details.
   - Click the Home icon to return to the statistics dashboard. Verify the folder name in breadcrumbs is static.
   - Scroll up to check horizontal auto-collapse. Scroll to bottom to check auto-expansion.
   - Click the manual chevron toggle to override.
   - Collapse the sidebar and verify the toolbar clears macOS traffic lights.
4. **Verify Database Sync**:
   - Open a Cursor Composer session, run a command, and then delete a turn.
   - Click the "Refresh Index" button or wait for background watcher.
   - Verify that the deleted turn is removed from Codeoba's session view.
5. **Verify App Settings Dialog & Website Links**:
   - Click the Settings Gear Cog icon in the Sidebar Header to open the multi-category Settings Overlay.
   - Switch to the "Sources" tab. Verify that all 5 supported source adapters (Claude, Cursor, Aider, Codex, Antigravity) are listed.
   - Verify that clicking "Visit Website" next to any source opens the product page in the default system browser.
   - Toggle the switch of a source to disabled/off. Verify:
     - The segmented selector ("Default" / "Force Monitor") disappears.
     - The source is removed from the sidebar's "Filter by Source" row.
   - Toggle the switch back to enabled/on. Verify:
     - The segmented selector reappears with "Default" and "Force Monitor".
     - The source filters show up again in the sidebar if the source has active indexed sessions.
6. **Verify Dynamic Sidebar Filtering**:
   - Verify that if a source has no indexed sessions (e.g. Aider is installed but has no active chat log sessions), it does not show up as a badge filter option in the sidebar, even if its setting switch is checked.
7. **Verify Help Menu & Status Monitoring**:
   - Run `./gradlew :app-desktop:run`.
   - Click the "Help" menu in the desktop MenuBar.
   - Click "Google AI Studio Status", "Anthropic Status", "OpenAI Status", or "Cursor Status".
   - Verify that the link opens correctly in your default web browser.
8. **Verify Context Menu Path Copying**:
   - Right-click on any conversation list item in the sidebar.
   - Verify that a context menu with "Copy Source File Path" appears.
   - Click the menu item and verify that a premium checkmark toast ("Source file path copied to clipboard") appears at the bottom center of the sidebar and fades away after 2 seconds.
   - Paste the clipboard contents to confirm the absolute path of the session file was copied.
9. **Verify Sidebar List Sorting**:
   - Run the application. Notice the "Sort by" chips row above the conversation list.
   - Click "Tokens" and verify the list is sorted by total estimated tokens descending.
   - Click "Turns" and verify it's sorted by turns descending.
   - Click "Duration" and verify it's sorted by compute time.
   - Click "Speed" and verify it's sorted by token speed.
   - Type a query, click "Relevance" to sort by search match score. Clear the search and verify it falls back to "Updated".

