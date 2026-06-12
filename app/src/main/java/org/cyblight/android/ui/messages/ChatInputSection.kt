package org.cyblight.android.ui.messages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cyblight.android.R
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatInputSection(
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
    suppressedPreviewUrl: String?,
    onSuppressPreview: () -> Unit,
    replyTarget: ChatReplyTarget? = null,
    editTarget: ChatEditTarget? = null,
    onClearReply: () -> Unit = {},
    onClearEdit: () -> Unit = {},
    isSending: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showLinkDialog) {
        LinkInsertDialog(
            selectedText = draft.text.substring(draft.selection.min, draft.selection.max),
            onDismiss = { showLinkDialog = false },
            onConfirm = { label, url ->
                insertAtSelection(draft, onDraftChange, "[$label]($url)")
                showLinkDialog = false
            },
        )
    }

    if (showEmojiPicker) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            sheetState = sheetState,
        ) {
            EmojiPickerContent(
                onEmojiSelected = { emoji ->
                    insertAtSelection(draft, onDraftChange, emoji)
                },
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        replyTarget?.let { reply ->
            ChatComposeBanner(
                title = stringResource(R.string.chat_reply_to, reply.author),
                subtitle = reply.preview,
                onDismiss = onClearReply,
            )
        }
        editTarget?.let {
            ChatComposeBanner(
                title = stringResource(R.string.chat_editing_message),
                subtitle = null,
                onDismiss = onClearEdit,
            )
        }

        ChatFormattingToolbar(
            onBold = { wrapSelection(draft, onDraftChange, "**", "**") },
            onItalic = { wrapSelection(draft, onDraftChange, "_", "_") },
            onMono = { wrapSelection(draft, onDraftChange, "`", "`") },
            onStrike = { wrapSelection(draft, onDraftChange, "~~", "~~") },
            onSpoiler = { wrapSelection(draft, onDraftChange, "||", "||") },
            onLink = { showLinkDialog = true },
            onCode = {
                val selected = draft.text.substring(draft.selection.min, draft.selection.max)
                val block = "```\n${selected.ifBlank { "код" }}\n```"
                insertAtSelection(draft, onDraftChange, block)
            },
        )

        ComposeLinkPreview(
            draft = draft.text,
            suppressedUrl = suppressedPreviewUrl,
            onSuppress = onSuppressPreview,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(onClick = { showEmojiPicker = true }) {
                Icon(Icons.Outlined.EmojiEmotions, contentDescription = stringResource(R.string.chat_emoji))
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = {
                    Text(
                        if (editTarget != null) {
                            stringResource(R.string.chat_edit_placeholder)
                        } else {
                            stringResource(R.string.message_placeholder)
                        },
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    val text = draft.text.trim()
                    if (text.isBlank()) return@IconButton
                    var content = text
                    val firstUrl = ChatFormatUtils.extractFirstUrl(text)
                    if (suppressedPreviewUrl != null && firstUrl != null && suppressedPreviewUrl == firstUrl) {
                        content = ChatFormatUtils.appendNoPreviewToken(content, firstUrl)
                    }
                    onDraftChange(TextFieldValue())
                    onSend(content)
                },
                enabled = !isSending && draft.text.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.send))
            }
        }
    }
}

@Composable
private fun ChatComposeBanner(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun ChatFormattingToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onMono: () -> Unit,
    onStrike: () -> Unit,
    onSpoiler: () -> Unit,
    onLink: () -> Unit,
    onCode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FormatButton("B", fontWeight = FontWeight.Bold, onClick = onBold)
        FormatButton("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, onClick = onItalic)
        FormatButton("M", fontFamily = FontFamily.Monospace, onClick = onMono)
        FormatButton("S", textDecoration = TextDecoration.LineThrough, onClick = onStrike)
        FormatButton("||", onClick = onSpoiler)
        FormatButton("🔗", onClick = onLink)
        FormatButton("{ }", fontFamily = FontFamily.Monospace, onClick = onCode)
    }
}

@Composable
private fun FormatButton(
    label: String,
    fontWeight: FontWeight? = null,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontFamily: FontFamily? = null,
    textDecoration: TextDecoration? = null,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            label,
            style = TextStyle(
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontFamily = fontFamily,
                textDecoration = textDecoration,
                fontSize = 14.sp,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPickerContent(onEmojiSelected: (String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val categories = ChatEmojiData.categories

    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ChatEmojiData.quickEmojis.forEach { emoji ->
                TextButton(onClick = { onEmojiSelected(emoji) }) {
                    Text(emoji, fontSize = 22.sp)
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(category.icon) },
                )
            }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            categories[selectedTab].emojis.forEach { emoji ->
                TextButton(onClick = { onEmojiSelected(emoji) }) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun LinkInsertDialog(
    selectedText: String,
    onDismiss: () -> Unit,
    onConfirm: (label: String, url: String) -> Unit,
) {
    var url by remember { mutableStateOf("https://") }
    var label by remember { mutableStateOf(selectedText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_insert_link)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.chat_insert_link_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.chat_insert_link_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedUrl = url.trim()
                    if (trimmedUrl.isNotBlank()) {
                        onConfirm(label.trim().ifBlank { trimmedUrl }, trimmedUrl)
                    }
                },
            ) {
                Text(stringResource(R.string.send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun wrapSelection(
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

private fun insertAtSelection(
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
