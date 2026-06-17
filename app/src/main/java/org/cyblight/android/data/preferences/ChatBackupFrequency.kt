package org.cyblight.android.data.preferences

import java.util.concurrent.TimeUnit

enum class ChatBackupFrequency(val periodDays: Long) {
    OFF(0),
    DAILY(1),
    WEEKLY(7),
    MONTHLY(30),
    ;

    val intervalMs: Long
        get() = if (periodDays <= 0) 0L else TimeUnit.DAYS.toMillis(periodDays)

    companion object {
        fun fromName(name: String?): ChatBackupFrequency =
            entries.firstOrNull { it.name == name } ?: OFF
    }
}
