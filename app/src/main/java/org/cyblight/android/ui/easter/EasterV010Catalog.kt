package org.cyblight.android.ui.easter

import androidx.compose.ui.graphics.Color
import org.cyblight.android.R
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.EasterProgress

internal fun v010AppEasterEggs(
    flags: EasterFlagsDto?,
    progress: EasterProgress,
): List<EasterEggItem> = listOf(
    egg("🖋️", R.string.easter_typographer_title, flags?.typographer, EasterEggPalettesV010.typographer,
        R.string.easter_typographer_desc_unlocked, R.string.easter_typographer_desc_locked,
        R.string.easter_typographer_hint_locked, R.string.easter_typographer_hint_unlocked),
    egg("🫥", R.string.easter_spoiler_hunter_title, flags?.spoilerHunter, EasterEggPalettesV010.spoilerHunter,
        R.string.easter_spoiler_hunter_desc_unlocked, R.string.easter_spoiler_hunter_desc_locked,
        R.string.easter_spoiler_hunter_hint_locked, R.string.easter_spoiler_hunter_hint_unlocked,
        progress.spoilerReveals, 5),
    egg("✨", R.string.easter_no_markers_title, flags?.noMarkers, EasterEggPalettesV010.noMarkers,
        R.string.easter_no_markers_desc_unlocked, R.string.easter_no_markers_desc_locked,
        R.string.easter_no_markers_hint_locked, R.string.easter_no_markers_hint_unlocked),
    egg("↵", R.string.easter_enter_master_title, flags?.enterMaster, EasterEggPalettesV010.enterMaster,
        R.string.easter_enter_master_desc_unlocked, R.string.easter_enter_master_desc_locked,
        R.string.easter_enter_master_hint_locked, R.string.easter_enter_master_hint_unlocked,
        progress.enterSendCount, 10),
    egg("🔤", R.string.easter_font_extremes_title, flags?.fontExtremes, EasterEggPalettesV010.fontExtremes,
        R.string.easter_font_extremes_desc_unlocked, R.string.easter_font_extremes_desc_locked,
        R.string.easter_font_extremes_hint_locked, R.string.easter_font_extremes_hint_unlocked),
    egg("☁️", R.string.easter_cloud_keeper_title, flags?.cloudKeeper, EasterEggPalettesV010.cloudKeeper,
        R.string.easter_cloud_keeper_desc_unlocked, R.string.easter_cloud_keeper_desc_locked,
        R.string.easter_cloud_keeper_hint_locked, R.string.easter_cloud_keeper_hint_unlocked),
    egg("🚗", R.string.easter_drive_pilot_title, flags?.drivePilot, EasterEggPalettesV010.drivePilot,
        R.string.easter_drive_pilot_desc_unlocked, R.string.easter_drive_pilot_desc_locked,
        R.string.easter_drive_pilot_hint_locked, R.string.easter_drive_pilot_hint_unlocked,
        progress.driveAccountPicks, 3),
    egg("⚡", R.string.easter_live_wire_title, flags?.liveWire, EasterEggPalettesV010.liveWire,
        R.string.easter_live_wire_desc_unlocked, R.string.easter_live_wire_desc_locked,
        R.string.easter_live_wire_hint_locked, R.string.easter_live_wire_hint_unlocked),
    egg("👤", R.string.easter_from_shadow_title, flags?.fromShadow, EasterEggPalettesV010.fromShadow,
        R.string.easter_from_shadow_desc_unlocked, R.string.easter_from_shadow_desc_locked,
        R.string.easter_from_shadow_hint_locked, R.string.easter_from_shadow_hint_unlocked),
    egg("🛡️", R.string.easter_watchman_title, flags?.watchman, EasterEggPalettesV010.watchman,
        R.string.easter_watchman_desc_unlocked, R.string.easter_watchman_desc_locked,
        R.string.easter_watchman_hint_locked, R.string.easter_watchman_hint_unlocked,
        progress.watchmanOpens, 3),
    egg("🎠", R.string.easter_carousel_watcher_title, flags?.carouselWatcher, EasterEggPalettesV010.carouselWatcher,
        R.string.easter_carousel_watcher_desc_unlocked, R.string.easter_carousel_watcher_desc_locked,
        R.string.easter_carousel_watcher_hint_locked, R.string.easter_carousel_watcher_hint_unlocked,
        progress.carouselSeconds, 30, progressIsSeconds = true),
    egg("🔄", R.string.easter_synchronist_title, flags?.synchronist, EasterEggPalettesV010.synchronist,
        R.string.easter_synchronist_desc_unlocked, R.string.easter_synchronist_desc_locked,
        R.string.easter_synchronist_hint_locked, R.string.easter_synchronist_hint_unlocked),
    egg("💬", R.string.easter_quote_day_title, flags?.quoteDay, EasterEggPalettesV010.quoteDay,
        R.string.easter_quote_day_desc_unlocked, R.string.easter_quote_day_desc_locked,
        R.string.easter_quote_day_hint_locked, R.string.easter_quote_day_hint_unlocked,
        progress.quoteCount, 3),
    egg("🌑", R.string.easter_midnight_editor_title, flags?.midnightEditor, EasterEggPalettesV010.midnightEditor,
        R.string.easter_midnight_editor_desc_unlocked, R.string.easter_midnight_editor_desc_locked,
        R.string.easter_midnight_editor_hint_locked, R.string.easter_midnight_editor_hint_unlocked),
    egg("🌍", R.string.easter_polyglot_friend_title, flags?.polyglotFriend, EasterEggPalettesV010.polyglotFriend,
        R.string.easter_polyglot_friend_desc_unlocked, R.string.easter_polyglot_friend_desc_locked,
        R.string.easter_polyglot_friend_hint_locked, R.string.easter_polyglot_friend_hint_unlocked,
        progress.polyglotLocalesCount, 3),
    egg("🤫", R.string.easter_silence_title, flags?.silence, EasterEggPalettesV010.silence,
        R.string.easter_silence_desc_unlocked, R.string.easter_silence_desc_locked,
        R.string.easter_silence_hint_locked, R.string.easter_silence_hint_unlocked),
    egg("👍", R.string.easter_reaction_streak_title, flags?.reactionStreak, EasterEggPalettesV010.reactionStreak,
        R.string.easter_reaction_streak_desc_unlocked, R.string.easter_reaction_streak_desc_locked,
        R.string.easter_reaction_streak_hint_locked, R.string.easter_reaction_streak_hint_unlocked,
        progress.reactionStreak, 10),
)

internal fun v010BridgeEasterEggs(
    flags: EasterFlagsDto?,
    progress: EasterProgress,
): List<EasterEggItem> = listOf(
    egg("🪞", R.string.easter_format_mirror_title, flags?.formatMirror, EasterEggPalettesV010.formatMirror,
        R.string.easter_format_mirror_desc_unlocked, R.string.easter_format_mirror_desc_locked,
        R.string.easter_format_mirror_hint_locked, R.string.easter_format_mirror_hint_unlocked,
        progress.formatMirrorPlatformsToday, 2),
)

private fun egg(
    emoji: String,
    titleRes: Int,
    unlocked: Boolean?,
    palette: EasterEggPalette,
    descUnlockedRes: Int,
    descLockedRes: Int,
    hintLockedRes: Int,
    hintUnlockedRes: Int,
    progressCurrent: Int? = null,
    progressTotal: Int? = null,
    progressIsSeconds: Boolean = false,
): EasterEggItem = EasterEggItem(
    emoji = emoji,
    titleRes = titleRes,
    unlocked = unlocked == true,
    palette = palette,
    descUnlockedRes = descUnlockedRes,
    descLockedRes = descLockedRes,
    hintUnlockedRes = hintUnlockedRes,
    hintLockedRes = hintLockedRes,
    progressCurrent = if (unlocked != true) progressCurrent else null,
    progressTotal = if (unlocked != true) progressTotal else null,
    progressIsSeconds = progressIsSeconds,
)

private object EasterEggPalettesV010 {
    private fun palette(unlocked: Long, locked: Long, content: Long, lockedContent: Long) = EasterEggPalette(
        unlockedContainer = Color(unlocked),
        lockedContainer = Color(locked),
        unlockedContent = Color(content),
        lockedContent = Color(lockedContent),
    )

    val typographer = palette(0xFF3E2723, 0xFF2E201E, 0xFFEFEBE9, 0xFFD7CCC8)
    val spoilerHunter = palette(0xFF4A3B12, 0xFF2B261C, 0xFFFFE082, 0xFFB0A48A)
    val noMarkers = palette(0xFF1B3A4B, 0xFF222830, 0xFF81D4FA, 0xFF93A4A8)
    val enterMaster = palette(0xFF1B4332, 0xFF242A2E, 0xFF95D5B2, 0xFF90A4A8)
    val fontExtremes = palette(0xFF4A148C, 0xFF262230, 0xFFCE93D8, 0xFF9E95AB)
    val cloudKeeper = palette(0xFF0D47A1, 0xFF222B38, 0xFF90CAF9, 0xFF93A4B8)
    val drivePilot = palette(0xFF33691E, 0xFF222E2C, 0xFFAED581, 0xFF90A8A4)
    val liveWire = palette(0xFFBF360C, 0xFF2E2A22, 0xFFFFAB91, 0xFFB0A090)
    val fromShadow = palette(0xFF263238, 0xFF242A2E, 0xFF80DEEA, 0xFF90A4A8)
    val watchman = palette(0xFF1A237E, 0xFF222830, 0xFF9FA8DA, 0xFF949AA8)
    val carouselWatcher = palette(0xFF880E4F, 0xFF302226, 0xFFF48FB1, 0xFFB09098)
    val synchronist = palette(0xFF004D40, 0xFF222E2C, 0xFF80CBC4, 0xFF90A8A4)
    val quoteDay = palette(0xFF37474F, 0xFF242A2E, 0xFFB0BEC5, 0xFF949AA8)
    val midnightEditor = palette(0xFF1A2744, 0xFF222830, 0xFF9FA8DA, 0xFF949AA8)
    val polyglotFriend = palette(0xFF1565C0, 0xFF222B38, 0xFF64B5F6, 0xFF93A4B8)
    val silence = palette(0xFF424242, 0xFF242830, 0xFFE0E0E0, 0xFF949AA8)
    val reactionStreak = palette(0xFFE65100, 0xFF2E2A22, 0xFFFFCC80, 0xFFB0A090)
    val formatMirror = palette(0xFF311B92, 0xFF262230, 0xFFB39DDB, 0xFF949AA8)
}

internal fun v010AppEasterFlagValues(flags: EasterFlagsDto?): List<Boolean> = listOfNotNull(
    flags?.typographer,
    flags?.spoilerHunter,
    flags?.noMarkers,
    flags?.enterMaster,
    flags?.fontExtremes,
    flags?.cloudKeeper,
    flags?.drivePilot,
    flags?.liveWire,
    flags?.fromShadow,
    flags?.watchman,
    flags?.carouselWatcher,
    flags?.synchronist,
    flags?.quoteDay,
    flags?.midnightEditor,
    flags?.polyglotFriend,
    flags?.silence,
    flags?.reactionStreak,
).map { it }

internal fun allV010EasterFlagValues(flags: EasterFlagsDto?): List<Boolean> =
    v010AppEasterFlagValues(flags) + listOfNotNull(flags?.formatMirror).map { it }
