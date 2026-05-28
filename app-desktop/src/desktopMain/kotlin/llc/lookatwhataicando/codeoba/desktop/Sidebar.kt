package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.input.key.*
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.search.ArchivalFilter
import llc.lookatwhataicando.codeoba.core.domain.search.SearchResult
import llc.lookatwhataicando.codeoba.core.domain.source.SourceRegistry

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
    onSessionSelect: (Session?) -> Unit,
    isIndexing: Boolean,
    ignoredSources: Set<String>,
    activeStatusFilters: Set<ArchivalFilter>,
    onStatusFilterToggle: (ArchivalFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var toastMessage by remember { mutableStateOf<String?>(null) }

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

    val sortedSearchResults = remember(searchResults, effectiveSortBy, sortAscending) {
        if (effectiveSortBy == SidebarSortDimension.RELEVANCE) {
            if (sortAscending) {
                searchResults.sortedWith(
                    compareBy<SearchResult> { it.score }
                        .thenBy { it.session.updatedAt }
                )
            } else {
                searchResults.sortedWith(
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
                searchResults.sortedWith(comparator)
            } else {
                searchResults.sortedWith(comparator.reversed())
            }
        }
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
            Spacer(modifier = Modifier.width(76.dp)) // clear macOS traffic lights
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
                            modifier = Modifier.fillMaxWidth().horizontalScroll(sourceScrollState),
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
                    modifier = Modifier.fillMaxWidth().horizontalScroll(sortScrollState),
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
                        text = searchResults.size.toString(),
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isIndexing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = AccentCyan
                    )
                }
            }

            // Sessions List
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isIndexing) "Indexing logs..." else "No conversations found.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sortedSearchResults.forEach { result ->
                            SessionItem(
                                result = result,
                                isSelected = selectedSession?.id == result.session.id,
                                onClick = { onSessionSelect(result.session) },
                                onCopyPath = { path ->
                                    copyToClipboard(path)
                                    toastMessage = "Source file path copied to clipboard"
                                }
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
    }
}
}

@Composable
fun SessionItem(
    result: SearchResult,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCopyPath: (String) -> Unit
) {
    val session = result.session
    val badgeColors = getSourceBadgeColors(session.sourceId)
    val formatter = remember { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT) }
    val formattedDate = remember(session.updatedAt) { formatter.format(java.util.Date(session.updatedAt)) }

    val lastMsg = session.turns.lastOrNull()?.userMessage ?: ""

    val alpha = if (session.isArchived) 0.5f else 1.0f

    ContextMenuArea(items = {
        listOf(
            ContextMenuItem("Copy Source File Path") {
                onCopyPath(session.filePath)
            }
        )
    }) {
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
            .clickable { onClick() }
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
                // Date
                Text(
                    text = formattedDate,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 11.sp
                )
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
