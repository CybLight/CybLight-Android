package org.cyblight.android.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.repository.ConversationPreview
import org.cyblight.android.ui.components.CybLightLogo
import org.cyblight.android.ui.components.LanguageMenu
import org.cyblight.android.ui.friends.FriendsScreen
import org.cyblight.android.ui.messages.ChatScreen
import org.cyblight.android.ui.messages.MessagesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: UserDto,
    locale: String,
    friends: List<FriendDto>,
    conversations: List<ConversationPreview>,
    friendsError: String?,
    messagesError: String?,
    chatFriendId: String?,
    chatFriendName: String?,
    chatMessages: List<MessageDto>,
    isChatLoading: Boolean,
    isSending: Boolean,
    onLocaleSelected: (String) -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onOpenChat: (friendId: String, username: String) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (String) -> Unit,
) {
    if (chatFriendId != null && chatFriendName != null) {
        ChatScreen(
            friendName = chatFriendName,
            messages = chatMessages,
            currentUserId = user.id,
            isLoading = isChatLoading,
            isSending = isSending,
            error = messagesError,
            onBack = onCloseChat,
            onSend = onSendMessage,
        )
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.welcome_user, user.login)) },
                navigationIcon = { CybLightLogo(size = 36.dp) },
                actions = {
                    LanguageMenu(currentLocale = locale, onLocaleSelected = onLocaleSelected)
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Outlined.Logout, contentDescription = stringResource(R.string.logout))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                    label = { Text(stringResource(R.string.friends)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Forum, contentDescription = null) },
                    label = { Text(stringResource(R.string.messages)) },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> FriendsScreen(
                friends = friends,
                isLoading = friends.isEmpty() && friendsError == null,
                error = friendsError,
                onRefresh = onRefresh,
                onOpenChat = onOpenChat,
                modifier = Modifier.padding(padding),
            )
            else -> MessagesScreen(
                conversations = conversations,
                isLoading = conversations.isEmpty() && messagesError == null,
                error = messagesError,
                onRefresh = onRefresh,
                onOpenChat = onOpenChat,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
