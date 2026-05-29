package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.launch
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import llc.lookatwhataicando.codeoba.core.domain.search.ArchivalFilter
import llc.lookatwhataicando.codeoba.core.domain.search.HashSemanticEmbedder
import llc.lookatwhataicando.codeoba.core.domain.search.LexicalSearchEngine
import llc.lookatwhataicando.codeoba.core.domain.search.SearchFilter
import llc.lookatwhataicando.codeoba.core.domain.search.SearchResult
import llc.lookatwhataicando.codeoba.core.domain.search.SemanticSearchEngine
import llc.lookatwhataicando.codeoba.core.domain.search.buildFindRegex
import llc.lookatwhataicando.codeoba.core.domain.source.SourceRegistry
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter
import llc.lookatwhataicando.codeoba.core.manager.IndexManager
import llc.lookatwhataicando.codeoba.core.source.DesktopAiderSource
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import androidx.compose.ui.text.style.TextAlign
import llc.lookatwhataicando.codeoba.core.source.DesktopAntigravitySource
import llc.lookatwhataicando.codeoba.core.source.DesktopClaudeSource
import llc.lookatwhataicando.codeoba.core.source.DesktopCodexSource
import llc.lookatwhataicando.codeoba.core.source.DesktopCursorSource
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.round

// Premium Dark Color Palette
val ObsidianBg = Color(0xFF0C0C0E)
val SlateSurface = Color(0xFF14141A)
val CardSurface = Color(0xFF1E1E28)
val AccentCyan = Color(0xFF00E5FF)
val AccentPurple = Color(0xFFAB47BC)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9E9EAE)
val BorderColor = Color(0xFF2C2C3A)

enum class SearchMode {
    Lexical, Semantic
}

internal fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        }
    } catch (e: Exception) {
        log("Failed to open URL $url: ${e.message}")
    }
}

@Volatile
var cacheOverride: Boolean? = null

fun main(args: Array<String>) {
    if (args.contains("--no-cache")) {
        cacheOverride = false
        log("Main: Caching disabled via command-line option --no-cache.")
    } else if (args.contains("--cache")) {
        cacheOverride = true
        log("Main: Caching enabled via command-line option --cache.")
    }

    System.setProperty("apple.awt.application.name", "Codeoba")
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Codeoba")
    val sources = listOf(
        llc.lookatwhataicando.codeoba.core.source.DesktopClaudeSource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopAntigravitySource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopCursorSource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopCodexSource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopAiderSource()
    )
    for (src in sources) {
        println("DIAG_MAIN: ${src.id} -> isEffectiveEnabled = ${src.isEffectiveEnabled()}, isAppInstalled = ${src.isAppInstalled()}, isAvailable = ${src.isAvailable()}, decision = ${SettingsManager.getUserDecision(src.id)}")
    }
    mainEntry()
}

fun mainEntry() = application {
    val initialX = SettingsManager.getWindowX()
    val initialY = SettingsManager.getWindowY()
    val initialWidth = SettingsManager.getWindowWidth() ?: 1280
    val initialHeight = SettingsManager.getWindowHeight() ?: 800
    val initialMaximized = SettingsManager.getWindowMaximized() ?: false

    // Validate position coordinates: make sure the window intersects at least one active screen
    var validatedBounds: java.awt.Rectangle? = null
    if (initialX != null && initialY != null) {
        val savedRect = java.awt.Rectangle(initialX, initialY, initialWidth, initialHeight)
        val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
        for (screen in ge.screenDevices) {
            val bounds = screen.defaultConfiguration.bounds
            if (bounds.intersects(savedRect)) {
                validatedBounds = savedRect
                break
            }
        }
    }

    val finalX = validatedBounds?.x
    val finalY = validatedBounds?.y
    val finalWidth = validatedBounds?.width ?: initialWidth
    val finalHeight = validatedBounds?.height ?: initialHeight

    val windowState = rememberWindowState(
        placement = if (initialMaximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = if (finalX != null && finalY != null) {
            WindowPosition.Absolute(finalX.dp, finalY.dp)
        } else {
            WindowPosition(Alignment.Center)
        },
        width = finalWidth.dp,
        height = finalHeight.dp
    )

    val scope = rememberCoroutineScope()
    var windowInstance by remember { mutableStateOf<ComposeWindow?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var indexUpdateTrigger by remember { mutableStateOf(0) }

    val sourceRegistry = remember {
        SourceRegistry().apply {
            register(DesktopClaudeSource())
            register(DesktopAntigravitySource())
            register(DesktopCursorSource())
            register(DesktopCodexSource())
            register(DesktopAiderSource())
        }
    }

    val activeFilters = remember {
        mutableStateListOf<String>().apply {
            addAll(SettingsManager.getLastActiveFilters())
        }
    }

    val ignoredSources = remember(refreshTrigger) {
        sourceRegistry.getAllAdapters()
            .filter { !it.isEffectiveEnabled() }
            .map { it.id }
            .toSet()
    }

    LaunchedEffect(ignoredSources) {
        sourceRegistry.setIgnoredSources(ignoredSources)
        activeFilters.removeAll { it in ignoredSources }
        while (true) {
            kotlinx.coroutines.delay(5000)
            val currentIgnored = sourceRegistry.getAllAdapters()
                .filter { !it.isEffectiveEnabled() }
                .map { it.id }
                .toSet()
            if (currentIgnored != ignoredSources) {
                log("Main UI: Detected change in effectively enabled sources! Ignored sources changed from $ignoredSources to $currentIgnored. Triggering refresh.")
                refreshTrigger++
                break
            }
        }
    }

    val temporarilyIgnoredSources = remember { mutableStateListOf<String>() }

    val sourcesToWarnAbout = remember(refreshTrigger, indexUpdateTrigger, temporarilyIgnoredSources.size) {
        sourceRegistry.getAllAdapters().filter { adapter ->
            adapter.isAvailable() &&
            !adapter.isAppInstalled() &&
            adapter.id !in temporarilyIgnoredSources &&
            SettingsManager.getUserDecision(adapter.id) == SettingsManager.Decision.UNDECIDED
        }
    }

    val lexicalEngine = remember { LexicalSearchEngine() }
    val semanticEngine = remember { SemanticSearchEngine(HashSemanticEmbedder()) }

    var searchMode by remember { mutableStateOf(SearchMode.Lexical) }
    val currentEngine = if (searchMode == SearchMode.Lexical) lexicalEngine else semanticEngine

    var activeIndexManager by remember { mutableStateOf<IndexManager?>(null) }

    var queryValue by remember { mutableStateOf(TextFieldValue("")) }
    var searchMatchCase by remember { mutableStateOf(false) }
    var searchWholeWord by remember { mutableStateOf(false) }
    var searchUseRegex by remember { mutableStateOf(false) }
    var activeStatusFilters by remember { mutableStateOf(SettingsManager.getLastStatusFilters()) }
    val activeArchivedFilter = remember(activeStatusFilters) {
        when {
            activeStatusFilters.contains(ArchivalFilter.ACTIVE) && !activeStatusFilters.contains(ArchivalFilter.ARCHIVED) -> ArchivalFilter.ACTIVE
            !activeStatusFilters.contains(ArchivalFilter.ACTIVE) && activeStatusFilters.contains(ArchivalFilter.ARCHIVED) -> ArchivalFilter.ARCHIVED
            else -> ArchivalFilter.ALL
        }
    }

    LaunchedEffect(activeFilters.toList()) {
        SettingsManager.setLastActiveFilters(activeFilters.toList())
    }

    LaunchedEffect(activeStatusFilters) {
        SettingsManager.setLastStatusFilters(activeStatusFilters)
    }

    // Navigation History Stack
    val navigationStack = remember { mutableStateListOf<String?>(null) }
    var navigationIndex by remember { mutableStateOf(0) }
    var selectedSession by remember { mutableStateOf<Session?>(null) }

    fun navigateTo(sessionId: String?) {
        val currentId = if (navigationIndex in navigationStack.indices) navigationStack[navigationIndex] else null
        if (currentId == sessionId) return
        
        while (navigationStack.size > navigationIndex + 1) {
            navigationStack.removeAt(navigationStack.lastIndex)
        }
        navigationStack.add(sessionId)
        navigationIndex = navigationStack.lastIndex
    }

    fun navigateBack() {
        if (navigationIndex > 0) {
            navigationIndex--
        }
    }

    fun navigateForward() {
        if (navigationIndex < navigationStack.lastIndex) {
            navigationIndex++
        }
    }

    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isIndexing by remember { mutableStateOf(true) }

    var pinnedSessionIds by remember { mutableStateOf(SettingsManager.getPinnedSessionIds()) }

    fun toggleSessionPinned(session: Session) {
        SettingsManager.toggleSessionPinned(session.id)
        pinnedSessionIds = SettingsManager.getPinnedSessionIds()
    }

    var showDetailFindBar by remember { mutableStateOf(false) }
    var findQueryValue by remember { mutableStateOf(TextFieldValue("")) }
    var findMatchCase by remember { mutableStateOf(false) }
    var findWholeWord by remember { mutableStateOf(false) }
    var findUseRegex by remember { mutableStateOf(false) }
    var activeMatchIndex by remember { mutableStateOf(0) }

    val findRegex = remember(findQueryValue.text, findMatchCase, findWholeWord, findUseRegex) {
        buildFindRegex(findQueryValue.text, findMatchCase, findWholeWord, findUseRegex)
    }

    val matches = remember(selectedSession, findRegex) {
        selectedSession?.let { findSessionMatches(it, findRegex) } ?: emptyList()
    }

    LaunchedEffect(selectedSession, findQueryValue.text, findMatchCase, findWholeWord, findUseRegex) {
        activeMatchIndex = 0
    }

    var isSidebarCollapsed by remember { mutableStateOf(SettingsManager.getSidebarCollapsed() ?: false) }
    var sidebarWidth by remember { mutableStateOf((SettingsManager.getSidebarWidth() ?: 360f).dp) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var activeFileToView by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sidebarWidth, isSidebarCollapsed) {
        // Debounce saving sidebar settings
        kotlinx.coroutines.delay(500)
        SettingsManager.setSidebarWidth(sidebarWidth.value)
        SettingsManager.setSidebarCollapsed(isSidebarCollapsed)
    }

    LaunchedEffect(Unit) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                val desktop = java.awt.Desktop.getDesktop()
                if (desktop.isSupported(java.awt.Desktop.Action.APP_PREFERENCES)) {
                    desktop.setPreferencesHandler {
                        showSettingsDialog = true
                    }
                }
            }
        } catch (e: Exception) {
            log("Failed to register preferences handler: ${e.message}")
        }
    }

    val currentSessionId = if (navigationIndex in navigationStack.indices) navigationStack[navigationIndex] else null

    LaunchedEffect(currentSessionId, searchResults, pinnedSessionIds) {
        selectedSession = currentSessionId?.let { id ->
            val sess = currentEngine.getSession(id)
            sess?.copy(isPinned = pinnedSessionIds.contains(id))
        }
    }

    LaunchedEffect(selectedSession) {
        showDetailFindBar = false
        findQueryValue = TextFieldValue("")
    }

    // Load and index sessions
    LaunchedEffect(searchMode, refreshTrigger) {
        log("Main UI: LaunchedEffect started for searchMode: $searchMode, refreshTrigger: $refreshTrigger")
        isIndexing = true
        try {
            activeIndexManager?.stopWatchers()
            val manager = IndexManager(
                sourceRegistry = sourceRegistry,
                searchEngine = currentEngine,
                scope = scope,
                cacheEnabled = cacheOverride ?: SettingsManager.getCacheEnabled()
            )
            activeIndexManager = manager
            manager.addIndexUpdatedListener {
                log("Main UI: Index update callback received")
                indexUpdateTrigger++
                scope.launch {
                    val filter = SearchFilter(
                        sourceIds = activeFilters.toSet(),
                        matchCase = searchMatchCase,
                        wholeWord = searchWholeWord,
                        useRegex = searchUseRegex,
                        archivalFilter = activeArchivedFilter
                    )
                    searchResults = currentEngine.search(queryValue.text, filter)
                    log("Main UI: Search results updated inside listener, count: ${searchResults.size}")
                }
            }
            log("Main UI: Calling manager.initialScanAndWatch()...")
            manager.initialScanAndWatch()
            log("Main UI: manager.initialScanAndWatch() completed.")
            val filter = SearchFilter(
                sourceIds = activeFilters.toSet(),
                matchCase = searchMatchCase,
                wholeWord = searchWholeWord,
                useRegex = searchUseRegex,
                archivalFilter = activeArchivedFilter
            )
            searchResults = currentEngine.search(queryValue.text, filter)
            log("Main UI: Initial search done, found ${searchResults.size} results.")
        } catch (e: Throwable) {
            log("Main UI: Error during initial scan and watch:", e)
        } finally {
            isIndexing = false
            log("Main UI: Setting isIndexing = false")
        }
    }

    // Refresh search results when query or filters change
    LaunchedEffect(queryValue.text, activeFilters.size, searchMatchCase, searchWholeWord, searchUseRegex, activeArchivedFilter) {
        val filter = SearchFilter(
            sourceIds = activeFilters.toSet(),
            matchCase = searchMatchCase,
            wholeWord = searchWholeWord,
            useRegex = searchUseRegex,
            archivalFilter = activeArchivedFilter
        )
        searchResults = currentEngine.search(queryValue.text, filter)
    }

    fun saveWindowState() {
        windowInstance?.let { win ->
            try {
                val state = win.extendedState
                val isMax = (state and java.awt.Frame.MAXIMIZED_BOTH) != 0
                val isMin = (state and java.awt.Frame.ICONIFIED) != 0
                if (!isMax && !isMin) {
                    SettingsManager.setWindowX(win.bounds.x)
                    SettingsManager.setWindowY(win.bounds.y)
                    SettingsManager.setWindowWidth(win.bounds.width)
                    SettingsManager.setWindowHeight(win.bounds.height)
                }
                SettingsManager.setWindowMaximized(isMax)
                val device = win.graphicsConfiguration?.device
                if (device != null) {
                    SettingsManager.setWindowScreen(device.getIDstring())
                }
                SettingsManager.setSidebarWidth(sidebarWidth.value)
                SettingsManager.setSidebarCollapsed(isSidebarCollapsed)
            } catch (e: Exception) {
                log("Error saving window state: ${e.message}")
            }
        }
    }

    Window(
        onCloseRequest = {
            saveWindowState()
            activeIndexManager?.stopWatchers()
            exitApplication()
            java.lang.System.exit(0)
        },
        title = "Codeoba — Unified Agent Session Search",
        state = windowState,
        icon = painterResource("icon.png"),
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown &&
                keyEvent.key == Key.F &&
                (keyEvent.isMetaPressed || keyEvent.isCtrlPressed)
            ) {
                if (selectedSession != null) {
                    showDetailFindBar = !showDetailFindBar
                    if (!showDetailFindBar) {
                        findQueryValue = TextFieldValue("")
                    }
                }
                true
            } else if (keyEvent.type == KeyEventType.KeyDown &&
                keyEvent.key == Key.G &&
                (keyEvent.isMetaPressed || keyEvent.isCtrlPressed)
            ) {
                if (selectedSession != null && showDetailFindBar) {
                    if (keyEvent.isShiftPressed) {
                        if (matches.isNotEmpty()) {
                            activeMatchIndex = (activeMatchIndex - 1 + matches.size) % matches.size
                        }
                    } else {
                        if (matches.isNotEmpty()) {
                            activeMatchIndex = (activeMatchIndex + 1) % matches.size
                        }
                    }
                }
                true
            } else if (keyEvent.type == KeyEventType.KeyDown &&
                keyEvent.key == Key.Escape &&
                showDetailFindBar
            ) {
                showDetailFindBar = false
                findQueryValue = TextFieldValue("")
                true
            } else {
                false
            }
        }
    ) {
        windowInstance = window

        LaunchedEffect(window) {
            var saveJob: kotlinx.coroutines.Job? = null
            val componentListener = object : java.awt.event.ComponentAdapter() {
                override fun componentMoved(e: java.awt.event.ComponentEvent) {
                    triggerSave()
                }
                override fun componentResized(e: java.awt.event.ComponentEvent) {
                    triggerSave()
                }
                private fun triggerSave() {
                    saveJob?.cancel()
                    saveJob = scope.launch {
                        kotlinx.coroutines.delay(500)
                        val state = window.extendedState
                        val isMax = (state and java.awt.Frame.MAXIMIZED_BOTH) != 0
                        val isMin = (state and java.awt.Frame.ICONIFIED) != 0
                        if (!isMax && !isMin) {
                            SettingsManager.setWindowX(window.bounds.x)
                            SettingsManager.setWindowY(window.bounds.y)
                            SettingsManager.setWindowWidth(window.bounds.width)
                            SettingsManager.setWindowHeight(window.bounds.height)
                        }
                        val device = window.graphicsConfiguration?.device
                        if (device != null) {
                            SettingsManager.setWindowScreen(device.getIDstring())
                        }
                    }
                }
            }
            window.addComponentListener(componentListener)

            val stateListener = java.awt.event.WindowStateListener { e ->
                saveJob?.cancel()
                saveJob = scope.launch {
                    kotlinx.coroutines.delay(500)
                    val isMax = (e.newState and java.awt.Frame.MAXIMIZED_BOTH) != 0
                    SettingsManager.setWindowMaximized(isMax)
                }
            }
            window.addWindowStateListener(stateListener)

            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                window.removeComponentListener(componentListener)
                window.removeWindowStateListener(stateListener)
            }
        }

        LaunchedEffect(window.rootPane) {
            with(window.rootPane) {
                putClientProperty("apple.awt.transparentTitleBar", true)
                putClientProperty("apple.awt.fullWindowContent", true)
                putClientProperty("apple.awt.windowTitleVisible", false)
            }
        }

        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item("Refresh Index", onClick = { refreshTrigger++ }, shortcut = KeyShortcut(Key.R, meta = true))
                Item("Toggle Sidebar", onClick = { isSidebarCollapsed = !isSidebarCollapsed }, shortcut = KeyShortcut(Key.B, meta = true))
                Item("Settings...", onClick = { showSettingsDialog = true }, shortcut = KeyShortcut(Key.Comma, meta = true))
                Separator()
                Item("Exit", onClick = {
                    saveWindowState()
                    activeIndexManager?.stopWatchers()
                    exitApplication()
                    java.lang.System.exit(0)
                })
            }
            Menu("Edit", mnemonic = 'E') {
                Item("Find...", onClick = {
                    if (selectedSession != null) {
                        showDetailFindBar = !showDetailFindBar
                        if (!showDetailFindBar) {
                            findQueryValue = TextFieldValue("")
                        }
                    }
                }, shortcut = KeyShortcut(Key.F, meta = true))
            }
            Menu("Help", mnemonic = 'H') {
                Menu("Server Status") {
                    Item("Google AI Studio Status", onClick = { openUrl("https://aistudio.google.com/status") })
                    Item("Anthropic Status", onClick = { openUrl("https://status.anthropic.com/") })
                    Item("OpenAI Status", onClick = { openUrl("https://status.openai.com/") })
                    Item("Cursor Status", onClick = { openUrl("https://status.cursor.com") })
                }
            }
        }

        MaterialTheme(
            colorScheme = darkColorScheme(
                background = ObsidianBg,
                surface = SlateSurface,
                primary = AccentCyan,
                secondary = AccentPurple,
                onBackground = TextPrimary,
                onSurface = TextPrimary
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianBg
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Sidebar
                        if (!isSidebarCollapsed) {
                            Sidebar(
                                queryValue = queryValue,
                                onQueryValueChange = { queryValue = it },
                                matchCase = searchMatchCase,
                                onMatchCaseChange = { searchMatchCase = it },
                                wholeWord = searchWholeWord,
                                onWholeWordChange = { searchWholeWord = it },
                                useRegex = searchUseRegex,
                                onUseRegexChange = { searchUseRegex = it },
                                searchMode = searchMode,
                                onSearchModeChange = { searchMode = it },
                                activeFilters = activeFilters,
                                sourceRegistry = sourceRegistry,
                                searchResults = searchResults,
                                selectedSession = selectedSession,
                                onSessionSelect = { navigateTo(it?.id) },
                                isIndexing = isIndexing,
                                ignoredSources = ignoredSources,
                                activeStatusFilters = activeStatusFilters,
                                onStatusFilterToggle = { filter ->
                                    activeStatusFilters = if (activeStatusFilters.contains(filter)) {
                                        activeStatusFilters - filter
                                    } else {
                                        activeStatusFilters + filter
                                    }
                                },
                                pinnedSessionIds = pinnedSessionIds,
                                onTogglePin = { toggleSessionPinned(it) },
                                modifier = Modifier.width(sidebarWidth)
                            )

                            // Resizable Divider
                            val density = LocalDensity.current
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(6.dp)
                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
                                    .pointerInput(density) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            with(density) {
                                                val dragAmountDp = dragAmount.x.toDp()
                                                val newWidth = sidebarWidth + dragAmountDp
                                                sidebarWidth = newWidth.coerceIn(240.dp, 600.dp)
                                            }
                                        }
                                    }
                                    .drawBehind {
                                        drawLine(
                                            color = BorderColor,
                                            start = Offset(size.width / 2, 0f),
                                            end = Offset(size.width / 2, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                            )
                        }

                        // Detail Pane
                        DetailPane(
                            session = selectedSession,
                            searchResults = searchResults,
                            query = queryValue.text,
                            showFindBar = showDetailFindBar,
                            queryValue = findQueryValue,
                            onQueryValueChange = { findQueryValue = it },
                            matchCase = findMatchCase,
                            onMatchCaseChange = { findMatchCase = it },
                            wholeWord = findWholeWord,
                            onWholeWordChange = { findWholeWord = it },
                            useRegex = findUseRegex,
                            onUseRegexChange = { findUseRegex = it },
                            matches = matches,
                            activeMatchIndex = activeMatchIndex,
                            onPrevMatch = {
                                if (matches.isNotEmpty()) {
                                    activeMatchIndex = (activeMatchIndex - 1 + matches.size) % matches.size
                                }
                            },
                            onNextMatch = {
                                if (matches.isNotEmpty()) {
                                    activeMatchIndex = (activeMatchIndex + 1) % matches.size
                                }
                            },
                            onCloseFind = {
                                showDetailFindBar = false
                                findQueryValue = TextFieldValue("")
                            },
                            isSidebarCollapsed = isSidebarCollapsed,
                            onToggleSidebar = { isSidebarCollapsed = !isSidebarCollapsed },
                            canGoBack = navigationIndex > 0,
                            canGoForward = navigationIndex < navigationStack.lastIndex,
                            onBack = { navigateBack() },
                            onForward = { navigateForward() },
                            onRefresh = { refreshTrigger++ },
                            onSessionSelect = { navigateTo(it?.id) },
                            onOpenSettings = { showSettingsDialog = true },
                            onTogglePin = { toggleSessionPinned(it) },
                            onUrlClick = { url ->
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    openUrl(url)
                                } else {
                                    try {
                                        var decodedPath = java.net.URLDecoder.decode(url, "UTF-8")
                                        decodedPath = decodedPath.substringBefore('#').substringBefore('?')
                                        if (decodedPath.startsWith("file:///")) {
                                            decodedPath = "/" + decodedPath.removePrefix("file:///").removePrefix("file:/")
                                        } else if (decodedPath.startsWith("file://")) {
                                            decodedPath = decodedPath.removePrefix("file://")
                                        } else if (decodedPath.startsWith("file:/")) {
                                            decodedPath = "/" + decodedPath.removePrefix("file:/")
                                        }
                                        while (decodedPath.contains("//")) {
                                            decodedPath = decodedPath.replace("//", "/")
                                        }
                                        val isWindows = System.getProperty("os.name").lowercase().contains("win")
                                        if (isWindows && decodedPath.startsWith("/") && decodedPath.length > 2 && decodedPath[2] == ':') {
                                            decodedPath = decodedPath.substring(1)
                                        }
                                        activeFileToView = decodedPath
                                    } catch (e: Exception) {
                                        log("Failed to parse file URL $url: ${e.message}")
                                        activeFileToView = url
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val activeWarnSource = sourcesToWarnAbout.firstOrNull()
                if (activeWarnSource != null) {
                    WarningOverlay(
                        source = activeWarnSource,
                        onDecision = { decision ->
                            SettingsManager.setUserDecision(activeWarnSource.id, decision)
                            refreshTrigger++
                        },
                        onDeleteConfirm = {
                            val success = activeWarnSource.deleteDataPaths()
                            log("Deleted data paths for ${activeWarnSource.displayName}, success: $success")
                            SettingsManager.setUserDecision(activeWarnSource.id, SettingsManager.Decision.IGNORE)
                            refreshTrigger++
                        },
                        onClose = {
                            temporarilyIgnoredSources.add(activeWarnSource.id)
                        }
                    )
                }
                if (showSettingsDialog) {
                    SettingsDialog(
                        sourceRegistry = sourceRegistry,
                        onClose = {
                            showSettingsDialog = false
                        },
                        onSettingsChanged = {
                            refreshTrigger++
                        }
                    )
                }
                if (activeFileToView != null) {
                    FileViewerDialog(
                        filePath = activeFileToView!!,
                        onClose = { activeFileToView = null },
                        onUrlClick = { url ->
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                openUrl(url)
                            } else {
                                try {
                                    var decodedPath = java.net.URLDecoder.decode(url, "UTF-8")
                                    decodedPath = decodedPath.substringBefore('#').substringBefore('?')
                                    if (decodedPath.startsWith("file:///")) {
                                        decodedPath = "/" + decodedPath.removePrefix("file:///").removePrefix("file:/")
                                    } else if (decodedPath.startsWith("file://")) {
                                        decodedPath = decodedPath.removePrefix("file://")
                                    } else if (decodedPath.startsWith("file:/")) {
                                        decodedPath = "/" + decodedPath.removePrefix("file:/")
                                    }
                                    while (decodedPath.contains("//")) {
                                        decodedPath = decodedPath.replace("//", "/")
                                    }
                                    val isWindows = System.getProperty("os.name").lowercase().contains("win")
                                    if (isWindows && decodedPath.startsWith("/") && decodedPath.length > 2 && decodedPath[2] == ':') {
                                        decodedPath = decodedPath.substring(1)
                                    }
                                    activeFileToView = decodedPath
                                } catch (e: Exception) {
                                    log("Failed to parse file URL $url: ${e.message}")
                                    activeFileToView = url
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
