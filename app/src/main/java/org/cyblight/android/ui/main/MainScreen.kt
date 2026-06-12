package org.cyblight.android.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.repository.ConversationPreview
import org.cyblight.android.data.repository.SecurityOverview
import org.cyblight.android.ui.components.AppMenu
import org.cyblight.android.ui.components.CybLightLogo
import org.cyblight.android.ui.easter.EasterEggsScreen
import org.cyblight.android.ui.friends.FriendsScreen
import org.cyblight.android.ui.messages.ChatScreen
import org.cyblight.android.ui.messages.MessagesScreen
import org.cyblight.android.ui.security.SecurityScreen

private const val TAB_FRIENDS = 0
private const val TAB_MESSAGES = 1
private const val TAB_SECURITY = 2
private const val TAB_EASTER = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: UserDto,
    friends: List<FriendDto>,
    pendingRequests: List<FriendDto>,
    sentRequests: List<FriendDto>,
    isFriendsLoading: Boolean,
    friendSearchResults: List<FriendDto>,
    isFriendSearchLoading: Boolean,
    friendSearchError: String?,
    friendsActionMessage: String?,
    friendsActionError: String?,
    conversations: List<ConversationPreview>,
    friendsError: String?,
    messagesError: String?,
    chatFriendId: String?,
    chatFriendName: String?,
    chatFriendIsOnline: Boolean,
    chatFriendLastSeenAt: Long?,
    chatMessages: List<MessageDto>,
    isChatLoading: Boolean,
    isSending: Boolean,
    easterFlags: EasterFlagsDto?,
    isEasterLoading: Boolean,
    easterError: String?,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onCheckUpdates: () -> Unit,
    onReportBug: () -> Unit,
    onDonate: () -> Unit,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFriendProfile: (username: String) -> Unit,
    onRefresh: () -> Unit,
    onSearchUsers: (String) -> Unit,
    onClearFriendSearch: () -> Unit,
    onClearFriendsActionFeedback: () -> Unit,
    onAddFriend: (String) -> Unit,
    onAcceptFriendRequest: (String, String) -> Unit,
    onRejectFriendRequest: (String) -> Unit,
    onRemoveFriend: (String, String) -> Unit,
    onCancelFriendRequest: (String) -> Unit,
    onOpenChat: (friendId: String, username: String) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (String) -> Unit,
    securityOverview: SecurityOverview?,
    isSecurityLoading: Boolean,
    isSecurityRefreshing: Boolean,
    onSecurityTabSelected: () -> Unit,
    onRefreshSecurity: () -> Unit,
    onOpenSecurityCheck: () -> Unit,
    onOpenAccountSecurity: () -> Unit,
    onOpenPasskeys: () -> Unit,
    onOpenTrustedDevices: () -> Unit,
    onOpenLoginHistory: () -> Unit,
    onOpenSessions: () -> Unit,
    onEasterTabSelected: () -> Unit,
) {
    if (chatFriendId != null && chatFriendName != null) {
        ChatScreen(
            friendName = chatFriendName,
            friendIsOnline = chatFriendIsOnline,
            friendLastSeenAt = chatFriendLastSeenAt,
            messages = chatMessages,
            currentUserId = user.id,
            isLoading = isChatLoading,
            isSending = isSending,
            error = messagesError,
            onBack = onCloseChat,
            onOpenProfile = onOpenFriendProfile,
            onSend = onSendMessage,
        )
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_FRIENDS) }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            TAB_SECURITY -> onSecurityTabSelected()
            TAB_EASTER -> onEasterTabSelected()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.welcome_user, user.login),
                        modifier = Modifier.clickable(onClick = onOpenProfile),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenProfile) {
                        CybLightLogo(size = 36.dp)
                    }
                },
                actions = {
                    AppMenu(
                        onSettings = onSettings,
                        onHelp = onHelp,
                        onAbout = onAbout,
                        onCheckUpdates = onCheckUpdates,
                        onReportBug = onReportBug,
                        onDonate = onDonate,
                        onLogout = onLogout,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == TAB_FRIENDS,
                    onClick = { selectedTab = TAB_FRIENDS },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                    label = { Text(stringResource(R.string.friends)) },
                )
                NavigationBarItem(
                    selected = selectedTab == TAB_MESSAGES,
                    onClick = { selectedTab = TAB_MESSAGES },
                    icon = { Icon(Icons.Outlined.Forum, contentDescription = null) },
                    label = { Text(stringResource(R.string.messages)) },
                )
                NavigationBarItem(
                    selected = selectedTab == TAB_SECURITY,
                    onClick = { selectedTab = TAB_SECURITY },
                    icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                    label = { Text(stringResource(R.string.security_title)) },
                )
                NavigationBarItem(
                    selected = selectedTab == TAB_EASTER,
                    onClick = { selectedTab = TAB_EASTER },
                    icon = { Icon(Icons.Outlined.Celebration, contentDescription = null) },
                    label = { Text(stringResource(R.string.easter_eggs_title)) },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            TAB_FRIENDS -> FriendsScreen(
                friends = friends,
                pendingRequests = pendingRequests,
                sentRequests = sentRequests,
                isLoading = isFriendsLoading,
                error = friendsError,
                searchResults = friendSearchResults,
                isSearchLoading = isFriendSearchLoading,
                searchError = friendSearchError,
                actionMessage = friendsActionMessage,
                actionError = friendsActionError,
                onRefresh = onRefresh,
                onSearch = onSearchUsers,
                onClearSearch = onClearFriendSearch,
                onClearActionFeedback = onClearFriendsActionFeedback,
                onOpenChat = onOpenChat,
                onOpenProfile = onOpenFriendProfile,
                onAddFriend = onAddFriend,
                onAcceptRequest = onAcceptFriendRequest,
                onRejectRequest = onRejectFriendRequest,
                onRemoveFriend = onRemoveFriend,
                onCancelRequest = onCancelFriendRequest,
                modifier = Modifier.padding(padding),
            )
            TAB_MESSAGES -> MessagesScreen(
                conversations = conversations,
                isLoading = conversations.isEmpty() && messagesError == null,
                error = messagesError,
                onRefresh = onRefresh,
                onOpenChat = onOpenChat,
                onOpenProfile = onOpenFriendProfile,
                modifier = Modifier.padding(padding),
            )
            TAB_SECURITY -> SecurityScreen(
                overview = securityOverview,
                isLoading = isSecurityLoading,
                isRefreshing = isSecurityRefreshing,
                onRefresh = onRefreshSecurity,
                onOpenSecurityCheck = onOpenSecurityCheck,
                onOpenEmail = onOpenAccountSecurity,
                onOpenPassword = onOpenAccountSecurity,
                onOpenTwoFactor = onOpenAccountSecurity,
                onOpenPasskeys = onOpenPasskeys,
                onOpenTrustedDevices = onOpenTrustedDevices,
                onOpenLoginHistory = onOpenLoginHistory,
                onOpenSessions = onOpenSessions,
                modifier = Modifier.padding(padding),
            )
            TAB_EASTER -> EasterEggsScreen(
                flags = easterFlags,
                isLoading = isEasterLoading,
                error = easterError,
                onBack = null,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
