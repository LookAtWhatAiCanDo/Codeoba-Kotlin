package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whataicando.touch.compose.touchScrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import llc.lookatwhataicando.codeoba.core.auth.LocalAuthServer
import llc.lookatwhataicando.codeoba.core.domain.auth.FirebaseAuthClient
import llc.lookatwhataicando.codeoba.core.domain.parser.LogParserFactory
import llc.lookatwhataicando.codeoba.core.domain.parser.ParserMode
import llc.lookatwhataicando.codeoba.core.domain.source.SourceAdapter
import llc.lookatwhataicando.codeoba.core.domain.source.SourceRegistry
import llc.lookatwhataicando.codeoba.core.security.DeviceKeyManager
import llc.lookatwhataicando.codeoba.core.util.AppConfig
import llc.lookatwhataicando.codeoba.core.util.Logger.log
import java.awt.Cursor
import com.whataicando.touch.compose.touchScrim
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import llc.lookatwhataicando.codeoba.core.util.PlatformUtils
import java.awt.Cursor
import java.io.File

enum class SettingsCategory(val displayName: String) {
    General("General"),
    Sources("Sources"),
    Semantic("Semantic"),
    Permissions("Permissions"),
    Account("Account & Subscription")
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
            .touchScrim(priority = 1)
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
                                            .dragToScroll(scrollState, priority = 1)
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
                                                    text = "Log Parsing Mode",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Configure how conversation transcripts are processed and summarized.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }

                                            val isSubscribed = SettingsManager.getEcosystemActive()
                                            var currentPreferredMode by remember { mutableStateOf(SettingsManager.getPreferredParserMode()) }

                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                // Option 1: Standard Parsing (Always available)
                                                val isStandardSelected = !isSubscribed || currentPreferredMode == ParserMode.STANDARD
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isStandardSelected) SlateSurface else Color.Transparent)
                                                        .clickable {
                                                            currentPreferredMode = ParserMode.STANDARD
                                                            SettingsManager.setPreferredParserMode(ParserMode.STANDARD)
                                                            LogParserFactory.setParserMode(ParserMode.STANDARD)
                                                            onSettingsChanged()
                                                        }
                                                        .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isStandardSelected) AccentCyan else Color.Transparent)
                                                            .border(1.5.dp, if (isStandardSelected) AccentCyan else BorderColor, CircleShape)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = "Standard Parsing",
                                                            color = if (isStandardSelected) TextPrimary else TextSecondary,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = if (isStandardSelected) FontWeight.SemiBold else FontWeight.Normal
                                                        )
                                                        Text(
                                                            text = "Parses conversation logs normally without AI-generated summaries.",
                                                            color = TextSecondary,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp)
                                                        )
                                                    }
                                                }

                                                // Option 2: AI-Powered Summarization (Requires active subscription)
                                                val isSummarizingSelected = isSubscribed && currentPreferredMode == ParserMode.SUMMARIZING
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSummarizingSelected) SlateSurface else Color.Transparent)
                                                        .clickable {
                                                            if (isSubscribed) {
                                                                currentPreferredMode = ParserMode.SUMMARIZING
                                                                SettingsManager.setPreferredParserMode(ParserMode.SUMMARIZING)
                                                                LogParserFactory.setParserMode(ParserMode.SUMMARIZING)
                                                                onSettingsChanged()
                                                            }
                                                        }
                                                        .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(if (isSubscribed) java.awt.Cursor.HAND_CURSOR else java.awt.Cursor.DEFAULT_CURSOR)))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSummarizingSelected) AccentCyan else Color.Transparent)
                                                            .border(1.5.dp, if (isSummarizingSelected) AccentCyan else BorderColor, CircleShape)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "AI-Powered Summarization",
                                                                color = if (isSummarizingSelected) TextPrimary else if (isSubscribed) TextSecondary else TextSecondary.copy(alpha = 0.5f),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = if (isSummarizingSelected) FontWeight.SemiBold else FontWeight.Normal
                                                            )
                                                            if (!isSubscribed) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(AccentPurple.copy(alpha = 0.12f))
                                                                        .border(0.5.dp, AccentPurple.copy(alpha = 0.40f), RoundedCornerShape(4.dp))
                                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Premium",
                                                                        color = AccentPurple,
                                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp),
                                                                        modifier = Modifier.offset(y = 0.5.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = "Runs local LLM inference to automatically generate conversation summaries, error reports, and performance charts.",
                                                            color = if (isSubscribed) TextSecondary else TextSecondary.copy(alpha = 0.5f),
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp)
                                                        )
                                                    }
                                                }
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
                                            .dragToScroll(lazyListState, priority = 1)
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
                                            .dragToScroll(scrollState, priority = 1)
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
                            SettingsCategory.Permissions -> {
                                var permissionsList by remember { mutableStateOf(PermissionManager.getAllDecisions()) }

                                Text(
                                    text = "Workspace Path Permissions",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                if (permissionsList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No custom path permissions saved.",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    val permissionsScrollState = rememberScrollState()
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .dragToScroll(permissionsScrollState, priority = 1)
                                            .verticalScroll(permissionsScrollState)
                                    ) {
                                        permissionsList.forEach { entry ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(
                                                            text = entry.path,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                                            color = TextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                            Text(
                                                                text = "Preview: ${entry.previewDecision}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = if (entry.previewDecision == PermissionManager.Decision.ALLOW) AccentCyan else TextSecondary
                                                            )
                                                            Text(
                                                                text = "External Open: ${entry.externalDecision}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = if (entry.externalDecision == PermissionManager.Decision.ALLOW) AccentCyan else TextSecondary
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                                    ) {
                                                        if (entry.previewDecision != PermissionManager.Decision.ASK) {
                                                            Button(
                                                                onClick = {
                                                                    PermissionManager.removeActionDecision(entry.path, PermissionManager.Action.PREVIEW)
                                                                    permissionsList = PermissionManager.getAllDecisions()
                                                                    onSettingsChanged()
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = SlateSurface, contentColor = TextPrimary),
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier.height(28.dp),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                            ) {
                                                                Text("Reset Preview", style = MaterialTheme.typography.labelSmall)
                                                            }
                                                        }

                                                        if (entry.externalDecision != PermissionManager.Decision.ASK) {
                                                            Button(
                                                                onClick = {
                                                                    PermissionManager.removeActionDecision(entry.path, PermissionManager.Action.EXTERNAL_OPEN)
                                                                    permissionsList = PermissionManager.getAllDecisions()
                                                                    onSettingsChanged()
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = SlateSurface, contentColor = TextPrimary),
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier.height(28.dp),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                            ) {
                                                                Text("Reset External", style = MaterialTheme.typography.labelSmall)
                                                            }
                                                        }

                                                        Button(
                                                            onClick = {
                                                                PermissionManager.removeDecision(entry.path)
                                                                permissionsList = PermissionManager.getAllDecisions()
                                                                onSettingsChanged()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = SlateSurface, contentColor = Color(0xFFEF5350)),
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier.height(28.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                        ) {
                                                            Text("Clear All", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            SettingsCategory.Account -> {
                                AccountSettingsSection(onSettingsChanged)
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
                                                .dragToScroll(lazyListState, priority = 1)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountSettingsSection(onSettingsChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingPortal by remember { mutableStateOf(false) }

    val savedEmail = SettingsManager.getFirebaseUserEmail()
    val savedUid = SettingsManager.getFirebaseUserUid()
    
    val isSubscribed = SettingsManager.getEcosystemActive()

    Text(
        text = "Account & Subscription",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (!savedUid.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "User Profile",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Email Address", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(savedEmail?.ifBlank { "Unknown" } ?: "Unknown", color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("User UID", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(savedUid ?: "Unknown", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    
                    Text(
                        text = "Ecosystem Sync Status",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Device Identity Status", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        
                        val statusText = if (isSubscribed) "Synced & Authenticated" else "Local Mode"
                        val statusColor = if (isSubscribed) AccentCyan else TextSecondary
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(statusText, color = statusColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    val deviceId = SettingsManager.getDeviceId()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Device Identifier", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(deviceId, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }

                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    Text(
                        text = "Sync Mode",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    var currentSyncMode by remember { mutableStateOf(SettingsManager.getSyncMode()) }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsManager.SyncMode.values().forEach { mode ->
                            val isSelected = currentSyncMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SlateSurface else Color.Transparent)
                                    .clickable {
                                        currentSyncMode = mode
                                        SettingsManager.setSyncMode(mode)
                                        onSettingsChanged()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) AccentCyan else Color.Transparent)
                                        .border(1.5.dp, if (isSelected) AccentCyan else BorderColor, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = mode.name.replace("_", " "),
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    val desc = when (mode) {
                                        SettingsManager.SyncMode.LOCAL_ONLY -> "No data leaves your device. Local lexical/semantic search only."
                                        SettingsManager.SyncMode.METADATA_ONLY -> "Only task names, state, and durations are synced to the Sync Hub (Default)."
                                        SettingsManager.SyncMode.SUMMARIES_ONLY -> "Syncs high-level task summaries without raw logs or terminal output."
                                        SettingsManager.SyncMode.FULL_SYNC -> "Syncs raw conversation turns & terminal outputs. Required for remote command approvals."
                                    }
                                    Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp))
                                }
                            }
                        }
                    }

                    if (currentSyncMode == SettingsManager.SyncMode.FULL_SYNC) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD32F2F).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFFD32F2F).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Warning: Full Sync uploads raw agent conversations, terminal output, file paths, source snippets, and command results to the Sync Hub. Enable only for workspaces where cloud sync is permitted. Content is encrypted at rest under keys Codeoba manages to enable remote search capabilities.",
                                color = Color(0xFFEF5350),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    Text(
                        text = "Workspace Path Exclusions",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    var pathInput by remember { mutableStateOf("") }
                    var excludedPaths by remember { mutableStateOf(SettingsManager.getExcludedPaths()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pathInput,
                            onValueChange = { pathInput = it },
                            placeholder = { Text("e.g. /Users/name/sensitive-dir", color = TextSecondary, style = MaterialTheme.typography.bodySmall) },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SlateSurface,
                                unfocusedContainerColor = SlateSurface
                            ),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                if (pathInput.isNotBlank()) {
                                    val updated = excludedPaths + pathInput.trim()
                                    excludedPaths = updated
                                    SettingsManager.setExcludedPaths(updated)
                                    pathInput = ""
                                    onSettingsChanged()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Add", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (excludedPaths.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            excludedPaths.forEach { path ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SlateSurface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(path, color = TextPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    IconButton(
                                        onClick = {
                                            val updated = excludedPaths.filter { it != path }
                                            excludedPaths = updated
                                            SettingsManager.setExcludedPaths(updated)
                                            onSettingsChanged()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Remove",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    Text(
                        text = "Remote Control Execution Policy",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    var currentPolicy by remember { mutableStateOf(SettingsManager.getRemoteControlPolicy()) }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsManager.RemoteControlPolicy.values().forEach { policy ->
                            val isPolicySelected = currentPolicy == policy
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPolicySelected) SlateSurface else Color.Transparent)
                                    .clickable {
                                        currentPolicy = policy
                                        SettingsManager.setRemoteControlPolicy(policy)
                                        onSettingsChanged()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isPolicySelected) AccentCyan else Color.Transparent)
                                        .border(1.5.dp, if (isPolicySelected) AccentCyan else BorderColor, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = policy.name.replace("_", " "),
                                        color = if (isPolicySelected) TextPrimary else TextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isPolicySelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    val desc = when (policy) {
                                        SettingsManager.RemoteControlPolicy.ALLOW_ALL -> "Allow all authorized ecosystem devices to run commands without pairing confirmation."
                                        SettingsManager.RemoteControlPolicy.ALLOW_PAIRED_ONLY -> "Only allow execution of modification commands from explicitly paired companion devices."
                                        SettingsManager.RemoteControlPolicy.BLOCK_ALL -> "Block all remote command execution requests on this machine."
                                    }
                                    Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                SettingsManager.setFirebaseUserEmail(null)
                                SettingsManager.setFirebaseUserUid(null)
                                SettingsManager.setFirebaseAuthIdToken(null)
                                SettingsManager.setFirebaseAuthRefreshToken(null)
                                SettingsManager.setEcosystemActive(false)
                                LogParserFactory.setParserMode(SettingsManager.getEffectiveParserMode())
                                onSettingsChanged()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateSurface, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sign Out", style = MaterialTheme.typography.labelLarge)
                        }

                        val polarButtonText = if (!isSubscribed) "Subscribe" else "Manage Subscription"
                        val polarButtonColors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSubscribed) AccentPurple else AccentCyan,
                            contentColor = if (!isSubscribed) Color.White else ObsidianBg
                        )

                        Button(
                            onClick = {
                                if (!isSubscribed) {
                                    try {
                                        val targetUrl = "${AppConfig.getWebConsoleUrl()}/dashboard?tab=subs"
                                        java.awt.Desktop.getDesktop().browse(java.net.URI(targetUrl))
                                    } catch (_: Exception) {}
                                } else {
                                    isLoadingPortal = true
                                    scope.launch {
                                        try {
                                            val idToken = SettingsManager.getFirebaseAuthIdToken()
                                            if (idToken != null) {
                                                try {
                                                    val portalUrl = FirebaseAuthClient.getCustomerPortalUrl(idToken)
                                                    java.awt.Desktop.getDesktop().browse(java.net.URI(portalUrl))
                                                } catch (e: Exception) {
                                                    val isAuthError = e.message?.contains("auth", ignoreCase = true) == true ||
                                                                    e.message?.contains("token", ignoreCase = true) == true ||
                                                                    e.message?.contains("expired", ignoreCase = true) == true
                                                    
                                                    if (isAuthError) {
                                                        val refreshToken = SettingsManager.getFirebaseAuthRefreshToken()
                                                        if (refreshToken != null) {
                                                            try {
                                                                val refreshedAuth = FirebaseAuthClient.refreshIdToken(refreshToken)
                                                                if (refreshedAuth.idToken.isNotBlank()) {
                                                                    SettingsManager.setFirebaseAuthIdToken(refreshedAuth.idToken)
                                                                }
                                                                if (refreshedAuth.refreshToken.isNotBlank()) {
                                                                    SettingsManager.setFirebaseAuthRefreshToken(refreshedAuth.refreshToken)
                                                                }
                                                                val portalUrl = FirebaseAuthClient.getCustomerPortalUrl(refreshedAuth.idToken)
                                                                java.awt.Desktop.getDesktop().browse(java.net.URI(portalUrl))
                                                            } catch (refreshEx: Exception) {
                                                                log("Failed to refresh token after auth error: ${refreshEx.message}")
                                                                throw e
                                                            }
                                                        } else {
                                                            throw e
                                                        }
                                                    } else {
                                                        throw e
                                                    }
                                                }
                                            } else {
                                                // Fallback if no token is available
                                                val targetUrl = "${AppConfig.getWebConsoleUrl()}/dashboard?tab=subs"
                                                java.awt.Desktop.getDesktop().browse(java.net.URI(targetUrl))
                                            }
                                        } catch (e: Exception) {
                                            log("Failed to load customer portal directly: ${e.message}", e)
                                            try {
                                                val targetUrl = "${AppConfig.getWebConsoleUrl()}/dashboard?tab=subs"
                                                java.awt.Desktop.getDesktop().browse(java.net.URI(targetUrl))
                                            } catch (_: Exception) {}
                                        } finally {
                                            isLoadingPortal = false
                                        }
                                    }
                                }
                            },
                            enabled = !isLoadingPortal,
                            modifier = Modifier.weight(1f),
                            colors = polarButtonColors,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isLoadingPortal) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = ObsidianBg,
                                        strokeWidth = 2.dp
                                    )
                                    Text("Loading Portal...", style = MaterialTheme.typography.labelLarge)
                                }
                            } else {
                                Text(polarButtonText, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Connect Codeoba Account",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Text(
                        text = "To sync your configurations, remote control policies, and access device features, connect your local Codeoba instance to your account. This will open a secure authentication portal in your web browser.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp)
                    )

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val port = LocalAuthServer.start { idToken, refreshToken, email, uid ->
                                        scope.launch {
                                            try {
                                                SettingsManager.setFirebaseUserEmail(email)
                                                SettingsManager.setFirebaseUserUid(uid)
                                                SettingsManager.setFirebaseAuthIdToken(idToken)
                                                SettingsManager.setFirebaseAuthRefreshToken(refreshToken)

                                                val deviceId = SettingsManager.getDeviceId()
                                                val host = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { try { java.net.InetAddress.getLocalHost().hostName } catch (_: Exception) { "Unknown" } }
                                                val deviceName = "${when { PlatformUtils.isMac() -> "macOS"; PlatformUtils.isWindows() -> "Windows"; else -> "Linux" }} ($host)"
                                                val publicKeyPem = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { DeviceKeyManager.getPublicKeyPem() }
                                                val nonce = FirebaseAuthClient.getRegistrationChallenge(idToken, deviceId)
                                                val signature = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { DeviceKeyManager.signPayload(nonce) }

                                                val registered = FirebaseAuthClient.registerEcosystemDevice(idToken, deviceId, deviceName, publicKeyPem, nonce, signature)
                                                require(registered) { "Device registration failed." }
                                                SettingsManager.setEcosystemActive(true)
                                                SettingsManager.setPreferredParserMode(ParserMode.SUMMARIZING)
                                                LogParserFactory.setParserMode(SettingsManager.getEffectiveParserMode())

                                                onSettingsChanged()
                                            } catch (e: Exception) {
                                                log("Error configuring device keys: ${e.message}")
                                                // Roll back partial sign-in state so the user can retry cleanly.
                                                SettingsManager.setFirebaseUserEmail(null)
                                                SettingsManager.setFirebaseUserUid(null)
                                                SettingsManager.setFirebaseAuthIdToken(null)
                                                SettingsManager.setFirebaseAuthRefreshToken(null)
                                                SettingsManager.setEcosystemActive(false)
                                                onSettingsChanged()
                                                errorMessage = e.message ?: "Error configuring device credentials."
                                                isLoading = false
                                            }
                                        }
                                    }

                                    val baseUrl = "${AppConfig.getWebConsoleUrl()}/connect"
                                    val stateToken = LocalAuthServer.expectedState ?: ""
                                    val url = "$baseUrl?port=$port&state=${java.net.URLEncoder.encode(stateToken, "UTF-8")}"
                                    
                                    try {
                                        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                                        // Match LocalAuthServer's 5-minute safety timeout so the UI can recover.
                                        scope.launch {
                                            kotlinx.coroutines.delay(5 * 60 * 1000L)
                                            if (isLoading && SettingsManager.getFirebaseUserEmail() == null) {
                                                errorMessage = "Authentication timed out. Please try again."
                                                isLoading = false
                                                LocalAuthServer.stop()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to launch system browser. Please open the URL manually:\n$url"
                                        isLoading = false
                                        LocalAuthServer.stop()
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to start local authentication listener."
                                    isLoading = false
                                    LocalAuthServer.stop()
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (isLoading) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = ObsidianBg, modifier = Modifier.size(18.dp))
                                Text("Awaiting Browser Authentication...", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Text("Connect Codeoba Account", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
