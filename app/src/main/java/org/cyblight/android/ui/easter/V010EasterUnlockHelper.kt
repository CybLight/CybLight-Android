package org.cyblight.android.ui.easter

import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.data.preferences.V010EasterProgressSnapshot
import org.cyblight.android.data.repository.ProfileRepository
import org.cyblight.android.util.EasterEggChecks
import org.cyblight.android.util.EasterLogger

internal class V010EasterUnlockHelper(
    private val profileRepository: ProfileRepository,
    private val appPreferences: AppPreferences,
    private val currentLogin: () -> String?,
    private val currentFlags: () -> EasterFlagsDto?,
    private val currentLocale: () -> String,
    private val currentFontSize: () -> org.cyblight.android.data.preferences.ChatFontSize,
    private val isAppLockEnabled: () -> Boolean,
    private val onFlagsRefreshed: () -> Unit,
    private val onUnlocked: (EasterCelebrationKind) -> Unit,
) {
    var draftFormattedViaMenu: Boolean = false
    var openedChatFromNotification: Boolean = false

    fun markDraftFormattedViaMenu() {
        draftFormattedViaMenu = true
    }

    fun markOpenedChatFromNotification() {
        openedChatFromNotification = true
    }

    suspend fun onMessageSent(content: String, sentViaEnter: Boolean) {
        val flags = currentFlags() ?: return
        val plain = content.trim()
        if (plain.isEmpty()) return

        if (!flags.typographer && EasterEggChecks.isTypographerMessage(plain)) {
            unlock(EasterEggKind.Typographer)
        }
        if (!flags.noMarkers && draftFormattedViaMenu && EasterEggChecks.hasFormatting(plain)) {
            unlock(EasterEggKind.NoMarkers)
        }
        if (!flags.silence && EasterEggChecks.isSilenceMessage(plain)) {
            unlock(EasterEggKind.Silence)
        }
        if (!flags.midnightEditor && draftFormattedViaMenu && EasterEggChecks.isMidnightNow()) {
            unlock(EasterEggKind.MidnightEditor)
        }
        if (EasterEggChecks.hasQuote(plain)) {
            bumpProgress { progress ->
                val next = progress.copy(quoteCount = progress.quoteCount + 1)
                if (!flags.quoteDay && next.quoteCount >= 3) {
                    unlock(EasterEggKind.QuoteDay)
                }
                next
            }
        }
        if (EasterEggChecks.hasFormatting(plain)) {
            val hadFormatMirror = flags.formatMirror
            profileRepository.touchFormatApp().onSuccess { response ->
                if (!hadFormatMirror && response.formatMirror) {
                    onUnlocked(EasterCelebrationKind.FORMAT_MIRROR)
                }
                onFlagsRefreshed()
            }
        }

        val locale = currentLocale()
        bumpProgress { progress ->
            val locales = progress.polyglotLocales + locale
            val next = progress.copy(polyglotLocales = locales)
            if (!flags.polyglotFriend && setOf("ru", "uk", "en").all { it in locales }) {
                unlock(EasterEggKind.PolyglotFriend)
            }
            next
        }

        val fontSize = currentFontSize()
        bumpProgress { progress ->
            var next = progress
            if (fontSize == org.cyblight.android.data.preferences.ChatFontSize.SMALL) {
                next = next.copy(fontMinSent = true)
            }
            if (fontSize == org.cyblight.android.data.preferences.ChatFontSize.EXTRA_LARGE) {
                next = next.copy(fontMaxSent = true)
            }
            if (!flags.fontExtremes && next.fontMinSent && next.fontMaxSent) {
                unlock(EasterEggKind.FontExtremes)
            }
            next
        }

        if (sentViaEnter) {
            bumpProgress { progress ->
                val next = progress.copy(enterSendCount = progress.enterSendCount + 1)
                if (!flags.enterMaster && next.enterSendCount >= 10) {
                    unlock(EasterEggKind.EnterMaster)
                }
                next
            }
        }

        draftFormattedViaMenu = false
    }

    suspend fun onSpoilerRevealed() {
        val flags = currentFlags() ?: return
        if (flags.spoilerHunter) return
        bumpProgress { progress ->
            val next = progress.copy(spoilerReveals = progress.spoilerReveals + 1)
            if (next.spoilerReveals >= 5) {
                unlock(EasterEggKind.SpoilerHunter)
            }
            next
        }
    }

    suspend fun onReactionAdded(chatId: String) {
        val flags = currentFlags() ?: return
        if (flags.reactionStreak) return
        bumpProgress { progress ->
            val sameChat = progress.reactionStreakChatId == chatId
            val streak = if (sameChat) progress.reactionStreak + 1 else 1
            val next = progress.copy(reactionStreak = streak, reactionStreakChatId = chatId)
            if (next.reactionStreak >= 10) {
                unlock(EasterEggKind.ReactionStreak)
            }
            next
        }
    }

    suspend fun onGoogleDriveBackupSuccess() {
        unlockIfLocked(EasterEggKind.CloudKeeper)
    }

    suspend fun onGoogleDriveRestoreSuccess() {
        unlockIfLocked(EasterEggKind.Synchronist)
    }

    suspend fun onGoogleAccountPicked() {
        val flags = currentFlags() ?: return
        if (flags.drivePilot) return
        bumpProgress { progress ->
            val next = progress.copy(driveAccountPicks = progress.driveAccountPicks + 1)
            if (next.driveAccountPicks >= 3) {
                unlock(EasterEggKind.DrivePilot)
            }
            next
        }
    }

    suspend fun onWebSocketMessageReceived() {
        unlockIfLocked(EasterEggKind.LiveWire)
    }

    suspend fun onOpenedChatFromNotificationHandled() {
        if (!openedChatFromNotification) return
        openedChatFromNotification = false
        unlockIfLocked(EasterEggKind.FromShadow)

        if (!isAppLockEnabled()) return
        val flags = currentFlags() ?: return
        if (flags.watchman) return
        bumpProgress { progress ->
            val next = progress.copy(watchmanOpens = progress.watchmanOpens + 1)
            if (next.watchmanOpens >= 3) {
                unlock(EasterEggKind.Watchman)
            }
            next
        }
    }

    suspend fun addCarouselWatchSeconds(seconds: Int) {
        val flags = currentFlags() ?: return
        if (flags.carouselWatcher) return
        bumpProgress { progress ->
            val next = progress.copy(carouselSeconds = (progress.carouselSeconds + seconds).coerceAtMost(30))
            if (next.carouselSeconds >= 30) {
                unlock(EasterEggKind.CarouselWatcher)
            }
            next
        }
    }

    private suspend fun bumpProgress(transform: suspend (V010EasterProgressSnapshot) -> V010EasterProgressSnapshot) {
        val current = appPreferences.getV010EasterProgress()
        appPreferences.saveV010EasterProgress(transform(current))
    }

    private suspend fun unlockIfLocked(kind: EasterEggKind) {
        val flags = currentFlags() ?: return
        if (kind.isUnlocked(flags)) return
        unlock(kind)
    }

    private suspend fun unlock(kind: EasterEggKind) {
        kind.unlock(profileRepository).onSuccess {
            currentLogin()?.let { login -> kind.log(appPreferences, login) }
            onUnlocked(kind.toCelebrationKind())
            onFlagsRefreshed()
        }
    }
}

private fun EasterEggKind.toCelebrationKind(): EasterCelebrationKind = when (this) {
    EasterEggKind.Typographer -> EasterCelebrationKind.TYPOGRAPHER
    EasterEggKind.SpoilerHunter -> EasterCelebrationKind.SPOILER_HUNTER
    EasterEggKind.NoMarkers -> EasterCelebrationKind.NO_MARKERS
    EasterEggKind.EnterMaster -> EasterCelebrationKind.ENTER_MASTER
    EasterEggKind.FontExtremes -> EasterCelebrationKind.FONT_EXTREMES
    EasterEggKind.CloudKeeper -> EasterCelebrationKind.CLOUD_KEEPER
    EasterEggKind.DrivePilot -> EasterCelebrationKind.DRIVE_PILOT
    EasterEggKind.LiveWire -> EasterCelebrationKind.LIVE_WIRE
    EasterEggKind.FromShadow -> EasterCelebrationKind.FROM_SHADOW
    EasterEggKind.Watchman -> EasterCelebrationKind.WATCHMAN
    EasterEggKind.CarouselWatcher -> EasterCelebrationKind.CAROUSEL_WATCHER
    EasterEggKind.Synchronist -> EasterCelebrationKind.SYNCHRONIST
    EasterEggKind.QuoteDay -> EasterCelebrationKind.QUOTE_DAY
    EasterEggKind.MidnightEditor -> EasterCelebrationKind.MIDNIGHT_EDITOR
    EasterEggKind.PolyglotFriend -> EasterCelebrationKind.POLYGLOT_FRIEND
    EasterEggKind.Silence -> EasterCelebrationKind.SILENCE
    EasterEggKind.ReactionStreak -> EasterCelebrationKind.REACTION_STREAK
}

private enum class EasterEggKind {
    Typographer,
    SpoilerHunter,
    NoMarkers,
    EnterMaster,
    FontExtremes,
    CloudKeeper,
    DrivePilot,
    LiveWire,
    FromShadow,
    Watchman,
    CarouselWatcher,
    Synchronist,
    QuoteDay,
    MidnightEditor,
    PolyglotFriend,
    Silence,
    ReactionStreak,
    ;

    fun isUnlocked(flags: EasterFlagsDto): Boolean = when (this) {
        Typographer -> flags.typographer
        SpoilerHunter -> flags.spoilerHunter
        NoMarkers -> flags.noMarkers
        EnterMaster -> flags.enterMaster
        FontExtremes -> flags.fontExtremes
        CloudKeeper -> flags.cloudKeeper
        DrivePilot -> flags.drivePilot
        LiveWire -> flags.liveWire
        FromShadow -> flags.fromShadow
        Watchman -> flags.watchman
        CarouselWatcher -> flags.carouselWatcher
        Synchronist -> flags.synchronist
        QuoteDay -> flags.quoteDay
        MidnightEditor -> flags.midnightEditor
        PolyglotFriend -> flags.polyglotFriend
        Silence -> flags.silence
        ReactionStreak -> flags.reactionStreak
    }

    suspend fun unlock(repo: ProfileRepository): Result<Unit> = when (this) {
        Typographer -> repo.unlockTypographer()
        SpoilerHunter -> repo.unlockSpoilerHunter()
        NoMarkers -> repo.unlockNoMarkers()
        EnterMaster -> repo.unlockEnterMaster()
        FontExtremes -> repo.unlockFontExtremes()
        CloudKeeper -> repo.unlockCloudKeeper()
        DrivePilot -> repo.unlockDrivePilot()
        LiveWire -> repo.unlockLiveWire()
        FromShadow -> repo.unlockFromShadow()
        Watchman -> repo.unlockWatchman()
        CarouselWatcher -> repo.unlockCarouselWatcher()
        Synchronist -> repo.unlockSynchronist()
        QuoteDay -> repo.unlockQuoteDay()
        MidnightEditor -> repo.unlockMidnightEditor()
        PolyglotFriend -> repo.unlockPolyglotFriend()
        Silence -> repo.unlockSilence()
        ReactionStreak -> repo.unlockReactionStreak()
    }

    fun log(prefs: AppPreferences, login: String) {
        when (this) {
            Typographer -> EasterLogger.logTypographer(prefs, login)
            SpoilerHunter -> EasterLogger.logSpoilerHunter(prefs, login)
            NoMarkers -> EasterLogger.logNoMarkers(prefs, login)
            EnterMaster -> EasterLogger.logEnterMaster(prefs, login)
            FontExtremes -> EasterLogger.logFontExtremes(prefs, login)
            CloudKeeper -> EasterLogger.logCloudKeeper(prefs, login)
            DrivePilot -> EasterLogger.logDrivePilot(prefs, login)
            LiveWire -> EasterLogger.logLiveWire(prefs, login)
            FromShadow -> EasterLogger.logFromShadow(prefs, login)
            Watchman -> EasterLogger.logWatchman(prefs, login)
            CarouselWatcher -> EasterLogger.logCarouselWatcher(prefs, login)
            Synchronist -> EasterLogger.logSynchronist(prefs, login)
            QuoteDay -> EasterLogger.logQuoteDay(prefs, login)
            MidnightEditor -> EasterLogger.logMidnightEditor(prefs, login)
            PolyglotFriend -> EasterLogger.logPolyglotFriend(prefs, login)
            Silence -> EasterLogger.logSilence(prefs, login)
            ReactionStreak -> EasterLogger.logReactionStreak(prefs, login)
        }
    }
}
