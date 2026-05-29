package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import llc.lookatwhataicando.codeoba.core.domain.search.SearchResult
import llc.lookatwhataicando.codeoba.core.domain.search.buildFindRegex
import kotlinx.coroutines.launch
import llc.lookatwhataicando.codeoba.core.domain.model.ConversationGroup
import llc.lookatwhataicando.codeoba.core.domain.model.GroupTask
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset


@Composable
fun DetailPaneToolbar(
    session: Session?,
    isSidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onSessionSelect: (Session?) -> Unit,
    onOpenSettings: () -> Unit,
    isHeaderExpanded: Boolean,
    onToggleHeader: () -> Unit,
    onTogglePin: (Session) -> Unit,
    groups: List<ConversationGroup> = emptyList(),
    onGroupAdd: (Session, String) -> Unit = { _, _ -> },
    onGroupRemove: (Session, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showGroupsSubmenu by remember { mutableStateOf(false) }

    val workspaceName = remember(session?.cwd) {
        val cwd = session?.cwd
        if (cwd != null) {
            cwd.trimEnd('/').substringAfterLast('/')
        } else {
            "Codeoba"
        }
    }

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        Box(
            modifier = modifier
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(SlateSurface.copy(alpha = 0.90f))
                .border(1.dp, BorderColor, RoundedCornerShape(23.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Group: Sidebar Toggle, Home, Back, Forward, Refresh
                IconButton(
                    onClick = onToggleSidebar,
                    modifier = Modifier.size(28.dp)
                ) {
                    SidebarToggleIcon(tint = AccentCyan)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { onSessionSelect(null) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Go Home",
                        tint = if (session == null) AccentCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = if (canGoBack) AccentCyan else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Navigate forward",
                        tint = if (canGoForward) AccentCyan else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh index",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(BorderColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Middle Group: details with horizontal animation / static dashboard title
                if (session == null) {
                    Text(
                        text = "Workspace Statistics & Insights",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 12.dp)
                    )
                } else {
                    AnimatedVisibility(
                        visible = isHeaderExpanded,
                        enter = expandHorizontally(expandFrom = Alignment.Start),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Static workspace name (no click-to-home behavior)
                            Text(
                                text = workspaceName,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            Text(
                                text = " / ",
                                color = TextSecondary.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )

                            Text(
                                text = session.threadName ?: "Workspace Statistics & Insights",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (session.isPinned) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Source Badge
                            val badgeColors = getSourceBadgeColors(session.sourceId)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColors.second)
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = formatSourceDisplayName(session.sourceId),
                                    color = badgeColors.first,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 9.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val sdf = remember { SimpleDateFormat("MMM d, HH:mm") }
                            val formattedDate = remember(session.updatedAt) { sdf.format(Date(session.updatedAt)) }

                            Text(
                                text = formattedDate,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Box {
                                IconButton(
                                    onClick = { 
                                        showGroupsSubmenu = false
                                        showMenu = true 
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Actions",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { 
                                        showMenu = false
                                        showGroupsSubmenu = false
                                    },
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
                                                        imageVector = Icons.Default.ArrowBack,
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
                                                                val sessionGroups = groups.filter { it.sessionIds.contains(session.id) }
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

                                        val sessionGroups = remember(groups, session.id) {
                                            groups.filter { it.sessionIds.contains(session.id) }
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
                                                showMenu = false
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
                                                showMenu = false
                                                try {
                                                    val file = File(session.filePath)
                                                    if (file.exists()) {
                                                        Desktop.getDesktop().open(file)
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Copy Session ID", color = TextPrimary, fontSize = 13.sp) },
                                            onClick = {
                                                showMenu = false
                                                copyToClipboard(session.id)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Copy File Path", color = TextPrimary, fontSize = 13.sp) },
                                            onClick = {
                                                showMenu = false
                                                copyToClipboard(session.filePath)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Group: manual toggle arrow
                if (session != null) {
                    if (!isHeaderExpanded) {
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = onToggleHeader,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isHeaderExpanded) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
                            contentDescription = if (isHeaderExpanded) "Collapse details" else "Expand details",
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailHeaderCard(
    session: Session,
    groups: List<ConversationGroup>,
    onGroupAdd: (Session, String) -> Unit,
    onGroupRemove: (Session, String) -> Unit,
    dragDropState: DragDropState
) {
    val durationMs = getSessionComputeTimeMs(session)
    val totalTurns = session.turns.size
    val userCharCount = session.turns.sumOf { it.userMessage.length }
    val assistantCharCount = session.turns.sumOf { it.assistantMessage.length }
    val promptTokens = (userCharCount + 3) / 4
    val responseTokens = (assistantCharCount + 3) / 4
    val totalTokens = promptTokens + responseTokens
    val models = session.turns.mapNotNull { it.extraData["model"] }.distinct()

    val sessionGroups = remember(groups, session.id) {
        groups.filter { it.sessionIds.contains(session.id) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                dragDropState.detailHeaderBounds = coords.boundsInRoot()
            },
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Clickable CWD Link
            val cwd = session.cwd
            if (cwd != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            try {
                                Desktop.getDesktop().open(File(cwd))
                            } catch (_: Exception) {}
                        }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = "Open directory",
                        tint = AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = cwd,
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Group Tags Section
            Text(
                text = "Tags / Groups",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Existing tag chips
                sessionGroups.forEach { group ->
                    var chipCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentCyan.copy(alpha = 0.12f))
                            .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .onGloballyPositioned { chipCoords = it }
                            .pointerInput(group.name) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val rootOffset = chipCoords?.localToRoot(offset) ?: Offset.Zero
                                        dragDropState.draggingTag = Pair(session, group.name)
                                        dragDropState.dragPosition = rootOffset
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDropState.updateDragPosition(dragDropState.dragPosition + dragAmount)
                                    },
                                    onDragEnd = {
                                        val headerBounds = dragDropState.detailHeaderBounds
                                        if (headerBounds != null && !headerBounds.contains(dragDropState.dragPosition)) {
                                            onGroupRemove(session, group.name)
                                        }
                                        dragDropState.reset()
                                    },
                                    onDragCancel = {
                                        dragDropState.reset()
                                    }
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = group.name,
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.offset(y = 1.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove tag",
                                tint = AccentCyan,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .clickable { onGroupRemove(session, group.name) }
                            )
                        }
                    }
                }
                
                // Add Tag Button with DropdownMenu
                var showAddTagMenu by remember { mutableStateOf(false) }
                var newTagName by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }
                
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                            .clickable { 
                                showAddTagMenu = true
                                newTagName = ""
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add tag",
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Add Tag...",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.offset(y = 1.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showAddTagMenu,
                        onDismissRequest = { showAddTagMenu = false },
                        modifier = Modifier
                            .width(220.dp)
                            .background(SlateSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    ) {
                        // Search / Create Tag Field
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
                                .padding(8.dp)
                                .focusRequester(focusRequester)
                        )
                        
                        LaunchedEffect(showAddTagMenu) {
                            if (showAddTagMenu) {
                                focusRequester.requestFocus()
                            }
                        }
                        
                        val filteredGroups = remember(groups, newTagName, sessionGroups) {
                            groups.filter { group ->
                                !sessionGroups.any { it.name == group.name } &&
                                group.name.contains(newTagName, ignoreCase = true)
                            }
                        }
                        
                        // Option to create a new tag if it doesn't exist yet and input is not empty
                        if (newTagName.isNotBlank()) {
                            val exists = groups.any { it.name.equals(newTagName.trim(), ignoreCase = true) }
                            if (!exists) {
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
                                        showAddTagMenu = false
                                    }
                                )
                            }
                        }
                        
                        if (filteredGroups.isNotEmpty()) {
                            filteredGroups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name, color = TextPrimary, fontSize = 12.sp) },
                                    onClick = {
                                        onGroupAdd(session, group.name)
                                        showAddTagMenu = false
                                    }
                                )
                            }
                        } else if (newTagName.isBlank()) {
                            DropdownMenuItem(
                                enabled = false,
                                text = { Text("No other tags available", color = TextSecondary, fontSize = 11.sp) },
                                onClick = {}
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Stats FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionStatBadge(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "$totalTurns dialogue ${if (totalTurns == 1) "turn" else "turns"}"
                )
                SessionStatBadge(
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    label = "Est. Tokens: ${formatNumber(totalTokens.toLong())} (Prompts: ${formatNumber(promptTokens.toLong())} / Replies: ${formatNumber(responseTokens.toLong())})"
                )
                SessionStatBadge(
                    icon = Icons.Default.AccessTime,
                    label = "Active Work: ${formatDuration(durationMs)}"
                )
                SessionStatBadge(
                    icon = Icons.Default.History,
                    label = "Speed: ${formatSpeed(totalTokens.toLong(), durationMs)}"
                )
                if (models.isNotEmpty()) {
                    SessionStatBadge(
                        icon = Icons.Default.Bolt,
                        label = "Model(s): ${models.joinToString(", ")}"
                    )
                }
                val compactionCount = session.turns.count { it.extraData["isCompaction"] == "true" }
                if (compactionCount > 0) {
                    SessionStatBadge(
                        icon = Icons.Default.Settings,
                        label = "Compactions: $compactionCount"
                    )
                }
            }

            // Model Breakdown
            if (models.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Model Breakdown",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val localModelStats = session.turns.groupBy { it.extraData["model"] ?: "Unknown Model" }
                    localModelStats.forEach { (modelName, modelTurns) ->
                        val mUserChars = modelTurns.sumOf { it.userMessage.length }
                        val mAssistantChars = modelTurns.sumOf { it.assistantMessage.length }
                        val mTokens = (mUserChars + mAssistantChars + 3) / 4
                        val mDurationMs = modelTurns.sumOf { turn ->
                            val ms = turn.extraData["computeTimeMs"]?.toLongOrNull()
                            if (ms != null && ms > 0) {
                                ms.coerceAtMost(900_000L)
                            } else if (turn.assistantMessage.isNotEmpty()) {
                                val estMs = (turn.assistantMessage.length / 120.0 * 1000.0).toLong()
                                estMs.coerceIn(2000L, 60000L)
                            } else 0L
                        }
                        val pctTurns = (modelTurns.size.toFloat() / totalTurns * 100).toInt()
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BorderColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$modelName: ${formatNumber(mTokens.toLong())} tkn | ${formatDuration(mDurationMs)} ($pctTurns%)",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailPane(
    session: Session?,
    searchResults: List<SearchResult>,
    query: String,
    showFindBar: Boolean,
    queryValue: TextFieldValue,
    onQueryValueChange: (TextFieldValue) -> Unit,
    matchCase: Boolean,
    onMatchCaseChange: (Boolean) -> Unit,
    wholeWord: Boolean,
    onWholeWordChange: (Boolean) -> Unit,
    useRegex: Boolean,
    onUseRegexChange: (Boolean) -> Unit,
    matches: List<FindMatch>,
    activeMatchIndex: Int,
    onPrevMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onCloseFind: () -> Unit,
    isSidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onSessionSelect: (Session?) -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePin: (Session) -> Unit,
    groups: List<ConversationGroup>,
    onGroupAdd: (Session, String) -> Unit,
    onGroupRemove: (Session, String) -> Unit,
    onGroupUpdate: (ConversationGroup) -> Unit,
    onGroupDelete: (String) -> Unit,
    onToggleGroupPin: (String, Boolean) -> Unit,
    dragDropState: DragDropState = remember { DragDropState() },
    onUrlClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lazyListState = remember(session?.id) {
        LazyListState(
            firstVisibleItemIndex = if (session != null) session.turns.size + 1 else 0
        )
    }
    var isScrollLocked by remember(session?.id) { mutableStateOf(true) }
    var isScrollingProgrammatically by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var sortBy by remember { mutableStateOf(ModelSortDimension.TURNS) }
    var sortAscending by remember { mutableStateOf(false) }

    val activeMatch = remember(activeMatchIndex, matches) {
        matches.getOrNull(activeMatchIndex)
    }
    val findRegex = remember(queryValue.text, matchCase, wholeWord, useRegex) {
        buildFindRegex(queryValue.text, matchCase, wholeWord, useRegex)
    }

    var isHeaderExpanded by remember(session?.id) { mutableStateOf(true) }

    LaunchedEffect(isScrollLocked) {
        isHeaderExpanded = isScrollLocked
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Find Bar
            if (session != null && showFindBar) {
                // If find bar is visible, we shift it down so it is not covered by the toolbar
                Spacer(modifier = Modifier.height(80.dp))
                FindBar(
                    queryValue = queryValue,
                    onQueryValueChange = onQueryValueChange,
                    matchCase = matchCase,
                    onMatchCaseChange = onMatchCaseChange,
                    wholeWord = wholeWord,
                    onWholeWordChange = onWholeWordChange,
                    useRegex = useRegex,
                    onUseRegexChange = onUseRegexChange,
                    matchCount = matches.size,
                    activeMatchIndex = activeMatchIndex,
                    onPrevMatch = onPrevMatch,
                    onNextMatch = onNextMatch,
                    onClose = onCloseFind
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (session == null) {
                    var selectedTab by remember { mutableStateOf(0) }
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(72.dp))
                        
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = SlateSurface,
                            contentColor = AccentCyan,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = AccentCyan
                                )
                            },
                            divider = { HorizontalDivider(color = BorderColor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Global Stats", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 1.dp)) },
                                unselectedContentColor = TextSecondary,
                                selectedContentColor = AccentCyan
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Groups Dashboard", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 1.dp)) },
                                unselectedContentColor = TextSecondary,
                                selectedContentColor = AccentCyan
                            )
                        }
                        
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (selectedTab == 0) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    if (searchResults.isEmpty()) {
                                        // Empty state (no data at all)
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = BorderColor,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Text(
                                                text = "No conversations found matching filters",
                                                color = TextSecondary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        // Compute statistics
                                        val totalConversations = searchResults.size
                                        val totalTurns = searchResults.sumOf { it.session.turns.size }
                                        val totalUserChars = searchResults.sumOf { it.session.turns.sumOf { turn -> turn.userMessage.length } }
                                        val totalAssistantChars = searchResults.sumOf { it.session.turns.sumOf { turn -> turn.assistantMessage.length } }
                                        val promptTokens = (totalUserChars + 3) / 4
                                        val responseTokens = (totalAssistantChars + 3) / 4
                                        val totalEstTokens = promptTokens + responseTokens
                                        val avgTurns = if (totalConversations > 0) totalTurns.toFloat() / totalConversations else 0f
                                        val totalDurationMs = searchResults.sumOf { getSessionComputeTimeMs(it.session) }
                                        val avgDurationMs = if (totalConversations > 0) totalDurationMs / totalConversations else 0L
                                        val avgSpeedText = formatSpeed(totalEstTokens.toLong(), totalDurationMs)

                                        val totalCompactions = searchResults.sumOf { res ->
                                            res.session.turns.count { it.extraData["isCompaction"] == "true" }
                                        }
                                        val totalCompactionTimeMs = searchResults.sumOf { res ->
                                            res.session.turns.sumOf { it.extraData["compactionTimeMs"]?.toLongOrNull() ?: 0L }
                                        }

                                        val modelStatsList = remember(searchResults) {
                                            class ModelStats(
                                                var turnCount: Int = 0,
                                                var promptChars: Long = 0,
                                                var responseChars: Long = 0,
                                                var computeTimeMs: Long = 0
                                            )
                                            val modelStatsMap = mutableMapOf<String, ModelStats>()
                                            for (res in searchResults) {
                                                for (turn in res.session.turns) {
                                                    val mName = turn.extraData["model"] ?: "Unknown Model"
                                                    val stats = modelStatsMap.getOrPut(mName) { ModelStats() }
                                                    stats.turnCount++
                                                    stats.promptChars += turn.userMessage.length
                                                    stats.responseChars += turn.assistantMessage.length
                                                    val ms = turn.extraData["computeTimeMs"]?.toLongOrNull()
                                                    if (ms != null && ms > 0) {
                                                        stats.computeTimeMs += ms.coerceAtMost(900_000L)
                                                    } else if (turn.assistantMessage.isNotEmpty()) {
                                                        val estMs = (turn.assistantMessage.length / 120.0 * 1000.0).toLong()
                                                        stats.computeTimeMs += estMs.coerceIn(2000L, 60000L)
                                                    }
                                                }
                                            }

                                            modelStatsMap.entries.map { (modelName, stats) ->
                                                val modelPromptTokens = (stats.promptChars + 3) / 4
                                                val modelResponseTokens = (stats.responseChars + 3) / 4
                                                val modelTotalTokens = modelPromptTokens + modelResponseTokens
                                                val speedTps = if (stats.computeTimeMs > 0) {
                                                    (modelTotalTokens.toDouble() * 1000.0) / stats.computeTimeMs
                                                } else 0.0
                                                ModelItemStats(
                                                    modelName = modelName,
                                                    turnCount = stats.turnCount,
                                                    promptChars = stats.promptChars,
                                                    responseChars = stats.responseChars,
                                                    computeTimeMs = stats.computeTimeMs,
                                                    totalTokens = modelTotalTokens,
                                                    speedTps = speedTps
                                                )
                                            }
                                        }

                                        val sortedModelStats = remember(modelStatsList, sortBy, sortAscending) {
                                            val comparator = when (sortBy) {
                                                ModelSortDimension.TURNS -> compareBy<ModelItemStats> { it.turnCount }
                                                ModelSortDimension.TOKENS -> compareBy<ModelItemStats> { it.totalTokens }
                                                ModelSortDimension.SPEED -> compareBy<ModelItemStats> { it.speedTps }
                                                ModelSortDimension.COMPUTE -> compareBy<ModelItemStats> { it.computeTimeMs }
                                                ModelSortDimension.NAME -> compareBy<ModelItemStats> { it.modelName.lowercase() }
                                            }
                                            if (sortAscending) {
                                                modelStatsList.sortedWith(comparator)
                                            } else {
                                                modelStatsList.sortedWith(comparator.reversed())
                                            }
                                        }

                                        // Grid of main metrics (4 rows of 2 cards each)
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    StatCard(
                                                        title = "Total Conversations",
                                                        value = formatNumber(totalConversations.toLong()),
                                                        subtitle = "Indexed dialog histories",
                                                        icon = Icons.Default.Folder
                                                    )
                                                }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    StatCard(
                                                        title = "Dialogue Exchanges",
                                                        value = formatNumber(totalTurns.toLong()),
                                                        subtitle = "Avg. Depth: ${String.format("%.1f", avgTurns)} turns / session",
                                                        icon = Icons.Default.Chat
                                                    )
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    StatCard(
                                                        title = "Avg. Generation Speed",
                                                        value = avgSpeedText,
                                                        subtitle = "Tokens generated per second",
                                                        icon = Icons.Default.History
                                                    )
                                                }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    StatCard(
                                                        title = "Est. Total Tokens",
                                                        value = formatNumber(totalEstTokens.toLong()),
                                                        subtitle = "Prompt + Completion tokens",
                                                        icon = Icons.Default.CompareArrows
                                                    )
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    StatCard(
                                                        title = "Total Compute Time",
                                                        value = formatDuration(totalDurationMs),
                                                        subtitle = "Aggregated active agent work",
                                                        icon = Icons.Default.AccessTime
                                                    )
                                                }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    StatCard(
                                                        title = "Avg. Session Duration",
                                                        value = formatDuration(avgDurationMs),
                                                        subtitle = "Average active work per session",
                                                        icon = Icons.Default.Timer
                                                    )
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    StatCard(
                                                        title = "Context Compactions",
                                                        value = formatNumber(totalCompactions.toLong()),
                                                        subtitle = "Context summaries triggered",
                                                        icon = Icons.Default.Settings
                                                    )
                                                }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    val avgCompactionText = if (totalCompactions > 0) {
                                                        val avg = totalCompactionTimeMs.toDouble() / totalCompactions
                                                        if (avg < 1000.0) "${avg.toInt()} ms" else String.format("%.2f s", avg / 1000.0)
                                                    } else "0 ms"
                                                    StatCard(
                                                        title = "Est. Compaction Time",
                                                        value = formatDuration(totalCompactionTimeMs),
                                                        subtitle = "Avg: $avgCompactionText per compaction",
                                                        icon = Icons.Default.Refresh
                                                    )
                                                }
                                            }
                                        }

                                        // Model Performance & Usage Breakdown Section
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                            border = BorderStroke(1.dp, BorderColor)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Text(
                                                    text = "Model Performance & Usage Breakdown",
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                // Sort selection flow row
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.CenterStart,
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text(
                                                            text = "Sort by:",
                                                            color = TextSecondary,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier.offset(y = 1.dp)
                                                        )
                                                    }
                                                    ModelSortDimension.entries.forEach { dimension ->
                                                        val isSelected = sortBy == dimension
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
                                                                        sortAscending = (dimension == ModelSortDimension.NAME)
                                                                    }
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = dimension.displayName,
                                                                    color = if (isSelected) AccentCyan else TextSecondary,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    lineHeight = 11.sp,
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

                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    sortedModelStats.forEach { stats ->
                                                        val pctTurns = if (totalTurns > 0) (stats.turnCount.toFloat() / totalTurns * 100) else 0f
                                                        val pctTime = if (totalDurationMs > 0) (stats.computeTimeMs.toFloat() / totalDurationMs * 100) else 0f
                                                        val speed = formatSpeed(stats.totalTokens, stats.computeTimeMs)

                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(CardSurface)
                                                                .padding(12.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(
                                                                    text = stats.modelName,
                                                                    color = AccentCyan,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Text(
                                                                    text = speed,
                                                                    color = TextPrimary,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text("Tokens", color = TextSecondary, fontSize = 10.sp)
                                                                    Text(formatNumber(stats.totalTokens), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                                }
                                                                Column(modifier = Modifier.weight(1.5f)) {
                                                                    Text("Dialogue Turns", color = TextSecondary, fontSize = 10.sp)
                                                                    Text("${stats.turnCount} turns (${String.format("%.1f", pctTurns)}%)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                                }
                                                                Column(modifier = Modifier.weight(1.5f)) {
                                                                    Text("Compute Duration", color = TextSecondary, fontSize = 10.sp)
                                                                    Text("${formatDuration(stats.computeTimeMs)} (${String.format("%.1f", pctTime)}%)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Source Breakdown Section
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                            border = BorderStroke(1.dp, BorderColor)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Text(
                                                    text = "Source Distribution Breakdown",
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                val sourceGroups = searchResults.groupBy { it.session.sourceId }
                                                    .mapValues { it.value.size }
                                                    .toList()
                                                    .sortedByDescending { it.second }

                                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    sourceGroups.forEach { (sourceId, count) ->
                                                        SourceDistributionRow(
                                                            sourceId = sourceId,
                                                            count = count,
                                                            total = totalConversations
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Dialogue Details Breakdown Card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                            border = BorderStroke(1.dp, BorderColor)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "Dialogue Metric Specifications",
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("Dialogue Side", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text("User Prompts (Requests)", color = TextPrimary, fontSize = 13.sp)
                                                        Text("AI Responses (Replies)", color = TextPrimary, fontSize = 13.sp)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("Total Count", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text(formatNumber(totalTurns.toLong()), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text(formatNumber(totalTurns.toLong()), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("Est. Tokens", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text(formatNumber(promptTokens.toLong()), color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text(formatNumber(responseTokens.toLong()), color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                GroupsDashboard(
                                    groups = groups,
                                    searchResults = searchResults,
                                    onGroupUpdate = onGroupUpdate,
                                    onGroupDelete = onGroupDelete,
                                    onToggleGroupPin = onToggleGroupPin,
                                    onSessionSelect = onSessionSelect
                                )
                            }
                        }
                    }
                } else {
                    // Automatic scrolling to active find match
                    LaunchedEffect(activeMatchIndex, matches) {
                        if (activeMatchIndex in matches.indices) {
                            val match = matches[activeMatchIndex]
                            val targetTurnIndex = match.blockKey.turnIndex
                            val listIndex = targetTurnIndex + 1
                            isScrollingProgrammatically = true
                            try {
                                lazyListState.animateScrollToItem(listIndex)
                            } finally {
                                isScrollingProgrammatically = false
                            }
                        }
                    }

                    // When scroll lock is on, jump to the last item whenever the turns list updates (e.g. text grows or turns are added)
                    val lastIndex = session.turns.size + 1
                    LaunchedEffect(session.turns, isScrollLocked) {
                        if (isScrollLocked && session.turns.isNotEmpty()) {
                            isScrollingProgrammatically = true
                            try {
                                lazyListState.scrollToItem(lastIndex)
                            } finally {
                                isScrollingProgrammatically = false
                            }
                        }
                    }

                    // Release scroll lock when user scrolls away from the bottom; reclaim when they return
                    LaunchedEffect(lazyListState.isScrollInProgress) {
                        if (lazyListState.isScrollInProgress && !isScrollingProgrammatically) {
                            val layoutInfo = lazyListState.layoutInfo
                            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            val atBottom = lastVisible >= lastIndex
                            if (atBottom != isScrollLocked) isScrollLocked = atBottom
                        }
                    }

                    val topPadding = if (showFindBar) 16.dp else 80.dp

                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = topPadding,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp)
                    ) {
                        item(key = "header_card") {
                            DetailHeaderCard(
                                session = session,
                                groups = groups,
                                onGroupAdd = onGroupAdd,
                                onGroupRemove = onGroupRemove,
                                dragDropState = dragDropState
                            )
                        }
                        itemsIndexed(session.turns, key = { _, turn -> turn.turnId }) { turnIndex, turn ->
                            TurnCard(
                                turn = turn,
                                turnIndex = turnIndex,
                                query = query,
                                findRegex = findRegex,
                                activeMatch = activeMatch,
                                onUrlClick = onUrlClick
                            )
                        }
                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp),
                        adapter = rememberScrollbarAdapter(scrollState = lazyListState),
                        style = defaultScrollbarStyle().copy(
                            unhoverColor = AccentCyan.copy(alpha = 0.4f),
                            hoverColor = AccentCyan.copy(alpha = 0.8f),
                            thickness = 8.dp
                        )
                    )

                    // Floating "Scroll to End" button when user has scrolled away from the tail
                    this@Column.AnimatedVisibility(
                        visible = !isScrollLocked && session.turns.isNotEmpty(),
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isScrollingProgrammatically = true
                                    try {
                                        lazyListState.animateScrollToItem(lastIndex)
                                    } finally {
                                        isScrollingProgrammatically = false
                                    }
                                    isScrollLocked = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Scroll to End",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Float the DetailPaneToolbar on top of everything inside the root Box
        DetailPaneToolbar(
            session = session,
            isSidebarCollapsed = isSidebarCollapsed,
            onToggleSidebar = onToggleSidebar,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            onBack = onBack,
            onForward = onForward,
            onRefresh = onRefresh,
            onSessionSelect = onSessionSelect,
            onOpenSettings = onOpenSettings,
            isHeaderExpanded = isHeaderExpanded,
            onToggleHeader = { isHeaderExpanded = !isHeaderExpanded },
            onTogglePin = onTogglePin,
            groups = groups,
            onGroupAdd = onGroupAdd,
            onGroupRemove = onGroupRemove,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = if (isSidebarCollapsed) 90.dp else 16.dp,
                    top = 10.dp,
                    end = 16.dp
                )
        )
    }
}

@Composable
fun TurnCard(
    turn: Turn,
    turnIndex: Int,
    query: String,
    findRegex: Regex?,
    activeMatch: FindMatch?,
    onUrlClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // User Message
        if (turn.userMessage.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "User",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                            if (turn.timestamp > 0L) {
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    color = TextSecondary.copy(alpha = 0.4f)
                                )
                                val turnDateStr = remember(turn.timestamp) { formatTurnTimestamp(turn.timestamp) }
                                Text(
                                    text = turnDateStr,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = TextSecondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(
                            onClick = { copyToClipboard(turn.userMessage) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectionContainer {
                        ContextMenuArea(items = {
                            listOf(ContextMenuItem("Copy") { copyToClipboard(turn.userMessage) })
                        }) {
                            MarkdownView(
                                text = turn.userMessage,
                                turnIndex = turnIndex,
                                isUser = true,
                                partIndex = 0,
                                query = query,
                                findRegex = findRegex,
                                activeMatch = activeMatch,
                                color = TextPrimary,
                                highlightColor = Color(0x66FF9100),
                                onUrlClick = onUrlClick
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Assistant Message
        if (turn.assistantMessage.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateSurface)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val model = turn.extraData["model"]
                        val computeMs = turn.extraData["computeTimeMs"]?.toLongOrNull()
                        val speedLabel = if (computeMs != null && computeMs > 0) {
                            val turnTokens = turn.assistantMessage.length / 4
                            " | ${formatDuration(computeMs)} (${formatSpeed(turnTokens.toLong(), computeMs)})"
                        } else ""
                        val headerText = if (model != null) "Assistant ($model$speedLabel)" else "Assistant$speedLabel"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = headerText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple
                            )
                            if (turn.timestamp > 0L) {
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    color = TextSecondary.copy(alpha = 0.4f)
                                )
                                val turnDateStr = remember(turn.timestamp) { formatTurnTimestamp(turn.timestamp) }
                                Text(
                                    text = turnDateStr,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = TextSecondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(
                            onClick = { copyToClipboard(turn.assistantMessage) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val parts = remember(turn.assistantMessage) {
                        parseAssistantMessage(turn.assistantMessage)
                    }
                    val lastToolIndex = remember(parts) {
                        parts.indexOfLast { it is MessagePart.Tool }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (lastToolIndex >= 0) {
                            val workParts = remember(parts, lastToolIndex) {
                                parts.subList(0, lastToolIndex + 1)
                            }
                            val responseParts = remember(parts, lastToolIndex) {
                                parts.subList(lastToolIndex + 1, parts.size)
                            }

                            val computeMs = turn.extraData["computeTimeMs"]?.toLongOrNull() ?: 0L

                            WorkedForBlock(
                                durationMs = computeMs,
                                parts = workParts,
                                startIndex = 0,
                                turnIndex = turnIndex,
                                query = query,
                                findRegex = findRegex,
                                activeMatch = activeMatch,
                                onUrlClick = onUrlClick
                            )

                            responseParts.forEachIndexed { relativeIndex, part ->
                                val partIndex = lastToolIndex + 1 + relativeIndex
                                MessagePartView(
                                    part = part,
                                    turnIndex = turnIndex,
                                    partIndex = partIndex,
                                    query = query,
                                    findRegex = findRegex,
                                    activeMatch = activeMatch,
                                    onUrlClick = onUrlClick
                                )
                            }
                        } else {
                            parts.forEachIndexed { partIndex, part ->
                                MessagePartView(
                                    part = part,
                                    turnIndex = turnIndex,
                                    partIndex = partIndex,
                                    query = query,
                                    findRegex = findRegex,
                                    activeMatch = activeMatch,
                                    onUrlClick = onUrlClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessagePartView(
    part: MessagePart,
    turnIndex: Int,
    partIndex: Int,
    query: String,
    findRegex: Regex?,
    activeMatch: FindMatch?,
    onUrlClick: (String) -> Unit
) {
    when (part) {
        is MessagePart.Text -> {
            SelectionContainer {
                ContextMenuArea(items = {
                    listOf(ContextMenuItem("Copy") { copyToClipboard(part.content) })
                }) {
                    MarkdownView(
                        text = part.content,
                        turnIndex = turnIndex,
                        isUser = false,
                        partIndex = partIndex,
                        query = query,
                        findRegex = findRegex,
                        activeMatch = activeMatch,
                        color = TextPrimary.copy(alpha = 0.9f),
                        highlightColor = Color(0x66FF9100),
                        onUrlClick = onUrlClick
                    )
                }
            }
        }
        is MessagePart.Tool -> {
            ToolOutputBlock(
                type = part.type,
                header = part.header,
                content = part.content,
                timestamp = part.timestamp,
                turnIndex = turnIndex,
                partIndex = partIndex,
                query = query,
                findRegex = findRegex,
                activeMatch = activeMatch
            )
        }
    }
}

@Composable
fun WorkedForBlock(
    durationMs: Long,
    parts: List<MessagePart>,
    startIndex: Int,
    turnIndex: Int,
    query: String,
    findRegex: Regex?,
    activeMatch: FindMatch?,
    onUrlClick: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Auto-expansion logic on find search match inside the work block
    val lastToolIndex = parts.size - 1
    val hasActiveMatchInWork = remember(activeMatch, lastToolIndex) {
        activeMatch != null && !activeMatch.blockKey.isUser && activeMatch.blockKey.turnIndex == turnIndex && activeMatch.blockKey.partIndex in startIndex..(startIndex + lastToolIndex)
    }
    LaunchedEffect(hasActiveMatchInWork) {
        if (hasActiveMatchInWork) {
            isExpanded = true
        }
    }

    val durationText = remember(durationMs) {
        if (durationMs > 0) "Worked for ${formatDuration(durationMs)}" else "Worked"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { isExpanded = !isExpanded }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = durationText,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 12.dp)
            ) {
                // Vertical thread line showing nesting clearly
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(BorderColor.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    parts.forEachIndexed { index, part ->
                        val partIndex = startIndex + index
                        MessagePartView(
                            part = part,
                            turnIndex = turnIndex,
                            partIndex = partIndex,
                            query = query,
                            findRegex = findRegex,
                            activeMatch = activeMatch,
                            onUrlClick = onUrlClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    turnIndex: Int,
    partIndex: Int,
    query: String,
    findRegex: Regex?,
    activeMatch: FindMatch?,
    color: Color,
    highlightColor: Color
) {
    val blockKey = remember(turnIndex, partIndex) {
        BlockKey(turnIndex, isUser = false, partIndex = partIndex, blockIndex = 0)
    }

    if (findRegex != null) {
        val annotatedString = remember(text, findRegex, blockKey, activeMatch) {
            val base = buildAnnotatedString { append(text) }
            highlightAnnotatedString(
                annotatedString = base,
                regex = findRegex,
                blockKey = blockKey,
                activeMatch = activeMatch,
                highlightColor = highlightColor,
                activeHighlightColor = Color(0xFFFF9100)
            )
        }
        Text(
            text = annotatedString,
            color = color,
            fontSize = 13.5.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 20.sp
        )
        return
    }

    if (query.isBlank()) {
        Text(
            text = text,
            color = color,
            fontSize = 13.5.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 20.sp
        )
        return
    }

    val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (terms.isEmpty()) {
        Text(
            text = text,
            color = color,
            fontSize = 13.5.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 20.sp
        )
        return
    }

    val annotatedString = remember(text, query) {
        buildAnnotatedString {
            var currentIndex = 0
            while (currentIndex < text.length) {
                var earliestMatchIndex = -1
                var matchedTerm = ""
                for (term in terms) {
                    val index = text.lowercase().indexOf(term, currentIndex)
                    if (index != -1 && (earliestMatchIndex == -1 || index < earliestMatchIndex)) {
                        earliestMatchIndex = index
                        matchedTerm = term
                    }
                }

                if (earliestMatchIndex != -1) {
                    append(text.substring(currentIndex, earliestMatchIndex))
                    withStyle(style = SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)) {
                        append(text.substring(earliestMatchIndex, earliestMatchIndex + matchedTerm.length))
                    }
                    currentIndex = earliestMatchIndex + matchedTerm.length
                } else {
                    append(text.substring(currentIndex))
                    break
                }
            }
        }
    }

    Text(
        text = annotatedString,
        color = color,
        fontSize = 13.5.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 20.sp
    )
}

@Composable
fun ToolOutputBlock(
    type: String,
    header: String,
    content: String,
    timestamp: Long = 0L,
    turnIndex: Int,
    partIndex: Int,
    query: String,
    findRegex: Regex?,
    activeMatch: FindMatch?
) {
    var isExpanded by remember { mutableStateOf(false) }

    val blockKey = remember(turnIndex, partIndex) {
        BlockKey(turnIndex, isUser = false, partIndex = partIndex, blockIndex = 0)
    }
    val isActiveInThisBlock = activeMatch != null && activeMatch.blockKey == blockKey

    LaunchedEffect(isActiveInThisBlock) {
        if (isActiveInThisBlock) {
            isExpanded = true
        }
    }

    val (icon, tint) = when (type) {
        "VIEW_FILE" -> Pair(Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFF00E676))
        "RUN_COMMAND" -> Pair(Icons.Default.Terminal, Color(0xFF2979FF))
        "CODE_ACTION" -> Pair(Icons.Default.Edit, Color(0xFFFFD600))
        "GREP_SEARCH", "SEARCH_WEB" -> Pair(Icons.Default.Search, Color(0xFFD500F9))
        "SYSTEM_MESSAGE" -> Pair(Icons.Default.Info, Color(0xFFAB47BC))
        "ERROR_MESSAGE" -> Pair(Icons.Default.Error, Color(0xFFEF5350))
        else -> Pair(Icons.Default.Build, Color(0xFF00E5FF))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x11757575))
            .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = header,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (timestamp > 0L) {
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextSecondary.copy(alpha = 0.4f)
                    )
                    val stepDateStr = remember(timestamp) { formatTurnTimestamp(timestamp) }
                    Text(
                        text = stepDateStr,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                text = if (isExpanded) "Collapse ▲" else "Expand ▼",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isExpanded) {
            HorizontalDivider(color = BorderColor.copy(alpha = 0.2f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070709))
                    .padding(12.dp)
            ) {
                SelectionContainer {
                    ContextMenuArea(items = {
                        listOf(ContextMenuItem("Copy") { copyToClipboard(content) })
                    }) {
                        if (content.trim().isEmpty()) {
                            Text(
                                text = "No output",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            ChunkedText(
                                text = content,
                                turnIndex = turnIndex,
                                partIndex = partIndex,
                                query = query,
                                findRegex = findRegex,
                                activeMatch = activeMatch,
                                color = TextPrimary.copy(alpha = 0.85f),
                                highlightColor = Color(0x66FF9100)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChunkedText(
    text: String,
    turnIndex: Int,
    partIndex: Int,
    query: String,
    findRegex: Regex?,
    activeMatch: FindMatch?,
    color: Color,
    highlightColor: Color,
    chunkSize: Int = 8000
) {
    HighlightedText(
        text = text,
        turnIndex = turnIndex,
        partIndex = partIndex,
        query = query,
        findRegex = findRegex,
        activeMatch = activeMatch,
        color = color,
        highlightColor = highlightColor
    )
}

@Composable
fun MarkdownView(
    text: String,
    turnIndex: Int,
    isUser: Boolean,
    partIndex: Int,
    query: String,
    findRegex: Regex?,
    activeMatch: FindMatch?,
    color: Color,
    highlightColor: Color,
    onUrlClick: (String) -> Unit
) {
    val lines = text.split("\n")
    val blocks = remember(text) { parseMarkdownBlocks(lines) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEachIndexed { blockIndex, block ->
            val blockKey = remember(turnIndex, isUser, partIndex, blockIndex) {
                BlockKey(turnIndex, isUser, partIndex, blockIndex)
            }
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 20.sp
                        2 -> 18.sp
                        3 -> 16.sp
                        else -> 14.sp
                    }
                    val fontWeight = FontWeight.Bold
                    val headerColor = when (block.level) {
                        1, 2 -> AccentCyan
                        else -> color
                    }
                    
                    val annotatedText = remember(block.content, query, findRegex, blockKey, activeMatch) {
                        if (findRegex != null) {
                            parseInlineMarkdown(
                                text = block.content,
                                findRegex = findRegex,
                                blockKey = blockKey,
                                activeMatch = activeMatch,
                                highlightColor = highlightColor,
                                activeHighlightColor = Color(0xFFFF9100)
                            )
                        } else {
                            parseInlineMarkdown(
                                text = block.content,
                                query = query,
                                highlightColor = highlightColor,
                                exactMatch = false
                            )
                        }
                    }
                    ClickableMarkdownText(
                        text = annotatedText,
                        color = headerColor,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
                        onUrlClick = onUrlClick,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070709))
                            .border(1.dp, BorderColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            if (block.language.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatLanguageName(block.language),
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { copyToClipboard(block.content) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            val annotatedText = remember(block.content, query, findRegex, blockKey, activeMatch) {
                                if (findRegex != null) {
                                    parseOnlyQueryHighlights(
                                        text = block.content,
                                        findRegex = findRegex,
                                        blockKey = blockKey,
                                        activeMatch = activeMatch,
                                        highlightColor = highlightColor,
                                        activeHighlightColor = Color(0xFFFF9100)
                                    )
                                } else {
                                    parseOnlyQueryHighlights(
                                        text = block.content,
                                        query = query,
                                        highlightColor = highlightColor,
                                        exactMatch = false
                                    )
                                }
                            }
                            Text(
                                text = annotatedText,
                                color = TextPrimary.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = (block.indentLevel * 12).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (block.ordered) "${block.number}." else "•",
                            color = AccentCyan,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(16.dp)
                        )
                        val annotatedText = remember(block.content, query, findRegex, blockKey, activeMatch) {
                            if (findRegex != null) {
                                parseInlineMarkdown(
                                    text = block.content,
                                    findRegex = findRegex,
                                    blockKey = blockKey,
                                    activeMatch = activeMatch,
                                    highlightColor = highlightColor,
                                    activeHighlightColor = Color(0xFFFF9100)
                                )
                            } else {
                                parseInlineMarkdown(
                                    text = block.content,
                                    query = query,
                                    highlightColor = highlightColor,
                                    exactMatch = false
                                )
                            }
                        }
                        ClickableMarkdownText(
                            text = annotatedText,
                            color = color,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 20.sp,
                            onUrlClick = onUrlClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    if (block.content.trim() == "---" || block.content.trim() == "___" || block.content.trim() == "***") {
                        HorizontalDivider(
                            color = BorderColor.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (block.content.isNotEmpty()) {
                        val annotatedText = remember(block.content, query, findRegex, blockKey, activeMatch) {
                            if (findRegex != null) {
                                parseInlineMarkdown(
                                    text = block.content,
                                    findRegex = findRegex,
                                    blockKey = blockKey,
                                    activeMatch = activeMatch,
                                    highlightColor = highlightColor,
                                    activeHighlightColor = Color(0xFFFF9100)
                                )
                            } else {
                                parseInlineMarkdown(
                                    text = block.content,
                                    query = query,
                                    highlightColor = highlightColor,
                                    exactMatch = false
                                )
                            }
                        }
                        ClickableMarkdownText(
                            text = annotatedText,
                            color = color,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 20.sp,
                            onUrlClick = onUrlClick
                        )
                    }
                }
            }
        }
    }
}

enum class ModelSortDimension(val displayName: String) {
    TURNS("Turns"),
    TOKENS("Tokens"),
    SPEED("Speed"),
    COMPUTE("Duration"),
    NAME("Model Name")
}

data class ModelItemStats(
    val modelName: String,
    val turnCount: Int,
    val promptChars: Long,
    val responseChars: Long,
    val computeTimeMs: Long,
    val totalTokens: Long,
    val speedTps: Double
)

@Composable
fun ClickableMarkdownText(
    text: AnnotatedString,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontFamily: FontFamily,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight? = null,
    onUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isHoveringLink by remember { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .pointerHoverIcon(if (isHoveringLink) PointerIcon.Hand else PointerIcon.Default)
            .pointerInput(text) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Exit) {
                            isHoveringLink = false
                        } else {
                            val position = event.changes.firstOrNull()?.position
                            if (position != null) {
                                layoutResult?.let { layout ->
                                    val offset = layout.getOffsetForPosition(position)
                                    val annotation = text.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()
                                    if (annotation != null) {
                                        val lineIndex = layout.getLineForOffset(offset)
                                        val linkStart = maxOf(annotation.start, layout.getLineStart(lineIndex))
                                        val linkEnd = minOf(annotation.end - 1, layout.getLineEnd(lineIndex) - 1)
                                        if (linkStart <= linkEnd) {
                                            val leftBound = layout.getBoundingBox(linkStart).left
                                            val rightBound = layout.getBoundingBox(linkEnd).right
                                            val topBound = layout.getLineTop(lineIndex)
                                            val bottomBound = layout.getLineBottom(lineIndex)
                                            isHoveringLink = position.x >= leftBound && position.x <= rightBound &&
                                                             position.y >= topBound && position.y <= bottomBound
                                        } else {
                                            isHoveringLink = false
                                        }
                                    } else {
                                        isHoveringLink = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(text) {
                detectTapGestures { offset ->
                    layoutResult?.let { layout ->
                        val position = layout.getOffsetForPosition(offset)
                        text.getStringAnnotations(tag = "URL", start = position, end = position)
                            .firstOrNull()?.let { annotation ->
                                val lineIndex = layout.getLineForOffset(position)
                                val linkStart = maxOf(annotation.start, layout.getLineStart(lineIndex))
                                val linkEnd = minOf(annotation.end - 1, layout.getLineEnd(lineIndex) - 1)
                                if (linkStart <= linkEnd) {
                                    val leftBound = layout.getBoundingBox(linkStart).left
                                    val rightBound = layout.getBoundingBox(linkEnd).right
                                    val topBound = layout.getLineTop(lineIndex)
                                    val bottomBound = layout.getLineBottom(lineIndex)
                                    if (offset.x >= leftBound && offset.x <= rightBound &&
                                        offset.y >= topBound && offset.y <= bottomBound) {
                                        onUrlClick(annotation.item)
                                    }
                                }
                            }
                    }
                }
            }
    )
}

@Composable
fun GroupsDashboard(
    groups: List<ConversationGroup>,
    searchResults: List<SearchResult>,
    onGroupUpdate: (ConversationGroup) -> Unit,
    onGroupDelete: (String) -> Unit,
    onToggleGroupPin: (String, Boolean) -> Unit,
    onSessionSelect: (Session?) -> Unit
) {
    // Sort groups so that pinned ones float to the top
    val sortedGroups = remember(groups) {
        groups.sortedWith(
            compareByDescending<ConversationGroup> { it.isPinned }
                .thenBy { it.name.lowercase() }
        )
    }

    var selectedGroupName by remember { mutableStateOf<String?>(null) }
    
    // Auto-select first group if none selected and list is not empty
    LaunchedEffect(sortedGroups) {
        if (selectedGroupName == null && sortedGroups.isNotEmpty()) {
            selectedGroupName = sortedGroups.first().name
        }
    }

    val selectedGroup = remember(sortedGroups, selectedGroupName) {
        sortedGroups.find { it.name == selectedGroupName }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .background(SlateSurface)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Left Column: List of Groups (Width = 240.dp)
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(SlateSurface)
        ) {
            Text(
                text = "Groups",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            HorizontalDivider(color = BorderColor)
            
            if (sortedGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No groups created",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sortedGroups, key = { it.name }) { group ->
                        val isSelected = group.name == selectedGroupName
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CardSurface else Color.Transparent)
                                .clickable { selectedGroupName = group.name }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (group.isPinned) AccentCyan else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = group.name,
                                        color = if (isSelected) AccentCyan else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.offset(y = 1.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (group.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    
                                    // Session Count Badge
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BorderColor)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "${group.sessionIds.size}",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 9.sp,
                                            modifier = Modifier.offset(y = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(BorderColor)
        )
        
        // Right Column: Group details
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(ObsidianBg)
        ) {
            if (selectedGroup == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a group from the list",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            } else {
                GroupDetailsView(
                    group = selectedGroup,
                    searchResults = searchResults,
                    onGroupUpdate = onGroupUpdate,
                    onGroupDelete = {
                        onGroupDelete(it)
                        selectedGroupName = null
                    },
                    onToggleGroupPin = onToggleGroupPin,
                    onSessionSelect = onSessionSelect
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupDetailsView(
    group: ConversationGroup,
    searchResults: List<SearchResult>,
    onGroupUpdate: (ConversationGroup) -> Unit,
    onGroupDelete: (String) -> Unit,
    onToggleGroupPin: (String, Boolean) -> Unit,
    onSessionSelect: (Session?) -> Unit
) {
    // Group Sessions
    val groupSessions = remember(searchResults, group.sessionIds) {
        searchResults.filter { group.sessionIds.contains(it.session.id) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Row: Header and Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = group.name,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                IconButton(
                    onClick = { onToggleGroupPin(group.name, !group.isPinned) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = if (group.isPinned) "Unpin Group" else "Pin Group",
                        tint = if (group.isPinned) AccentCyan else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Dropdown
                var statusExpanded by remember { mutableStateOf(false) }
                val statusColors = when (group.status) {
                    "Completed" -> Pair(Color(0xFF81C784), Color(0xFF1E3524)) // Green
                    "Paused" -> Pair(Color(0xFFE57373), Color(0xFF381F1F)) // Red/Amber
                    else -> Pair(AccentCyan, Color(0xFF00373E)) // Active (Cyan)
                }
                
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColors.second)
                            .border(1.dp, statusColors.first, RoundedCornerShape(6.dp))
                            .clickable { statusExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = group.status,
                            color = statusColors.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = 1.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        modifier = Modifier
                            .background(CardSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    ) {
                        listOf("Active", "Completed", "Paused").forEach { statusOption ->
                            DropdownMenuItem(
                                text = { Text(statusOption, color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    statusExpanded = false
                                    onGroupUpdate(group.copy(status = statusOption, updatedAt = System.currentTimeMillis()))
                                }
                            )
                        }
                    }
                }
                
                // Delete Action
                IconButton(
                    onClick = { onGroupDelete(group.name) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Group",
                        tint = TextSecondary.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        HorizontalDivider(color = BorderColor)
        
        // Editable description & summary notes
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Description",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = group.description,
                onValueChange = {
                    onGroupUpdate(group.copy(description = it, updatedAt = System.currentTimeMillis()))
                },
                placeholder = { 
                    Text(
                        "Enter group description...", 
                        color = TextSecondary.copy(alpha = 0.4f), 
                        fontSize = 12.sp,
                        style = androidx.compose.ui.text.TextStyle.Default,
                        lineHeight = 12.sp
                    ) 
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Past Work Summary & Notes",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = group.pastWorkSummary,
                onValueChange = {
                    onGroupUpdate(group.copy(pastWorkSummary = it, updatedAt = System.currentTimeMillis()))
                },
                placeholder = { 
                    Text(
                        "Summarize past research or decisions made across this group of agent runs...", 
                        color = TextSecondary.copy(alpha = 0.4f), 
                        fontSize = 12.sp,
                        style = androidx.compose.ui.text.TextStyle.Default,
                        lineHeight = 12.sp
                    ) 
                },
                minLines = 4,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextPrimary,
                    fontSize = 12.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Future Tasks Checklist
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tasks & Action Items",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    var showAddTaskDialog by remember { mutableStateOf(false) }
                    var newTaskTitle by remember { mutableStateOf("") }
                    
                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    if (showAddTaskDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddTaskDialog = false },
                            title = { Text("Add New Task", color = TextPrimary) },
                            text = {
                                OutlinedTextField(
                                    value = newTaskTitle,
                                    onValueChange = { newTaskTitle = it },
                                    label = { Text("Task description", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = BorderColor,
                                        focusedContainerColor = ObsidianBg,
                                        unfocusedContainerColor = ObsidianBg,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newTaskTitle.isNotBlank()) {
                                            val newTask = GroupTask(
                                                id = java.util.UUID.randomUUID().toString(),
                                                title = newTaskTitle.trim(),
                                                isCompleted = false
                                            )
                                            onGroupUpdate(group.copy(
                                                tasks = group.tasks + newTask,
                                                updatedAt = System.currentTimeMillis()
                                            ))
                                        }
                                        newTaskTitle = ""
                                        showAddTaskDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg)
                                ) {
                                    Text("Add")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddTaskDialog = false }) {
                                    Text("Cancel", color = TextSecondary)
                                }
                            },
                            containerColor = SlateSurface
                        )
                    }
                }
                
                HorizontalDivider(color = BorderColor)
                
                if (group.tasks.isEmpty()) {
                    Text(
                        text = "No tasks listed. Add a task using the '+' button above.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.tasks.forEach { task ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ObsidianBg.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { checked ->
                                            val updatedTasks = group.tasks.map {
                                                if (it.id == task.id) it.copy(isCompleted = checked) else it
                                            }
                                            onGroupUpdate(group.copy(tasks = updatedTasks, updatedAt = System.currentTimeMillis()))
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AccentCyan,
                                            checkmarkColor = ObsidianBg,
                                            uncheckedColor = BorderColor
                                        )
                                    )
                                    Text(
                                        text = task.title,
                                        color = if (task.isCompleted) TextSecondary else TextPrimary,
                                        fontSize = 12.sp,
                                        style = if (task.isCompleted) androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.offset(y = 1.dp)
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Session link badge/button
                                    var sessionLinkExpanded by remember { mutableStateOf(false) }
                                    val linkedSession = groupSessions.find { it.session.id == task.associatedSessionId }
                                    
                                    Box {
                                        if (linkedSession != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(AccentPurple.copy(alpha = 0.15f))
                                                    .border(1.dp, AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                    .clickable { onSessionSelect(linkedSession.session) }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Link,
                                                        contentDescription = null,
                                                        tint = AccentPurple,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = linkedSession.session.threadName ?: "Untitled Run",
                                                        color = AccentPurple,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.widthIn(max = 100.dp).offset(y = 1.dp)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Unlink",
                                                        tint = AccentPurple,
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clickable {
                                                                val updatedTasks = group.tasks.map {
                                                                    if (it.id == task.id) it.copy(associatedSessionId = null) else it
                                                                }
                                                                onGroupUpdate(group.copy(tasks = updatedTasks, updatedAt = System.currentTimeMillis()))
                                                            }
                                                    )
                                                }
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { sessionLinkExpanded = true },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = "Link Session",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        
                                        DropdownMenu(
                                            expanded = sessionLinkExpanded,
                                            onDismissRequest = { sessionLinkExpanded = false },
                                            modifier = Modifier
                                                .background(CardSurface)
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            if (groupSessions.isEmpty()) {
                                                DropdownMenuItem(
                                                    enabled = false,
                                                    text = { Text("No sessions in group", color = TextSecondary, fontSize = 11.sp) },
                                                    onClick = {}
                                                )
                                            } else {
                                                groupSessions.forEach { res ->
                                                    DropdownMenuItem(
                                                        text = { Text(res.session.threadName ?: "Untitled Run", color = TextPrimary, fontSize = 12.sp) },
                                                        onClick = {
                                                            val updatedTasks = group.tasks.map {
                                                                if (it.id == task.id) it.copy(associatedSessionId = res.session.id) else it
                                                            }
                                                            onGroupUpdate(group.copy(tasks = updatedTasks, updatedAt = System.currentTimeMillis()))
                                                            sessionLinkExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Trash Icon
                                    IconButton(
                                        onClick = {
                                            val updatedTasks = group.tasks.filter { it.id != task.id }
                                            onGroupUpdate(group.copy(tasks = updatedTasks, updatedAt = System.currentTimeMillis()))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete task",
                                            tint = TextSecondary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Agent Status Monitoring Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Agent Run Monitoring",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                
                HorizontalDivider(color = BorderColor)
                
                if (groupSessions.isEmpty()) {
                    Text(
                        text = "No active agent run logs tagged with this group.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    val sdf = remember { SimpleDateFormat("MMM d, HH:mm") }
                    val sourceSummary = remember(groupSessions) {
                        groupSessions.groupBy { it.session.sourceId }
                            .map { (sourceId, searchResults) ->
                                val turnCount = searchResults.sumOf { it.session.turns.size }
                                val maxUpdated = searchResults.maxOf { it.session.updatedAt }
                                val models = searchResults.flatMap { res -> res.session.turns.mapNotNull { it.extraData["model"] } }.distinct()
                                Triple(sourceId, turnCount, Pair(maxUpdated, models))
                            }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sourceSummary.forEach { (sourceId, turns, meta) ->
                            val badgeColors = getSourceBadgeColors(sourceId)
                            val (lastUpdated, models) = meta
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ObsidianBg.copy(alpha = 0.5f))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Source Badge
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeColors.second)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = formatSourceDisplayName(sourceId),
                                            color = badgeColors.first,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 9.sp,
                                            modifier = Modifier.offset(y = 1.dp)
                                        )
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Dialogue turns: $turns",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (models.isNotEmpty()) {
                                            Text(
                                                text = "Model(s): ${models.joinToString(", ")}",
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                                
                                Text(
                                    text = "Last Active: ${sdf.format(Date(lastUpdated))}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

