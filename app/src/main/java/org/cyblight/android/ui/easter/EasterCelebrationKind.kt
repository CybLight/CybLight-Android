package org.cyblight.android.ui.easter

import androidx.annotation.StringRes
import org.cyblight.android.R

enum class EasterCelebrationKind {
    LIGHT_CATCHER,
    NIGHT_GUARD,
    TRUSTED_FINGERPRINT,
    ECHO,
    ARCHIVIST,
    TYPOGRAPHER,
    SPOILER_HUNTER,
    NO_MARKERS,
    ENTER_MASTER,
    FONT_EXTREMES,
    CLOUD_KEEPER,
    DRIVE_PILOT,
    LIVE_WIRE,
    FROM_SHADOW,
    WATCHMAN,
    CAROUSEL_WATCHER,
    SYNCHRONIST,
    QUOTE_DAY,
    MIDNIGHT_EDITOR,
    POLYGLOT_FRIEND,
    SILENCE,
    REACTION_STREAK,
    BRIDGE,
    FORMAT_MIRROR,
}

data class EasterCelebrationContent(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
)

object EasterCelebrationCatalog {
    fun content(kind: EasterCelebrationKind): EasterCelebrationContent = when (kind) {
        EasterCelebrationKind.LIGHT_CATCHER -> EasterCelebrationContent(
            "💡", R.string.easter_light_catcher_title, R.string.easter_light_catcher_hint_unlocked,
        )
        EasterCelebrationKind.NIGHT_GUARD -> EasterCelebrationContent(
            "🌙", R.string.easter_night_guard_title, R.string.easter_night_guard_hint_unlocked,
        )
        EasterCelebrationKind.TRUSTED_FINGERPRINT -> EasterCelebrationContent(
            "👆", R.string.easter_trusted_fingerprint_title, R.string.easter_trusted_fingerprint_hint_unlocked,
        )
        EasterCelebrationKind.ECHO -> EasterCelebrationContent(
            "🔔", R.string.easter_echo_title, R.string.easter_echo_hint_unlocked,
        )
        EasterCelebrationKind.ARCHIVIST -> EasterCelebrationContent(
            "📚", R.string.easter_archivist_title, R.string.easter_archivist_hint_unlocked,
        )
        EasterCelebrationKind.TYPOGRAPHER -> EasterCelebrationContent(
            "🖋️", R.string.easter_typographer_title, R.string.easter_typographer_hint_unlocked,
        )
        EasterCelebrationKind.SPOILER_HUNTER -> EasterCelebrationContent(
            "🫥", R.string.easter_spoiler_hunter_title, R.string.easter_spoiler_hunter_hint_unlocked,
        )
        EasterCelebrationKind.NO_MARKERS -> EasterCelebrationContent(
            "✨", R.string.easter_no_markers_title, R.string.easter_no_markers_hint_unlocked,
        )
        EasterCelebrationKind.ENTER_MASTER -> EasterCelebrationContent(
            "↵", R.string.easter_enter_master_title, R.string.easter_enter_master_hint_unlocked,
        )
        EasterCelebrationKind.FONT_EXTREMES -> EasterCelebrationContent(
            "🔤", R.string.easter_font_extremes_title, R.string.easter_font_extremes_hint_unlocked,
        )
        EasterCelebrationKind.CLOUD_KEEPER -> EasterCelebrationContent(
            "☁️", R.string.easter_cloud_keeper_title, R.string.easter_cloud_keeper_hint_unlocked,
        )
        EasterCelebrationKind.DRIVE_PILOT -> EasterCelebrationContent(
            "🚗", R.string.easter_drive_pilot_title, R.string.easter_drive_pilot_hint_unlocked,
        )
        EasterCelebrationKind.LIVE_WIRE -> EasterCelebrationContent(
            "⚡", R.string.easter_live_wire_title, R.string.easter_live_wire_hint_unlocked,
        )
        EasterCelebrationKind.FROM_SHADOW -> EasterCelebrationContent(
            "👤", R.string.easter_from_shadow_title, R.string.easter_from_shadow_hint_unlocked,
        )
        EasterCelebrationKind.WATCHMAN -> EasterCelebrationContent(
            "🛡️", R.string.easter_watchman_title, R.string.easter_watchman_hint_unlocked,
        )
        EasterCelebrationKind.CAROUSEL_WATCHER -> EasterCelebrationContent(
            "🎠", R.string.easter_carousel_watcher_title, R.string.easter_carousel_watcher_hint_unlocked,
        )
        EasterCelebrationKind.SYNCHRONIST -> EasterCelebrationContent(
            "🔄", R.string.easter_synchronist_title, R.string.easter_synchronist_hint_unlocked,
        )
        EasterCelebrationKind.QUOTE_DAY -> EasterCelebrationContent(
            "💬", R.string.easter_quote_day_title, R.string.easter_quote_day_hint_unlocked,
        )
        EasterCelebrationKind.MIDNIGHT_EDITOR -> EasterCelebrationContent(
            "🌑", R.string.easter_midnight_editor_title, R.string.easter_midnight_editor_hint_unlocked,
        )
        EasterCelebrationKind.POLYGLOT_FRIEND -> EasterCelebrationContent(
            "🌍", R.string.easter_polyglot_friend_title, R.string.easter_polyglot_friend_hint_unlocked,
        )
        EasterCelebrationKind.SILENCE -> EasterCelebrationContent(
            "🤫", R.string.easter_silence_title, R.string.easter_silence_hint_unlocked,
        )
        EasterCelebrationKind.REACTION_STREAK -> EasterCelebrationContent(
            "👍", R.string.easter_reaction_streak_title, R.string.easter_reaction_streak_hint_unlocked,
        )
        EasterCelebrationKind.BRIDGE -> EasterCelebrationContent(
            "🌉", R.string.easter_bridge_title, R.string.easter_bridge_hint_unlocked,
        )
        EasterCelebrationKind.FORMAT_MIRROR -> EasterCelebrationContent(
            "🪞", R.string.easter_format_mirror_title, R.string.easter_format_mirror_hint_unlocked,
        )
    }
}
