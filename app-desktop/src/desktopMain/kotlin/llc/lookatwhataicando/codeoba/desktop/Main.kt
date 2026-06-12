package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.whataicando.touch.compose.WindowsTouch
import kotlinx.coroutines.launch
import llc.lookatwhataicando.codeoba.core.domain.model.ConversationGroup
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.search.ArchivalFilter
import llc.lookatwhataicando.codeoba.core.domain.search.HashSemanticEmbedder
import llc.lookatwhataicando.codeoba.core.domain.search.LexicalSearchEngine
import llc.lookatwhataicando.codeoba.core.domain.search.OnnxSemanticEmbedder
import llc.lookatwhataicando.codeoba.core.domain.search.SearchEngine
import llc.lookatwhataicando.codeoba.core.domain.search.SearchFilter
import llc.lookatwhataicando.codeoba.core.domain.search.SearchResult
import llc.lookatwhataicando.codeoba.core.domain.search.SemanticEmbedder
import llc.lookatwhataicando.codeoba.core.domain.search.SemanticSearchEngine
import llc.lookatwhataicando.codeoba.core.domain.search.buildFindRegex
import llc.lookatwhataicando.codeoba.core.domain.source.SourceRegistry
import llc.lookatwhataicando.codeoba.core.manager.EmbeddingCacheManager
import llc.lookatwhataicando.codeoba.core.manager.GroupManager
import llc.lookatwhataicando.codeoba.core.manager.IndexManager
import llc.lookatwhataicando.codeoba.core.source.DesktopAiderSource
import llc.lookatwhataicando.codeoba.core.source.DesktopAntigravitySource
import llc.lookatwhataicando.codeoba.core.source.DesktopClaudeSource
import llc.lookatwhataicando.codeoba.core.source.DesktopCodexSource
import llc.lookatwhataicando.codeoba.core.source.DesktopCopilotSource
import llc.lookatwhataicando.codeoba.core.source.DesktopCursorSource
import llc.lookatwhataicando.codeoba.core.util.LocalFileResolution
import llc.lookatwhataicando.codeoba.core.util.LocalFileResolver
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import llc.lookatwhataicando.codeoba.core.util.ModelDownloader
import java.awt.Cursor
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Date

// Premium Dark Color Palette (dynamic definitions located in Theme.kt)

enum class SearchMode {
    Lexical, Semantic
}

class DynamicSemanticEmbedder(fallback: SemanticEmbedder) : SemanticEmbedder {
    @Volatile
    var delegate: SemanticEmbedder = fallback
    override suspend fun getEmbeddings(text: String): FloatArray {
        return delegate.getEmbeddings(text)
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

    if (args.contains("--update-ignore-throttling")) {
        UpdateManager.ignoreUpdateThrottling = true
        log("Main: Update throttling disabled via command-line option --update-ignore-throttling.")
    }
    if (args.contains("--update-force")) {
        UpdateManager.forceUpdateAvailable = true
        log("Main: Forced update check availability via command-line option --update-force.")
    }
    if (args.contains("--update-mock-notes")) {
        UpdateManager.mockUpdateNotes = true
        log("Main: Mock hostile changelog notes enabled via command-line option --update-mock-notes.")
    }

    System.setProperty("apple.awt.application.name", "Codeoba")
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Codeoba")
    val sources = listOf(
        llc.lookatwhataicando.codeoba.core.source.DesktopClaudeSource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopAntigravitySource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopCursorSource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopCodexSource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopAiderSource(),
        llc.lookatwhataicando.codeoba.core.source.DesktopCopilotSource()
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
            register(DesktopCopilotSource())
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
    val dynamicEmbedder = remember { DynamicSemanticEmbedder(HashSemanticEmbedder()) }
    val semanticEngine = remember {
        SemanticSearchEngine(
            embedder = dynamicEmbedder,
            cache = EmbeddingCacheManager,
            similarityThreshold = SettingsManager.getSimilarityThreshold()
        )
    }

    var isModelDownloaded by remember { mutableStateOf(ModelDownloader.isModelDownloaded()) }
    var isModelDownloading by remember { mutableStateOf(false) }
    var modelDownloadProgress by remember { mutableStateOf(0f) }
    var modelDownloadError by remember { mutableStateOf<String?>(null) }

    fun loadOnnxEmbedder() {
        if (ModelDownloader.isModelDownloaded()) {
            try {
                val onnxEmbedder = OnnxSemanticEmbedder(
                    ModelDownloader.getModelFile(),
                    ModelDownloader.getVocabFile()
                )
                val old = dynamicEmbedder.delegate
                if (old is AutoCloseable) {
                    old.close()
                }
                dynamicEmbedder.delegate = onnxEmbedder
                log("Main: Loaded local ONNX Semantic Embedder.")
            } catch (e: Throwable) {
                log("Main: Failed to load ONNX semantic embedder, falling back to HashSemanticEmbedder.", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        EmbeddingCacheManager.loadCache()
        if (isModelDownloaded) {
            loadOnnxEmbedder()
        }
    }

    LaunchedEffect(refreshTrigger) {
        semanticEngine.similarityThreshold = SettingsManager.getSimilarityThreshold()
    }

    val onDownloadModel = {
        if (!isModelDownloading) {
            isModelDownloading = true
            modelDownloadError = null
            modelDownloadProgress = 0f
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    ModelDownloader.downloadModel { progress ->
                        modelDownloadProgress = progress
                    }
                    loadOnnxEmbedder()
                    isModelDownloaded = true
                    refreshTrigger++
                } catch (e: Exception) {
                    log("Main: Failed to download model: ${e.message}", e)
                    modelDownloadError = e.message ?: "Unknown error"
                } finally {
                    isModelDownloading = false
                }
            }
        }
    }

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
    var selectedSessionIds by remember { mutableStateOf(emptySet<String>()) }
    var selectionAnchorId by remember { mutableStateOf<String?>(null) }

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
    var indexingProgressText by remember { mutableStateOf("Initializing...") }

    var pinnedSessionIds by remember { mutableStateOf(SettingsManager.getPinnedSessionIds()) }
    
    var activeGroupFilter by remember { mutableStateOf(SettingsManager.getSelectedGroupFilter()) }
    var groupsState by remember { mutableStateOf(emptyList<ConversationGroup>()) }

    var unassignedSessionCount by remember { mutableStateOf(0) }
    LaunchedEffect(indexUpdateTrigger, groupsState, searchMode) {
        try {
            val allSessions = currentEngine.search("", SearchFilter(archivalFilter = ArchivalFilter.ALL))
            val allSessionIds = allSessions.map { it.session.id }.toSet()
            val assignedSessionIds = groupsState.flatMap { it.sessionIds }.toSet()
            unassignedSessionCount = (allSessionIds - assignedSessionIds).size
        } catch (_: Exception) {
            unassignedSessionCount = 0
        }
    }

    LaunchedEffect(activeGroupFilter) {
        SettingsManager.setSelectedGroupFilter(activeGroupFilter)
    }

    LaunchedEffect(Unit) {
        GroupManager.loadGroups()
        groupsState = GroupManager.getGroups()
    }

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
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestReleaseForUpdate by remember { mutableStateOf<GitHubRelease?>(null) }
    var activeFileToView by remember { mutableStateOf<String?>(null) }
    var pendingMainUrlClickPath by remember { mutableStateOf<Path?>(null) }
    var mainDontAskAgainChecked by remember { mutableStateOf(false) }
    var mainToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mainToastMessage) {
        if (mainToastMessage != null) {
            kotlinx.coroutines.delay(3000)
            mainToastMessage = null
        }
    }

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

    LaunchedEffect(Unit) {
        if (SettingsManager.getAutoUpdateEnabled()) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val lastCheck = SettingsManager.getLastUpdateCheck()
                val now = System.currentTimeMillis()
                val intervalMs = SettingsManager.getMinUpdateCheckIntervalSeconds().coerceAtLeast(0L).coerceAtMost(Long.MAX_VALUE / 1000L) * 1000L
                val elapsed = now - lastCheck
                
                if (elapsed in 0 until intervalMs && !UpdateManager.ignoreUpdateThrottling) {
                    log("Main Startup Update: Throttled (last check: ${Date(lastCheck)}, interval: ${intervalMs / 1000}s)")
                    return@launch
                }

                log("Main Startup Update: Running update check...")
                val release = UpdateManager.checkLatestRelease()
                if (release != null) {
                    SettingsManager.setLastUpdateCheck(System.currentTimeMillis())
                    SettingsManager.setMinUpdateCheckIntervalSeconds(release.minAutoUpdateCheckIntervalSeconds.coerceIn(0L, Long.MAX_VALUE / 1000L))
                    if (UpdateManager.isUpdateAvailable(release)) {
                        val skipped = SettingsManager.getSkippedVersion()
                        if (release.tagName != skipped) {
                            scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                kotlinx.coroutines.delay(release.uiDelayMillis.coerceAtLeast(0L))
                                latestReleaseForUpdate = release
                                showUpdateDialog = true
                            }
                        }
                    }
                }
            }
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
        if (selectedSession != null) {
            if (!selectedSessionIds.contains(selectedSession!!.id)) {
                selectedSessionIds = setOf(selectedSession!!.id)
                selectionAnchorId = selectedSession!!.id
            }
        } else {
            selectedSessionIds = emptySet()
            selectionAnchorId = null
        }
    }

    // Load and index sessions
    LaunchedEffect(searchMode, refreshTrigger) {
        log("Main UI: LaunchedEffect started for searchMode: $searchMode, refreshTrigger: $refreshTrigger")
        isIndexing = true
        try {
            activeIndexManager?.stopWatchers()
            indexingProgressText = "Initializing..."
            val manager = IndexManager(
                sourceRegistry = sourceRegistry,
                searchEngine = currentEngine,
                scope = scope,
                cacheEnabled = cacheOverride ?: SettingsManager.getCacheEnabled()
            )
            manager.setOnProgressListener { text ->
                indexingProgressText = text
            }
            activeIndexManager = manager
            manager.addIndexUpdatedListener {
                log("Main UI: Index update callback received")
                indexUpdateTrigger++
                scope.launch {
                    val allSessions = currentEngine.search("", SearchFilter(archivalFilter = ArchivalFilter.ALL))
                    val allSessionIds = allSessions.map { it.session.id }.toSet()
                    GroupManager.cleanOrphanedSessions(allSessionIds)
                    groupsState = GroupManager.getGroups()

                    val filter = SearchFilter(
                        sourceIds = activeFilters.toSet(),
                        matchCase = searchMatchCase,
                        wholeWord = searchWholeWord,
                        useRegex = searchUseRegex,
                        archivalFilter = activeArchivedFilter,
                        sessionIds = getSessionIdsForGroupAndDescendants(activeGroupFilter, groupsState, currentEngine)
                    )
                    searchResults = currentEngine.search(queryValue.text, filter)
                    log("Main UI: Search results updated inside listener, count: ${searchResults.size}")
                    if (searchMode == SearchMode.Semantic) {
                        EmbeddingCacheManager.saveCache()
                    }
                }
            }
            log("Main UI: Calling manager.initialScanAndWatch()...")
            manager.initialScanAndWatch()
            log("Main UI: manager.initialScanAndWatch() completed.")
            if (searchMode == SearchMode.Semantic) {
                EmbeddingCacheManager.saveCache()
            }
            val allSessions = currentEngine.search("", SearchFilter(archivalFilter = ArchivalFilter.ALL))
            val allSessionIds = allSessions.map { it.session.id }.toSet()
            GroupManager.cleanOrphanedSessions(allSessionIds)
            groupsState = GroupManager.getGroups()

            val filter = SearchFilter(
                sourceIds = activeFilters.toSet(),
                matchCase = searchMatchCase,
                wholeWord = searchWholeWord,
                useRegex = searchUseRegex,
                archivalFilter = activeArchivedFilter,
                sessionIds = getSessionIdsForGroupAndDescendants(activeGroupFilter, groupsState, currentEngine)
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
    LaunchedEffect(queryValue.text, activeFilters.size, searchMatchCase, searchWholeWord, searchUseRegex, activeArchivedFilter, activeGroupFilter, groupsState) {
        val filter = SearchFilter(
            sourceIds = activeFilters.toSet(),
            matchCase = searchMatchCase,
            wholeWord = searchWholeWord,
            useRegex = searchUseRegex,
            archivalFilter = activeArchivedFilter,
            sessionIds = getSessionIdsForGroupAndDescendants(activeGroupFilter, groupsState, currentEngine)
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
            if (searchMode == SearchMode.Semantic) {
                EmbeddingCacheManager.saveCache()
            }
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
            WindowsTouch.install(window)
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
            val dragDropState = remember { DragDropState() }
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
                                selectedSessionIds = selectedSessionIds,
                                selectionAnchorId = selectionAnchorId,
                                onSelectionChange = { activeSession, selectedIds, anchorId ->
                                    selectedSessionIds = selectedIds
                                    selectionAnchorId = anchorId
                                    navigateTo(activeSession?.id)
                                },
                                isIndexing = isIndexing,
                                indexingProgressText = indexingProgressText,
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
                                groups = groupsState,
                                activeGroupFilter = activeGroupFilter,
                                onActiveGroupFilterChange = { activeGroupFilter = it },
                                unassignedSessionCount = unassignedSessionCount,
                                isModelDownloaded = isModelDownloaded,
                                isModelDownloading = isModelDownloading,
                                modelDownloadProgress = modelDownloadProgress,
                                modelDownloadError = modelDownloadError,
                                onDownloadModel = onDownloadModel,
                                onAddGroup = { name -> GroupManager.addGroup(name).also { if (it) groupsState = GroupManager.getGroups() } },
                                onRenameGroup = { old, new -> GroupManager.renameGroup(old, new).also { if (it) { if (activeGroupFilter == old) activeGroupFilter = new; groupsState = GroupManager.getGroups() } } },
                                onDeleteGroup = { name -> GroupManager.deleteGroup(name).also { if (activeGroupFilter == name) activeGroupFilter = null; groupsState = GroupManager.getGroups() } },
                                onToggleGroupPin = { name, pinned -> GroupManager.setGroupPinned(name, pinned); groupsState = GroupManager.getGroups() },
                                onGroupAdd = { session, groupName ->
                                    val targets = if (selectedSessionIds.contains(session.id)) {
                                        selectedSessionIds.mapNotNull { id -> searchResults.firstOrNull { it.session.id == id }?.session }
                                    } else {
                                        listOf(session)
                                    }
                                    targets.forEach { GroupManager.assignSessionToGroup(it.id, groupName) }
                                    groupsState = GroupManager.getGroups()
                                },
                                onGroupRemove = { session, groupName ->
                                    val targets = if (selectedSessionIds.contains(session.id)) {
                                        selectedSessionIds.mapNotNull { id -> searchResults.firstOrNull { it.session.id == id }?.session }
                                    } else {
                                        listOf(session)
                                    }
                                    targets.forEach { GroupManager.removeSessionFromGroup(it.id, groupName) }
                                    groupsState = GroupManager.getGroups()
                                },
                                dragDropState = dragDropState,
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
                                dragDropState = dragDropState,
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
                                groups = groupsState,
                                onGroupAdd = { session, groupName ->
                                    val targets = if (selectedSessionIds.contains(session.id)) {
                                        selectedSessionIds.mapNotNull { id -> searchResults.firstOrNull { it.session.id == id }?.session }
                                    } else {
                                        listOf(session)
                                    }
                                    targets.forEach { GroupManager.assignSessionToGroup(it.id, groupName) }
                                    groupsState = GroupManager.getGroups()
                                },
                                onGroupRemove = { session, groupName ->
                                    val targets = if (selectedSessionIds.contains(session.id)) {
                                        selectedSessionIds.mapNotNull { id -> searchResults.firstOrNull { it.session.id == id }?.session }
                                    } else {
                                        listOf(session)
                                    }
                                    targets.forEach { GroupManager.removeSessionFromGroup(it.id, groupName) }
                                    groupsState = GroupManager.getGroups()
                                },
                                onGroupUpdate = { group -> GroupManager.addOrUpdateGroup(group); groupsState = GroupManager.getGroups() },
                                onGroupDelete = { groupName -> GroupManager.deleteGroup(groupName).also { if (activeGroupFilter == groupName) activeGroupFilter = null; groupsState = GroupManager.getGroups() } },
                                onToggleGroupPin = { name, pinned -> GroupManager.setGroupPinned(name, pinned); groupsState = GroupManager.getGroups() },
                            onUrlClick = { url ->
                                val trimmed = url.trim()
                                if (isWebUrl(trimmed)) {
                                    openUrl(trimmed)
                                } else {
                                    val session = selectedSession
                                    if (session != null) {
                                        val trustedRoot = try {
                                            if (!session.cwd.isNullOrBlank()) Paths.get(session.cwd) else null
                                        } catch (_: Exception) {
                                            null
                                        }
                                        val baseDirectory = try {
                                            if (!session.cwd.isNullOrBlank()) {
                                                Paths.get(session.cwd)
                                            } else {
                                                val p = Paths.get(session.filePath)
                                                if (session.filePath.startsWith("composerData:") || !p.isAbsolute) null else p.parent
                                            }
                                        } catch (_: Exception) {
                                            null
                                        }

                                        when (val res = LocalFileResolver.resolveLocalFileLink(trimmed, baseDirectory, trustedRoot)) {
                                            is LocalFileResolution.Allowed -> {
                                                activeFileToView = res.path.toString()
                                            }
                                            is LocalFileResolution.ConfirmationRequired -> {
                                                val pathStr = res.path.toString()
                                                val decision = PermissionManager.getDecision(pathStr, PermissionManager.Action.PREVIEW)
                                                if (decision == PermissionManager.Decision.ALLOW) {
                                                    activeFileToView = pathStr
                                                } else if (decision == PermissionManager.Decision.DENY) {
                                                    mainToastMessage = "Access denied by user settings"
                                                } else {
                                                    pendingMainUrlClickPath = res.path
                                                    mainDontAskAgainChecked = false
                                                }
                                            }
                                            is LocalFileResolution.Rejected -> {
                                                mainToastMessage = res.reason
                                            }
                                        }
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
                        },
                        onUpdateAvailable = { release ->
                            latestReleaseForUpdate = release
                            showUpdateDialog = true
                        }
                    )
                }
                if (showUpdateDialog && latestReleaseForUpdate != null) {
                    UpdateDialog(
                        latestRelease = latestReleaseForUpdate!!,
                        onClose = {
                            showUpdateDialog = false
                            latestReleaseForUpdate = null
                        }
                    )
                }
                if (activeFileToView != null) {
                    FileViewerDialog(
                        filePath = activeFileToView!!,
                        trustedRootPath = selectedSession?.cwd,
                        onClose = { activeFileToView = null },
                        onUrlClick = { url ->
                            val trimmed = url.trim()
                            if (isWebUrl(trimmed)) {
                                openUrl(trimmed)
                            } else {
                                activeFileToView = trimmed
                            }
                        }
                    )
                }

                // Styled Main Toast Overlay
                AnimatedVisibility(
                    visible = mainToastMessage != null,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = mainToastMessage ?: "",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                modifier = Modifier.offset(y = 1.dp)
                            )
                        }
                    }
                }

                // Main Preview Permission Modal
                val pendingPath = pendingMainUrlClickPath
                if (pendingPath != null) {
                    val canonicalStr = pendingPath.toString()
                    AlertDialog(
                        onDismissRequest = { pendingMainUrlClickPath = null },
                        title = {
                            Text(
                                text = "Permission Required",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "This file lies outside your active session workspace. Do you want to preview it?",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = canonicalStr,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable { mainDontAskAgainChecked = !mainDontAskAgainChecked }
                                ) {
                                    Checkbox(
                                        checked = mainDontAskAgainChecked,
                                        onCheckedChange = { mainDontAskAgainChecked = it },
                                        colors = CheckboxDefaults.colors(checkedColor = AccentCyan, uncheckedColor = BorderColor)
                                    )
                                    Text(
                                        text = "Don't ask again",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.offset(y = (-0.5).dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val pathStr = pendingPath.toString()
                                    if (mainDontAskAgainChecked) {
                                        PermissionManager.setDecision(pathStr, PermissionManager.Action.PREVIEW, PermissionManager.Decision.ALLOW)
                                    }
                                    activeFileToView = pathStr
                                    pendingMainUrlClickPath = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg)
                            ) {
                                Text("Allow Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    val pathStr = pendingPath.toString()
                                    if (mainDontAskAgainChecked) {
                                        PermissionManager.setDecision(pathStr, PermissionManager.Action.PREVIEW, PermissionManager.Decision.DENY)
                                    }
                                    pendingMainUrlClickPath = null
                                }
                            ) {
                                Text("Block", color = Color(0xFFEF5350), fontSize = 12.sp)
                            }
                        },
                        containerColor = SlateSurface,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Floating Drag & Drop Previews
                if (dragDropState.draggingSession != null) {
                    val session = dragDropState.draggingSession!!
                    val isPartOfSelection = selectedSessionIds.contains(session.id)
                    val count = if (isPartOfSelection) selectedSessionIds.size else 1
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { dragDropState.dragPosition.x.toDp() } - 100.dp,
                                y = with(LocalDensity.current) { dragDropState.dragPosition.y.toDp() } - 20.dp
                            )
                            .width(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardSurface.copy(alpha = 0.85f))
                            .border(1.5.dp, AccentCyan, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (count > 1) "Moving $count conversations" else (session.threadName ?: "Dialogue Session"),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (dragDropState.draggingTag != null) {
                    val (_, tagName) = dragDropState.draggingTag!!
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { dragDropState.dragPosition.x.toDp() } - 50.dp,
                                y = with(LocalDensity.current) { dragDropState.dragPosition.y.toDp() } - 15.dp
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentCyan.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tagName,
                            color = ObsidianBg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private suspend fun getSessionIdsForGroupAndDescendants(
    groupName: String?,
    groups: List<ConversationGroup>,
    engine: SearchEngine
): Set<String>? {
    if (groupName == null) return null
    if (groupName == "_none_") {
        val assigned = groups.flatMap { it.sessionIds }.toSet()
        val allSessionIds = try {
            engine.search("", SearchFilter(archivalFilter = ArchivalFilter.ALL))
                .map { it.session.id }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
        return allSessionIds - assigned
    }
    val targetName = groupName.lowercase()
    val targetPrefix = "$targetName/"
    val matchingGroups = groups.filter {
        val nameLower = it.name.lowercase()
        nameLower == targetName || nameLower.startsWith(targetPrefix)
    }
    return matchingGroups.flatMap { it.sessionIds }.toSet()
}

