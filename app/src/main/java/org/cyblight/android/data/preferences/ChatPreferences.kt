package org.cyblight.android.data.preferences

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ChatDefaultTheme {
    SYSTEM,
    CYBLIGHT,
    CLASSIC,
    MIDNIGHT,
    OCEAN,
    SUNSET,
    FOREST,
    LAVENDER,
    ;

    companion object {
        fun fromName(name: String?): ChatDefaultTheme =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

enum class ChatFontSize(val scale: Float) {
    SMALL(0.85f),
    MEDIUM(1f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
    ;

    companion object {
        fun fromName(name: String?): ChatFontSize =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

data class ChatThemePalette(
    val wallpaper: Color,
    val outgoingBubble: Color,
    val outgoingText: Color,
    val incomingBubble: Color,
    val incomingText: Color,
    val quoteBar: Color = Color(0xFF3B82F6),
)

object ChatThemeDefinitions {
    fun palette(theme: ChatDefaultTheme): ChatThemePalette = when (theme) {
        ChatDefaultTheme.SYSTEM -> error("Use resolveChatThemePalette for SYSTEM")
        ChatDefaultTheme.CYBLIGHT -> ChatThemePalette(
            wallpaper = Color(0xFF0F172A),
            outgoingBubble = Color(0xFF2563EB),
            outgoingText = Color(0xFFF8FAFC),
            incomingBubble = Color(0xFF1E293B),
            incomingText = Color(0xFFE2E8F0),
            quoteBar = Color(0xFF38BDF8),
        )
        ChatDefaultTheme.CLASSIC -> ChatThemePalette(
            wallpaper = Color(0xFF0B141A),
            outgoingBubble = Color(0xFF005C4B),
            outgoingText = Color(0xFFE9EDEF),
            incomingBubble = Color(0xFF1F2C34),
            incomingText = Color(0xFFE9EDEF),
            quoteBar = Color(0xFF53BDEB),
        )
        ChatDefaultTheme.MIDNIGHT -> ChatThemePalette(
            wallpaper = Color(0xFF120A1F),
            outgoingBubble = Color(0xFF7C3AED),
            outgoingText = Color(0xFFF5F3FF),
            incomingBubble = Color(0xFF2A1B3D),
            incomingText = Color(0xFFEDE9FE),
            quoteBar = Color(0xFFA78BFA),
        )
        ChatDefaultTheme.OCEAN -> ChatThemePalette(
            wallpaper = Color(0xFF071A2C),
            outgoingBubble = Color(0xFF0284C7),
            outgoingText = Color(0xFFF0F9FF),
            incomingBubble = Color(0xFF12324A),
            incomingText = Color(0xFFE0F2FE),
            quoteBar = Color(0xFF38BDF8),
        )
        ChatDefaultTheme.SUNSET -> ChatThemePalette(
            wallpaper = Color(0xFF2A1208),
            outgoingBubble = Color(0xFFEA580C),
            outgoingText = Color(0xFFFFF7ED),
            incomingBubble = Color(0xFF4A2412),
            incomingText = Color(0xFFFFEDD5),
            quoteBar = Color(0xFFFED7AA),
        )
        ChatDefaultTheme.FOREST -> ChatThemePalette(
            wallpaper = Color(0xFF0F1A12),
            outgoingBubble = Color(0xFF15803D),
            outgoingText = Color(0xFFF0FDF4),
            incomingBubble = Color(0xFF1F3326),
            incomingText = Color(0xFFDCFCE7),
            quoteBar = Color(0xFF4ADE80),
        )
        ChatDefaultTheme.LAVENDER -> ChatThemePalette(
            wallpaper = Color(0xFF1A1528),
            outgoingBubble = Color(0xFF9333EA),
            outgoingText = Color(0xFFFAF5FF),
            incomingBubble = Color(0xFF2D2440),
            incomingText = Color(0xFFF3E8FF),
            quoteBar = Color(0xFFC084FC),
        )
    }
}

@Composable
fun resolveChatThemePalette(theme: ChatDefaultTheme): ChatThemePalette {
    val palette = if (theme == ChatDefaultTheme.SYSTEM) {
        ChatThemePalette(
            wallpaper = MaterialTheme.colorScheme.background,
            outgoingBubble = MaterialTheme.colorScheme.primary,
            outgoingText = MaterialTheme.colorScheme.onPrimary,
            incomingBubble = MaterialTheme.colorScheme.surfaceVariant,
            incomingText = MaterialTheme.colorScheme.onSurfaceVariant,
            quoteBar = Color(0xFF0EA5E9),
        )
    } else {
        ChatThemeDefinitions.palette(theme)
    }
    return palette
}
