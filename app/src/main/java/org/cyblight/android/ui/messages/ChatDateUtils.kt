package org.cyblight.android.ui.messages

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object ChatDateUtils {
    private const val DAY_MS = 86_400_000L

    fun formatDateSeparator(timestampMs: Long, localeTag: String): String {
        val locale = Locale.forLanguageTag(localeTag.ifBlank { "ru" })
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val targetCalendar = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val targetStart = targetCalendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diffDays = TimeUnit.MILLISECONDS.toDays(todayStart - targetStart).toInt()

        return when (diffDays) {
            0 -> when (localeTag.take(2).lowercase()) {
                "en" -> "Today"
                "uk" -> "Сьогодні"
                else -> "Сегодня"
            }
            1 -> when (localeTag.take(2).lowercase()) {
                "en" -> "Yesterday"
                "uk" -> "Вчора"
                else -> "Вчера"
            }
            in 2..6 -> {
                val pattern = SimpleDateFormat("EEEE", locale).format(Date(timestampMs))
                if (localeTag.take(2).lowercase() == "en") pattern else pattern.lowercase(locale)
            }
            else -> SimpleDateFormat("dd.MM.yyyy", locale).format(Date(timestampMs))
        }
    }

    fun buildTimeline(messages: List<org.cyblight.android.data.api.MessageDto>, localeTag: String): List<ChatTimelineItem> {
        if (messages.isEmpty()) return emptyList()

        val items = mutableListOf<ChatTimelineItem>()
        var lastDayKey: String? = null

        messages.forEach { message ->
            val timestamp = message.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            val dayKey = dayKey(timestamp)
            if (dayKey != lastDayKey) {
                items += ChatTimelineItem.DateSeparator(
                    label = formatDateSeparator(timestamp, localeTag),
                )
                lastDayKey = dayKey
            }
            items += ChatTimelineItem.MessageItem(message)
        }

        return items
    }

    private fun dayKey(timestampMs: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "$year-$month-$day"
    }
}

sealed class ChatTimelineItem {
    data class DateSeparator(val label: String) : ChatTimelineItem()
    data class MessageItem(val message: org.cyblight.android.data.api.MessageDto) : ChatTimelineItem()
}
