# Codeoba Source Extensions Architecture Proposal

This document outlines the design, runtime environment, security sandboxing, and distribution strategies for migrating Codeoba's chat log adapters from hard-coded Kotlin sources to independent, first-class **Extensions**.

---

## 🧭 Architectural Goals

1. **Decoupled Development**: Allow official sources (Claude, Cursor, Codex, etc.) to be developed in their own repositories, release cycles, and versioning.
2. **Third-Party Extensibility**: Enable advanced users to write and load their own custom log parsers (e.g., for local LLM UIs, custom scripts, or proprietary tools).
3. **Robust Security & Trust**: Prevent untrusted extensions from accessing sensitive user data (e.g., SSH keys, browser cookies), running arbitrary binaries, or exfiltrating transcripts to the internet.
4. **Performance Parity**: Ensure that parsing overhead remains low and does not regress Codeoba's fast startup times (which cache parsing results).

---

## 🏗️ Extensibility Options Analysis

We analyze two primary methods for executing external extension code within Codeoba's JVM environment.

### Option A: Subprocess JSON IPC (Any Language)
In this model, an extension is a standalone executable (compiled binary, Python script, or Node.js tool). Codeoba communicates with the extension via standard input/output (stdin/stdout) using a structured CLI command set.

*   **Workflow**:
    *   `extension info` $\rightarrow$ returns JSON metadata (display name, id, watch paths).
    *   `extension parse --file <path>` $\rightarrow$ returns JSON representation of the `Session` entity.
    *   `extension cleanup` $\rightarrow$ executes data path deletion.
*   **Pros**:
    *   Extension authors can write code in Go, Rust, Python, Node, or Bash.
    *   No complex language interpreters need to be embedded in the JVM.
*   **Cons & Security Risk**:
    *   **Extremely Dangerous**: A native binary has full access to the host OS. A malicious extension could run `curl` to exfiltrate SSH keys or infect the user's machine.
    *   **Sandboxing Complexity**: Requires wrapping the subprocess call in OS-specific sandboxes (e.g., `sandbox-exec` on macOS, `bubblewrap` on Linux, `AppContainer` on Windows). Managing these sandboxes across platforms is fragile and complex.

---

### Option B: Sandboxed JavaScript/TypeScript Scriptlets (Recommended)
Extensions are written as lightweight JavaScript files. Codeoba embeds a tiny, highly restricted JavaScript runtime (such as **QuickJS** via JNI, or **GraalJS** in sandboxed mode) inside the JVM.

```
┌────────────────────────────────────────────────────────┐
│                      Codeoba App                       │
│                                                        │
│   ┌──────────────────┐          ┌──────────────────┐   │
│   │  Core Search/UI  │          │ Permission Gate  │   │
│   └────────┬─────────┘          └────────▲─────────┘   │
│            │                             │             │
│            ▼ (Read/Parse Requests)       │ (File APIs) │
│   ┌──────────────────────────────────────┴─────────┐   │
│   │             JS Sandboxed Runtime               │   │
│   │                                                │   │
│   │  - No Network APIs (fetch/xhr/websocket)       │   │
│   │  - No Process APIs (child_process/exec)       │   │
│   │  - Restrictive FS APIs (gated by Codeoba)      │   │
│   │                                                │   │
│   │   ┌───────────────┐        ┌───────────────┐   │   │
│   │   │  Claude JS    │        │ Custom UI JS  │   │   │
│   │   └───────────────┘        └───────────────┘   │   │
│   └────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

*   **Pros**:
    *   **Secure by Default**: The JS context has *no* access to standard Node.js or browser APIs (no `fetch`, no `child_process`, no raw Java/JVM bindings).
    *   **Granular File Control**: The JS filesystem helper (`fs.readFile`) is a bridge back to Kotlin. Codeoba can intercept and reject any file read attempt outside the extension's declared/approved log directories.
    *   **Cross-platform Consistency**: Runs identically on macOS, Windows, and Linux without OS-level sandboxing tools.
    *   **Ease of Writing**: Writing simple string parsers in JS is highly accessible and fast.
*   **Cons**:
    *   Requires packaging a JS engine library inside the desktop app bundle (e.g. QuickJS JNI adds ~1-2 MB).

---

## 🔒 Security & Sandboxing Framework

To ensure user trust, Codeoba will implement a strict **three-tier security model** for extensions:

### 1. The Permission Gate (Filesystem Scoping)
Every extension must declare the log paths it needs to monitor in its `manifest.json`.
*   When the extension calls `fs.readFile("/path/to/log")`, the JVM sandbox intercepts the request.
*   If the path is within the declared log directory (e.g., `~/.cursor`), Codeoba allows it.
*   If the path is outside the allowed scope (e.g., `/etc/passwd` or `~/.ssh`), the request is blocked and logged.
*   **Network & Command Execution**: Explicitly blocked at the compiler/runtime configuration level. The JS engine exposes *no* network sockets or subprocess APIs.

### 2. Cryptographic Vetting & Signatures
To separate trusted redistributions from local scriptlets:
*   **Official Extensions**: Cryptographically signed by the Codeoba Team. The app ships with the public key. Signed extensions are trusted automatically.
*   **User/Community Extensions**: Unsigned. When loaded, Codeoba prompts the user with an explicit warning showing:
    *   The extension name and author.
    *   The exact local directories the extension is requesting to read.
    *   A warning that this code runs locally (though sandboxed).

### 3. Static Code Analysis (Vetting Tool)
A helper CLI tool will be provided for vetting community extensions:
*   Scans the JS code for dynamic execution (`eval()`, `Function()`).
*   Verifies that the extension matches the JSON schemas.

---

## 📦 Distribution & Repository Layout

To support both independent development and official packaging, we propose a hybrid repository approach:

### 1. Developer Setup (Git Submodules)
Each official extension resides in its own repository (e.g., `github.com/lookatwhataicando/codeoba-extension-claude`).
During build/distribution, these are pulled into the main Codeoba repository via **Git Submodules** under the `extensions/` directory:

```
Codeoba/
├── app-desktop/
├── core/
└── extensions/                 <-- Git Submodules
    ├── claude/
    │   ├── manifest.json
    │   └── parser.js
    └── cursor/
        ├── manifest.json
        └── parser.js
```

### 2. Gradle Integration
A Gradle build task compiles/packages these official extensions into the application resources (`src/commonMain/resources/extensions/`). At runtime, Codeoba reads them from the classpath.

### 3. User Extensions Loading
Users can load local extensions by creating a folder: `~/.codeoba/extensions/<extension_name>/`.
Codeoba scans this directory on startup, parses the `manifest.json`, and registers the parser after displaying a permission prompt to the user.

---

## 📝 Extension Interface & Manifest Specification

An extension consists of at least two files: `manifest.json` and a JavaScript script (e.g., `index.js`).

### 1. `manifest.json`
```json
{
  "id": "claude-extension",
  "name": "Claude Code",
  "version": "1.0.0",
  "author": "Look At What AI Can Do",
  "description": "Parses Claude Code conversation logs.",
  "productUrl": "https://claude.ai",
  "brandColors": {
    "accent": "#CC855C",
    "backgroundAlpha": "#1FCC855C"
  },
  "logConfig": {
    "defaultPaths": {
      "macos": "~/Library/Application Support/claude/logs",
      "linux": "~/.config/claude/logs",
      "windows": "%APPDATA%\\claude\\logs"
    },
    "watchFilter": "\\.jsonl$"
  },
  "permissions": [
    "fs:read:defaultPaths"
  ],
  "entry": "index.js"
}
```

### 2. `index.js` (The parser)
The script exposes a standard set of callbacks to the sandbox:

```javascript
/**
 * Detects if the source application is installed.
 * Exposes a helper `env.isExecutableOnPath(name)`.
 */
function isAppInstalled() {
  return env.isExecutableOnPath("claude") || fs.exists(env.getDefaultPath());
}

/**
 * Parses a single log file into the unified Codeoba Session format.
 */
function parseSession(filePath, fileContent) {
  // Parsing logic using JS built-ins (JSON.parse, regex, etc.)
  const lines = fileContent.split('\n');
  const turns = [];
  
  for (const line of lines) {
    if (!line.trim()) continue;
    const log = JSON.parse(line);
    
    // map to Codeoba's Turn structure
    turns.push({
      turnId: log.id + "_" + turns.length,
      userMessage: log.prompt,
      assistantMessage: log.response,
      timestamp: new Date(log.timestamp).getTime(),
      extraData: {
        model: log.model || "Unknown"
      }
    });
  }
  
  return {
    id: filePath.split('/').pop().replace('.jsonl', ''),
    sourceId: "claude",
    filePath: filePath,
    timestamp: turns[0] ? turns[0].timestamp : Date.now(),
    updatedAt: turns[turns.length - 1] ? turns[turns.length - 1].timestamp : Date.now(),
    cwd: "/project/path", // extracted from log if available
    threadName: "Claude Session",
    turns: turns,
    isArchived: false
  };
}
```

---

## 🚀 Step-by-Step Implementation Roadmap

```mermaid
timeline
    title Codeoba Extension System Roadmap
    section Phase 1: Core Sandboxing
        Embed Sandboxed JS Engine (GraalJS/QuickJS) : Done in dev
        Implement Kotlin-JS Bridge (fs, env wrappers) : Done in dev
        Apply strict filesystem restrictions & disable network : Done in dev
    section Phase 2: Schema & Adapter
        Define Session/Turn JSON Schemas : Planning
        Write DynamicSourceAdapter wrapping JS engine : Planning
        Port one source (e.g. Codex) to JS Extension as a benchmark : Planning
    section Phase 3: Manifest & Loading
        Implement manifest.json parser : Planning
        Build user-load UI directory watcher : Planning
        Add user permission prompt on loading unsigned script : Planning
    section Phase 4: Migration
        Migrate remaining 5 sources to submodules : Planning
        Introduce code signing for official extensions : Planning
```

---

## 💬 Summary of Recommendations

1. **Adopt Option B (Sandboxed JS)**: It is the only way to allow user-loaded extensions without exposing users to severe security vulnerabilities (like token stealing or malware execution).
2. **Submodules for Official Sources**: Keeps sources modular and separately maintainable, while keeping the final release bundle single-binary and zero-install.
3. **Refactor UI Theme Registration**: Move color formatting out of hardcoded `FormatUtils.kt` to read values directly from the extension's `manifest.json`.
