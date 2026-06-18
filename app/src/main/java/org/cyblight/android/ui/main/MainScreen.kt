package org.cyblight.android.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cyblight.android.R
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.EasterProgress
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.PinnedMessageDto
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.home.HomeContent
import org.cyblight.android.data.repository.ConversationPreview
import org.cyblight.android.ui.components.AppMenu
import org.cyblight.android.ui.components.CybLightLogo
import org.cyblight.android.ui.easter.EasterEggsScreen
import org.cyblight.android.ui.friends.FriendsScreen
import org.cyblight.android.ui.home.HomeScreen
import org.cyblight.android.ui.messages.ChatEditTarget
import org.cyblight.android.ui.messages.ChatReplyTarget
import org.cyblight.android.ui.messages.ChatScreen
import org.cyblight.android.ui.messages.MessagesScreen

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
    chatDrafts: Map<String, String> = emptyMap(),
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
    chatFormatToolbarHidden: Boolean,
    chatDefaultTheme: org.cyblight.android.data.preferences.ChatDefaultTheme,
    chatSendWithEnter: Boolean,
    chatFontSize: org.cyblight.android.data.preferences.ChatFontSize,
    localeTag: String,
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
    onChatFormatToolbarHiddenChange: (Boolean) -> Unit,
    onEasterTabSelected: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onFriendsSubTabChange: (Int) -> Unit,
    onRefreshHome: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenChangelog: () -> Unit,
    homeWhatsNewBannerHidden: Boolean,
    onHomeWhatsNewBannerHiddenChange: (Boolean) -> Unit,
    encryptionReminderChatDismissed: Boolean,
    onDismissEncryptionReminderChat: () -> Unit,
    onOpenSecurityBackup: () -> Unit,
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
            localeTag = localeTag,
            formatToolbarHidden = chatFormatToolbarHidden,
            chatDefaultTheme = chatDefaultTheme,
            chatSendWithEnter = chatSendWithEnter,
            chatFontSize = chatFontSize,
            onFormatToolbarHiddenChange = onChatFormatToolbarHiddenChange,
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
            showEncryptionReminder = !encryptionReminderChatDismissed,
            onDismissEncryptionReminder = onDismissEncryptionReminderChat,
            onOpenSecurityBackup = onOpenSecurityBackup,
        )
        return
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            MainTab.Home -> onRefreshHome()
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
                    label = { BottomNavLabel(stringResource(R.string.home)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Friends,
                    onClick = { onSelectTab(MainTab.Friends) },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                    label = { BottomNavLabel(stringResource(R.string.friends)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Messages,
                    onClick = { onSelectTab(MainTab.Messages) },
                    icon = { Icon(Icons.Outlined.Forum, contentDescription = null) },
                    label = { BottomNavLabel(stringResource(R.string.nav_tab_messages)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Easter,
                    onClick = { onSelectTab(MainTab.Easter) },
                    icon = { Icon(Icons.Outlined.Celebration, contentDescription = null) },
                    label = { BottomNavLabel(stringResource(R.string.nav_tab_easter)) },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.Home -> HomeScreen(
                content = homeContent,
                isLoading = isHomeLoading,
                error = homeError,
                whatsNewBannerHidden = homeWhatsNewBannerHidden,
                onDismissWhatsNewBanner = { onHomeWhatsNewBannerHiddenChange(true) },
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
                chatDrafts = chatDrafts,
                isLoading = conversations.isEmpty() && messagesError == null,
                error = messagesError,
                encryptionReminderHidden = encryptionReminderChatDismissed,
                onRefresh = onRefresh,
                onOpenChat = onOpenChat,
                onOpenProfile = onOpenFriendProfile,
                onOpenChatBackup = onOpenSecurityBackup,
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

@Composable
private fun BottomNavLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        lineHeight = 12.sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
