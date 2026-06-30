package org.cyblight.android.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
    formatToolbarHidden: Boolean = false,
    onFormatToolbarHiddenChange: (Boolean) -> Unit = {},
    sendWithEnter: Boolean = false,
    isSending: Boolean,
    onSend: (String, Boolean) -> Unit,
    quoteColor: Color = Color(0xFF60A5FA),
    onFormatFromMenu: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val visualTransformation = remember(quoteColor) {
        ChatInputVisualTransformation(quoteColor)
    }

    fun submitDraft(sentViaEnter: Boolean = false) {
        val text = draft.text.trim()
        if (text.isBlank() || isSending) return
        var content = text
        val firstUrl = ChatFormatUtils.extractFirstUrl(text)
        if (suppressedPreviewUrl != null && firstUrl != null && suppressedPreviewUrl == firstUrl) {
            content = ChatFormatUtils.appendNoPreviewToken(content, firstUrl)
        }
        onDraftChange(TextFieldValue())
        onSend(content, sentViaEnter)
    }

    if (showLinkDialog) {
        LinkInsertDialog(
            selectedText = draft.text.substring(draft.selection.min, draft.selection.max),
            onDismiss = { showLinkDialog = false },
            onConfirm = { label, url ->
                ChatFormatActions.insertAtSelection(draft, onDraftChange, "[$label]($url)")
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
                    ChatFormatActions.insertAtSelection(draft, onDraftChange, emoji)
                },
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        replyTarget?.let { reply ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatReplySnippet(
                    author = stringResource(R.string.chat_reply_to, reply.author),
                    text = reply.preview.ifBlank { stringResource(R.string.chat_reply_message) },
                    isOutgoing = false,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClearReply) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
        }
        editTarget?.let {
            ChatComposeBanner(
                title = stringResource(R.string.chat_editing_message),
                subtitle = null,
                onDismiss = onClearEdit,
            )
        }

        val hasSelection = ChatFormatActions.hasSelection(draft)

        if (!hasSelection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalIconButton(
                    onClick = { onFormatToolbarHiddenChange(!formatToolbarHidden) },
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (formatToolbarHidden) FontWeight.Normal else FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
                if (!formatToolbarHidden) {
                    ChatFormattingBar(
                        draft = draft,
                        onDraftChange = onDraftChange,
                        onShowLinkDialog = { showLinkDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        ComposeLinkPreview(
            draft = draft.text,
            suppressedUrl = suppressedPreviewUrl,
            onSuppress = onSuppressPreview,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )

        ChatTextSelectionBar(
            draft = draft,
            onDraftChange = onDraftChange,
            onShowLinkDialog = { showLinkDialog = true },
            onFormatFromMenu = onFormatFromMenu,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(onClick = { showEmojiPicker = true }) {
                Icon(Icons.Outlined.EmojiEmotions, contentDescription = stringResource(R.string.chat_emoji))
            }
            CompositionLocalProvider(LocalTextToolbar provides DisabledTextToolbar) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    visualTransformation = visualTransformation,
                    placeholder = {
                        Text(
                            if (editTarget != null) {
                                stringResource(R.string.chat_edit_placeholder)
                            } else {
                                stringResource(R.string.message_placeholder)
                            },
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { event ->
                            if (!sendWithEnter) return@onPreviewKeyEvent false
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            if (event.key != Key.Enter && event.key != Key.NumPadEnter) return@onPreviewKeyEvent false
                            if (event.isShiftPressed) return@onPreviewKeyEvent false
                            submitDraft(sentViaEnter = true)
                            true
                        },
                    maxLines = 4,
                )
            }
            IconButton(
                onClick = { submitDraft() },
                enabled = !isSending && draft.text.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.send))
            }
        }
    }
}

private object DisabledTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() = Unit

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPickerContent(onEmojiSelected: (String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val categories = ChatEmojiData.categories
    val filteredEmojis = remember(searchQuery, selectedTab) {
        ChatEmojiData.filterEmojis(searchQuery, selectedTab)
    }

    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.chat_emoji_search)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
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
                    onClick = {
                        selectedTab = index
                        searchQuery = ""
                    },
                    text = { Text(category.icon) },
                )
            }
        }

        if (filteredEmojis.isEmpty()) {
            Text(
                text = stringResource(R.string.chat_emoji_not_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(filteredEmojis) { emoji ->
                    TextButton(onClick = { onEmojiSelected(emoji) }) {
                        Text(emoji, fontSize = 24.sp)
                    }
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
