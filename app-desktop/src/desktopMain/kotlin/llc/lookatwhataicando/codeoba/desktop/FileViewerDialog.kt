package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun FileViewerDialog(
    filePath: String,
    onClose: () -> Unit,
    onUrlClick: (String) -> Unit
) {
    var fileContent by remember(filePath) { mutableStateOf<String?>(null) }
    var fileError by remember(filePath) { mutableStateOf<String?>(null) }
    var isBinaryFile by remember(filePath) { mutableStateOf(false) }
    var fileSize by remember(filePath) { mutableStateOf(0L) }
    var fileLastModified by remember(filePath) { mutableStateOf(0L) }
    var isLoading by remember(filePath) { mutableStateOf(true) }

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

            if (fileSize > 5 * 1024 * 1024) { // 5MB limit
                fileError = "File is too large to render inside Codeoba (${fileSize / 1024} KB). Please open in external editor."
                isLoading = false
                return@LaunchedEffect
            }

            val bytes = file.readBytes()
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
        try {
            val file = File(filePath)
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                                imageVector = Icons.Default.Launch,
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
    }
}
