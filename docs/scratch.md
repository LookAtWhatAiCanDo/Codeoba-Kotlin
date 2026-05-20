
There is at least one (and I suspect many) huge gap in displaying the conversation timeline:
ex: immediately before and after "shouldn't the documentation need to be updated?" in "audit the code and suggest ways" 

What is the deal with the large text terminal font text block that always starts with (different timestamps):
```
Created At: 2026-05-21T17:30:34Z
Completed At: 2026-05-21T17:30:34Z
```
As a developer I do appreciate having access to that raw detail, but we should be processing
and rendering that to be helpful to the user in another format and not show them the raw data.
Still, it is nice as a Codeoba developer to be able to easily peek at the raw data.

There are lots (dozens? scores? hundreds?) of coding agent apps out there.
Is it possible to embed a deterministic AI/LLM/Agent **IN CODEOBA** that can find other coding agent apps and intuit the needful from their data format? 

Why did the Agent revert a core SourceRegistry class and replace it with this in `llc.lookatwhataicando.codeoba.desktop.MainKt.main`:
```kotlin
    val sourceRegistry = remember {
        SourceRegistry().apply {
            register(DesktopClaudeSource())
            register(DesktopAntigravitySource())
            register(DesktopCursorSource())
            register(DesktopCodexSource())
            register(DesktopAiderSource())
        }
    }
```

"Semantic Search" seems broken/not-working?

Fit & Finish:
* Save and restore the app's last known viewed item and scroll position, or if last scrolled to end then scroll lock to follow tail.
* SCROLL HISTORY! Would imply a setting to clear history.

Ideas:
* DANGER ZONE: Ability to obliterate (delete) the conversation
  This could mess up any indexing/caching done by the other app

* Ability to MCP control each source and send prompt text or other commands to them

* Local LLM to summarize the conversation

* Add a Notes section

* Natural language summary/overview of the projects that you are working on.
* Plan for the next steps/tasks.
