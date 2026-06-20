# Codeoba Microsoft Store Listing Assets (Grounded Ground Truth)

This document contains updated, verified marketing copy, product features, and release notes based strictly on features currently implemented and active in the Codeoba desktop application.

---

## 📝 1. Default Description (Main Listing Copy)
* **Character Limit:** Up to 10,000 characters.
* **Formatting Note:** Emojis are supported and help break up text. Clean bullet points (`•`) and caps headers are used since the Store does not render Markdown.

***

**Codeoba — Cross-Tool AI Chat Search & Indexer**

Stop wasting time digging through hidden directories, SQLite database files, and markdown logs to find that one brilliant code block or design discussion. Codeoba is a platform-agnostic, local-first search application that aggregates and indexes conversation transcripts from all major AI coding assistants into a single unified desktop dashboard.

Whether you are using Claude Code, Google Antigravity, Cursor, OpenAI Codex, Aider, or GitHub Copilot, Codeoba watches your log directories in real-time, indexing your programming history so you can find exactly what you need in milliseconds.

---

### 🔍 DUAL SEARCH ENGINES: FIND EXACTLY WHAT YOU NEED

Codeoba provides two distinct search modes designed for developers:

• LEXICAL SEARCH (Keyword Match): Locate exact character sequences, specific variables, functions, TODO comments, or regex patterns (e.g., "onCloseRequest", "TODO:", "git merge").
• SEMANTIC SEARCH (Conceptual Match): Find ideas using natural language queries (e.g., "how did I handle database transaction locks last week?"). Codeoba automatically downloads a lightweight, quantized "all-MiniLM-L6-v2" transformer model (~23 MB) to run local vector embeddings completely offline on your own machine.

---

### ⚡ HIGH-PERFORMANCE DESKTOP FEATURES

• Live Incremental Watchers: Background directory watchers instantly update your search index as you chat with your AI.
• Startup Caching & Profiling: A thread-safe file caching system minimizes startup overhead, reducing log scanning times from seconds to milliseconds. It includes an integrated execution time profiler to monitor indexing speeds.
• In-App Markdown & Code Viewer: Inspect your conversation turns and preview files safely using a secure, memory-bounded (max 5MB), symlink-aware reader that respects your workspace boundaries and prompts for authorization.
• 8 Dark Themes: Personalize your command center with dynamic, professionally designed dark modes including Obsidian, Nordic Frost, Emerald Forest, Sunset Copper, Royal Amethyst, Dracula, Cyberpunk Neon, and Monochrome Slate.
• Session Pinning & Tagging: Pin critical conversations to the top of your workspace and organize sessions with custom tags.
• Model Performance Auditing: Sort and analyze AI agent usage by turns, tokens, compute duration, and speed.

---

### 💎 PREMIUM LOCAL SUMMARIES

Unlock the Premium tier ($5/month) to run advanced local AI diagnostics over your index:

• Local Summaries: Automatically generate structured reports detailing key activities, used models, exception/error diagnostics, and search performance charts.
• Context Compaction Tracking: Monitor and estimate the duration of Google Antigravity context compaction events to keep your token usage and execution times optimized.

---

### 🔒 PRIVACY AND SECURITY BY DESIGN

Your code is your intellectual property. Codeoba protects it with strict privacy controls:

• 100% Local-First: All parser steps, database operations, and semantic embeddings are stored locally with zero remote tracking or developer telemetry.
• Safe Authentication: Secure Firebase-based browser authentication integration to verify your Polar licensing state.
• Secure Key Storage: User credentials and licensing validation parameters are stored securely in your system's native OS credential store (Windows CNG, macOS Keychain).

Bring order to your AI development workflow. Try Codeoba today!

***

## 📋 2. Product Features
* **Character Limit:** Max 120 characters per feature.
* **Usage:** Enter these one-by-one by clicking "Add more" under "Product features" in Partner Center.

1. **Multi-Agent Aggregation:** Merges chat logs from Claude Code, Google Antigravity, Cursor, Codex, Aider, and Copilot.
2. **Lexical Keyword Search:** Instantly locate exact code snippets, functions, variables, regex patterns, or TODO tags.
3. **Offline Semantic Search:** Search conceptually using natural language via a local, quantized machine learning model.
4. **Real-Time Directory Watchers:** Instantly updates your conversation index in the background as you write code and chat.
5. **AI-Powered Session Summaries:** Review concise summaries of what your AI agent accomplished, turn-by-turn.
6. **In-App Code & Markdown Viewer:** Safely preview workspace files using a secure, memory-bounded, symlink-aware reader.
7. **8 Desktop Dark Themes:** Customize your developer dashboard with Obsidian, Nordic Frost, Emerald Forest, or Dracula themes.
8. **Context Compaction Analytics:** Track Google Antigravity context compaction events and measure estimated time overhead.
9. **Startup Cache & Profiler:** Optimizes startup scanning down to milliseconds with an integrated performance profiler.
10. **Session Pinning:** Keep critical conversation threads floating at the top of your sidebar list regardless of sorting.
11. **Inline Session Tagging:** Organize indexed conversations into custom categories with a filterable checklist.
12. **Model Performance Audits:** Sort and analyze agent usage by turns, speed (tokens/sec), duration, and model names.
13. **Secure OS Keychains:** Stores credentials in secure hardware elements (Windows CNG/TPM, macOS Keychain).
14. **Local-First Privacy:** 100% offline indexing and search. No telemetry, tracker logs, or cloud tracking of your private code.
15. **Interactive Metrics Charts:** View custom-plotted charts showing query speeds and background indexing latency.
16. **Seamless Auto-Updates:** Instantly checks for signed releases, downloading and applying native MSI or PKG installers.

***

## 🆕 3. What's New in this Version?
* **Character Limit:** Up to 1,500 characters.

***

What's new in Codeoba v3.5:

• LOCAL ONNX SEMANTIC SEARCH: Replaces word hashing with a real local neural text embedding pipeline using the quantized all-MiniLM-L6-v2 transformer model.
• PERSISTENT STARTUP CACHE & PROFILER: Accelerates startup scanning time by ~90% (from ~2.5s down to 0.25s) using a new thread-safe file serialization system and prints detailed profiling reports.
• DYNAMIC ACCENT THEMES: Users can now switch between 8 custom-styled developer themes including Nordic Frost, Royal Amethyst, Sunset Copper, and Cyberpunk Neon.
• SECURE IN-APP FILE VIEWER: Resolves local links within markdown transcripts securely, prompting for explicit user permissions when opening files outside the active workspace.
• DUAL SEARCH STATUS FILTERING: Added dedicated status chips to filter by Active and Archived conversations.
• MODEL PERFORMANCE BREAKDOWN: A new dashboard component allows sorting active AI models by turns, tokens, compute duration, and speed.
• SEAMLESS OS AUTO-UPDATES: Checks for new releases and downloads signed platform-native installers (MSI, PKG, DEB) automatically.

***

## 🏷️ 4. Supplemental & Additional Fields

### Short Description
* **Recommended Length:** 270 characters or fewer (Actual: 247 characters).
* **Value:**
A local-first search dashboard that indexes and aggregates conversation logs from Claude Code, Google Antigravity, Cursor, OpenAI Codex, Aider, and GitHub Copilot with offline lexical and concept-matching semantic search. Private, fast, and 100% offline.

---

### Additional System Requirements

#### Minimum Hardware:
• OS: Windows 10 (Version 1809) or Windows 11
• Architecture: x64 or ARM64
• Memory: 4 GB RAM
• Storage: 100 MB free space (excluding logs and models)

#### Recommended Hardware:
• OS: Windows 11
• Memory: 8 GB RAM
• Storage: 500 MB free space (for indexes, model caching, and embeddings)

---

### Additional Information

#### Keywords
* **Limit:** Up to 7 keywords. No more than 21 separate words across all keywords.
* **Value:**
1. AI search
2. Code search
3. Cursor search
4. Claude Code
5. Aider search
6. Developer tool
7. Semantic search

#### Copyright and Trademark Info
* **Value:**
Copyright © 2026 Look At What AI Can Do. All rights reserved.

***

## ⚖️ 5. Applicable License Terms (EULA)
* **Usage:** Copy and paste the text block below into the "Applicable license terms" text box in Partner Center.

***

CODEOBA END USER LICENSE AGREEMENT (EULA)

This License Agreement governs your use of the Codeoba desktop application (the "Software"). 

1. LICENSE GRANTS
• Free/Local-First Features: The core, single-device offline features of Codeoba (including local directory indexing, lexical keyword search, and local semantic search) are licensed to you free of charge under open-source terms.
• Premium Features: Access to the proprietary premium components (such as local conversation summaries, metrics auditing, and cross-device sync) is granted under a limited, non-exclusive, non-transferable license, subject to an active paid subscription to the Codeoba Ecosystem.

2. RESTRICTIONS ON PREMIUM & PROPRIETARY COMPONENTS
For any proprietary components (including but not limited to the premium analyzer module and backend services):
• You may not copy, modify, distribute, sell, or lease any part of these proprietary components.
• You may not reverse engineer, decompile, or attempt to extract the source code of these components, except to the extent permitted by law.
• You may not attempt to bypass, disable, or modify any license validation checkpoints or security controls.

3. PRIVACY AND LOCAL-FIRST DESIGN
• By default, all conversation indexing and search embeddings run 100% locally on your machine.
• Synced ecosystem data (if enabled by you) is subject to the Codeoba Privacy Policy.

4. WARRANTY AND LIMITATION OF LIABILITY
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER LIABILITY ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE.

***

## 🖼️ 6. Generated Store Art Assets
The following visual assets have been generated and saved directly to the project's store assets directory for upload:

* **1:1 Box Art Logo (1080 x 1080px):** [codeoba_box_art.png](./codeoba_box_art.png)
* **2:3 Poster Art (720 x 1080px portrait):** [codeoba_poster_art.png](./codeoba_poster_art.png)
