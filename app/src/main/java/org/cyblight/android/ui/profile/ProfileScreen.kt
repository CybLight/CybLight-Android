package org.cyblight.android.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.ProfileDto
import org.cyblight.android.ui.components.DetailScaffold
import java.text.DateFormat
import java.util.Date

@Composable
fun ProfileScreen(
    title: String,
    profile: ProfileDto?,
    isOwnProfile: Boolean,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
) {
    DetailScaffold(title = title, onBack = onBack) { padding ->
        when {
            isLoading && profile == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            !error.isNullOrBlank() && profile == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            profile != null -> {
                ProfileContent(
                    profile = profile,
                    isOwnProfile = isOwnProfile,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: ProfileDto,
    isOwnProfile: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = profileAvatarLabel(profile),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        Text(
            text = profile.username,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        if (profile.verified) {
            Text(
                text = stringResource(R.string.profile_verified),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        if (!isOwnProfile && profile.isOnline != null) {
            Text(
                text = if (profile.isOnline) {
                    stringResource(R.string.online)
                } else {
                    stringResource(R.string.offline)
                },
                color = if (profile.isOnline) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        ProfileField(
            label = stringResource(R.string.profile_friends_count),
            value = profile.friendsCount.toString(),
        )

        if (profile.createdAt > 0L) {
            val joined = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(profile.createdAt))
            ProfileField(
                label = stringResource(R.string.profile_member_since),
                value = joined,
            )
        }

        profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            ProfileField(label = stringResource(R.string.profile_bio), value = bio)
        }

        profile.aboutMe?.takeIf { it.isNotBlank() }?.let { about ->
            ProfileField(label = stringResource(R.string.profile_about), value = about)
        }

        profile.gender?.takeIf { it.isNotBlank() && it != "not_specified" }?.let { gender ->
            ProfileField(
                label = stringResource(R.string.profile_gender),
                value = genderLabel(gender),
            )
        }

        profile.dateOfBirth?.takeIf { it.isNotBlank() }?.let { dob ->
            ProfileField(label = stringResource(R.string.profile_birthday), value = dob)
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun genderLabel(gender: String): String = when (gender) {
    "male" -> stringResource(R.string.profile_gender_male)
    "female" -> stringResource(R.string.profile_gender_female)
    else -> stringResource(R.string.profile_gender_unspecified)
}

private fun profileAvatarLabel(profile: ProfileDto): String {
    val avatar = profile.avatar?.trim().orEmpty()
    return when {
        avatar.contains("cat", ignoreCase = true) -> "🐱"
        avatar.contains("dog", ignoreCase = true) -> "🐶"
        avatar.contains("fox", ignoreCase = true) -> "🦊"
        avatar.contains("bear", ignoreCase = true) -> "🐻"
        profile.username.isNotBlank() -> profile.username.first().uppercase()
        else -> "👤"
    }
}
