package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import llc.lookatwhataicando.codeoba.core.domain.model.Session
import llc.lookatwhataicando.codeoba.core.domain.model.Turn
import llc.lookatwhataicando.codeoba.core.domain.search.buildFindRegex

sealed class MessagePart {
    data class Text(val content: String) : MessagePart()
    data class Tool(
        val type: String,
        val header: String,
        val content: String,
        val timestamp: Long = 0L
    ) : MessagePart()
}

private fun String.unescapeToolTags(): String {
    return this.replace("\\[\\[\\[TOOL", "[[[TOOL")
        .replace("\\[\\[\\[/TOOL", "[[[/TOOL")
}

private fun isEscaped(text: String, index: Int): Boolean {
    var count = 0
    var i = index - 1
    while (i >= 0 && text[i] == '\\') {
        count++
        i--
    }
    return count % 2 != 0
}

fun parseAssistantMessage(message: String): List<MessagePart> {
    val parts = mutableListOf<MessagePart>()
    var currentIndex = 0
    while (currentIndex < message.length) {
        var startIdx = message.indexOf("[[[TOOL:", currentIndex)
        while (startIdx != -1 && isEscaped(message, startIdx)) {
            startIdx = message.indexOf("[[[TOOL:", startIdx + 8)
        }

        if (startIdx == -1) {
            val remaining = message.substring(currentIndex)
            if (remaining.isNotEmpty()) {
                parts.add(MessagePart.Text(remaining.unescapeToolTags()))
            }
            break
        }

        // Add preceding text if any
        if (startIdx > currentIndex) {
            val preceding = message.substring(currentIndex, startIdx)
            if (preceding.isNotEmpty()) {
                parts.add(MessagePart.Text(preceding.unescapeToolTags()))
            }
        }

        val headerEndIdx = message.indexOf("]]]", startIdx)
        if (headerEndIdx == -1) {
            parts.add(MessagePart.Text(message.substring(startIdx).unescapeToolTags()))
            break
        }

        val headerContent = message.substring(startIdx + 8, headerEndIdx)
        val partsOfHeader = headerContent.split('|')
        val type = partsOfHeader.getOrNull(0) ?: ""
        val header = partsOfHeader.getOrNull(1) ?: ""
        val timestamp = partsOfHeader.getOrNull(2)?.toLongOrNull() ?: 0L

        var endIdx = message.indexOf("[[[/TOOL]]]", headerEndIdx + 3)
        while (endIdx != -1 && isEscaped(message, endIdx)) {
            endIdx = message.indexOf("[[[/TOOL]]]", endIdx + 11)
        }

        if (endIdx == -1) {
            // Unclosed tool tag: treat prefix as text and search for subsequent tags
            val tagStart = message.substring(startIdx, startIdx + 8)
            parts.add(MessagePart.Text(tagStart.unescapeToolTags()))
            currentIndex = startIdx + 8
            continue
        }

        val content = message.substring(headerEndIdx + 3, endIdx)
        parts.add(MessagePart.Tool(
            type.unescapeToolTags(),
            header.unescapeToolTags(),
            content.unescapeToolTags(),
            timestamp
        ))
        currentIndex = endIdx + 11
    }
    return parts
}

sealed class MarkdownBlock {
    data class Header(val level: Int, val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val content: String) : MarkdownBlock()
    data class ListItem(val indentLevel: Int, val ordered: Boolean, val number: Int, val content: String) : MarkdownBlock()
    data class Paragraph(val content: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(lines: List<String>): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    var inCodeBlock = false
    var codeBlockLang = ""
    val codeBlockContent = StringBuilder()

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()

        if (inCodeBlock) {
            if (trimmed.startsWith("```")) {
                blocks.add(MarkdownBlock.CodeBlock(codeBlockLang, codeBlockContent.toString()))
                codeBlockContent.setLength(0)
                inCodeBlock = false
            } else {
                if (codeBlockContent.isNotEmpty()) {
                    codeBlockContent.append("\n")
                }
                codeBlockContent.append(line)
            }
            i++
            continue
        }

        if (trimmed.startsWith("```")) {
            inCodeBlock = true
            codeBlockLang = trimmed.substring(3).trim()
            i++
            continue
        }

        // Check for headers
        if (trimmed.startsWith("#")) {
            var level = 0
            while (level < trimmed.length && trimmed[level] == '#') {
                level++
            }
            if (level in 1..6 && level < trimmed.length && trimmed[level] == ' ') {
                val content = trimmed.substring(level + 1).trim()
                blocks.add(MarkdownBlock.Header(level, content))
                i++
                continue
            }
        }

        // Check for list items
        if ((trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("+ ")) && trimmed.length >= 2) {
            val indent = (line.length - trimmed.length) / 2
            val content = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.ListItem(indentLevel = indent, ordered = false, number = 0, content = content))
            i++
            continue
        }

        val dotIdx = trimmed.indexOf(". ")
        if (dotIdx != -1) {
            val numStr = trimmed.substring(0, dotIdx)
            if (numStr.all { it.isDigit() } && numStr.isNotEmpty()) {
                val indent = (line.length - trimmed.length) / 2
                val content = trimmed.substring(dotIdx + 2).trim()
                blocks.add(MarkdownBlock.ListItem(indentLevel = indent, ordered = true, number = numStr.toInt(), content = content))
                i++
                continue
            }
        }

        // Default: Paragraph
        blocks.add(MarkdownBlock.Paragraph(line))
        i++
    }

    if (inCodeBlock) {
        blocks.add(MarkdownBlock.CodeBlock(codeBlockLang, codeBlockContent.toString()))
    }

    return blocks
}

val MarkdownBlock.content: String
    get() = when (this) {
        is MarkdownBlock.Header -> content
        is MarkdownBlock.CodeBlock -> content
        is MarkdownBlock.ListItem -> content
        is MarkdownBlock.Paragraph -> content
    }

data class BlockKey(
    val turnIndex: Int,
    val isUser: Boolean,
    val partIndex: Int,
    val blockIndex: Int
)

data class FindMatch(
    val blockKey: BlockKey,
    val range: IntRange
)



fun buildSearchRegex(query: String, exactMatch: Boolean = false): Regex? {
    if (query.isBlank()) return null
    return try {
        if (exactMatch) {
            Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        } else {
            val terms = query.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (terms.isEmpty()) return null
            val pattern = terms.joinToString("|") { Regex.escape(it) }
            Regex(pattern, RegexOption.IGNORE_CASE)
        }
    } catch (e: Exception) {
        null
    }
}

fun stripInlineMarkdown(text: String): String {
    return buildString {
        var i = 0
        val n = text.length
        while (i < n) {
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    append(text.substring(i + 1, end))
                    i = end + 1
                    continue
                }
            }
            if (i + 1 < n && text[i] == '*' && text[i+1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    append(text.substring(i + 2, end))
                    i = end + 2
                    continue
                }
            }
            if (text[i] == '*') {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    append(text.substring(i + 1, end))
                    i = end + 1
                    continue
                }
            }
            var nextSpecial = n
            for (j in (i + 1) until n) {
                if (text[j] == '`' || text[j] == '*') {
                    nextSpecial = j
                    break
                }
            }
            append(text.substring(i, nextSpecial))
            i = nextSpecial
        }
    }
}

fun getBlockPlainText(block: MarkdownBlock): String {
    return when (block) {
        is MarkdownBlock.CodeBlock -> block.content
        else -> stripInlineMarkdown(block.content)
    }
}

fun findSessionMatches(
    session: Session,
    regex: Regex?
): List<FindMatch> {
    if (regex == null) return emptyList()
    val matches = mutableListOf<FindMatch>()

    session.turns.forEachIndexed { turnIndex, turn ->
        // User message
        if (turn.userMessage.isNotEmpty()) {
            val lines = turn.userMessage.split("\n")
            val blocks = parseMarkdownBlocks(lines)
            blocks.forEachIndexed { blockIndex, block ->
                val content = getBlockPlainText(block)
                regex.findAll(content).forEach { matchResult ->
                    matches.add(
                        FindMatch(
                            blockKey = BlockKey(turnIndex, isUser = true, partIndex = 0, blockIndex = blockIndex),
                            range = matchResult.range
                        )
                    )
                }
            }
        }

        // Assistant message
        if (turn.assistantMessage.isNotEmpty()) {
            val parts = parseAssistantMessage(turn.assistantMessage)
            parts.forEachIndexed { partIndex, part ->
                when (part) {
                    is MessagePart.Text -> {
                        val lines = part.content.split("\n")
                        val blocks = parseMarkdownBlocks(lines)
                        blocks.forEachIndexed { blockIndex, block ->
                            val content = getBlockPlainText(block)
                            regex.findAll(content).forEach { matchResult ->
                                matches.add(
                                    FindMatch(
                                        blockKey = BlockKey(turnIndex, isUser = false, partIndex = partIndex, blockIndex = blockIndex),
                                        range = matchResult.range
                                    )
                                )
                            }
                        }
                    }
                    is MessagePart.Tool -> {
                        regex.findAll(part.content).forEach { matchResult ->
                            matches.add(
                                FindMatch(
                                    blockKey = BlockKey(turnIndex, isUser = false, partIndex = partIndex, blockIndex = 0),
                                    range = matchResult.range
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    return matches
}

fun highlightAnnotatedString(
    annotatedString: AnnotatedString,
    regex: Regex?,
    blockKey: BlockKey?,
    activeMatch: FindMatch?,
    highlightColor: Color,
    activeHighlightColor: Color
): AnnotatedString {
    if (regex == null) return annotatedString
    val text = annotatedString.text
    val builder = AnnotatedString.Builder(annotatedString)
    
    regex.findAll(text).forEach { matchResult ->
        val range = matchResult.range
        val isActive = activeMatch != null && 
                       blockKey != null &&
                       activeMatch.blockKey == blockKey && 
                       activeMatch.range == range
        
        val style = SpanStyle(
            background = if (isActive) activeHighlightColor else highlightColor,
            fontWeight = FontWeight.Bold
        )
        builder.addStyle(style, range.first, range.last + 1)
    }
    return builder.toAnnotatedString()
}

fun parseInlineMarkdown(
    text: String,
    findRegex: Regex? = null,
    blockKey: BlockKey? = null,
    activeMatch: FindMatch? = null,
    highlightColor: Color = Color(0x66FF9100),
    activeHighlightColor: Color = Color(0xFFFF9100)
): AnnotatedString {
    val base = buildAnnotatedString {
        var i = 0
        val n = text.length
        while (i < n) {
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val codeContent = text.substring(i + 1, end)
                    withStyle(style = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x33757575),
                        color = AccentCyan
                    )) {
                        append(codeContent)
                    }
                    i = end + 1
                    continue
                }
            }
            
            if (i + 1 < n && text[i] == '*' && text[i+1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val boldContent = text.substring(i + 2, end)
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldContent)
                    }
                    i = end + 2
                    continue
                }
            }

            if (text[i] == '*') {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    val italicContent = text.substring(i + 1, end)
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(italicContent)
                    }
                    i = end + 1
                    continue
                }
            }

            var nextSpecial = n
            for (j in (i + 1) until n) {
                if (text[j] == '`' || text[j] == '*') {
                    nextSpecial = j
                    break
                }
            }
            val normalText = text.substring(i, nextSpecial)
            append(normalText)
            i = nextSpecial
        }
    }

    return highlightAnnotatedString(base, findRegex, blockKey, activeMatch, highlightColor, activeHighlightColor)
}

fun parseOnlyQueryHighlights(
    text: String,
    findRegex: Regex? = null,
    blockKey: BlockKey? = null,
    activeMatch: FindMatch? = null,
    highlightColor: Color = Color(0x66FF9100),
    activeHighlightColor: Color = Color(0xFFFF9100)
): AnnotatedString {
    val base = buildAnnotatedString {
        append(text)
    }
    return highlightAnnotatedString(base, findRegex, blockKey, activeMatch, highlightColor, activeHighlightColor)
}

fun parseInlineMarkdown(
    text: String,
    query: String,
    highlightColor: Color,
    exactMatch: Boolean
): AnnotatedString {
    val regex = buildSearchRegex(query, exactMatch)
    return parseInlineMarkdown(
        text = text,
        findRegex = regex,
        blockKey = null,
        activeMatch = null,
        highlightColor = highlightColor
    )
}

fun parseOnlyQueryHighlights(
    text: String,
    query: String,
    highlightColor: Color,
    exactMatch: Boolean
): AnnotatedString {
    val regex = buildSearchRegex(query, exactMatch)
    return parseOnlyQueryHighlights(
        text = text,
        findRegex = regex,
        blockKey = null,
        activeMatch = null,
        highlightColor = highlightColor
    )
}
