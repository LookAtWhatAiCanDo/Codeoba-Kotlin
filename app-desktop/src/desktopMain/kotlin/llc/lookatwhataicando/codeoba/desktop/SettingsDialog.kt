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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter
import llc.lookatwhataicando.codeoba.core.domain.source.SourceRegistry
import llc.lookatwhataicando.codeoba.core.util.Logger.log

enum class SettingsCategory(val displayName: String) {
    General("General"),
    Sources("Sources")
}

@Composable
fun SettingsDialog(
    sourceRegistry: SourceRegistry,
    onClose: () -> Unit,
    onSettingsChanged: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.Sources) }
    var deletingSource by remember { mutableStateOf<SourceAdapter?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {}, // Consume clicks
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

                                Text(
                                    text = "General Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
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
                                }
                            }
                            SettingsCategory.Sources -> {
                                Text(
                                    text = "Sources & Adapters",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
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
                            .clickable(enabled = false) {},
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
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 120.dp)
                                            .background(CardSurface, RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
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
