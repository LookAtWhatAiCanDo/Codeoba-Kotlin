# Problematic Parser Tokens & Remedies

This document outlines the problematic strings, variations, and tokens that can confuse the Codeoba log parser, along with their remedies and strategies.

---

## 1. Problematic Tokens & Variations

### A. Missing Closing Tags (`[[[TOOL:` without `[[[/TOOL]]]`)
- **Example String**: `[[[TOOL:GREP_SEARCH|Search|12345]]]\nSearch results content...` (but without a final `[[[/TOOL]]]` tag).
- **Occurrence Context**: This can happen if a transcript log is truncated prematurely, or if the assistant response includes a tool tag start but the system fails to write the closing tag.
- **Problem**: The parser loops through the message and finds `[[[TOOL:`. It splits the header and reads the remaining text as the tool content, effectively swallowing the rest of the conversation history as tool output.
- **Remedy**:
  - Do not treat a `[[[TOOL:` tag as a tool block if there is no matching closing tag `[[[/TOOL]]]` later in the message.
  - If no closing tag is found, treat the `[[[TOOL:` prefix as normal text and continue parsing the rest of the message.

### B. Literally Embedded Tags in Logs
- **Example String**: `{"File":"...","LineContent":"return \"[[[TOOL:$type...\""}`
- **Occurrence Context**: Encoutened in historical chats such as `9a9a9b5b-fa07-418a-b169-ed17f2a92c01` during search operations (e.g. grep searches looking for references to `[[[TOOL:` or `[[[/TOOL]]]` in source code).
- **Problem**: If the parser outputs literal `[[[TOOL:` inside the grep tool output itself, and it is not escaped, the UI parser interprets it as the start of a nested/new tool block, breaking the UI layout.
- **Remedy**:
  - The source adapter must proactively escape all variations of `[[[TOOL` and `[[[/TOOL` in parsed raw logs before combining them into assistant messages.
  - The UI parser must correctly identify escaped backslashes (`\\`) preceding these tags and skip them during scanner loops, then unescape them back to their original form for presentation.

### C. Variations without Colons (`[[[TOOL]]]` and `[[[/TOOL]]]`)
- **Example String**: `[[[TOOL]]]` or `[[[/TOOL]]]`
- **Occurrence Context**: Mentioned in developer rules (e.g. `AGENTS.md`) or during instruction prompt processing.
- **Problem**: If the text contains `[[[TOOL]]]` (no colon), the standard `escapeToolTags()` which only looks for `[[[TOOL:` leaves it unescaped. However, if a later parser revision searches for `[[[TOOL`, it might get confused, or the presence of literal `[[[/TOOL]]]` in text might prematurely close an active tool block.
- **Remedy**:
  - Match and escape the broader prefixes `[[[TOOL` and `[[[/TOOL` instead of only `[[[TOOL:` and `[[[/TOOL]]]`.
  - Specifically, replace `[[[TOOL` with `\\[\\[\\[TOOL` and `[[[/TOOL` with `\\[\\[\\[/TOOL` on parse, and reverse this replacement on display.

### D. Aider Markdown Heading Splitting (`#### `)
- **Example String**: `#### Problem description`
- **Occurrence Context**: When a user or assistant writes a Level 4 markdown header `#### ` in their Aider messages.
- **Problem**: The Aider parser naively splits chat transcripts on `#### `. If it encounters `#### ` followed by a non-role heading (e.g., a section header in code discussion), it splits the turn there and discards the subsequent block since it does not match a known role (`user`, `assistant`, `aider`, `bot`).
- **Remedy**:
  - Refactor splitting logic to use a strict regular expression that only splits on designated role headings, i.e., `Regex("(^|\n)#### (User|Assistant|Aider|Bot):", RegexOption.IGNORE_CASE)`.

### E. Antigravity XML Wrapper Tags (`</USER_REQUEST>`, `</SYSTEM_MESSAGE>`)
- **Example String**: `Explain how the closing tag </USER_REQUEST> is handled.`
- **Occurrence Context**: Literal discussions of the wrapper tags inside user or system messages.
- **Problem**: The parser extracts content by doing `substringBefore("</USER_REQUEST>")`. If the request contains this tag literally, the content is truncated prematurely and the rest of the text is lost.
- **Remedy**:
  - Instead of doing search-based substring slicing on the entire message string, match the tags at the outer boundaries, or extract using a non-greedy regex match that anchors the tags at the beginning and end of the envelope structure.

### F. Windows Cursor Database File Paths (`file:///` vs Drive Letters)
- **Example String**: `file:///C:/path/to/project`
- **Occurrence Context**: Parsing workspace directories from Cursor workspace databases on Windows.
- **Problem**: Storing the sliced path directly (e.g. using `substringAfter("file://")`) yields `/C:/Users/...`, which is an invalid path prefix on Windows platforms.
- **Remedy**:
  - Detect and format Windows-style drive letters when stripping `file://` prefixes (e.g. check if the path starts with a slash followed by a drive letter and colon, and strip the leading slash).

---

## 2. Parser Strategies & Rules

| Problematic String | Parsing Symptom | Strategy / Remedy |
| :--- | :--- | :--- |
| `[[[TOOL:` (No closing tag) | Swallows all remaining turn messages as tool output. | Validate existence of closing `[[[/TOOL]]]` tag; fallback to plain text if missing. |
| `\\[\\[\\[TOOL:` | Treated as a tool block if backslash detection is naive. | Count preceding backslashes; only skip if the backslash count is odd (escaped). |
| `[[[/TOOL]]]` (Embedded in tool text) | Closes the tool block prematurely. | Proactively escape `[[[/TOOL` in all raw log outputs. |
| `[[[TOOL]]]` (No colon) | May cause mismatched tags or premature tag detection. | Expand escaping/unescaping logic to target `[[[TOOL` and `[[[/TOOL` globally. |
| `#### ` (Not a role header) | Splits turn incorrectly and discards remaining content. | Split strictly on role-specific headings: `#### (User\|Assistant\|Aider\|Bot):`. |
| `</USER_REQUEST>` / `</SYSTEM_MESSAGE>` | Cuts off transcript text early and discards data. | Parse boundary wrapper tags strictly from envelope structure instead of simple substring matching. |
| `file:///C:/...` (Windows paths) | Creates invalid paths (e.g., `/C:/...`) on Windows. | Strip leading slash if path contains a drive letter. |


