package org.cyblight.android.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.repository.ConversationPreview
import org.cyblight.android.ui.components.PresenceLabel

@Composable
fun MessagesScreen(
    conversations: List<ConversationPreview>,
    chatDrafts: Map<String, String> = emptyMap(),
    isLoading: Boolean,
    error: String?,
    encryptionReminderHidden: Boolean,
    onRefresh: () -> Unit,
    onOpenChat: (friendId: String, username: String) -> Unit,
    onOpenProfile: (username: String) -> Unit,
    onOpenChatBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💬 ${stringResource(R.string.nav_tab_messages)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.messages_tab_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            isLoading && conversations.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.loading), modifier = Modifier.padding(top = 12.dp))
                }
            }
            !error.isNullOrBlank() && conversations.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!encryptionReminderHidden) {
                        EncryptionReminderBanner(
                            compact = false,
                            onOpenSecurityBackup = onOpenChatBackup,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onRefresh() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.error_load_messages), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            conversations.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!encryptionReminderHidden) {
                        EncryptionReminderBanner(
                            compact = false,
                            onOpenSecurityBackup = onOpenChatBackup,
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("💬", style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(R.string.no_conversations), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!encryptionReminderHidden) {
                        item(key = "encryption-reminder") {
                            EncryptionReminderBanner(
                                compact = false,
                                onOpenSecurityBackup = onOpenChatBackup,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    items(conversations, key = { it.friend.id }) { preview ->
                        ConversationCard(
                            preview = preview,
                            draftText = chatDrafts[preview.friend.id],
                            onOpenChat = onOpenChat,
                            onOpenProfile = onOpenProfile,
                        )
                    }
                }
            }
        }
    }
}

private val ChatDraftLabelColor = Color(0xFFFF9800)

@Composable
private fun ConversationCard(
    preview: ConversationPreview,
    draftText: String?,
    onOpenChat: (friendId: String, username: String) -> Unit,
    onOpenProfile: (username: String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onOpenChat(preview.friend.id, preview.friend.username) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preview.friend.username,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onOpenProfile(preview.friend.username) },
                )
                val trimmedDraft = draftText?.trim().orEmpty()
                when {
                    trimmedDraft.isNotEmpty() -> {
                        DraftPreviewText(draftText = trimmedDraft)
                    }
                    !preview.preview.isNullOrBlank() -> {
                        Text(
                            text = preview.preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    else -> {
                        PresenceLabel(
                            isOnline = preview.friend.isOnline,
                            lastSeenAt = preview.friend.lastSeenAt,
                        )
                    }
                }
            }
            if (preview.unreadCount > 0) {
                BadgedBox(badge = { Badge { Text(preview.unreadCount.toString()) } }) {
                    Text("💬")
                }
            }
        }
    }
}

@Composable
private fun DraftPreviewText(draftText: String) {
    val label = stringResource(R.string.chat_list_draft_label)
    val preview = remember(draftText) {
        MessagePreviewFormatter.truncatePreviewText(draftText, "")
    }
    if (preview.isBlank()) return

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = ChatDraftLabelColor, fontWeight = FontWeight.Medium)) {
                append(label)
            }
            append(": ")
            append(preview)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
