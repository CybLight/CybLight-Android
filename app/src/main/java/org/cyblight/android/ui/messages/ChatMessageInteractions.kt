package org.cyblight.android.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto

enum class MessageMenuAction {
    Reply,
    Copy,
    CopyLink,
    Forward,
    Pin,
    Unpin,
    Edit,
    Delete,
    Select,
}

data class MessageMenuState(
    val message: MessageDto,
    val isMine: Boolean,
    val isPinned: Boolean,
    val canEdit: Boolean,
    val hasLink: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenuSheet(
    state: MessageMenuState,
    onDismiss: () -> Unit,
    onAction: (MessageMenuAction) -> Unit,
    onReact: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        QuickReactionsRow(
            onReact = { emoji ->
                onReact(emoji)
                onDismiss()
            },
        )
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            MenuRow(Icons.AutoMirrored.Outlined.Reply, stringResource(R.string.chat_action_reply)) {
                onAction(MessageMenuAction.Reply)
            }
            MenuRow(Icons.Outlined.ContentCopy, stringResource(R.string.chat_action_copy)) {
                onAction(MessageMenuAction.Copy)
            }
            if (state.hasLink) {
                MenuRow(Icons.Outlined.Link, stringResource(R.string.chat_action_copy_link)) {
                    onAction(MessageMenuAction.CopyLink)
                }
            }
            MenuRow(Icons.AutoMirrored.Outlined.Forward, stringResource(R.string.chat_action_forward)) {
                onAction(MessageMenuAction.Forward)
            }
            if (state.isPinned) {
                MenuRow(Icons.Filled.PushPin, stringResource(R.string.chat_action_unpin)) {
                    onAction(MessageMenuAction.Unpin)
                }
            } else {
                MenuRow(Icons.Filled.PushPin, stringResource(R.string.chat_action_pin)) {
                    onAction(MessageMenuAction.Pin)
                }
            }
            if (state.isMine) {
                MenuRow(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(R.string.chat_action_edit),
                    enabled = state.canEdit,
                ) {
                    onAction(MessageMenuAction.Edit)
                }
                MenuRow(Icons.Outlined.Delete, stringResource(R.string.chat_action_delete)) {
                    onAction(MessageMenuAction.Delete)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            MenuRow(Icons.Outlined.RadioButtonUnchecked, stringResource(R.string.chat_action_select)) {
                onAction(MessageMenuAction.Select)
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { if (enabled) onClick() },
    )
}

@Composable
fun ForwardMessageDialog(
    friends: List<FriendDto>,
    previewText: String,
    onDismiss: () -> Unit,
    onForwardTo: (String) -> Unit,
) {
    val preview = ChatFormatUtils.stripMetadataTokens(previewText)
        .replace(Regex("""\s+"""), " ")
        .trim()
        .take(160)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_forward_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preview.isNotBlank()) {
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (friends.isEmpty()) {
                    Text(stringResource(R.string.chat_forward_no_friends))
                } else {
                    LazyColumn {
                        items(friends, key = { it.id }) { friend ->
                            TextButton(
                                onClick = { onForwardTo(friend.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    friend.username,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun DeleteMessagesDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_delete_title)) },
        text = { Text(stringResource(R.string.chat_delete_many, count)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.chat_action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun DeleteMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_delete_title)) },
        text = { Text(stringResource(R.string.chat_delete_one)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.chat_action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun UnpinMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_action_unpin)) },
        text = { Text(stringResource(R.string.chat_unpin_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.chat_action_unpin))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
