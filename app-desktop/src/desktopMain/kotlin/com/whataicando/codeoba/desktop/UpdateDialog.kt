package com.whataicando.codeoba.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whataicando.touch.compose.touchScrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.Cursor

@Composable
fun UpdateDialog(
    latestRelease: GitHubRelease,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadedSizeStr by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val matchingAsset = remember(latestRelease) {
        UpdateManager.getMatchingAsset(latestRelease)
    }

    fun startDownload() {
        val asset = matchingAsset
        if (asset == null) {
            downloadError = "No matching installer package found for your operating system."
            return
        }

        isDownloading = true
        downloadError = null
        downloadProgress = 0f
        downloadedSizeStr = "Connecting..."

        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                var lastUiUpdateMs = 0L
                val downloadedFile = kotlinx.coroutines.runInterruptible {
                    UpdateManager.downloadUpdate(asset) { progress, downloaded, total ->
                        val now = System.currentTimeMillis()
                        if (progress < 1f && now - lastUiUpdateMs < 100) return@downloadUpdate
                        lastUiUpdateMs = now
                        scope.launch(Dispatchers.Main) {
                            downloadProgress = progress
                            val downloadedMB = downloaded / (1024.0 * 1024.0)
                            downloadedSizeStr = if (total > 0) {
                                val totalMB = total / (1024.0 * 1024.0)
                                String.format("%.1f MB / %.1f MB (%.0f%%)", downloadedMB, totalMB, progress * 100f)
                            } else {
                                String.format("%.1f MB", downloadedMB)
                            }
                        }
                    }
                }

                // Download completed, execute installation
                scope.launch(Dispatchers.Main) {
                    try {
                        UpdateManager.installUpdate(downloadedFile)
                        // Exit the app immediately to release locks
                        java.lang.System.exit(0)
                    } catch (e: Exception) {
                        downloadError = "Failed to launch installer: ${e.localizedMessage ?: e.message}"
                        isDownloading = false
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                scope.launch(Dispatchers.Main) {
                    downloadError = e.localizedMessage ?: e.message ?: "Failed to download update"
                    isDownloading = false
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        isDownloading = false
        downloadProgress = 0f
        downloadedSizeStr = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .touchScrim(priority = 1)
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* consume clicks behind */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .height(480.dp)
                .padding(16.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New Update Available",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        
                        Text(
                            text = "v${UpdateManager.currentVersion} ➔ ${latestRelease.tagName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentCyan,
                            modifier = Modifier
                                .background(AccentCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "A new version of Codeoba is ready to install.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                // Body content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    if (isDownloading) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Downloading update package...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                color = AccentCyan,
                                trackColor = SlateSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = downloadedSizeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    } else if (downloadError != null) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Update Failed",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFEF5350)
                            )
                            Text(
                                text = downloadError ?: "An unknown error occurred.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Button(
                                onClick = { startDownload() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                            ) {
                                Text("Retry Download")
                            }
                        }
                    } else {
                        // Release changelog (rendered using MarkdownView)
                        val scrollState = rememberScrollState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .dragToScroll(scrollState)
                                    .verticalScroll(scrollState)
                                    .padding(end = 12.dp)
                            ) {
                                MarkdownView(
                                    text = latestRelease.body.ifBlank { "*No changelog description provided for this release.*" },
                                    turnIndex = 0,
                                    isUser = false,
                                    partIndex = 0,
                                    query = "",
                                    findRegex = null,
                                    activeMatch = null,
                                    color = TextPrimary.copy(alpha = 0.9f),
                                    highlightColor = Color.Transparent,
                                    onUrlClick = { openUrl(it) }
                                )
                            }
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp),
                                adapter = rememberScrollbarAdapter(scrollState),
                                style = themedScrollbarStyle().copy(thickness = 6.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isDownloading) {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { cancelDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier
                                .height(38.dp)
                                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Button(
                            onClick = {
                                SettingsManager.setSkippedVersion(latestRelease.tagName)
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                        ) {
                            Text("Skip This Version", style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier
                                .height(38.dp)
                                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                        ) {
                            Text("Later", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = { startDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                        ) {
                            Text("Update Now", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
