package org.cyblight.android.util

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.cyblight.android.BuildConfig
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.preferences.AppPreferences
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object EasterLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val logUrl: String
        get() = "${BuildConfig.WEBSITE_URL.trimEnd('/')}/e-log"

    private val androidEggFlags: List<Pair<String, (EasterFlagsDto) -> Boolean>> = listOf(
        EasterLogKey.LIGHT_CATCHER to EasterFlagsDto::lightCatcher,
        EasterLogKey.NIGHT_GUARD to EasterFlagsDto::nightGuard,
        EasterLogKey.TRUSTED_FINGERPRINT to EasterFlagsDto::trustedFingerprint,
        EasterLogKey.ECHO to EasterFlagsDto::echo,
        EasterLogKey.ARCHIVIST to EasterFlagsDto::archivist,
        EasterLogKey.BRIDGE to EasterFlagsDto::bridge,
        EasterLogKey.TYPOGRAPHER to EasterFlagsDto::typographer,
        EasterLogKey.SPOILER_HUNTER to EasterFlagsDto::spoilerHunter,
        EasterLogKey.NO_MARKERS to EasterFlagsDto::noMarkers,
        EasterLogKey.ENTER_MASTER to EasterFlagsDto::enterMaster,
        EasterLogKey.FONT_EXTREMES to EasterFlagsDto::fontExtremes,
        EasterLogKey.CLOUD_KEEPER to EasterFlagsDto::cloudKeeper,
        EasterLogKey.DRIVE_PILOT to EasterFlagsDto::drivePilot,
        EasterLogKey.LIVE_WIRE to EasterFlagsDto::liveWire,
        EasterLogKey.FROM_SHADOW to EasterFlagsDto::fromShadow,
        EasterLogKey.WATCHMAN to EasterFlagsDto::watchman,
        EasterLogKey.CAROUSEL_WATCHER to EasterFlagsDto::carouselWatcher,
        EasterLogKey.FORMAT_MIRROR to EasterFlagsDto::formatMirror,
        EasterLogKey.SYNCHRONIST to EasterFlagsDto::synchronist,
        EasterLogKey.QUOTE_DAY to EasterFlagsDto::quoteDay,
        EasterLogKey.MIDNIGHT_EDITOR to EasterFlagsDto::midnightEditor,
        EasterLogKey.POLYGLOT_FRIEND to EasterFlagsDto::polyglotFriend,
        EasterLogKey.SILENCE to EasterFlagsDto::silence,
        EasterLogKey.REACTION_STREAK to EasterFlagsDto::reactionStreak,
    )

    suspend fun syncLoggedFromServerFlags(
        appPreferences: AppPreferences,
        userName: String,
        flags: EasterFlagsDto,
    ) {
        androidEggFlags.forEach { (eggKey, isUnlocked) ->
            if (isUnlocked(flags)) {
                appPreferences.markEasterTelegramLogged(userName, eggKey)
            }
        }
    }

    fun logLightCatcher(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.LIGHT_CATCHER) {
            log(userName, "light_catcher", "android_version_seven_taps", "android/settings", "cyblight-android://settings/light-catcher")
        }
    }

    fun logNightGuard(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.NIGHT_GUARD) {
            log(userName, "night_guard", "android_dark_theme_after_midnight", "android/main", "cyblight-android://main/night-guard")
        }
    }

    fun logTrustedFingerprint(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.TRUSTED_FINGERPRINT) {
            log(userName, "trusted_fingerprint", "android_biometric_unlock_100", "android/app-lock", "cyblight-android://app-lock/trusted-fingerprint")
        }
    }

    fun logEcho(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.ECHO) {
            log(userName, "echo", "android_message_at_2359", "android/chat", "cyblight-android://chat/echo")
        }
    }

    fun logArchivist(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.ARCHIVIST) {
            log(userName, "archivist", "android_chat_tools_combo", "android/chat", "cyblight-android://chat/archivist")
        }
    }

    fun logBridge(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.BRIDGE) {
            log(userName, "bridge", "android_app_web_same_day", "android/bridge", "cyblight-android://bridge")
        }
    }

    fun logFormatMirror(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.FORMAT_MIRROR) {
            log(userName, "format_mirror", "android_format_mirror_same_day", "android/bridge", "cyblight-android://easter/format_mirror")
        }
    }

    fun logTypographer(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.TYPOGRAPHER, "typographer", "android_all_formats_message")

    fun logSpoilerHunter(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.SPOILER_HUNTER, "spoiler_hunter", "android_spoiler_reveal_5")

    fun logNoMarkers(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.NO_MARKERS, "no_markers", "android_format_menu_only")

    fun logEnterMaster(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.ENTER_MASTER, "enter_master", "android_send_with_enter_10")

    fun logFontExtremes(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.FONT_EXTREMES, "font_extremes", "android_font_min_max_send")

    fun logCloudKeeper(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.CLOUD_KEEPER, "cloud_keeper", "android_drive_backup_success")

    fun logDrivePilot(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.DRIVE_PILOT, "drive_pilot", "android_google_account_pick_3")

    fun logLiveWire(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.LIVE_WIRE, "live_wire", "android_websocket_message")

    fun logFromShadow(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.FROM_SHADOW, "from_shadow", "android_push_open_chat")

    fun logWatchman(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.WATCHMAN, "watchman", "android_lock_push_open_3")

    fun logCarouselWatcher(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.CAROUSEL_WATCHER, "carousel_watcher", "android_home_carousel_30s")

    fun logSynchronist(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.SYNCHRONIST, "synchronist", "android_drive_restore_success")

    fun logQuoteDay(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.QUOTE_DAY, "quote_day", "android_blockquote_send_3")

    fun logMidnightEditor(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.MIDNIGHT_EDITOR, "midnight_editor", "android_format_after_midnight")

    fun logPolyglotFriend(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.POLYGLOT_FRIEND, "polyglot_friend", "android_send_3_locales")

    fun logSilence(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.SILENCE, "silence", "android_emoji_spoiler_message")

    fun logReactionStreak(appPreferences: AppPreferences, userName: String) =
        logV010(appPreferences, userName, EasterLogKey.REACTION_STREAK, "reaction_streak", "android_reaction_streak_10")

    private fun logV010(
        appPreferences: AppPreferences,
        userName: String,
        eggKey: String,
        type: String,
        source: String,
    ) {
        logOnce(appPreferences, userName, eggKey) {
            log(userName, type, source, "android/easter", "cyblight-android://easter/$type")
        }
    }

    private fun logOnce(
        appPreferences: AppPreferences,
        userName: String,
        eggKey: String,
        send: () -> Unit,
    ) {
        scope.launch {
            if (appPreferences.isEasterTelegramLogged(userName, eggKey)) return@launch
            send()
            appPreferences.markEasterTelegramLogged(userName, eggKey)
        }
    }

    private fun log(
        userName: String,
        type: String,
        source: String,
        route: String,
        page: String,
    ) {
        val payload = mapOf(
            "type" to type,
            "userName" to userName,
            "source" to source,
            "route" to route,
            "page" to page,
            "timezone" to TimeZone.getDefault().id,
            "ua" to AppUserAgent.build(),
            "referrer" to null,
        )

        runCatching {
            val body = gson.toJson(payload)
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(logUrl)
                .post(body)
                .header("Origin", BuildConfig.WEBSITE_URL)
                .header("User-Agent", AppUserAgent.build())
                .build()
            client.newCall(request).execute().close()
        }
    }
}

private object EasterLogKey {
    const val LIGHT_CATCHER = "light_catcher"
    const val NIGHT_GUARD = "night_guard"
    const val TRUSTED_FINGERPRINT = "trusted_fingerprint"
    const val ECHO = "echo"
    const val ARCHIVIST = "archivist"
    const val BRIDGE = "bridge"
    const val TYPOGRAPHER = "typographer"
    const val SPOILER_HUNTER = "spoiler_hunter"
    const val NO_MARKERS = "no_markers"
    const val ENTER_MASTER = "enter_master"
    const val FONT_EXTREMES = "font_extremes"
    const val CLOUD_KEEPER = "cloud_keeper"
    const val DRIVE_PILOT = "drive_pilot"
    const val LIVE_WIRE = "live_wire"
    const val FROM_SHADOW = "from_shadow"
    const val WATCHMAN = "watchman"
    const val CAROUSEL_WATCHER = "carousel_watcher"
    const val FORMAT_MIRROR = "format_mirror"
    const val SYNCHRONIST = "synchronist"
    const val QUOTE_DAY = "quote_day"
    const val MIDNIGHT_EDITOR = "midnight_editor"
    const val POLYGLOT_FRIEND = "polyglot_friend"
    const val SILENCE = "silence"
    const val REACTION_STREAK = "reaction_streak"
}
