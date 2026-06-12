package org.cyblight.android.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cyblight.android.R

object PresenceFormatter {
    private const val ONLINE_THRESHOLD_MS = 5 * 60 * 1000L

    fun isOnline(isOnline: Boolean, lastSeenAt: Long?, now: Long = System.currentTimeMillis()): Boolean {
        if (isOnline) return true
        if (lastSeenAt == null || lastSeenAt <= 0L) return false
        return now - lastSeenAt <= ONLINE_THRESHOLD_MS
    }

    @Composable
    fun label(isOnline: Boolean, lastSeenAt: Long?, now: Long = System.currentTimeMillis()): String {
        if (isOnline(isOnline, lastSeenAt, now)) {
            return stringResource(R.string.online)
        }
        if (lastSeenAt == null || lastSeenAt <= 0L) {
            return stringResource(R.string.offline)
        }

        val diff = now - lastSeenAt
        return when {
            diff < 60_000 -> stringResource(R.string.presence_just_now)
            diff < 3_600_000 -> stringResource(R.string.presence_minutes_ago, (diff / 60_000).toInt())
            diff < 86_400_000 -> stringResource(R.string.presence_hours_ago, (diff / 3_600_000).toInt())
            else -> stringResource(R.string.presence_days_ago, (diff / 86_400_000).toInt())
        }
    }
}
