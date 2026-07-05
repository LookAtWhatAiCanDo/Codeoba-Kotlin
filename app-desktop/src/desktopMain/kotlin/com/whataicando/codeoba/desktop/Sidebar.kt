package com.whataicando.codeoba.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whataicando.codeoba.core.domain.model.ConversationGroup
import com.whataicando.codeoba.core.domain.model.Session
import com.whataicando.codeoba.core.domain.search.ArchivalFilter
import com.whataicando.codeoba.core.domain.search.SearchResult
import com.whataicando.codeoba.core.domain.source.SourceRegistry
import com.whataicando.codeoba.core.util.PlatformUtils
import com.whataicando.codeoba.desktop.provider.*

@Composable
fun Sidebar(
    queryValue: TextFieldValue,
    onQueryValueChange: (TextFieldValue) -> Unit,
    matchCase: Boolean,
    onMatchCaseChange: (Boolean) -> Unit,
    wholeWord: Boolean,
    onWholeWordChange: (Boolean) -> Unit,
    useRegex: Boolean,
    onUseRegexChange: (Boolean) -> Unit,
    searchMode: SearchMode,
    onSearchModeChange: (SearchMode) -> Unit,
    activeFilters: MutableList<String>,
    sourceRegistry: SourceRegistry,
    searchResults: List<SearchResult>,
    selectedSession: Session?,
    selectedSessionIds: Set<String> = emptySet(),
    selectionAnchorId: String? = null,
    onSelectionChange: (Session?, Set<String>, String?) -> Unit = { _, _, _ -> },
    isIndexing: Boolean,
    indexingProgressText: String = "",
    ignoredSources: Set<String>,
    activeStatusFilters: Set<ArchivalFilter>,
    onStatusFilterToggle: (ArchivalFilter) -> Unit,
    pinnedSessionIds: Set<String>,
    onTogglePin: (Session) -> Unit,
    groups: List<ConversationGroup>,
    activeGroupFilter: String?,
    onActiveGroupFilterChange: (String?) -> Unit,
    onAddGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onToggleGroupPin: (String, Boolean) -> Unit,
    onGroupAdd: (Session, String) -> Unit = { _, _ -> },
    onGroupRemove: (Session, String) -> Unit = { _, _ -> },
    dragDropState: DragDropState = remember { DragDropState() },
    unassignedSessionCount: Int = 0,
    statsProvider: WorkspaceStatsProvider = rememberWorkspaceStatsProvider(searchResults),
    isModelDownloaded: Boolean = true,
    isModelDownloading: Boolean = false,
    modelDownloadProgress: Float = 0f,
    modelDownloadError: String? = null,
    onDownloadModel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var toastMessage by remember { mutableStateOf<String?>(null) }

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showRenameGroupDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf<String?>(null) }
    var groupInputText by remember { mutableStateOf("") }

    var isGroupsSectionExpanded by remember { mutableStateOf(true) }

    val groupTree = remember(groups, pinnedSessionIds) {
        buildGroupTree(groups, pinnedSessionIds)
    }

    var sortBy by remember { mutableStateOf(SettingsManager.getSidebarSortBy()) }
    var sortAscending by remember { mutableStateOf(SettingsManager.getSidebarSortAscending()) }

    LaunchedEffect(sortBy, sortAscending) {
        SettingsManager.setSidebarSortBy(sortBy)
        SettingsManager.setSidebarSortAscending(sortAscending)
    }

    val availableDimensions = remember(queryValue.text) {
        if (queryValue.text.isNotEmpty()) {
            SidebarSortDimension.entries
        } else {
            SidebarSortDimension.entries.filter { it != SidebarSortDimension.RELEVANCE }
        }
    }

    val effectiveSortBy = remember(sortBy, queryValue.text) {
        if (sortBy == SidebarSortDimension.RELEVANCE && queryValue.text.isEmpty()) {
            SidebarSortDimension.UPDATED
        } else {
            sortBy
        }
    }

    val sortedSearchResults = remember(searchResults, effectiveSortBy, sortAscending, pinnedSessionIds) {
        val mappedResults = searchResults.map { result ->
            val pinned = pinnedSessionIds.contains(result.session.id)
            if (result.session.isPinned != pinned) {
                result.copy(session = result.session.copy(isPinned = pinned))
            } else {
                result
            }
        }

        val baseSorted = if (effectiveSortBy == SidebarSortDimension.RELEVANCE) {
            if (sortAscending) {
                mappedResults.sortedWith(
                    compareBy<SearchResult> { it.score }
                        .thenBy { it.session.updatedAt }
                )
            } else {
                mappedResults.sortedWith(
                    compareByDescending<SearchResult> { it.score }
                        .thenByDescending { it.session.updatedAt }
                )
            }
        } else {
            val comparator = when (effectiveSortBy) {
                SidebarSortDimension.UPDATED -> compareBy<SearchResult> { it.session.updatedAt }
                SidebarSortDimension.TOKENS -> compareBy<SearchResult> {
                    val charCount = it.session.turns.sumOf { turn -> turn.userMessage.length + turn.assistantMessage.length }
                    charCount / 4
                }
                SidebarSortDimension.SPEED -> compareBy<SearchResult> {
                    val charCount = it.session.turns.sumOf { turn -> turn.userMessage.length + turn.assistantMessage.length }
                    val estTokens = charCount / 4
                    val durationMs = getSessionComputeTimeMs(it.session)
                    if (durationMs > 0) {
                        (estTokens.toDouble() * 1000.0) / durationMs
                    } else 0.0
                }
                SidebarSortDimension.TURNS -> compareBy<SearchResult> { it.session.turns.size }
                SidebarSortDimension.DURATION -> compareBy<SearchResult> { getSessionComputeTimeMs(it.session) }
                else -> compareBy<SearchResult> { it.session.updatedAt }
            }
            if (sortAscending) {
                mappedResults.sortedWith(comparator)
            } else {
                mappedResults.sortedWith(comparator.reversed())
            }
        }

        baseSorted.sortedByDescending { it.session.isPinned }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            toastMessage = null
        }
    }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateSurface)
        ) {
        // Sidebar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .drawBehind {
                    drawLine(
                        color = BorderColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isMac = remember { PlatformUtils.isMac() }
            if (isMac) {
                Spacer(modifier = Modifier.width(76.dp)) // clear macOS traffic lights
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
        ) {
            // Search Box
            val focusRequester = remember { FocusRequester() }
            OutlinedTextField(
                value = queryValue,
                onValueChange = onQueryValueChange,
                placeholder = { Text("Search sessions...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan) },
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FindModifierButton("Cc", active = matchCase, onClick = { onMatchCaseChange(!matchCase) }, tooltip = "Match Case")
                        FindModifierButton("W", active = wholeWord, onClick = { onWholeWordChange(!wholeWord) }, tooltip = "Match Whole Word")
                        FindModifierButton(".*", active = useRegex, onClick = { onUseRegexChange(!useRegex) }, tooltip = "Use Regular Expression")
                        FindActionButton("\\n", onClick = {
                            val currentText = queryValue.text
                            val sel = queryValue.selection
                            val s = sel.min
                            val e = sel.max
                            val newText = currentText.substring(0, s) + "\n" + currentText.substring(e)
                            onQueryValueChange(queryValue.copy(text = newText, selection = TextRange(s + 1)))
                        }, tooltip = "Insert Line Feed")
                        if (queryValue.text.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryValueChange(TextFieldValue("")) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                },
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val isEnter = keyEvent.key == Key.Enter
                            if (isEnter) {
                                val isAltPressed = keyEvent.isAltPressed
                                val isCtrlPressed = keyEvent.isCtrlPressed
                                if (isAltPressed || isCtrlPressed) {
                                    val currentText = queryValue.text
                                    val sel = queryValue.selection
                                    val s = sel.min
                                    val e = sel.max
                                    val newText = currentText.substring(0, s) + "\n" + currentText.substring(e)
                                    val newSelection = TextRange(s + 1)
                                    onQueryValueChange(queryValue.copy(text = newText, selection = newSelection))
                                    true
                                } else {
                                    true // Consume Enter to prevent standard newline insertion in search field
                                }
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Search Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardSurface)
                    .padding(4.dp)
            ) {
                SearchMode.entries.forEach { mode ->
                    val selected = searchMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) SlateSurface else Color.Transparent)
                            .clickable { onSearchModeChange(mode) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            color = if (selected) AccentCyan else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Status Filters
            Text(
                text = "Filter by Status",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active Chip
                val isActiveSelected = activeStatusFilters.contains(ArchivalFilter.ACTIVE)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActiveSelected) AccentCyan.copy(alpha = 0.15f) else CardSurface)
                        .border(
                            width = 1.dp,
                            color = if (isActiveSelected) AccentCyan else BorderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onStatusFilterToggle(ArchivalFilter.ACTIVE)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Active",
                        color = if (isActiveSelected) AccentCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 12.sp,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }

                // Archived Chip
                val isArchivedSelected = activeStatusFilters.contains(ArchivalFilter.ARCHIVED)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isArchivedSelected) AccentPurple.copy(alpha = 0.15f) else CardSurface)
                        .border(
                            width = 1.dp,
                            color = if (isArchivedSelected) AccentPurple else BorderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onStatusFilterToggle(ArchivalFilter.ARCHIVED)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Archived",
                        color = if (isArchivedSelected) AccentPurple else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 12.sp,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            }

            val adapters = sourceRegistry.getAllAdapters().filter { adapter ->
                adapter.id !in ignoredSources
            }

            AnimatedVisibility(
                visible = adapters.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Filters Title
                    Text(
                        text = "Filter by Source",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    // Source Filters Column with Scrollbar
                    val sourceScrollState = rememberScrollState()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .dragToScroll(sourceScrollState, Orientation.Horizontal)
                                .horizontalScroll(sourceScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            adapters.forEach { adapter ->
                                val isSelected = activeFilters.contains(adapter.id)
                                val badgeColors = getSourceBadgeColors(adapter.id)

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) badgeColors.second else CardSurface)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) badgeColors.first else BorderColor,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (isSelected) activeFilters.remove(adapter.id)
                                            else activeFilters.add(adapter.id)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = adapter.displayName,
                                        color = if (isSelected) badgeColors.first else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                        if (sourceScrollState.maxValue > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            HorizontalScrollbar(
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                adapter = rememberScrollbarAdapter(scrollState = sourceScrollState),
                                style = defaultScrollbarStyle().copy(
                                    unhoverColor = AccentCyan.copy(alpha = 0.2f),
                                    hoverColor = AccentCyan.copy(alpha = 0.6f),
                                    thickness = 4.dp
                                )
                            )
                        }
                    }
                }
            }

            // Group Filters
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isGroupsSectionExpanded = !isGroupsSectionExpanded }
                ) {
                    Icon(
                        imageVector = if (isGroupsSectionExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Filter by Group",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
                IconButton(
                    onClick = {
                        groupInputText = ""
                        showCreateGroupDialog = true
                    },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Group",
                        tint = AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isGroupsSectionExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (groupTree.isEmpty()) {
                        Text(
                            text = "No groups defined. Click + to add.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                        )
                    } else {
                        // Unassigned / No Group filter row
                        val isNoGroupSelected = activeGroupFilter == "_none_"
                        val isNoGroupHovered = dragDropState.hoveredGroupByName == "_none_"
                        
                        DisposableEffect(Unit) {
                            onDispose {
                                dragDropState.dropTargetBounds.remove("_none_")
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isNoGroupSelected) CardSurface 
                                    else if (isNoGroupHovered) AccentCyan.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isNoGroupSelected) AccentCyan 
                                            else if (isNoGroupHovered) AccentCyan
                                            else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .onGloballyPositioned { coords ->
                                    dragDropState.dropTargetBounds["_none_"] = coords.boundsInRoot()
                                }
                                .clickable {
                                    if (isNoGroupSelected) {
                                        onActiveGroupFilterChange(null)
                                    } else {
                                        onActiveGroupFilterChange("_none_")
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(16.dp)) // Identical alignment spacing (no expand arrow)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isNoGroupSelected) AccentCyan else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[No Group]",
                                color = if (isNoGroupSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isNoGroupSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).offset(y = 1.dp)
                            )
                            
                            // Unassigned Session Count Badge
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BorderColor)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = formatNumber(statsProvider.getGroupSessionCount("_none_", unassignedSessionCount).toLong()),
                                    color = AccentCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 9.sp,
                                    modifier = Modifier.offset(y = 0.5.dp)
                                )
                            }
                        }

                        groupTree.forEach { rootNode ->
                            GroupTreeItem(
                                node = rootNode,
                                depth = 0,
                                activeGroupFilter = activeGroupFilter,
                                onSelect = onActiveGroupFilterChange,
                                onRename = onRenameGroup,
                                onDelete = { showDeleteGroupDialog = it },
                                onTogglePin = onToggleGroupPin,
                                onShowRenameDialog = { old ->
                                    groupInputText = old
                                    showRenameGroupDialog = old
                                },
                                dragDropState = dragDropState,
                                statsProvider = statsProvider
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sort Section
            Text(
                text = "Sort by",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            val sortScrollState = rememberScrollState()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragToScroll(sortScrollState, Orientation.Horizontal)
                        .horizontalScroll(sortScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableDimensions.forEach { dimension ->
                        val isSelected = effectiveSortBy == dimension
                        val arrowIcon = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else CardSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentCyan else BorderColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (sortBy == dimension) {
                                        sortAscending = !sortAscending
                                    } else {
                                        sortBy = dimension
                                        sortAscending = false
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = dimension.displayName,
                                    color = if (isSelected) AccentCyan else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 12.sp,
                                    modifier = Modifier.offset(y = 1.dp)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = arrowIcon,
                                        contentDescription = if (sortAscending) "Sorted Ascending" else "Sorted Descending",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (sortScrollState.maxValue > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    HorizontalScrollbar(
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        adapter = rememberScrollbarAdapter(scrollState = sortScrollState),
                        style = defaultScrollbarStyle().copy(
                            unhoverColor = AccentCyan.copy(alpha = 0.2f),
                            hoverColor = AccentCyan.copy(alpha = 0.6f),
                            thickness = 4.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Indexing Progress or Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Conversations",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                // Premium number badge showing count of conversations
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BorderColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatNumber(statsProvider.totalConversations.toLong()),
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isIndexing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (indexingProgressText.isNotEmpty()) {
                            Text(
                                text = indexingProgressText,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.offset(y = 0.5.dp)
                            )
                        }
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = AccentCyan
                        )
                    }
                }
            }

            // Sessions List
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (searchMode == SearchMode.Semantic && !isModelDownloaded) {
                    // Show Semantic Search Model Download Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Semantic Model Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "To perform concept-matching search, Codeoba needs to download a lightweight, quantized embedding model (~23 MB) from Hugging Face. This will run 100% locally on your machine once downloaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        if (isModelDownloading) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = { modelDownloadProgress },
                                    color = AccentCyan,
                                    trackColor = SlateSurface,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = String.format("Downloading... %.0f%%", modelDownloadProgress * 100f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        } else {
                            if (modelDownloadError != null) {
                                Text(
                                    text = "Error: $modelDownloadError",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onDownloadModel,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Download Model (~23MB)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }

                                Button(
                                    onClick = { onSearchModeChange(SearchMode.Lexical) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateSurface, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isIndexing) {
                                if (indexingProgressText.isNotEmpty()) indexingProgressText else "Indexing logs..."
                            } else "No conversations found.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .dragToScroll(scrollState)
                            .verticalScroll(scrollState)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sortedSearchResults.forEach { result ->
                            SessionItem(
                                result = result,
                                isSelected = selectedSessionIds.contains(result.session.id),
                                onClick = { isShift, isCmdOrCtrl ->
                                    val clickedSession = result.session
                                    val clickedId = clickedSession.id
                                    val currentIds = selectedSessionIds
                                    
                                    when {
                                        isShift -> {
                                            val anchorId = selectionAnchorId ?: selectedSession?.id ?: clickedId
                                            val allIds = sortedSearchResults.map { it.session.id }
                                            val anchorIndex = allIds.indexOf(anchorId).coerceAtLeast(0)
                                            val clickedIndex = allIds.indexOf(clickedId).coerceAtLeast(0)
                                            
                                            val start = minOf(anchorIndex, clickedIndex)
                                            val end = maxOf(anchorIndex, clickedIndex)
                                            val rangeIds = allIds.subList(start, end + 1).toSet()
                                            
                                            onSelectionChange(clickedSession, rangeIds, anchorId)
                                        }
                                        isCmdOrCtrl -> {
                                            val newIds = if (currentIds.contains(clickedId)) {
                                                currentIds - clickedId
                                            } else {
                                                currentIds + clickedId
                                            }
                                            
                                            val newActiveSession = if (newIds.contains(clickedId)) {
                                                clickedSession
                                            } else {
                                                val remainingId = newIds.lastOrNull()
                                                if (remainingId != null) {
                                                    searchResults.firstOrNull { it.session.id == remainingId }?.session
                                                } else {
                                                    null
                                                }
                                            }
                                            
                                            val newAnchor = if (newIds.contains(clickedId)) clickedId else selectionAnchorId
                                            onSelectionChange(newActiveSession, newIds, newAnchor)
                                        }
                                        else -> {
                                            onSelectionChange(clickedSession, setOf(clickedId), clickedId)
                                        }
                                    }
                                },
                                onCopyPath = { path ->
                                    copyToClipboard(path)
                                    toastMessage = "Source file path copied to clipboard"
                                },
                                onTogglePin = onTogglePin,
                                dragDropState = dragDropState,
                                onGroupAdd = onGroupAdd,
                                onGroupRemove = onGroupRemove,
                                activeGroupFilter = activeGroupFilter,
                                groups = groups,
                                selectedSessionIds = selectedSessionIds,
                                onSelectionChange = onSelectionChange
                            )
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp),
                        adapter = rememberScrollbarAdapter(scrollState = scrollState),
                        style = defaultScrollbarStyle().copy(
                            unhoverColor = AccentCyan.copy(alpha = 0.4f),
                            hoverColor = AccentCyan.copy(alpha = 0.8f),
                            thickness = 8.dp
                        )
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = toastMessage != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
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
                    Icons.Default.Check,
                    contentDescription = "Success",
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = toastMessage ?: "",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier.offset(y = 1.dp)
                )
            }
        }

        // Slide-up Drop Zone for removing tags/trash
        AnimatedVisibility(
            visible = dragDropState.draggingSession != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            val isTrashActive = dragDropState.isHoveringRemoveZone
            val activeFilter = activeGroupFilter
            val label = if (activeFilter != null && activeFilter != "_none_") {
                "Remove from '$activeFilter'"
            } else {
                "Clear all tags"
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isTrashActive) Color(0xFF3A1E1E) else Color(0xFF231818)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isTrashActive) Color(0xFFE57373) else Color(0xFFC62828),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .onGloballyPositioned { coords ->
                        dragDropState.removeZoneBounds = coords.boundsInRoot()
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove tag",
                        tint = if (isTrashActive) Color(0xFFFF8A80) else Color(0xFFEF5350),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = label,
                        color = if (isTrashActive) Color(0xFFF5F5F5) else Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        val createFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create New Group", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = groupInputText,
                    onValueChange = { groupInputText = it },
                    placeholder = { 
                        Text(
                            "e.g. Project/Area", 
                            color = TextSecondary.copy(alpha = 0.5f), 
                            fontSize = 12.sp,
                            style = TextStyle.Default,
                            lineHeight = 12.sp
                        ) 
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(createFocusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                if (groupInputText.isNotBlank()) {
                                    onAddGroup(groupInputText.trim())
                                }
                                groupInputText = ""
                                showCreateGroupDialog = false
                                true
                            } else {
                                false
                            }
                        }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupInputText.isNotBlank()) {
                            onAddGroup(groupInputText.trim())
                        }
                        groupInputText = ""
                        showCreateGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg)
                ) {
                    Text("Create", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 1.dp))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        groupInputText = ""
                        showCreateGroupDialog = false
                    }
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.offset(y = 1.dp))
                }
            },
            containerColor = SlateSurface,
            shape = RoundedCornerShape(12.dp)
        )
        LaunchedEffect(Unit) {
            createFocusRequester.requestFocus()
        }
    }

    val renameTarget = showRenameGroupDialog
    if (renameTarget != null) {
        val renameFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { showRenameGroupDialog = null },
            title = { Text("Rename Group", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = groupInputText,
                    onValueChange = { groupInputText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(renameFocusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                if (groupInputText.isNotBlank()) {
                                    onRenameGroup(renameTarget, groupInputText.trim())
                                }
                                groupInputText = ""
                                showRenameGroupDialog = null
                                true
                            } else {
                                false
                            }
                        }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupInputText.isNotBlank()) {
                            onRenameGroup(renameTarget, groupInputText.trim())
                        }
                        groupInputText = ""
                        showRenameGroupDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg)
                ) {
                    Text("Rename", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 1.dp))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        groupInputText = ""
                        showRenameGroupDialog = null
                    }
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.offset(y = 1.dp))
                }
            },
            containerColor = SlateSurface,
            shape = RoundedCornerShape(12.dp)
        )
        LaunchedEffect(Unit) {
            renameFocusRequester.requestFocus()
        }
    }

    val deleteTarget = showDeleteGroupDialog
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = null },
            title = { Text("Delete Group", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete '$deleteTarget'? Conversations will not be deleted, but this tag will be removed.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(deleteTarget)
                        showDeleteGroupDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg)
                ) {
                    Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 1.dp))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteGroupDialog = null
                    }
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.offset(y = 1.dp))
                }
            },
            containerColor = SlateSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SessionItem(
    result: SearchResult,
    isSelected: Boolean,
    onClick: (isShift: Boolean, isCmdOrCtrl: Boolean) -> Unit,
    onCopyPath: (String) -> Unit,
    onTogglePin: (Session) -> Unit,
    dragDropState: DragDropState,
    onGroupAdd: (Session, String) -> Unit,
    onGroupRemove: (Session, String) -> Unit,
    activeGroupFilter: String?,
    groups: List<ConversationGroup>,
    selectedSessionIds: Set<String> = emptySet(),
    onSelectionChange: (Session?, Set<String>, String?) -> Unit = { _, _, _ -> }
) {
    val session = result.session
    val sessionGroups = remember(groups, session.id) {
        groups.filter { it.sessionIds.contains(session.id) }
    }
    val currentSessionGroups by rememberUpdatedState(sessionGroups)
    val currentActiveGroupFilter by rememberUpdatedState(activeGroupFilter)
    val currentOnGroupRemove by rememberUpdatedState(onGroupRemove)
    val currentOnGroupAdd by rememberUpdatedState(onGroupAdd)
    val currentSelectedSessionIds by rememberUpdatedState(selectedSessionIds)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)

    val badgeColors = getSourceBadgeColors(session.sourceId)
    val formatter = remember { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT) }
    val formattedDate = remember(session.updatedAt) { formatter.format(java.util.Date(session.updatedAt)) }

    val lastMsg = session.turns.lastOrNull()?.userMessage ?: ""

    val alpha = if (session.isArchived) 0.5f else 1.0f

    var showContextMenu by remember { mutableStateOf(false) }
    var showGroupsSubmenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    val keyboardModifiers = LocalWindowInfo.current.keyboardModifiers
    val isShift = keyboardModifiers.isShiftPressed
    val isCmdOrCtrl = keyboardModifiers.isMetaPressed || keyboardModifiers.isCtrlPressed

    var itemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CardSurface else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentCyan else BorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .alpha(alpha)
            .onGloballyPositioned { itemCoords = it }
            .clickable { onClick(isShift, isCmdOrCtrl) }
            .pointerInput(session.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                val position = change.position
                                pressOffset = with(density) {
                                    DpOffset(position.x.toDp(), position.y.toDp())
                                }
                            }
                            showContextMenu = true
                            showGroupsSubmenu = false
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
            .pointerInput(session.id) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (!currentSelectedSessionIds.contains(session.id)) {
                            currentOnSelectionChange(session, setOf(session.id), session.id)
                        }
                        val rootOffset = itemCoords?.localToRoot(offset) ?: Offset.Zero
                        dragDropState.draggingSession = session
                        dragDropState.dragPosition = rootOffset
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDropState.updateDragPosition(dragDropState.dragPosition + dragAmount)
                    },
                    onDragEnd = {
                        val activeGroup = dragDropState.hoveredGroupByName
                        val isTrash = dragDropState.isHoveringRemoveZone
                        if (activeGroup != null) {
                            if (activeGroup == "_none_") {
                                currentSessionGroups.forEach { group ->
                                    currentOnGroupRemove(session, group.name)
                                }
                            } else {
                                currentOnGroupAdd(session, activeGroup)
                            }
                        } else if (isTrash) {
                            val activeFilter = currentActiveGroupFilter
                            if (activeFilter != null && activeFilter != "_none_") {
                                currentOnGroupRemove(session, activeFilter)
                            } else {
                                currentSessionGroups.forEach { group ->
                                    currentOnGroupRemove(session, group.name)
                                }
                            }
                        }
                        dragDropState.reset()
                    },
                    onDragCancel = {
                        dragDropState.reset()
                    }
                )
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Source badge and archived label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColors.second)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatSourceDisplayName(session.sourceId),
                            color = badgeColors.first,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 9.sp
                        )
                    }

                    if (session.isArchived) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BorderColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Archived",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 9.sp
                            )
                        }
                    }

                }
                // Date and Pin button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedDate,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onTogglePin(session) }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (session.isPinned) "Unpin Conversation" else "Pin Conversation",
                            tint = if (session.isPinned) AccentCyan else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Thread Name
            Text(
                text = session.threadName ?: "Untitled Session",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val models = session.turns.mapNotNull { it.extraData["model"] }.distinct()
            val durationMs = getSessionComputeTimeMs(session)
            val charCount = session.turns.sumOf { it.userMessage.length + it.assistantMessage.length }
            val estTokens = charCount / 4
            val turnsCount = session.turns.size

            if (models.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = models.joinToString(", "),
                        color = AccentCyan.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (durationMs > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = "Token speed",
                                tint = AccentCyan,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = formatSpeed(estTokens.toLong(), durationMs),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }

            if (lastMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                // Last message snippet
                Text(
                    text = lastMsg,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (sessionGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sessionGroups.forEach { group ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentPurple.copy(alpha = 0.12f))
                                .border(1.dp, AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = group.name,
                                color = AccentPurple,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 9.sp,
                                modifier = Modifier.offset(y = 0.5.dp)
                            )
                        }
                    }
                }
            }

            val cwd = session.cwd

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Folder path (CWD) if available
                if (cwd != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = cwd.substringAfterLast("/"),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Duration Stat
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = formatDuration(durationMs),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }

                // Turns Stat (e.g. "5 turns")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "$turnsCount ${if (turnsCount == 1) "turn" else "turns"}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }

                // Est Tokens Stat
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "${formatNumber(estTokens.toLong())} tkn",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { 
                showContextMenu = false
                showGroupsSubmenu = false
            },
            offset = pressOffset,
            modifier = Modifier
                .width(220.dp)
                .background(CardSurface)
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
        ) {
            if (showGroupsSubmenu) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Back to Actions", color = TextSecondary, fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        showGroupsSubmenu = false
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                // Search / Create Tag Field
                var newTagName by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { 
                        Text(
                            "Filter or create tag...", 
                            color = TextSecondary.copy(alpha = 0.5f), 
                            fontSize = 11.sp,
                            style = androidx.compose.ui.text.TextStyle.Default,
                            lineHeight = 11.sp
                        ) 
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                val trimmed = newTagName.trim()
                                if (trimmed.isNotBlank()) {
                                    val exists = groups.any { it.name.equals(trimmed, ignoreCase = true) }
                                    if (!exists) {
                                        onGroupAdd(session, trimmed)
                                    } else {
                                        val alreadyInGroup = sessionGroups.any { 
                                            it.name.equals(trimmed, ignoreCase = true)
                                        }
                                        if (!alreadyInGroup) {
                                            onGroupAdd(session, trimmed)
                                        }
                                    }
                                    newTagName = ""
                                }
                                true
                            } else {
                                false
                            }
                        }
                )
                
                LaunchedEffect(showGroupsSubmenu) {
                    if (showGroupsSubmenu) {
                        focusRequester.requestFocus()
                    }
                }

                // Option to create a new tag if it doesn't exist yet and input is not empty
                if (newTagName.isNotBlank()) {
                    val exactMatchExists = groups.any { it.name.equals(newTagName.trim(), ignoreCase = true) }
                    if (!exactMatchExists) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Create tag \"${newTagName.trim()}\"", 
                                    color = AccentCyan, 
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            onClick = {
                                onGroupAdd(session, newTagName.trim())
                                newTagName = ""
                            }
                        )
                    }
                }

                val filteredGroups = remember(groups, newTagName) {
                    groups.filter { group ->
                        group.name.contains(newTagName, ignoreCase = true)
                    }
                }

                if (filteredGroups.isNotEmpty()) {
                    filteredGroups.forEach { group ->
                        val inGroup = sessionGroups.any { it.name == group.name }
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(group.name, color = TextPrimary, fontSize = 12.sp)
                                    if (inGroup) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Assigned",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (inGroup) {
                                    onGroupRemove(session, group.name)
                                } else {
                                    onGroupAdd(session, group.name)
                                }
                            }
                        )
                    }
                } else if (newTagName.isBlank()) {
                    DropdownMenuItem(
                        enabled = false,
                        text = { Text("No tags available", color = TextSecondary, fontSize = 11.sp) },
                        onClick = {}
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text(if (session.isPinned) "Unpin Conversation" else "Pin Conversation", color = TextPrimary, fontSize = 13.sp) },
                    onClick = {
                        showContextMenu = false
                        onTogglePin(session)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Groups / Tags", color = TextPrimary, fontSize = 13.sp) },
                    onClick = {
                        showGroupsSubmenu = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open Session File", color = TextPrimary, fontSize = 13.sp) },
                    onClick = {
                        showContextMenu = false
                        try {
                            val file = java.io.File(session.filePath)
                            if (file.exists()) {
                                java.awt.Desktop.getDesktop().open(file)
                            }
                        } catch (_: Exception) {}
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Session ID", color = TextPrimary, fontSize = 13.sp) },
                    onClick = {
                        showContextMenu = false
                        copyToClipboard(session.id)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy File Path", color = TextPrimary, fontSize = 13.sp) },
                    onClick = {
                        showContextMenu = false
                        onCopyPath(session.filePath)
                    }
                )
            }
        }
    }
}

enum class SidebarSortDimension(val displayName: String) {
    RELEVANCE("Relevance"),
    UPDATED("Updated"),
    TOKENS("Tokens"),
    SPEED("Speed"),
    TURNS("Turns"),
    DURATION("Duration")
}

class GroupTreeNode(
    val segment: String,
    val fullName: String,
    val children: MutableList<GroupTreeNode> = mutableListOf(),
    var isPinned: Boolean = false,
    var directSessionCount: Int = 0,
    var recursiveSessionCount: Int = 0,
    var containsPinnedSessions: Boolean = false
)

fun buildGroupTree(
    groups: List<ConversationGroup>,
    pinnedSessionIds: Set<String>
): List<GroupTreeNode> {
    val rootNodes = mutableListOf<GroupTreeNode>()

    for (group in groups) {
        val parts = group.name.split("/")
        var currentLevel = rootNodes
        var currentFullName = ""

        for (i in parts.indices) {
            val part = parts[i]
            currentFullName = if (currentFullName.isEmpty()) part else "$currentFullName/$part"
            
            var node = currentLevel.find { it.segment.lowercase() == part.lowercase() }
            if (node == null) {
                node = GroupTreeNode(segment = part, fullName = currentFullName)
                currentLevel.add(node)
            }
            
            if (i == parts.lastIndex) {
                node.isPinned = group.isPinned
                node.directSessionCount = group.sessionIds.size
                node.containsPinnedSessions = group.sessionIds.any { pinnedSessionIds.contains(it) }
            }
            currentLevel = node.children
        }
    }

    fun finalizeNode(node: GroupTreeNode): Pair<Int, Boolean> {
        var childSessionsCount = 0
        var childHasPinnedSessions = false

        for (child in node.children) {
            val (cCount, cPinned) = finalizeNode(child)
            childSessionsCount += cCount
            if (cPinned) {
                childHasPinnedSessions = true
            }
        }

        node.recursiveSessionCount = node.directSessionCount + childSessionsCount
        node.containsPinnedSessions = node.containsPinnedSessions || childHasPinnedSessions

        node.children.sortWith(
            compareByDescending<GroupTreeNode> { it.isPinned }
                .thenBy { it.segment.lowercase() }
        )

        return Pair(node.recursiveSessionCount, node.containsPinnedSessions)
    }

    for (root in rootNodes) {
        finalizeNode(root)
    }

    rootNodes.sortWith(
        compareByDescending<GroupTreeNode> { it.isPinned }
            .thenBy { it.segment.lowercase() }
    )

    return rootNodes
}

@Composable
fun GroupTreeItem(
    node: GroupTreeNode,
    depth: Int,
    activeGroupFilter: String?,
    onSelect: (String?) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onShowRenameDialog: (String) -> Unit,
    dragDropState: DragDropState,
    statsProvider: WorkspaceStatsProvider
) {
    var isExpanded by remember { mutableStateOf(true) }
    val isSelected = activeGroupFilter != null && activeGroupFilter.lowercase() == node.fullName.lowercase()
    val isHovered = dragDropState.hoveredGroupByName == node.fullName

    DisposableEffect(node.fullName) {
        onDispose {
            dragDropState.dropTargetBounds.remove(node.fullName)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ContextMenuArea(
            items = {
                listOf(
                    ContextMenuItem(if (node.isPinned) "Unpin Group" else "Pin Group") {
                        onTogglePin(node.fullName, !node.isPinned)
                    },
                    ContextMenuItem("Rename Group") {
                        onShowRenameDialog(node.fullName)
                    },
                    ContextMenuItem("Delete Group") {
                        onDelete(node.fullName)
                    }
                )
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) CardSurface 
                        else if (isHovered) AccentCyan.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AccentCyan 
                                else if (isHovered) AccentCyan
                                else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .onGloballyPositioned { coords ->
                        dragDropState.dropTargetBounds[node.fullName] = coords.boundsInRoot()
                    }
                    .clickable {
                        if (isSelected) {
                            onSelect(null)
                        } else {
                            onSelect(node.fullName)
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width((depth * 10).dp))

                if (node.children.isNotEmpty()) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isSelected) AccentCyan else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = node.segment,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).offset(y = 1.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (node.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned Group",
                            tint = AccentCyan,
                            modifier = Modifier.size(10.dp)
                        )
                    } else if (node.containsPinnedSessions) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(AccentCyan)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BorderColor)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = formatNumber(statsProvider.getGroupSessionCount(node.segment, node.recursiveSessionCount).toLong()),
                            color = AccentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 9.sp,
                            modifier = Modifier.offset(y = 0.5.dp)
                        )
                    }
                }
            }
        }

        if (node.children.isNotEmpty() && isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                node.children.forEach { child ->
                    GroupTreeItem(
                        node = child,
                        depth = depth + 1,
                        activeGroupFilter = activeGroupFilter,
                        onSelect = onSelect,
                        onRename = onRename,
                        onDelete = onDelete,
                        onTogglePin = onTogglePin,
                        onShowRenameDialog = onShowRenameDialog,
                        dragDropState = dragDropState,
                        statsProvider = statsProvider
                    )
                }
            }
        }
    }
}

