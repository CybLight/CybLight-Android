package org.cyblight.android.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.EasterProgress
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.PinnedMessageDto
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.home.HomeContent
import org.cyblight.android.data.repository.ConversationPreview
import org.cyblight.android.data.repository.SecurityOverview
import org.cyblight.android.ui.components.AppMenu
import org.cyblight.android.ui.components.CybLightLogo
import org.cyblight.android.ui.easter.EasterEggsScreen
import org.cyblight.android.ui.friends.FriendsScreen
import org.cyblight.android.ui.home.HomeScreen
import org.cyblight.android.ui.messages.ChatEditTarget
import org.cyblight.android.ui.messages.ChatReplyTarget
import org.cyblight.android.ui.messages.ChatScreen
import org.cyblight.android.ui.messages.MessagesScreen
import org.cyblight.android.ui.security.SecurityScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: UserDto,
    selectedTab: MainTab,
    friendsSubTab: Int,
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
    chatPinnedMessage: PinnedMessageDto?,
    chatReplyTarget: ChatReplyTarget?,
    chatEditTarget: ChatEditTarget?,
    chatDraftText: String,
    isChatLoading: Boolean,
    isSending: Boolean,
    homeContent: HomeContent?,
    isHomeLoading: Boolean,
    homeError: String?,
    easterFlags: EasterFlagsDto?,
    easterProgress: EasterProgress,
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
    onClearChatReply: () -> Unit,
    onClearChatEdit: () -> Unit,
    onStartChatReply: (MessageDto) -> Unit,
    onStartChatEdit: (MessageDto) -> Unit,
    onPinChatMessage: (MessageDto) -> Unit,
    onUnpinChatMessage: (MessageDto) -> Unit,
    onDeleteChatMessage: (String) -> Unit,
    onDeleteChatMessages: (List<String>) -> Unit,
    onForwardChatMessage: (String, String) -> Unit,
    onReactChatMessage: (String, String) -> Unit,
    onUpdateChatDraft: (String) -> Unit,
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
    onSelectTab: (MainTab) -> Unit,
    onFriendsSubTabChange: (Int) -> Unit,
    onRefreshHome: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenChangelog: () -> Unit,
) {
    if (chatFriendId != null && chatFriendName != null) {
        ChatScreen(
            friendId = chatFriendId,
            friendName = chatFriendName,
            friendIsOnline = chatFriendIsOnline,
            friendLastSeenAt = chatFriendLastSeenAt,
            messages = chatMessages,
            pinnedMessage = chatPinnedMessage,
            friends = friends,
            currentUserId = user.id,
            isLoading = isChatLoading,
            isSending = isSending,
            error = messagesError,
            replyTarget = chatReplyTarget,
            editTarget = chatEditTarget,
            savedDraft = chatDraftText,
            onDraftSaved = onUpdateChatDraft,
            onBack = onCloseChat,
            onOpenProfile = onOpenFriendProfile,
            onSend = onSendMessage,
            onClearReply = onClearChatReply,
            onClearEdit = onClearChatEdit,
            onStartReply = onStartChatReply,
            onStartEdit = onStartChatEdit,
            onPinMessage = onPinChatMessage,
            onUnpinMessage = onUnpinChatMessage,
            onDeleteMessage = onDeleteChatMessage,
            onDeleteMessages = onDeleteChatMessages,
            onForwardMessage = onForwardChatMessage,
            onReactMessage = onReactChatMessage,
        )
        return
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            MainTab.Home -> onRefreshHome()
            MainTab.Security -> onSecurityTabSelected()
            MainTab.Easter -> onEasterTabSelected()
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            MainTab.Home -> stringResource(R.string.home)
                            else -> stringResource(R.string.welcome_user, user.login)
                        },
                        modifier = if (selectedTab == MainTab.Home) {
                            Modifier
                        } else {
                            Modifier.clickable(onClick = onOpenProfile)
                        },
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
                    selected = selectedTab == MainTab.Home,
                    onClick = { onSelectTab(MainTab.Home) },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.home)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Friends,
                    onClick = { onSelectTab(MainTab.Friends) },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                    label = { Text(stringResource(R.string.friends)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Messages,
                    onClick = { onSelectTab(MainTab.Messages) },
                    icon = { Icon(Icons.Outlined.Forum, contentDescription = null) },
                    label = { Text(stringResource(R.string.messages)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Security,
                    onClick = { onSelectTab(MainTab.Security) },
                    icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                    label = { Text(stringResource(R.string.security_title)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Easter,
                    onClick = { onSelectTab(MainTab.Easter) },
                    icon = { Icon(Icons.Outlined.Celebration, contentDescription = null) },
                    label = { Text(stringResource(R.string.easter_eggs_title)) },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.Home -> HomeScreen(
                content = homeContent,
                isLoading = isHomeLoading,
                error = homeError,
                onRefresh = onRefreshHome,
                onOpenUrl = onOpenUrl,
                onOpenChangelog = onOpenChangelog,
                modifier = Modifier.padding(padding),
            )
            MainTab.Friends -> FriendsScreen(
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
                selectedSubTab = friendsSubTab,
                onSubTabChange = onFriendsSubTabChange,
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
            MainTab.Messages -> MessagesScreen(
                conversations = conversations,
                isLoading = conversations.isEmpty() && messagesError == null,
                error = messagesError,
                onRefresh = onRefresh,
                onOpenChat = onOpenChat,
                onOpenProfile = onOpenFriendProfile,
                modifier = Modifier.padding(padding),
            )
            MainTab.Security -> SecurityScreen(
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
            MainTab.Easter -> EasterEggsScreen(
                flags = easterFlags,
                progress = easterProgress,
                isLoading = isEasterLoading,
                error = easterError,
                onBack = null,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
