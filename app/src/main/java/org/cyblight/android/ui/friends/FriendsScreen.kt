package org.cyblight.android.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.FriendDto

@Composable
fun FriendsScreen(
    friends: List<FriendDto>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenChat: (friendId: String, username: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && friends.isEmpty() -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading), modifier = Modifier.padding(top = 12.dp))
            }
        }
        !error.isNullOrBlank() && friends.isEmpty() -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .clickable { onRefresh() }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.error_load_friends), color = MaterialTheme.colorScheme.error)
            }
        }
        friends.isEmpty() -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("👥", style = MaterialTheme.typography.displaySmall)
                Text(stringResource(R.string.no_friends), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.no_friends_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.my_friends),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(friends, key = { it.id }) { friend ->
                    FriendCard(friend = friend, onOpenChat = onOpenChat)
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendDto,
    onOpenChat: (friendId: String, username: String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.username, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (friend.isOnline) stringResource(R.string.online) else stringResource(R.string.offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (friend.isOnline) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = { onOpenChat(friend.id, friend.username) }) {
                Icon(Icons.Outlined.Chat, contentDescription = stringResource(R.string.write_message))
            }
        }
    }
}
