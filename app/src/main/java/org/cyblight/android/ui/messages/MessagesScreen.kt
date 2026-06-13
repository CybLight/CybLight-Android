package org.cyblight.android.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenChat: (friendId: String, username: String) -> Unit,
    onOpenProfile: (username: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && conversations.isEmpty() -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading), modifier = Modifier.padding(top = 12.dp))
            }
        }
        !error.isNullOrBlank() && conversations.isEmpty() -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .clickable { onRefresh() }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.error_load_messages), color = MaterialTheme.colorScheme.error)
            }
        }
        conversations.isEmpty() -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("💬", style = MaterialTheme.typography.displaySmall)
                Text(stringResource(R.string.no_conversations), style = MaterialTheme.typography.titleMedium)
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(conversations, key = { it.friend.id }) { preview ->
                    ConversationCard(
                        preview = preview,
                        onOpenChat = onOpenChat,
                        onOpenProfile = onOpenProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    preview: ConversationPreview,
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
                if (!preview.preview.isNullOrBlank()) {
                    Text(
                        text = preview.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    PresenceLabel(
                        isOnline = preview.friend.isOnline,
                        lastSeenAt = preview.friend.lastSeenAt,
                    )
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
