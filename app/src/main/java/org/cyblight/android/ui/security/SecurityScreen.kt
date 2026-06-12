package org.cyblight.android.ui.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.repository.SecurityOverview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    overview: SecurityOverview?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSecurityCheck: () -> Unit,
    onOpenEmail: () -> Unit,
    onOpenPassword: () -> Unit,
    onOpenTwoFactor: () -> Unit,
    onOpenPasskeys: () -> Unit,
    onOpenTrustedDevices: () -> Unit,
    onOpenLoginHistory: () -> Unit,
    onOpenSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        if (isLoading && overview == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@PullToRefreshBox
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                val score = overview?.securityScore ?: 0
                SecurityListItem(
                    title = if (score >= 100) {
                        stringResource(R.string.security_check_title_protected)
                    } else {
                        stringResource(R.string.security_check_title)
                    },
                    subtitle = securityCheckSubtitle(overview),
                    badge = securityLevelBadge(overview),
                    badgeOk = when {
                        score >= 100 -> true
                        score >= 50 -> null
                        overview != null -> false
                        else -> null
                    },
                    icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                    onClick = onOpenSecurityCheck,
                )
            }
            item {
                val (emailBadgeText, emailBadgeOk) = emailBadge(overview)
                SecurityListItem(
                    title = stringResource(R.string.security_email_title),
                    subtitle = emailSubtitle(overview),
                    badge = emailBadgeText,
                    badgeOk = emailBadgeOk,
                    icon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    onClick = onOpenEmail,
                )
            }
            item {
                SecurityListItem(
                    title = stringResource(R.string.security_password_title),
                    subtitle = passwordSubtitle(overview),
                    icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    onClick = onOpenPassword,
                )
            }
            item {
                val (twoFaBadgeText, twoFaBadgeOk) = twoFactorBadge(overview)
                SecurityListItem(
                    title = stringResource(R.string.security_2fa_title),
                    subtitle = twoFactorSubtitle(overview),
                    badge = twoFaBadgeText,
                    badgeOk = twoFaBadgeOk,
                    icon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null) },
                    onClick = onOpenTwoFactor,
                )
            }
            item {
                SecurityListItem(
                    title = stringResource(R.string.security_passkeys_title),
                    subtitle = passkeysSubtitle(overview),
                    badge = passkeysBadge(overview),
                    badgeOk = if ((overview?.passkeyCount ?: 0) > 0) true else null,
                    icon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                    onClick = onOpenPasskeys,
                )
            }
            item {
                SecurityListItem(
                    title = stringResource(R.string.security_trusted_devices_title),
                    subtitle = stringResource(R.string.security_trusted_devices_hint),
                    icon = { Icon(Icons.Outlined.PhoneAndroid, contentDescription = null) },
                    onClick = onOpenTrustedDevices,
                )
            }
            item {
                SecurityListItem(
                    title = stringResource(R.string.security_login_history_title),
                    subtitle = stringResource(R.string.security_login_history_hint),
                    icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    onClick = onOpenLoginHistory,
                )
            }
            item {
                SecurityListItem(
                    title = stringResource(R.string.sessions_title),
                    subtitle = stringResource(R.string.sessions_hint),
                    icon = { Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null) },
                    onClick = onOpenSessions,
                )
            }
        }
    }
}

@Composable
private fun SecurityListItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    badge: String? = null,
    badgeOk: Boolean? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = icon,
        trailingContent = badge?.let { label ->
            {
                SecurityBadge(text = label, ok = badgeOk)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
    )
}

@Composable
private fun SecurityBadge(text: String, ok: Boolean?) {
    val containerColor = when (ok) {
        true -> MaterialTheme.colorScheme.primaryContainer
        false -> MaterialTheme.colorScheme.errorContainer
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    val labelColor = when (ok) {
        true -> MaterialTheme.colorScheme.onPrimaryContainer
        false -> MaterialTheme.colorScheme.onErrorContainer
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = containerColor,
            disabledLabelColor = labelColor,
        ),
    )
}

@Composable
private fun securityLevelBadge(overview: SecurityOverview?): String? {
    if (overview == null) return null
    val score = overview.securityScore
    return when {
        score >= 100 -> stringResource(R.string.security_level_good)
        score >= 50 -> stringResource(R.string.security_level_medium)
        else -> stringResource(R.string.security_level_low)
    }
}

@Composable
private fun emailBadge(overview: SecurityOverview?): Pair<String?, Boolean?> {
    if (overview == null) return null to null
    return when {
        !overview.pendingEmail.isNullOrBlank() ->
            stringResource(R.string.security_email_pending_change) to false
        overview.emailVerified ->
            stringResource(R.string.security_email_verified) to true
        !overview.email.isNullOrBlank() ->
            stringResource(R.string.security_email_not_verified) to false
        else -> null to null
    }
}

@Composable
private fun twoFactorBadge(overview: SecurityOverview?): Pair<String?, Boolean?> {
    if (overview == null) return null to null
    return if (overview.totpEnabled) {
        stringResource(R.string.security_2fa_enabled) to true
    } else {
        stringResource(R.string.security_2fa_disabled) to false
    }
}

@Composable
private fun passkeysBadge(overview: SecurityOverview?): String? {
    if (overview == null) return null
    val count = overview.passkeyCount
    return if (count > 0) count.toString() else null
}

@Composable
private fun securityCheckSubtitle(overview: SecurityOverview?): String {
    val score = overview?.securityScore ?: 0
    return when {
        score >= 100 -> stringResource(R.string.security_check_subtitle_protected)
        score >= 50 -> stringResource(R.string.security_level_medium)
        overview != null -> stringResource(R.string.security_check_subtitle_recommendations)
        else -> stringResource(R.string.security_hint)
    }
}

@Composable
private fun emailSubtitle(overview: SecurityOverview?): String {
    if (overview == null) return stringResource(R.string.security_manage_on_website)
    return when {
        !overview.pendingEmail.isNullOrBlank() -> overview.pendingEmail ?: ""
        overview.emailVerified && !overview.email.isNullOrBlank() -> overview.email
        !overview.email.isNullOrBlank() -> overview.email
        else -> stringResource(R.string.security_email_not_set)
    }
}

@Composable
private fun passwordSubtitle(overview: SecurityOverview?): String {
    if (overview == null) return stringResource(R.string.security_manage_on_website)
    val changedAt = overview.passChangedAt
    return if (changedAt != null && changedAt > 0L) {
        stringResource(R.string.security_password_changed_at, formatSecurityTimestamp(changedAt))
    } else {
        stringResource(R.string.security_password_never_changed)
    }
}

@Composable
private fun twoFactorSubtitle(overview: SecurityOverview?): String {
    if (overview == null) return stringResource(R.string.security_manage_on_website)
    return if (overview.totpEnabled) {
        stringResource(R.string.security_2fa_enabled)
    } else {
        stringResource(R.string.security_2fa_disabled)
    }
}

@Composable
private fun passkeysSubtitle(overview: SecurityOverview?): String {
    if (overview == null) return stringResource(R.string.security_manage_on_website)
    val count = overview.passkeyCount
    return if (count > 0) {
        stringResource(R.string.security_passkeys_count, count)
    } else {
        stringResource(R.string.security_passkeys_none)
    }
}
