package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whataicando.touch.compose.touchScrim
import com.whataicando.touch.compose.touchScrollable
import kotlinx.coroutines.launch
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter


@Composable
fun SidebarToggleIcon(tint: Color) {
    Canvas(
        modifier = Modifier
            .size(18.dp)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        val strokeWidth = 1.5.dp.toPx()
        val cornerRadius = 2.dp.toPx()
        // Border outline
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, 0f),
            size = size,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth)
        )
        // Vertical divider line representing sidebar partition
        val divideX = size.width * 0.33f
        drawLine(
            color = tint,
            start = Offset(divideX, 0f),
            end = Offset(divideX, size.height),
            strokeWidth = strokeWidth
        )
        // Shade left-side to visually represent the sidebar column layout
        drawRect(
            color = tint.copy(alpha = 0.25f),
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(divideX - strokeWidth / 2, size.height - strokeWidth)
        )
    }
}

@Composable
fun FindModifierButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    tooltip: String
) {
    Box(
        modifier = Modifier
            .size(width = 28.dp, height = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) AccentCyan.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (active) AccentCyan else BorderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) AccentCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 11.sp,
            style = TextStyle.Default,
            modifier = Modifier.offset(y = 1.dp)
        )
    }
}

@Composable
fun FindActionButton(
    text: String,
    onClick: () -> Unit,
    tooltip: String
) {
    Box(
        modifier = Modifier
            .size(width = 28.dp, height = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Transparent)
            .border(
                width = 1.dp,
                color = BorderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 11.sp,
            style = TextStyle.Default,
            modifier = Modifier.offset(y = 1.dp)
        )
    }
}

@Composable
fun FindBar(
    queryValue: TextFieldValue,
    onQueryValueChange: (TextFieldValue) -> Unit,
    matchCase: Boolean,
    onMatchCaseChange: (Boolean) -> Unit,
    wholeWord: Boolean,
    onWholeWordChange: (Boolean) -> Unit,
    useRegex: Boolean,
    onUseRegexChange: (Boolean) -> Unit,
    matchCount: Int,
    activeMatchIndex: Int,
    onPrevMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface)
            .drawBehind {
                drawLine(
                    color = BorderColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search in details",
            tint = AccentCyan,
            modifier = Modifier.size(18.dp)
        )

        OutlinedTextField(
            value = queryValue,
            onValueChange = onQueryValueChange,
            placeholder = { Text("Find in details...", color = TextSecondary) },
            singleLine = false,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = ObsidianBg,
                unfocusedContainerColor = ObsidianBg,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val isEnter = keyEvent.key == Key.Enter
                        if (isEnter) {
                            val isAltPressed = keyEvent.isAltPressed
                            val isCtrlPressed = keyEvent.isCtrlPressed
                            val isShiftPressed = keyEvent.isShiftPressed
                            if (isAltPressed || isCtrlPressed) {
                                val currentText = queryValue.text
                                val sel = queryValue.selection
                                val s = sel.min
                                val e = sel.max
                                val newText = currentText.substring(0, s) + "\n" + currentText.substring(e)
                                val newSelection = TextRange(s + 1)
                                onQueryValueChange(queryValue.copy(text = newText, selection = newSelection))
                                true
                            } else if (isShiftPressed) {
                                onPrevMatch()
                                true
                            } else {
                                onNextMatch()
                                true
                            }
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                },
            shape = RoundedCornerShape(8.dp),
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
                        focusRequester.requestFocus()
                    }, tooltip = "Insert Line Feed")
                }
            }
        )

        if (queryValue.text.isNotEmpty()) {
            val countText = if (matchCount > 0) {
                "${activeMatchIndex + 1} of $matchCount"
            } else {
                "No results"
            }
            val countColor = if (matchCount > 0) TextSecondary else Color(0xFFEF5350)
            Text(
                text = countText,
                color = countColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevMatch,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous match",
                        tint = if (matchCount > 0) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onNextMatch,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next match",
                        tint = if (matchCount > 0) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Close find bar",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun WarningOverlay(
    source: SourceAdapter,
    onDecision: (SettingsManager.Decision) -> Unit,
    onDeleteConfirm: () -> Unit,
    onClose: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .touchScrim(priority = 1)
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* consume clicks */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(480.dp)
                .padding(16.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close Warning",
                        tint = TextSecondary
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!showDeleteConfirm) {
                        Text(
                            text = "Orphaned Data Detected",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "We found historical conversation data for ${source.displayName}, but the application itself does not seem to be installed on this machine.\n\nWhat would you like to do with this data?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onDecision(SettingsManager.Decision.MONITOR) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Show Source: Monitor Data Path Anyway", style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = { onDecision(SettingsManager.Decision.IGNORE) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Hide Source: Ignore Data Path", style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clean Source: Delete Data Path", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else {
                        Text(
                            text = "Delete Data Permanently?",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFD32F2F)
                        )

                        Text(
                            text = "Are you sure you want to permanently delete the database and session files for ${source.displayName}?\n\nThis action cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        val pathsToDelete = source.getDataPathsToDelete()
                        if (pathsToDelete.isNotEmpty()) {
                            val lazyListState = rememberLazyListState()
                            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .dragToScroll(lazyListState)
                                        .background(CardSurface, RoundedCornerShape(8.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .padding(end = 12.dp)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "Target paths to be deleted:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                    }
                                    items(pathsToDelete) { path ->
                                        Text(
                                            text = path,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp),
                                    adapter = rememberScrollbarAdapter(lazyListState),
                                    style = themedScrollbarStyle().copy(thickness = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showDeleteConfirm = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = { onDeleteConfirm() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Confirm Delete", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.dragToScroll(
    scrollState: ScrollState,
    orientation: Orientation = Orientation.Vertical,
    priority: Int = 0
): Modifier = this.composed {
    val coroutineScope = rememberCoroutineScope()
    this
        .touchScrollable(scrollState, orientation, priority = priority)
        .draggable(
            orientation = orientation,
            state = rememberDraggableState { delta ->
                coroutineScope.launch {
                    scrollState.scrollBy(-delta)
                }
            }
        )
}

fun Modifier.dragToScroll(
    lazyListState: LazyListState,
    orientation: Orientation = Orientation.Vertical,
    priority: Int = 0
): Modifier = this.composed {
    val coroutineScope = rememberCoroutineScope()
    this
        .touchScrollable(lazyListState, orientation, priority = priority)
        .draggable(
            orientation = orientation,
            state = rememberDraggableState { delta ->
                coroutineScope.launch {
                    lazyListState.scrollBy(-delta)
                }
            }
        )
}

@Composable
fun themedScrollbarStyle(): ScrollbarStyle = defaultScrollbarStyle().copy(
    unhoverColor = AccentCyan.copy(alpha = 0.3f),
    hoverColor = AccentCyan.copy(alpha = 0.7f),
    thickness = 8.dp
)

