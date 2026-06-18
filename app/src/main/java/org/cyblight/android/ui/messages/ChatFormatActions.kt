package org.cyblight.android.ui.messages

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object ChatFormatActions {
    fun wrapSelection(
        current: TextFieldValue,
        onChange: (TextFieldValue) -> Unit,
        startToken: String,
        endToken: String,
    ) {
        val selected = current.text.substring(current.selection.min, current.selection.max)
        val newText = buildString {
            append(current.text.substring(0, current.selection.min))
            append(startToken)
            append(selected)
            append(endToken)
            append(current.text.substring(current.selection.max))
        }
        val cursorStart = current.selection.min + startToken.length
        val cursorEnd = cursorStart + selected.length
        onChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(cursorStart, cursorEnd),
            ),
        )
    }

    fun insertAtSelection(
        current: TextFieldValue,
        onChange: (TextFieldValue) -> Unit,
        insertion: String,
    ) {
        val newText = buildString {
            append(current.text.substring(0, current.selection.min))
            append(insertion)
            append(current.text.substring(current.selection.max))
        }
        val cursor = current.selection.min + insertion.length
        onChange(TextFieldValue(newText, TextRange(cursor, cursor)))
    }

    fun insertBlockquote(
        current: TextFieldValue,
        onChange: (TextFieldValue) -> Unit,
    ) {
        val start = current.selection.min
        val end = current.selection.max
        val text = current.text
        val selected = text.substring(start, end)
        if (selected.isNotEmpty()) {
            val quoted = selected
                .split('\n')
                .joinToString("\n") { line -> if (line.isEmpty()) ">" else "> $line" }
            insertAtSelection(current, onChange, quoted)
            return
        }

        val lineStart = text.lastIndexOf('\n', startIndex = (start - 1).coerceAtLeast(0)) + 1
        val newText = buildString {
            append(text.substring(0, lineStart))
            append("> ")
            append(text.substring(lineStart))
        }
        val cursor = start + 2
        onChange(TextFieldValue(newText, TextRange(cursor, cursor)))
    }

    fun stripFormatting(
        current: TextFieldValue,
        onChange: (TextFieldValue) -> Unit,
    ) {
        val start = current.selection.min
        val end = current.selection.max
        if (start == end) return
        val selected = current.text.substring(start, end)
        val plain = selected
            .replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
            .replace(Regex("""__([^_]+)__"""), "$1")
            .replace(Regex("""_([^_]+)_"""), "$1")
            .replace(Regex("""~~([^~]+)~~"""), "$1")
            .replace(Regex("""`([^`]+)`"""), "$1")
            .replace(Regex("""\|\|([^|]+)\|\|"""), "$1")
            .replace(Regex("""\[([^\]]+)]\(([^)]+)\)"""), "$1")
            .replace(Regex("""(?m)^>\s?"""), "")
        val newText = buildString {
            append(current.text.substring(0, start))
            append(plain)
            append(current.text.substring(end))
        }
        onChange(TextFieldValue(newText, TextRange(start, start + plain.length)))
    }

    fun cut(
        current: TextFieldValue,
        onChange: (TextFieldValue) -> Unit,
        setClipboard: (String) -> Unit,
    ) {
        val start = current.selection.min
        val end = current.selection.max
        if (start == end) return
        setClipboard(current.text.substring(start, end))
        val newText = buildString {
            append(current.text.substring(0, start))
            append(current.text.substring(end))
        }
        onChange(TextFieldValue(newText, TextRange(start, start)))
    }

    fun copy(
        current: TextFieldValue,
        setClipboard: (String) -> Unit,
    ) {
        val start = current.selection.min
        val end = current.selection.max
        if (start == end) return
        setClipboard(current.text.substring(start, end))
    }

    fun paste(
        current: TextFieldValue,
        onChange: (TextFieldValue) -> Unit,
        clipboardText: String?,
    ) {
        val insertion = clipboardText.orEmpty()
        if (insertion.isEmpty()) return
        insertAtSelection(current, onChange, insertion)
    }

    fun hasSelection(current: TextFieldValue): Boolean =
        current.selection.min != current.selection.max
}
