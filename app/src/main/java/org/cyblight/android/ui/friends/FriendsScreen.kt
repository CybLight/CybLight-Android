package org.cyblight.android.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.ui.components.PresenceLabel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val TAB_FRIENDS = 0
private const val TAB_INCOMING = 1
private const val TAB_SENT = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    friends: List<FriendDto>,
    pendingRequests: List<FriendDto>,
    sentRequests: List<FriendDto>,
    isLoading: Boolean,
    error: String?,
    searchResults: List<FriendDto>,
    isSearchLoading: Boolean,
    searchError: String?,
    actionMessage: String?,
    actionError: String?,
    selectedSubTab: Int = TAB_FRIENDS,
    onSubTabChange: (Int) -> Unit = {},
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onClearActionFeedback: () -> Unit,
    onOpenChat: (friendId: String, username: String) -> Unit,
    onOpenProfile: (username: String) -> Unit,
    onAddFriend: (username: String) -> Unit,
    onAcceptRequest: (friendId: String, username: String) -> Unit,
    onRejectRequest: (friendId: String) -> Unit,
    onRemoveFriend: (friendId: String, username: String) -> Unit,
    onCancelRequest: (friendId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTab = selectedSubTab.coerceIn(TAB_FRIENDS, TAB_SENT)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchResults by rememberSaveable { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<FriendDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val actionMessageText = when {
        actionMessage == "friend_request_sent" -> stringResource(R.string.friend_request_sent)
        actionMessage == "friend_request_rejected" -> stringResource(R.string.friend_request_rejected)
        actionMessage == "request_cancelled" -> stringResource(R.string.request_cancelled)
        actionMessage?.startsWith("friend_accepted:") == true -> {
            stringResource(R.string.friend_added, actionMessage.substringAfter(":"))
        }
        actionMessage?.startsWith("friend_removed:") == true -> {
            stringResource(R.string.friend_removed, actionMessage.substringAfter(":"))
        }
        actionMessage != null -> actionMessage
        else -> null
    }
    val actionErrorText = if (actionError != null) {
        stringResource(R.string.error_friend_action)
    } else {
        null
    }

    LaunchedEffect(actionMessageText) {
        actionMessageText?.let { text ->
            snackbarHostState.showSnackbar(text)
            onClearActionFeedback()
        }
    }

    LaunchedEffect(actionErrorText) {
        actionErrorText?.let { text ->
            snackbarHostState.showSnackbar(text)
            onClearActionFeedback()
        }
    }

    removeTarget?.let { friend ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text(stringResource(R.string.confirm_remove_friend_title)) },
            text = { Text(stringResource(R.string.confirm_remove_friend_text, friend.username)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveFriend(friend.id, friend.username)
                        removeTarget = null
                    },
                ) {
                    Text(stringResource(R.string.remove_friend))
                }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isLoading && friends.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                isLoading && friends.isEmpty() && pendingRequests.isEmpty() && sentRequests.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading), modifier = Modifier.padding(top = 12.dp))
                    }
                }
                !error.isNullOrBlank() && friends.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onRefresh() }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(R.string.error_load_friends),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    if (it.isBlank()) {
                                        showSearchResults = false
                                        onClearSearch()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.search_users_hint)) },
                            )
                            IconButton(
                                onClick = {
                                    showSearchResults = true
                                    onSearch(searchQuery)
                                },
                            ) {
                                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search_users))
                            }
                        }

                        if (showSearchResults && searchQuery.isNotBlank()) {
                            SearchResultsSection(
                                results = searchResults,
                                isLoading = isSearchLoading,
                                error = searchError,
                                onOpenProfile = onOpenProfile,
                                onAddFriend = onAddFriend,
                            )
                        }

                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 8.dp,
                        ) {
                            Tab(
                                selected = selectedTab == TAB_FRIENDS,
                                onClick = { onSubTabChange(TAB_FRIENDS) },
                                text = {
                                    Text(
                                        stringResource(R.string.my_friends_count, friends.size),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                            Tab(
                                selected = selectedTab == TAB_INCOMING,
                                onClick = { onSubTabChange(TAB_INCOMING) },
                                text = {
                                    Text(
                                        stringResource(R.string.incoming_requests_count, pendingRequests.size),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                            Tab(
                                selected = selectedTab == TAB_SENT,
                                onClick = { onSubTabChange(TAB_SENT) },
                                text = {
                                    Text(
                                        stringResource(R.string.sent_requests_count, sentRequests.size),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }

                        when (selectedTab) {
                            TAB_FRIENDS -> FriendsTab(
                                friends = friends,
                                onOpenChat = onOpenChat,
                                onOpenProfile = onOpenProfile,
                                onRemoveFriend = { removeTarget = it },
                            )
                            TAB_INCOMING -> IncomingRequestsTab(
                                requests = pendingRequests,
                                onOpenProfile = onOpenProfile,
                                onAccept = onAcceptRequest,
                                onReject = onRejectRequest,
                            )
                            TAB_SENT -> SentRequestsTab(
                                requests = sentRequests,
                                onOpenProfile = onOpenProfile,
                                onCancel = onCancelRequest,
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun SearchResultsSection(
    results: List<FriendDto>,
    isLoading: Boolean,
    error: String?,
    onOpenProfile: (username: String) -> Unit,
    onAddFriend: (username: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        when {
            isLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            error == "search_query_too_short" -> {
                Text(
                    stringResource(R.string.search_query_too_short),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            !error.isNullOrBlank() -> {
                Text(
                    stringResource(R.string.search_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            results.isEmpty() -> {
                Text(stringResource(R.string.search_no_results), modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    stringResource(R.string.search_no_results_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text(
                    stringResource(R.string.search_found, results.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                results.forEach { user ->
                    SearchResultCard(
                        user = user,
                        onOpenProfile = onOpenProfile,
                        onAddFriend = onAddFriend,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    user: FriendDto,
    onOpenProfile: (username: String) -> Unit,
    onAddFriend: (username: String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenProfile(user.username) },
            ) {
                Text(user.username, fontWeight = FontWeight.SemiBold)
                PresenceLabel(isOnline = user.isOnline, lastSeenAt = user.lastSeenAt)
            }
            TextButton(onClick = { onAddFriend(user.username) }) {
                Text(stringResource(R.string.add_friend))
            }
            IconButton(onClick = { onOpenProfile(user.username) }) {
                Icon(Icons.Outlined.Person, contentDescription = stringResource(R.string.profile))
            }
        }
    }
}

@Composable
private fun FriendsTab(
    friends: List<FriendDto>,
    onOpenChat: (friendId: String, username: String) -> Unit,
    onOpenProfile: (username: String) -> Unit,
    onRemoveFriend: (FriendDto) -> Unit,
) {
    if (friends.isEmpty()) {
        EmptyState(
            icon = "👥",
            title = stringResource(R.string.no_friends),
            subtitle = stringResource(R.string.no_friends_hint),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
    ) {
        items(friends, key = { it.id }) { friend ->
            FriendCard(
                friend = friend,
                onOpenChat = onOpenChat,
                onOpenProfile = onOpenProfile,
                onRemoveFriend = { onRemoveFriend(friend) },
            )
        }
    }
}

@Composable
private fun IncomingRequestsTab(
    requests: List<FriendDto>,
    onOpenProfile: (username: String) -> Unit,
    onAccept: (friendId: String, username: String) -> Unit,
    onReject: (friendId: String) -> Unit,
) {
    if (requests.isEmpty()) {
        EmptyState(
            icon = "📭",
            title = stringResource(R.string.no_pending_requests),
            subtitle = stringResource(R.string.no_pending_requests_hint),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
    ) {
        items(requests, key = { it.id }) { request ->
            RequestCard(
                request = request,
                meta = stringResource(
                    R.string.friend_request_incoming,
                    formatRequestDate(request.createdAt),
                ),
                onOpenProfile = onOpenProfile,
                primaryAction = stringResource(R.string.accept_friend),
                onPrimary = { onAccept(request.id, request.username) },
                secondaryAction = stringResource(R.string.reject_friend),
                onSecondary = { onReject(request.id) },
            )
        }
    }
}

@Composable
private fun SentRequestsTab(
    requests: List<FriendDto>,
    onOpenProfile: (username: String) -> Unit,
    onCancel: (friendId: String) -> Unit,
) {
    if (requests.isEmpty()) {
        EmptyState(
            icon = "📨",
            title = stringResource(R.string.no_sent_requests),
            subtitle = stringResource(R.string.no_sent_requests_hint),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
    ) {
        items(requests, key = { it.id }) { request ->
            RequestCard(
                request = request,
                meta = stringResource(
                    R.string.friend_request_waiting,
                    formatRequestDate(request.createdAt),
                ),
                onOpenProfile = onOpenProfile,
                primaryAction = stringResource(R.string.profile),
                onPrimary = { onOpenProfile(request.username) },
                secondaryAction = stringResource(R.string.cancel_request),
                onSecondary = { onCancel(request.id) },
            )
        }
    }
}

@Composable
private fun RequestCard(
    request: FriendDto,
    meta: String,
    onOpenProfile: (username: String) -> Unit,
    primaryAction: String,
    onPrimary: () -> Unit,
    secondaryAction: String,
    onSecondary: () -> Unit,
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenProfile(request.username) },
            ) {
                Text(request.username, fontWeight = FontWeight.SemiBold)
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onPrimary) {
                Text(primaryAction)
            }
            TextButton(onClick = onSecondary) {
                Text(secondaryAction)
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendDto,
    onOpenChat: (friendId: String, username: String) -> Unit,
    onOpenProfile: (username: String) -> Unit,
    onRemoveFriend: () -> Unit,
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenProfile(friend.username) },
            ) {
                Text(friend.username, fontWeight = FontWeight.SemiBold)
                PresenceLabel(isOnline = friend.isOnline, lastSeenAt = friend.lastSeenAt)
            }
            IconButton(onClick = { onOpenChat(friend.id, friend.username) }) {
                Icon(Icons.Outlined.Chat, contentDescription = stringResource(R.string.write_message))
            }
            IconButton(onClick = onRemoveFriend) {
                Text("❌", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun EmptyState(icon: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, style = MaterialTheme.typography.displaySmall)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatRequestDate(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return ""
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        .format(Date(timestamp))
}
