package org.cyblight.android.ui.messages

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cyblight.android.R
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.PinnedMessageDto
import org.cyblight.android.ui.components.PresenceLabel
import java.text.DateFormat
import java.util.Date

private const val EDIT_TIME_LIMIT_MS = 15 * 60 * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    friendId: String,
    friendName: String,
    friendIsOnline: Boolean,
    friendLastSeenAt: Long?,
    messages: List<MessageDto>,
    pinnedMessage: PinnedMessageDto?,
    friends: List<FriendDto>,
    currentUserId: String,
    isLoading: Boolean,
    isSending: Boolean,
    error: String?,
    replyTarget: ChatReplyTarget?,
    editTarget: ChatEditTarget?,
    savedDraft: String,
    onDraftSaved: (String) -> Unit,
    onBack: () -> Unit,
    onOpenProfile: (username: String) -> Unit,
    onSend: (String) -> Unit,
    onClearReply: () -> Unit,
    onClearEdit: () -> Unit,
    onStartReply: (MessageDto) -> Unit,
    onStartEdit: (MessageDto) -> Unit,
    onPinMessage: (MessageDto) -> Unit,
    onUnpinMessage: (MessageDto) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onDeleteMessages: (List<String>) -> Unit,
    onForwardMessage: (String, String) -> Unit,
    onReactMessage: (String, String) -> Unit,
) {
    var draft by remember(friendId) { mutableStateOf(TextFieldValue(savedDraft)) }
    var suppressedPreviewUrl by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var menuState by remember { mutableStateOf<MessageMenuState?>(null) }
    var forwardContent by remember { mutableStateOf<String?>(null) }
    var pendingDeleteIds by remember { mutableStateOf<List<String>?>(null) }
    var pendingUnpinMessage by remember { mutableStateOf<MessageDto?>(null) }

    LaunchedEffect(friendId, editTarget?.messageId) {
        draft = if (editTarget != null) {
            TextFieldValue(ChatFormatUtils.stripMetadataTokens(editTarget.content))
        } else {
            TextFieldValue(savedDraft)
        }
        if (editTarget == null) {
            selectionMode = false
            selectedIds = emptySet()
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (isLoading || messages.isEmpty() || selectionMode) return@LaunchedEffect
        delay(50)
        runCatching {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun clearSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun selectedMessages(): List<MessageDto> =
        messages.filter { selectedIds.contains(it.id) }

    fun copyText(text: String) {
        clipboard.setText(AnnotatedString(ChatFormatUtils.stripMetadataTokens(text)))
        Toast.makeText(context, context.getString(R.string.chat_copied), Toast.LENGTH_SHORT).show()
    }

    fun handleMenuAction(action: MessageMenuAction, state: MessageMenuState) {
        val message = state.message
        menuState = null
        when (action) {
            MessageMenuAction.Reply -> onStartReply(message)
            MessageMenuAction.Copy -> copyText(message.content)
            MessageMenuAction.CopyLink -> {
                val url = ChatFormatUtils.extractFirstUrl(message.content)
                if (url != null) {
                    clipboard.setText(AnnotatedString(url))
                    Toast.makeText(context, context.getString(R.string.chat_link_copied), Toast.LENGTH_SHORT).show()
                }
            }
            MessageMenuAction.Forward -> forwardContent = message.content
            MessageMenuAction.Pin -> onPinMessage(message)
            MessageMenuAction.Unpin -> pendingUnpinMessage = message
            MessageMenuAction.Edit -> onStartEdit(message)
            MessageMenuAction.Delete -> pendingDeleteIds = listOf(message.id)
            MessageMenuAction.Select -> {
                selectionMode = true
                selectedIds = setOf(message.id)
            }
        }
    }

    menuState?.let { state ->
        MessageContextMenuSheet(
            state = state,
            onDismiss = { menuState = null },
            onAction = { handleMenuAction(it, state) },
            onReact = { emoji -> onReactMessage(state.message.id, emoji) },
        )
    }

    forwardContent?.let { content ->
        ForwardMessageDialog(
            friends = friends.filter { it.username != friendName },
            previewText = content,
            onDismiss = { forwardContent = null },
            onForwardTo = { targetId ->
                onForwardMessage(targetId, content)
                forwardContent = null
                clearSelection()
                Toast.makeText(context, context.getString(R.string.chat_forwarded), Toast.LENGTH_SHORT).show()
            },
        )
    }

    pendingDeleteIds?.let { ids ->
        if (ids.size == 1) {
            DeleteMessageDialog(
                onDismiss = { pendingDeleteIds = null },
                onConfirm = {
                    onDeleteMessage(ids.first())
                    pendingDeleteIds = null
                    clearSelection()
                },
            )
        } else {
            DeleteMessagesDialog(
                count = ids.size,
                onDismiss = { pendingDeleteIds = null },
                onConfirm = {
                    onDeleteMessages(ids)
                    pendingDeleteIds = null
                    clearSelection()
                },
            )
        }
    }

    pendingUnpinMessage?.let {
        UnpinMessageDialog(
            onDismiss = { pendingUnpinMessage = null },
            onConfirm = {
                onUnpinMessage(it)
                pendingUnpinMessage = null
            },
        )
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.chat_selected_count, selectedIds.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = { clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    },
                    actions = {
                        val selected = selectedMessages()
                        val single = selected.singleOrNull()
                        if (single != null && single.senderId == currentUserId && canEditMessage(single)) {
                            IconButton(onClick = { onStartEdit(single); clearSelection() }) {
                                Icon(Icons.Outlined.Edit, contentDescription = null)
                            }
                        }
                        IconButton(
                            onClick = {
                                val text = selected.joinToString("\n\n") {
                                    ChatFormatUtils.stripMetadataTokens(it.content)
                                }
                                if (text.isNotBlank()) copyText(text)
                            },
                            enabled = selected.isNotEmpty(),
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        }
                        IconButton(
                            onClick = {
                                val text = selected.joinToString("\n\n") {
                                    ChatFormatUtils.stripMetadataTokens(it.content)
                                }
                                if (text.isNotBlank()) forwardContent = text
                            },
                            enabled = selected.isNotEmpty(),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null)
                        }
                        IconButton(
                            onClick = {
                                val ownIds = selected.filter { it.senderId == currentUserId }.map { it.id }
                                if (ownIds.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.chat_delete_foreign),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    pendingDeleteIds = ownIds
                                }
                            },
                            enabled = selected.isNotEmpty(),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.clickable { onOpenProfile(friendName) }) {
                            Text(friendName)
                            PresenceLabel(
                                isOnline = friendIsOnline,
                                lastSeenAt = friendLastSeenAt,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                !error.isNullOrBlank() && messages.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.error_load_messages), color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!selectionMode) {
                            pinnedMessage?.let { pinned ->
                                PinnedMessageBar(
                                    pinned = pinned,
                                    onScrollToMessage = {
                                        val index = messages.indexOfFirst { it.id == pinned.messageId }
                                        if (index >= 0) {
                                            scope.launch {
                                                runCatching { listState.animateScrollToItem(index) }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(
                                items = messages,
                                key = { index, message -> "${message.id}-$index" },
                            ) { _, message ->
                                val isMine = message.senderId == currentUserId
                                val isSelected = selectedIds.contains(message.id)
                                MessageBubble(
                                    message = message,
                                    isMine = isMine,
                                    isSelectionMode = selectionMode,
                                    isSelected = isSelected,
                                    onReact = { emoji -> onReactMessage(message.id, emoji) },
                                    onTap = {
                                        if (selectionMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - message.id
                                            } else {
                                                selectedIds + message.id
                                            }
                                        } else {
                                            menuState = MessageMenuState(
                                                message = message,
                                                isMine = isMine,
                                                isPinned = pinnedMessage?.messageId == message.id,
                                                canEdit = isMine && canEditMessage(message),
                                                hasLink = ChatFormatUtils.extractFirstUrl(message.content) != null,
                                            )
                                        }
                                    },
                                    onLongPress = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                            selectedIds = setOf(message.id)
                                        } else {
                                            selectedIds = if (isSelected) {
                                                selectedIds - message.id
                                            } else {
                                                selectedIds + message.id
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (!error.isNullOrBlank() && messages.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.error_send_message),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (selectionMode && selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            selectedMessages().singleOrNull()?.let {
                                onStartReply(it)
                                clearSelection()
                            }
                        },
                        enabled = selectedIds.size == 1,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null)
                        Text(
                            stringResource(R.string.chat_action_reply),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val text = selectedMessages().joinToString("\n\n") {
                                ChatFormatUtils.stripMetadataTokens(it.content)
                            }
                            if (text.isNotBlank()) forwardContent = text
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null)
                        Text(
                            stringResource(R.string.chat_action_forward),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }

            if (!selectionMode) {
                ChatInputSection(
                    draft = draft,
                    onDraftChange = { value ->
                        draft = value
                        if (editTarget == null) {
                            onDraftSaved(value.text)
                        }
                        val url = ChatFormatUtils.extractFirstUrl(value.text)
                        if (url == null) {
                            suppressedPreviewUrl = null
                        }
                    },
                    suppressedPreviewUrl = suppressedPreviewUrl,
                    onSuppressPreview = {
                        suppressedPreviewUrl = ChatFormatUtils.extractFirstUrl(draft.text)
                    },
                    replyTarget = replyTarget,
                    editTarget = editTarget,
                    onClearReply = onClearReply,
                    onClearEdit = {
                        onClearEdit()
                        draft = TextFieldValue(savedDraft)
                    },
                    isSending = isSending,
                    onSend = { content ->
                        val wasEditing = editTarget != null
                        onSend(content)
                        if (wasEditing) {
                            draft = TextFieldValue(savedDraft)
                        } else {
                            onDraftSaved("")
                            draft = TextFieldValue()
                        }
                    },
                )
            }
        }
    }
}

data class ChatReplyTarget(
    val messageId: String,
    val author: String,
    val preview: String,
)

data class ChatEditTarget(
    val messageId: String,
    val content: String,
)

private fun canEditMessage(message: MessageDto): Boolean {
    val createdAt = message.createdAt.takeIf { it > 0L } ?: return false
    return System.currentTimeMillis() - createdAt <= EDIT_TIME_LIMIT_MS
}

@Composable
private fun PinnedMessageBar(
    pinned: PinnedMessageDto,
    onScrollToMessage: () -> Unit,
) {
    val previewText = remember(pinned.content) {
        ChatFormatUtils.stripMetadataTokens(pinned.content).lineSequence().firstOrNull().orEmpty()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onScrollToMessage)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.chat_pinned_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = previewText.ifBlank { stringResource(R.string.chat_pinned_fallback) },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageDto,
    isMine: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onReact: (String) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val bg = if (isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bubbleLinkColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val timestamp = message.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))
    val edited = message.editedAt?.takeIf { it > 0L } != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(22.dp),
            )
        }
        Column(horizontalAlignment = alignment) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .combinedClickable(
                        onClick = onTap,
                        onLongClick = onLongPress,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val replyMeta = remember(message.content) {
                        ChatFormatUtils.extractReplyMeta(message.content)
                    }
                    replyMeta?.let { meta ->
                        ChatReplySnippet(
                            author = meta.author,
                            text = meta.text.ifBlank { stringResource(R.string.chat_reply_message) },
                            isOutgoing = isMine,
                        )
                    }
                    ChatMessageContent(
                        rawContent = message.content,
                        textColor = fg,
                        linkColor = bubbleLinkColor,
                        messageId = message.id,
                    )
                }
            }
            MessageReactionsRow(
                reactions = message.reactions,
                onReactionClick = onReact,
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (edited) {
                    Text(
                        text = stringResource(R.string.chat_edited),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isMine) {
                    MessageReadStatus(
                        readAt = message.readAt,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        readTint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageReadStatus(
    readAt: Long?,
    tint: androidx.compose.ui.graphics.Color,
    readTint: androidx.compose.ui.graphics.Color,
) {
    val isRead = readAt != null && readAt > 0L
    Icon(
        imageVector = if (isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
        contentDescription = stringResource(
            if (isRead) R.string.chat_status_read else R.string.chat_status_sent,
        ),
        modifier = Modifier.size(14.dp),
        tint = if (isRead) readTint else tint,
    )
}
