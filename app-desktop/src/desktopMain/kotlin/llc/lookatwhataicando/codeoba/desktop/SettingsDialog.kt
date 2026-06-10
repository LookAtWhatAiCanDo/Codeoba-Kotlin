package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter
import llc.lookatwhataicando.codeoba.core.domain.source.SourceRegistry
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar


enum class SettingsCategory(val displayName: String) {
    General("General"),
    Sources("Sources"),
    Semantic("Semantic")
}

@Composable
fun SettingsDialog(
    sourceRegistry: SourceRegistry,
    onClose: () -> Unit,
    onSettingsChanged: () -> Unit,
    onUpdateAvailable: (GitHubRelease) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.General) }
    var deletingSource by remember { mutableStateOf<SourceAdapter?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* consume clicks */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(760.dp)
                .height(520.dp)
                .padding(16.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Header/Close button in top right
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close Settings",
                        tint = TextSecondary
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left sidebar
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .drawBehind {
                                drawLine(
                                    color = BorderColor,
                                    start = Offset(size.width, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 20.dp, start = 8.dp)
                        )

                        // Category Buttons
                        SettingsCategory.values().forEach { category ->
                            val isSelected = selectedCategory == category
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CardSurface else Color.Transparent)
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(16.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(AccentCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) AccentCyan else TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Right Content Pane
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp)
                            .padding(top = 16.dp) // Leave room for Close button
                    ) {
                        when (selectedCategory) {
                            SettingsCategory.General -> {
                                var cacheEnabled by remember { mutableStateOf(SettingsManager.getCacheEnabled()) }
                                var autoUpdateEnabled by remember { mutableStateOf(SettingsManager.getAutoUpdateEnabled()) }

                                Text(
                                    text = "General Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                val scrollState = rememberScrollState()
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .dragToScroll(scrollState)
                                            .verticalScroll(scrollState)
                                            .padding(end = 12.dp)
                                    ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Persistent Startup Cache",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Speed up startup time by caching parsed sessions on disk.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }

                                            Switch(
                                                checked = cacheEnabled,
                                                onCheckedChange = { isChecked ->
                                                    cacheEnabled = isChecked
                                                    SettingsManager.setCacheEnabled(isChecked)
                                                    onSettingsChanged()
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = ObsidianBg,
                                                    checkedTrackColor = AccentCyan,
                                                    uncheckedThumbColor = TextSecondary,
                                                    uncheckedTrackColor = SlateSurface,
                                                    uncheckedBorderColor = BorderColor
                                                ),
                                                modifier = Modifier.pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Auto-Updates",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "Automatically check for new versions on startup.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }

                                                Switch(
                                                    checked = autoUpdateEnabled,
                                                    onCheckedChange = { isChecked ->
                                                        autoUpdateEnabled = isChecked
                                                        SettingsManager.setAutoUpdateEnabled(isChecked)
                                                        onSettingsChanged()
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = ObsidianBg,
                                                        checkedTrackColor = AccentCyan,
                                                        uncheckedThumbColor = TextSecondary,
                                                        uncheckedTrackColor = SlateSurface,
                                                        uncheckedBorderColor = BorderColor
                                                    ),
                                                    modifier = Modifier.pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
                                                )
                                            }

                                            HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))

                                            var checkingUpdates by remember { mutableStateOf(false) }
                                            var updateCheckResult by remember { mutableStateOf<String?>(null) }
                                            val coroutineScope = rememberCoroutineScope()

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Current Version: v${UpdateManager.currentVersion}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )

                                                Button(
                                                    onClick = {
                                                        if (!checkingUpdates) {
                                                            checkingUpdates = true
                                                            updateCheckResult = null
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                val release = UpdateManager.checkLatestRelease()
                                                                coroutineScope.launch(Dispatchers.Main) {
                                                                    checkingUpdates = false
                                                                    if (release != null) {
                                                                        SettingsManager.setLastUpdateCheck(System.currentTimeMillis())
                                                                        SettingsManager.setMinUpdateCheckIntervalSeconds(release.minAutoUpdateCheckIntervalSeconds.coerceIn(0L, Long.MAX_VALUE / 1000L))
                                                                        if (UpdateManager.isUpdateAvailable(release)) {
                                                                            onUpdateAvailable(release)
                                                                            onClose()
                                                                        } else {
                                                                            updateCheckResult = "Codeoba is up to date!"
                                                                        }
                                                                    } else {
                                                                        val detail = UpdateManager.lastCheckError
                                                                        updateCheckResult = if (detail != null) {
                                                                            "Unable to contact update server: $detail"
                                                                        } else {
                                                                            "Unable to contact update server."
                                                                        }
                                                                    }
                                                                }
                                                             }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = SlateSurface,
                                                        contentColor = AccentCyan
                                                    ),
                                                    border = BorderStroke(1.dp, BorderColor),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                ) {
                                                    if (checkingUpdates) {
                                                        CircularProgressIndicator(
                                                            color = AccentCyan,
                                                            modifier = Modifier.size(14.dp),
                                                            strokeWidth = 1.5.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Checking...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                    } else {
                                                        Text("Check for Updates", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }

                                            if (updateCheckResult != null) {
                                                Text(
                                                    text = updateCheckResult!!,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = if (updateCheckResult!!.contains("up to date")) AccentCyan else Color(0xFFEF5350)
                                                    ),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "Color Theme",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Select a handsome theme to style the workspace interface.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }

                                            val themeList = themes.values.toList()
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                themeList.chunked(3).forEach { rowThemes ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        rowThemes.forEach { theme ->
                                                            ThemeSelectorItem(theme, onSettingsChanged)
                                                        }
                                                        repeat(3 - rowThemes.size) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }

                                            if (ThemeManager.currentThemeCode == "custom") {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                CustomThemeEditorSection(onSettingsChanged)
                                            }
                                        }
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp),
                                    adapter = rememberScrollbarAdapter(scrollState),
                                    style = themedScrollbarStyle().copy(thickness = 6.dp)
                                )
                            }
                            }
                            SettingsCategory.Sources -> {
                                Text(
                                    text = "Sources & Adapters",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                val lazyListState = rememberLazyListState()
                                Box(modifier = Modifier.weight(1f)) {
                                    LazyColumn(
                                        state = lazyListState,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .dragToScroll(lazyListState)
                                            .padding(end = 12.dp)
                                    ) {
                                        items(sourceRegistry.getAllAdapters()) { adapter ->
                                            SourceSettingItem(
                                                source = adapter,
                                                onDecisionChange = { decision ->
                                                    SettingsManager.setUserDecision(adapter.id, decision)
                                                    onSettingsChanged()
                                                },
                                                onDeleteClick = {
                                                    deletingSource = adapter
                                                }
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
                            SettingsCategory.Semantic -> {
                                var similarityThreshold by remember { mutableStateOf(SettingsManager.getSimilarityThreshold()) }

                                Text(
                                    text = "Semantic Search Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                val scrollState = rememberScrollState()
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .dragToScroll(scrollState)
                                            .verticalScroll(scrollState)
                                            .padding(end = 12.dp)
                                    ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "Similarity Threshold",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Configure the minimum confidence score required for search matches. Lower values return more results (fuzzier), higher values return fewer results (stricter).",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Slider(
                                                    value = similarityThreshold,
                                                    onValueChange = { newValue ->
                                                        similarityThreshold = newValue
                                                        SettingsManager.setSimilarityThreshold(newValue)
                                                        onSettingsChanged()
                                                    },
                                                    valueRange = 0.0f..1.0f,
                                                    steps = 19, // Every 0.05 step
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = AccentCyan,
                                                        activeTrackColor = AccentCyan,
                                                        inactiveTrackColor = SlateSurface
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .width(52.dp)
                                                        .height(32.dp)
                                                        .background(SlateSurface, RoundedCornerShape(6.dp))
                                                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = String.format("%.2f", similarityThreshold),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = TextPrimary
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Button(
                                                    onClick = {
                                                        similarityThreshold = 0.30f
                                                        SettingsManager.setSimilarityThreshold(0.30f)
                                                        onSettingsChanged()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = SlateSurface,
                                                        contentColor = AccentCyan
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(32.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                                ) {
                                                    Text(
                                                        text = "Restore to Default",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        modifier = Modifier.offset(y = 0.5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp),
                                    adapter = rememberScrollbarAdapter(scrollState),
                                    style = themedScrollbarStyle().copy(thickness = 6.dp)
                                )
                            }
                            }
                        }
                    }
                }

                // Inner confirmation overlay for deleting source data
                val sourceToConfirmDelete = deletingSource
                if (sourceToConfirmDelete != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { /* consume clicks */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .width(400.dp)
                                .padding(24.dp)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Delete Data Permanently?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFD32F2F)
                                )

                                Text(
                                    text = "Are you sure you want to permanently delete the database and session files for ${sourceToConfirmDelete.displayName}?\n\nThis action cannot be undone.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                val pathsToDelete = sourceToConfirmDelete.getDataPathsToDelete()
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
                                        onClick = { deletingSource = null },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                                    }

                                    Button(
                                        onClick = {
                                            val success = sourceToConfirmDelete.deleteDataPaths()
                                            log("Deleted data paths for ${sourceToConfirmDelete.displayName}, success: $success")
                                            SettingsManager.setUserDecision(sourceToConfirmDelete.id, SettingsManager.Decision.IGNORE)
                                            deletingSource = null
                                            onSettingsChanged()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Confirm", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceSettingItem(
    source: SourceAdapter,
    onDecisionChange: (SettingsManager.Decision) -> Unit,
    onDeleteClick: () -> Unit
) {
    val decision = SettingsManager.getUserDecision(source.id)
    val appInstalled = source.isAppInstalled()
    val available = source.isAvailable()
    val isEffectiveEnabled = source.isEffectiveEnabled()

    println("RENDER_ITEM: ${source.id} -> decision = $decision, appInstalled = $appInstalled, available = $available, isEffectiveEnabled = $isEffectiveEnabled")

    val statusText = when {
        appInstalled && decision != SettingsManager.Decision.IGNORE -> "Active & Installed"
        appInstalled && decision == SettingsManager.Decision.IGNORE -> "Ignored (Installed)"
        !appInstalled && available && decision == SettingsManager.Decision.MONITOR -> "Monitored (Orphaned)"
        !appInstalled && available && decision == SettingsManager.Decision.IGNORE -> "Ignored (Orphaned)"
        !appInstalled && available && decision == SettingsManager.Decision.UNDECIDED -> "Orphaned Data (Will prompt)"
        !appInstalled && !available && decision == SettingsManager.Decision.MONITOR -> "Force Monitoring (Not Detected)"
        !appInstalled && !available && decision == SettingsManager.Decision.IGNORE -> "Ignored (Not Detected)"
        else -> "Not Detected"
    }

    val statusColor = when {
        statusText.startsWith("Active") -> AccentCyan
        statusText.startsWith("Monitored") || statusText.startsWith("Force Monitoring") -> AccentCyan.copy(alpha = 0.8f)
        statusText.startsWith("Ignored") -> TextSecondary
        statusText.startsWith("Orphaned") -> AccentPurple
        else -> TextSecondary.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = source.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        val productUrl = getProductUrl(source.id)
                        if (productUrl != null) {
                            Text(
                                text = "Visit Website",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = AccentCyan
                                ),
                                modifier = Modifier
                                    .clickable { openUrl(productUrl) }
                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Status: $statusText",
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                        if (available && !appInstalled) {
                            TooltipArea(
                                tooltip = {
                                    Surface(
                                        modifier = Modifier.shadow(4.dp),
                                        color = SlateSurface,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Text(
                                            text = "Clean up (delete) orphaned data",
                                            modifier = Modifier.padding(8.dp),
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                delayMillis = 500,
                                tooltipPlacement = TooltipPlacement.CursorPoint(
                                    alignment = Alignment.BottomEnd,
                                    offset = DpOffset(0.dp, 16.dp)
                                )
                            ) {
                                IconButton(
                                    onClick = onDeleteClick,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clean data paths",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isEffectiveEnabled,
                        onCheckedChange = { isChecked ->
                            val newDecision = if (isChecked) {
                                if (appInstalled || available) {
                                    SettingsManager.Decision.UNDECIDED
                                } else {
                                    SettingsManager.Decision.MONITOR
                                }
                            } else {
                                SettingsManager.Decision.IGNORE
                            }
                            onDecisionChange(newDecision)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = AccentCyan,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SlateSurface,
                            uncheckedBorderColor = BorderColor
                        ),
                        modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                    )
                }
            }

            // Setting Selector Row: RadioButtons or Segmented styled buttons
            if (isEffectiveEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        SettingsManager.Decision.UNDECIDED to "Default",
                        SettingsManager.Decision.MONITOR to "Force Monitor"
                    ).forEach { (mode, label) ->
                        val isSelected = decision == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AccentCyan else SlateSurface)
                                .border(1.dp, if (isSelected) Color.Transparent else BorderColor, RoundedCornerShape(6.dp))
                                .clickable { onDecisionChange(mode) }
                                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                                color = if (isSelected) ObsidianBg else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.ThemeSelectorItem(
    theme: ColorTheme,
    onSettingsChanged: () -> Unit
) {
    val isSelected = ThemeManager.currentThemeCode == theme.code
    Card(
        modifier = Modifier
            .weight(1f)
            .height(68.dp)
            .border(
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) theme.accentCyan else BorderColor
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                ThemeManager.currentThemeCode = theme.code
                SettingsManager.setThemeCode(theme.code)
                onSettingsChanged()
            }
            .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))),
        colors = CardDefaults.cardColors(containerColor = theme.slateSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = theme.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Background color dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(theme.obsidianBg)
                        .border(0.5.dp, theme.borderColor, CircleShape)
                )
                // Surface/Card color dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(theme.cardSurface)
                        .border(0.5.dp, theme.borderColor, CircleShape)
                )
                // Accent Cyan color dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(theme.accentCyan)
                )
                // Accent Purple color dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(theme.accentPurple)
                )
            }
        }
    }
}

@Composable
fun CustomThemeEditorSection(onSettingsChanged: () -> Unit) {
    var activeColorIndex by remember { mutableStateOf(2) } // default to Accent 1

    val activeColorValue = when (activeColorIndex) {
        0 -> ThemeManager.customBg
        1 -> ThemeManager.customSurface
        2 -> ThemeManager.customAccent1
        else -> ThemeManager.customAccent2
    }

    val hsl = remember(activeColorValue) { colorToHsl(activeColorValue) }
    var hue by remember(activeColorValue) { mutableStateOf(hsl[0]) }
    var saturation by remember(activeColorValue) { mutableStateOf(hsl[1]) }
    var lightness by remember(activeColorValue) { mutableStateOf(hsl[2]) }

    fun updateActiveColor(h: Float, s: Float, l: Float) {
        val newColor = hslToColor(h, s, l)
        when (activeColorIndex) {
            0 -> ThemeManager.updateCustomBg(newColor)
            1 -> ThemeManager.updateCustomSurface(newColor)
            2 -> ThemeManager.updateCustomAccent1(newColor)
            3 -> ThemeManager.updateCustomAccent2(newColor)
        }
    }

    fun rollComplementaryTheme() {
        val h1 = (0..359).random().toFloat()
        val relation = listOf(150, 180, 210).random()
        val h2 = (h1 + relation) % 360f
        val bgHue = (h1 + listOf(-30, 0, 30).random() + 360) % 360f
        
        val bgSat = (8..15).random() / 100f
        val bgLight = (4..7).random() / 100f
        val bg = hslToColor(bgHue, bgSat, bgLight)
        
        val surfaceLight = (8..11).random() / 100f
        val surface = hslToColor(bgHue, bgSat, surfaceLight)
        
        val accent1 = hslToColor(h1, (85..100).random() / 100f, (50..60).random() / 100f)
        val accent2 = hslToColor(h2, (75..95).random() / 100f, (55..65).random() / 100f)
        
        ThemeManager.updateCustomBg(bg)
        ThemeManager.updateCustomSurface(surface)
        ThemeManager.updateCustomAccent1(accent1)
        ThemeManager.updateCustomAccent2(accent2)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Theme Designer",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Button(
                onClick = {
                    rollComplementaryTheme()
                    onSettingsChanged()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = ObsidianBg
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(28.dp)
                    .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Roll Compliments",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.offset(y = (-0.5).dp)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "Bg" to ThemeManager.customBg,
                "Surface" to ThemeManager.customSurface,
                "Accent 1" to ThemeManager.customAccent1,
                "Accent 2" to ThemeManager.customAccent2
            ).forEachIndexed { index, (label, color) ->
                val isActive = activeColorIndex == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { activeColorIndex = index }
                        .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) AccentCyan else BorderColor.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isActive) AccentCyan else TextSecondary
                    )
                }
            }
        }

        HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hue: ${hue.toInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        updateActiveColor(hue, saturation, lightness)
                        onSettingsChanged()
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = BorderColor
                    ),
                    modifier = Modifier.weight(1f).height(18.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sat: ${(saturation * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = saturation,
                    onValueChange = {
                        saturation = it
                        updateActiveColor(hue, saturation, lightness)
                        onSettingsChanged()
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = BorderColor
                    ),
                    modifier = Modifier.weight(1f).height(18.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Light: ${(lightness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = lightness,
                    onValueChange = {
                        lightness = it
                        updateActiveColor(hue, saturation, lightness)
                        onSettingsChanged()
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = BorderColor
                    ),
                    modifier = Modifier.weight(1f).height(18.dp)
                )
            }
        }
    }
}


