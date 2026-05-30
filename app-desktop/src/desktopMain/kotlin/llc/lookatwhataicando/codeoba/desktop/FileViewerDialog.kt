package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whataicando.touch.compose.touchScrim
import llc.lookatwhataicando.codeoba.core.util.LocalFileResolution
import llc.lookatwhataicando.codeoba.core.util.LocalFileResolver
import java.awt.Cursor
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun FileViewerDialog(
    filePath: String,
    trustedRootPath: String?,
    onClose: () -> Unit,
    onUrlClick: (String) -> Unit
) {
    var fileContent by remember(filePath) { mutableStateOf<String?>(null) }
    var fileError by remember(filePath) { mutableStateOf<String?>(null) }
    var isBinaryFile by remember(filePath) { mutableStateOf(false) }
    var fileSize by remember(filePath) { mutableStateOf(0L) }
    var fileLastModified by remember(filePath) { mutableStateOf(0L) }
    var isLoading by remember(filePath) { mutableStateOf(true) }

    var pendingUrlClickPath by remember { mutableStateOf<java.nio.file.Path?>(null) }
    var pendingExternalOpen by remember { mutableStateOf(false) }
    var dontAskAgainChecked by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val currentFileParent = remember(filePath) {
        try { java.nio.file.Paths.get(filePath).parent } catch (_: Exception) { null }
    }
    val trustedRoot = remember(trustedRootPath) {
        try {
            if (!trustedRootPath.isNullOrBlank()) java.nio.file.Paths.get(trustedRootPath) else null
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(filePath) {
        isLoading = true
        fileError = null
        isBinaryFile = false
        fileContent = null
        try {
            val file = File(filePath)
            if (!file.exists()) {
                fileError = "File not found"
                isLoading = false
                return@LaunchedEffect
            }
            if (file.isDirectory) {
                fileError = "Selected path is a directory."
                isLoading = false
                return@LaunchedEffect
            }
            fileSize = file.length()
            fileLastModified = file.lastModified()

            val sizeLimit = 5 * 1024 * 1024 // 5MB limit
            val bytes = java.nio.file.Files.newInputStream(file.toPath()).use { input ->
                input.readNBytes(sizeLimit + 1)
            }
            if (bytes.size > sizeLimit) {
                fileError = "File is too large to render inside Codeoba (exceeds 5MB limit). Please open in external editor."
                isLoading = false
                return@LaunchedEffect
            }

            if (bytes.contains(0.toByte())) {
                isBinaryFile = true
                isLoading = false
                return@LaunchedEffect
            }

            fileContent = String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            fileError = e.message ?: "Failed to read file"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(3000)
            toastMessage = null
        }
    }

    val fileName = remember(filePath) {
        File(filePath).name
    }

    val formattedSize = remember(fileSize) {
        val kb = fileSize / 1024.0
        if (kb < 1024.0) {
            String.format("%.1f KB", kb)
        } else {
            String.format("%.2f MB", kb / 1024.0)
        }
    }

    val formattedDate = remember(fileLastModified) {
        if (fileLastModified > 0L) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            sdf.format(Date(fileLastModified))
        } else ""
    }

    fun openExternally() {
        val pathObj = try { java.nio.file.Paths.get(filePath) } catch (_: Exception) { null }
        val isTrusted = try {
            val realPath = pathObj?.toRealPath()
            val realRoot = trustedRoot?.toRealPath() ?: trustedRoot?.toAbsolutePath()?.normalize()
            realRoot != null && realPath != null && realPath.startsWith(realRoot)
        } catch (_: Exception) {
            false
        }

        if (isTrusted || trustedRoot == null) {
            try {
                val file = File(filePath)
                if (file.exists() && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file)
                }
            } catch (_: Exception) {}
        } else {
            val decision = PermissionManager.getDecision(filePath, PermissionManager.Action.EXTERNAL_OPEN)
            if (decision == PermissionManager.Decision.ALLOW) {
                try {
                    val file = File(filePath)
                    if (file.exists() && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file)
                    }
                } catch (_: Exception) {}
            } else if (decision == PermissionManager.Decision.DENY) {
                toastMessage = "External open blocked by user settings"
            } else {
                dontAskAgainChecked = false
                pendingExternalOpen = true
            }
        }
    }

    val handleUrlClick: (String) -> Unit = { url ->
        val trimmed = url.trim()
        if (isWebUrl(trimmed)) {
            onUrlClick(trimmed)
        } else {
            val fileParent = currentFileParent
            val resolved = LocalFileResolver.resolveLocalFileLink(trimmed, fileParent, trustedRoot)
            when (resolved) {
                is LocalFileResolution.Allowed -> {
                    onUrlClick(resolved.path.toString())
                }
                is LocalFileResolution.ConfirmationRequired -> {
                    val decision = PermissionManager.getDecision(resolved.path.toString(), PermissionManager.Action.PREVIEW)
                    if (decision == PermissionManager.Decision.ALLOW) {
                        onUrlClick(resolved.path.toString())
                    } else if (decision == PermissionManager.Decision.DENY) {
                        toastMessage = "Access denied by user settings"
                    } else {
                        pendingUrlClickPath = resolved.path
                        dontAskAgainChecked = false
                    }
                }
                is LocalFileResolution.Rejected -> {
                    toastMessage = resolved.reason
                }
            }
        }
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
                .width(860.dp)
                .height(640.dp)
                .padding(16.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = fileName,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (formattedSize.isNotEmpty() && !isLoading && fileError == null && !isBinaryFile) {
                                    Text(
                                        text = "($formattedSize)",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = filePath,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { openExternally() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardSurface,
                                contentColor = AccentCyan
                            ),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(14.dp).padding(end = 4.dp)
                            )
                            Text("Open Externally", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Close file viewer",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                // Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(ObsidianBg)
                        .padding(20.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (fileError != null) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = fileError ?: "An error occurred",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = { openExternally() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Open in External Editor")
                            }
                        }
                    } else if (isBinaryFile) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Binary file cannot be displayed inside the application.",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = { openExternally() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Open in External Editor")
                            }
                        }
                    } else {
                        val content = fileContent ?: ""
                        val isMarkdown = fileName.endsWith(".md", ignoreCase = true)

                        if (isMarkdown) {
                            val scrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .dragToScroll(scrollState)
                                        .verticalScroll(scrollState)
                                        .padding(end = 12.dp)
                                ) {
                                    SelectionContainer {
                                        MarkdownView(
                                            text = content,
                                            turnIndex = 0,
                                            isUser = false,
                                            partIndex = 0,
                                            query = "",
                                            findRegex = null,
                                            activeMatch = null,
                                            color = TextPrimary.copy(alpha = 0.9f),
                                            highlightColor = Color.Transparent,
                                            onUrlClick = onUrlClick
                                        )
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp),
                                    adapter = rememberScrollbarAdapter(scrollState),
                                    style = themedScrollbarStyle().copy(thickness = 6.dp)
                                )
                            }
                        } else {
                            val lines = remember(content) { content.split("\n") }
                            val lineCount = lines.size
                            val lineNumbersText = remember(lineCount) {
                                (1..lineCount).joinToString("\n") { it.toString() }
                            }

                            val verticalScrollState = rememberScrollState()
                            val horizontalScrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .dragToScroll(verticalScrollState, Orientation.Vertical)
                                        .dragToScroll(horizontalScrollState, Orientation.Horizontal)
                                        .verticalScroll(verticalScrollState)
                                        .horizontalScroll(horizontalScrollState)
                                        .padding(end = 12.dp, bottom = 12.dp)
                                ) {
                                    SelectionContainer {
                                        Row(modifier = Modifier.fillMaxHeight()) {
                                            Text(
                                                text = lineNumbersText,
                                                color = TextSecondary.copy(alpha = 0.4f),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.5.sp,
                                                lineHeight = 18.sp,
                                                modifier = Modifier.padding(end = 16.dp),
                                                fontWeight = FontWeight.Normal
                                            )

                                            Text(
                                                text = content,
                                                color = TextPrimary.copy(alpha = 0.85f),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.5.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(bottom = 12.dp).width(6.dp),
                                    adapter = rememberScrollbarAdapter(verticalScrollState),
                                    style = themedScrollbarStyle().copy(thickness = 6.dp)
                                )
                                HorizontalScrollbar(
                                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 12.dp).height(6.dp),
                                    adapter = rememberScrollbarAdapter(horizontalScrollState),
                                    style = themedScrollbarStyle().copy(thickness = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Footer
                if (!isLoading && fileError == null && !isBinaryFile && formattedDate.isNotEmpty()) {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Last Modified: $formattedDate",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Codeoba File Previewer",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Styled Toast Overlay
        AnimatedVisibility(
            visible = toastMessage != null,
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
                        contentDescription = "Notification",
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

        // Preview Permission Modal
        val pendingPath = pendingUrlClickPath
        if (pendingPath != null) {
            val canonicalStr = pendingPath.toString()
            AlertDialog(
                onDismissRequest = { pendingUrlClickPath = null },
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
                            modifier = Modifier.clickable { dontAskAgainChecked = !dontAskAgainChecked }
                        ) {
                            Checkbox(
                                checked = dontAskAgainChecked,
                                onCheckedChange = { dontAskAgainChecked = it },
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
                            if (dontAskAgainChecked) {
                                PermissionManager.setDecision(pathStr, PermissionManager.Action.PREVIEW, PermissionManager.Decision.ALLOW)
                            }
                            onUrlClick(pathStr)
                            pendingUrlClickPath = null
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
                            if (dontAskAgainChecked) {
                                PermissionManager.setDecision(pathStr, PermissionManager.Action.PREVIEW, PermissionManager.Decision.DENY)
                            }
                            pendingUrlClickPath = null
                        }
                    ) {
                        Text("Block", color = Color(0xFFEF5350), fontSize = 12.sp)
                    }
                },
                containerColor = SlateSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // External Open Permission Modal
        if (pendingExternalOpen) {
            AlertDialog(
                onDismissRequest = { pendingExternalOpen = false },
                title = {
                    Text(
                        text = "External Link Warning",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "This file lies outside your active session workspace. Do you want to open it externally in your OS handler?",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = filePath,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { dontAskAgainChecked = !dontAskAgainChecked }
                        ) {
                            Checkbox(
                                checked = dontAskAgainChecked,
                                onCheckedChange = { dontAskAgainChecked = it },
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
                            if (dontAskAgainChecked) {
                                PermissionManager.setDecision(filePath, PermissionManager.Action.EXTERNAL_OPEN, PermissionManager.Decision.ALLOW)
                            }
                            pendingExternalOpen = false
                            try {
                                val file = File(filePath)
                                if (file.exists() && Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().open(file)
                                }
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ObsidianBg)
                    ) {
                        Text("Open Externally", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (dontAskAgainChecked) {
                                PermissionManager.setDecision(filePath, PermissionManager.Action.EXTERNAL_OPEN, PermissionManager.Decision.DENY)
                            }
                            pendingExternalOpen = false
                        }
                    ) {
                        Text("Block", color = Color(0xFFEF5350), fontSize = 12.sp)
                    }
                },
                containerColor = SlateSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
