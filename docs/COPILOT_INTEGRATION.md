# GitHub Copilot Desktop Integration Guide

This document explains the session state structure of the GitHub Copilot desktop application and how it was integrated into Codeoba as a supported source.

---

## 🏗️ GitHub Copilot Session State Structure

GitHub Copilot stores its local application database and logs under `~/.copilot`. The chat conversation sessions are organized as subdirectories inside:
```
~/.copilot/session-state/
```
Each subdirectory is named with a unique session UUID (e.g. `59f75dfb-f8ad-46cc-a546-bbc1988993d0`) and contains the following core files:

1. **`workspace.yaml`**: A flat metadata file describing the workspace and repository binding.
2. **`events.jsonl`**: A JSON Lines log recording the chronological trajectory of conversational events, tool requests, and assistant reasoning.

---

## 📄 File Schema Details

### 1. `workspace.yaml`

This file uses a flat YAML structure containing session properties:
```yaml
id: 59f75dfb-f8ad-46cc-a546-bbc1988993d0
cwd: /Users/pv/.copilot/repos/copilot-worktrees/Codeoba/paulpv-fantastic-spoon
git_root: /Users/pv/.copilot/repos/copilot-worktrees/Codeoba/paulpv-fantastic-spoon
repository: LookAtWhatAiCanDo/Codeoba
host_type: github
branch: paulpv/fantastic-spoon
name: Code review audit
created_at: 2026-06-10T21:10:14.691Z
updated_at: 2026-06-10T21:10:21.486Z
...
```

*   **`id`**: Unique session UUID.
*   **`name`**: The user-facing title of the thread.
*   **`cwd`**: The temporary workspace directory path used by the Copilot app (typically inside `~/.copilot/repos/copilot-worktrees/...`).
*   **`repository`**: The canonical name of the git repository.
*   **`branch`**: The active git branch.
*   **`created_at` / `updated_at`**: ISO-8601 creation and last-modified timestamps.

### 2. `events.jsonl`

This file logs events line-by-line in JSON. The main event types are:

*   **`user.message`**: Indicates a query submitted by the user.
    ```json
    {"type":"user.message","timestamp":"...","data":{"content":"user query here..."}}
    ```
*   **`assistant.message`**: Indicates a response message from the assistant. May contain text content, reasoning details, and targeted model details:
    ```json
    {"type":"assistant.message","timestamp":"...","data":{"content":"...","reasoningText":"assistant's thought process...","model":"claude-sonnet-4.6"}}
    ```
*   **`tool.execution_start`**: Indicates that the assistant has requested the execution of a local tool (e.g. bash command, file edit).
    ```json
    {"type":"tool.execution_start","timestamp":"...","data":{"toolCallId":"call_abc","toolName":"run_command","arguments":{"CommandLine":"ls -la"}}}
    ```
*   **`tool.execution_complete`**: Logs the success status and results returned by a completed tool.
    ```json
    {"type":"tool.execution_complete","timestamp":"...","data":{"toolCallId":"call_abc","success":true,"result":{"content":"Intent logged","detailedContent":"Reviewing codebase"}}}
    ```

---

## ⚙️ Codeoba Parser Implementation (`DesktopCopilotSource`)

Codeoba parses and indexes Copilot sessions in [DesktopCopilotSource.kt](../core/src/desktopMain/kotlin/llc/lookatwhataicando/codeoba/core/source/DesktopCopilotSource.kt) as follows:

1.  **Metadata Extraction**: Matches directories in `~/.copilot/session-state/` and parses `workspace.yaml` line-by-line to retrieve the session ID, thread title, git branch, repository binding, and path boundaries (`cwd`).
2.  **Event Processing**: Parses `events.jsonl` line-by-line.
    *   Tracks active tools in a map by `toolCallId`.
    *   Formats completed tools into Codeoba's native collapsible markdown blocks:
        ```
        [[[TOOL:toolCategory|header|startTime]]]
        toolOutputContent
        [[[/TOOL]]]
        ```
    *   Injects the assistant's internal `reasoningText` at the beginning of the respective assistant chat bubble styled as a premium Github-style info alert:
        ```markdown
        > [!NOTE]
        > **Reasoning:**
        > Assistant's thought process here...
        ```
3.  **Chronological Sorting**: Sorts all user queries, text outputs, and completed tool blocks by their occurrence timestamps to ensure correct sequence.
4.  **Turn Grouping**: Collects the events list into standard `Turn` objects, grouping all assistant texts and tool blocks following a user message into a single conversational bubble.
5.  **Compute Time Calculation**: Computes active elapsed compute times by measuring gaps between consecutive events in a turn, capping gaps at 2 minutes to exclude idle waiting times.
