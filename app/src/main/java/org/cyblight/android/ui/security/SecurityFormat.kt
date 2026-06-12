package org.cyblight.android.ui.security

import java.text.DateFormat
import java.util.Date

fun formatSecurityTimestamp(value: Long?): String {
    if (value == null || value <= 0L) return "—"
    val millis = if (value > 10_000_000_000L) value else value * 1000L
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(millis))
}
