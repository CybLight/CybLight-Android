package org.cyblight.android.ui.easter

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cyblight.android.R
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.EasterProgress
import org.cyblight.android.ui.components.DetailScaffold

internal data class EasterEggPalette(
    val unlockedContainer: Color,
    val lockedContainer: Color,
    val unlockedContent: Color,
    val lockedContent: Color,
)

internal data class EasterEggItem(
    val emoji: String,
    @StringRes val titleRes: Int,
    val unlocked: Boolean,
    val palette: EasterEggPalette,
    @StringRes val descUnlockedRes: Int,
    @StringRes val descLockedRes: Int,
    @StringRes val hintUnlockedRes: Int? = null,
    @StringRes val hintLockedRes: Int? = null,
    val progressCurrent: Int? = null,
    val progressTotal: Int? = null,
    val progressIsSeconds: Boolean = false,
)

private val EasterBadgeFoundGreen = Color(0xFF22C55E)
private val EasterBadgeFoundBackground = Color(0x2622C55E)
private val EasterBadgeLockedRed = Color(0xFFEF4444)
private val EasterBadgeLockedBackground = Color(0x26EF4444)

private val EasterTabActiveBorder = Color(0x8CFB923C)
private val EasterTabActiveBackground = Color(0x24FB923C)
private val EasterTabActiveText = Color(0xFFFFD7A8)
private val EasterTabActiveCountBackground = Color(0x38FB923C)
private val EasterTabActiveCountText = Color(0xFFFFE4C4)

private const val EASTER_SITE_TOTAL = 6
private const val EASTER_APP_BASE_TOTAL = 5
private const val V010_APP_EGGS_TOTAL = 17
private const val EASTER_BRIDGE_TOTAL = 2
private const val EASTER_EGGS_TOTAL =
    EASTER_SITE_TOTAL + EASTER_APP_BASE_TOTAL + V010_APP_EGGS_TOTAL + EASTER_BRIDGE_TOTAL

private enum class EasterEggTab(val emoji: String, @StringRes val titleRes: Int) {
    APP("📱", R.string.easter_section_app),
    SITE("🌐", R.string.easter_section_website),
    BRIDGE("🌉", R.string.easter_section_bridge),
}

private fun countSiteUnlockedEggs(flags: EasterFlagsDto?): Int {
    if (flags == null) return 0
    return listOf(
        flags.strawberry,
        flags.profileMirror,
        flags.darkTrigger,
        flags.postmaster,
        flags.developerMode,
        flags.themeFlux,
    ).count { it }
}

private fun countAppUnlockedEggs(flags: EasterFlagsDto?): Int {
    if (flags == null) return 0
    return listOf(
        flags.lightCatcher,
        flags.nightGuard,
        flags.trustedFingerprint,
        flags.echo,
        flags.archivist,
    ).count { it } + v010AppEasterFlagValues(flags).count { it }
}

private fun countBridgeUnlockedEggs(flags: EasterFlagsDto?): Int {
    if (flags == null) return 0
    return listOf(flags.bridge, flags.formatMirror).count { it }
}

private fun countUnlockedEasterEggs(flags: EasterFlagsDto?): Int =
    countSiteUnlockedEggs(flags) + countAppUnlockedEggs(flags) + countBridgeUnlockedEggs(flags)

private object EasterEggPalettes {
    val lightCatcher = EasterEggPalette(
        unlockedContainer = Color(0xFF4A3B12),
        lockedContainer = Color(0xFF2B261C),
        unlockedContent = Color(0xFFFFE082),
        lockedContent = Color(0xFFB0A48A),
    )
    val strawberry = EasterEggPalette(
        unlockedContainer = Color(0xFF5A1F2B),
        lockedContainer = Color(0xFF302226),
        unlockedContent = Color(0xFFFF8A9B),
        lockedContent = Color(0xFFB09098),
    )
    val profileMirror = EasterEggPalette(
        unlockedContainer = Color(0xFF1A3558),
        lockedContainer = Color(0xFF222B38),
        unlockedContent = Color(0xFF90CAF9),
        lockedContent = Color(0xFF93A4B8),
    )
    val darkTrigger = EasterEggPalette(
        unlockedContainer = Color(0xFF352452),
        lockedContainer = Color(0xFF262230),
        unlockedContent = Color(0xFFCE93D8),
        lockedContent = Color(0xFF9E95AB),
    )
    val postmaster = EasterEggPalette(
        unlockedContainer = Color(0xFF1A4540),
        lockedContainer = Color(0xFF222E2C),
        unlockedContent = Color(0xFF80CBC4),
        lockedContent = Color(0xFF90A8A4),
    )
    val themeFlux = EasterEggPalette(
        unlockedContainer = Color(0xFF2A3550),
        lockedContainer = Color(0xFF242830),
        unlockedContent = Color(0xFFB39DDB),
        lockedContent = Color(0xFF949AA8),
    )
    val developerMode = EasterEggPalette(
        unlockedContainer = Color(0xFF4A3318),
        lockedContainer = Color(0xFF2E2A22),
        unlockedContent = Color(0xFFFFCC80),
        lockedContent = Color(0xFFB0A090),
    )
    val nightGuard = EasterEggPalette(
        unlockedContainer = Color(0xFF1A2744),
        lockedContainer = Color(0xFF222830),
        unlockedContent = Color(0xFF9FA8DA),
        lockedContent = Color(0xFF949AA8),
    )
    val trustedFingerprint = EasterEggPalette(
        unlockedContainer = Color(0xFF263238),
        lockedContainer = Color(0xFF242A2E),
        unlockedContent = Color(0xFF80DEEA),
        lockedContent = Color(0xFF90A4A8),
    )
    val bridge = EasterEggPalette(
        unlockedContainer = Color(0xFF3E2723),
        lockedContainer = Color(0xFF2A2422),
        unlockedContent = Color(0xFFFFAB91),
        lockedContent = Color(0xFFB0A090),
    )
    val echo = EasterEggPalette(
        unlockedContainer = Color(0xFF311B92),
        lockedContainer = Color(0xFF262230),
        unlockedContent = Color(0xFFB39DDB),
        lockedContent = Color(0xFF9E95AB),
    )
    val archivist = EasterEggPalette(
        unlockedContainer = Color(0xFF37474F),
        lockedContainer = Color(0xFF262B2E),
        unlockedContent = Color(0xFFB0BEC5),
        lockedContent = Color(0xFF939AA0),
    )
}

@Composable
fun EasterEggsScreen(
    flags: EasterFlagsDto?,
    progress: EasterProgress,
    isLoading: Boolean,
    error: String?,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val body: @Composable (Modifier) -> Unit = { contentModifier ->
        when {
            isLoading && flags == null -> {
                Column(
                    modifier = contentModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            !error.isNullOrBlank() && flags == null -> {
                Column(
                    modifier = contentModifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                EasterEggsContent(
                    flags = flags,
                    progress = progress,
                    modifier = contentModifier,
                )
            }
        }
    }

    if (onBack != null) {
        DetailScaffold(
            title = stringResource(R.string.easter_eggs_title),
            onBack = onBack,
            modifier = modifier,
        ) { padding ->
            body(Modifier.padding(padding))
        }
    } else {
        body(modifier)
    }
}

@Composable
private fun EasterEggsContent(
    flags: EasterFlagsDto?,
    progress: EasterProgress,
    modifier: Modifier = Modifier,
) {
    val appEggs = remember(flags, progress) {
        listOf(
            EasterEggItem(
                emoji = "💡",
                titleRes = R.string.easter_light_catcher_title,
                unlocked = flags?.lightCatcher == true,
                palette = EasterEggPalettes.lightCatcher,
                descUnlockedRes = R.string.easter_light_catcher_desc_unlocked,
                descLockedRes = R.string.easter_light_catcher_desc_locked,
                hintUnlockedRes = R.string.easter_light_catcher_hint_unlocked,
                hintLockedRes = R.string.easter_light_catcher_hint_locked,
            ),
            EasterEggItem(
                emoji = "🌙",
                titleRes = R.string.easter_night_guard_title,
                unlocked = flags?.nightGuard == true,
                palette = EasterEggPalettes.nightGuard,
                descUnlockedRes = R.string.easter_night_guard_desc_unlocked,
                descLockedRes = R.string.easter_night_guard_desc_locked,
                hintLockedRes = R.string.easter_night_guard_hint_locked,
                hintUnlockedRes = R.string.easter_night_guard_hint_unlocked,
                progressCurrent = if (flags?.nightGuard != true) progress.nightGuardSeconds else null,
                progressTotal = if (flags?.nightGuard != true) 30 else null,
                progressIsSeconds = true,
            ),
            EasterEggItem(
                emoji = "👆",
                titleRes = R.string.easter_trusted_fingerprint_title,
                unlocked = flags?.trustedFingerprint == true,
                palette = EasterEggPalettes.trustedFingerprint,
                descUnlockedRes = R.string.easter_trusted_fingerprint_desc_unlocked,
                descLockedRes = R.string.easter_trusted_fingerprint_desc_locked,
                hintLockedRes = R.string.easter_trusted_fingerprint_hint_locked,
                hintUnlockedRes = R.string.easter_trusted_fingerprint_hint_unlocked,
                progressCurrent = if (flags?.trustedFingerprint != true) progress.biometricUnlockCount else null,
                progressTotal = if (flags?.trustedFingerprint != true) 100 else null,
            ),
            EasterEggItem(
                emoji = "🔔",
                titleRes = R.string.easter_echo_title,
                unlocked = flags?.echo == true,
                palette = EasterEggPalettes.echo,
                descUnlockedRes = R.string.easter_echo_desc_unlocked,
                descLockedRes = R.string.easter_echo_desc_locked,
                hintLockedRes = R.string.easter_echo_hint_locked,
                hintUnlockedRes = R.string.easter_echo_hint_unlocked,
            ),
            EasterEggItem(
                emoji = "📚",
                titleRes = R.string.easter_archivist_title,
                unlocked = flags?.archivist == true,
                palette = EasterEggPalettes.archivist,
                descUnlockedRes = R.string.easter_archivist_desc_unlocked,
                descLockedRes = R.string.easter_archivist_desc_locked,
                hintLockedRes = R.string.easter_archivist_hint_locked,
                hintUnlockedRes = R.string.easter_archivist_hint_unlocked,
                progressCurrent = if (flags?.archivist != true) progress.archivistStepsCompleted else null,
                progressTotal = if (flags?.archivist != true) 4 else null,
            ),
        ) + v010AppEasterEggs(flags, progress)
    }
    val bridgeEggs = remember(flags, progress) {
        listOf(
            EasterEggItem(
                emoji = "🌉",
                titleRes = R.string.easter_bridge_title,
                unlocked = flags?.bridge == true,
                palette = EasterEggPalettes.bridge,
                descUnlockedRes = R.string.easter_bridge_desc_unlocked,
                descLockedRes = R.string.easter_bridge_desc_locked,
                hintUnlockedRes = R.string.easter_bridge_hint_unlocked,
                hintLockedRes = R.string.easter_bridge_hint_locked,
                progressCurrent = if (flags?.bridge != true) progress.bridgePlatformsToday else null,
                progressTotal = if (flags?.bridge != true) 2 else null,
            ),
        ) + v010BridgeEasterEggs(flags, progress)
    }
    val websiteEggs = remember(flags) {
        listOf(
            EasterEggItem(
                emoji = "🍓",
                titleRes = R.string.easter_strawberry_title,
                unlocked = flags?.strawberry == true,
                palette = EasterEggPalettes.strawberry,
                descUnlockedRes = R.string.easter_strawberry_desc_unlocked,
                descLockedRes = R.string.easter_strawberry_desc_locked,
                hintUnlockedRes = R.string.easter_strawberry_hint_unlocked,
                hintLockedRes = R.string.easter_strawberry_hint_locked,
            ),
            EasterEggItem(
                emoji = "🪞",
                titleRes = R.string.easter_profile_mirror_title,
                unlocked = flags?.profileMirror == true,
                palette = EasterEggPalettes.profileMirror,
                descUnlockedRes = R.string.easter_profile_mirror_desc_unlocked,
                descLockedRes = R.string.easter_profile_mirror_desc_locked,
                hintUnlockedRes = R.string.easter_profile_mirror_hint_unlocked,
                hintLockedRes = R.string.easter_profile_mirror_hint_locked,
            ),
            EasterEggItem(
                emoji = "🌑",
                titleRes = R.string.easter_dark_trigger_title,
                unlocked = flags?.darkTrigger == true,
                palette = EasterEggPalettes.darkTrigger,
                descUnlockedRes = R.string.easter_dark_trigger_desc_unlocked,
                descLockedRes = R.string.easter_dark_trigger_desc_locked,
                hintUnlockedRes = R.string.easter_dark_trigger_hint_unlocked,
                hintLockedRes = R.string.easter_dark_trigger_hint_locked,
            ),
            EasterEggItem(
                emoji = "📬",
                titleRes = R.string.easter_postmaster_title,
                unlocked = flags?.postmaster == true,
                palette = EasterEggPalettes.postmaster,
                descUnlockedRes = R.string.easter_postmaster_desc_unlocked,
                descLockedRes = R.string.easter_postmaster_desc_locked,
                hintUnlockedRes = R.string.easter_postmaster_hint_unlocked,
                hintLockedRes = R.string.easter_postmaster_hint_locked,
            ),
            EasterEggItem(
                emoji = "🛠️",
                titleRes = R.string.easter_developer_mode_title,
                unlocked = flags?.developerMode == true,
                palette = EasterEggPalettes.developerMode,
                descUnlockedRes = R.string.easter_developer_mode_desc_unlocked,
                descLockedRes = R.string.easter_developer_mode_desc_locked,
                hintUnlockedRes = R.string.easter_developer_mode_hint_unlocked,
                hintLockedRes = R.string.easter_developer_mode_hint_locked,
            ),
            EasterEggItem(
                emoji = "🌗",
                titleRes = R.string.easter_theme_flux_title,
                unlocked = flags?.themeFlux == true,
                palette = EasterEggPalettes.themeFlux,
                descUnlockedRes = R.string.easter_theme_flux_desc_unlocked,
                descLockedRes = R.string.easter_theme_flux_desc_locked,
                hintUnlockedRes = R.string.easter_theme_flux_hint_unlocked,
                hintLockedRes = R.string.easter_theme_flux_hint_locked,
            ),
        )
    }
    val foundCount = remember(flags) { countUnlockedEasterEggs(flags) }
    val siteFoundCount = remember(flags) { countSiteUnlockedEggs(flags) }
    val appFoundCount = remember(flags) { countAppUnlockedEggs(flags) }
    val bridgeFoundCount = remember(flags) { countBridgeUnlockedEggs(flags) }
    var selectedTab by remember { mutableStateOf(EasterEggTab.APP) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.easter_eggs_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        EasterCollectionSummary(
            found = foundCount,
            total = EASTER_EGGS_TOTAL,
        )

        EasterSubtabsRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            siteFound = siteFoundCount,
            appFound = appFoundCount,
            bridgeFound = bridgeFoundCount,
        )

        when (selectedTab) {
            EasterEggTab.APP -> appEggs.forEach { item -> EasterEggCard(item = item) }
            EasterEggTab.SITE -> websiteEggs.forEach { item -> EasterEggCard(item = item) }
            EasterEggTab.BRIDGE -> bridgeEggs.forEach { item -> EasterEggCard(item = item) }
        }
    }
}

@Composable
private fun EasterSubtabsRow(
    selectedTab: EasterEggTab,
    onTabSelected: (EasterEggTab) -> Unit,
    siteFound: Int,
    appFound: Int,
    bridgeFound: Int,
    modifier: Modifier = Modifier,
) {
    val tabCounts = mapOf(
        EasterEggTab.APP to (appFound to EASTER_APP_BASE_TOTAL + V010_APP_EGGS_TOTAL),
        EasterEggTab.SITE to (siteFound to EASTER_SITE_TOTAL),
        EasterEggTab.BRIDGE to (bridgeFound to EASTER_BRIDGE_TOTAL),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EasterEggTab.entries.forEach { tab ->
            val (found, total) = tabCounts.getValue(tab)
            EasterSubtab(
                emoji = tab.emoji,
                title = stringResource(tab.titleRes),
                found = found,
                total = total,
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
            )
        }
    }
}

@Composable
private fun EasterSubtab(
    emoji: String,
    title: String,
    found: Int,
    total: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isComplete = found >= total
    val borderColor = if (selected) EasterTabActiveBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val containerColor = if (selected) {
        EasterTabActiveBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val titleColor = if (selected) EasterTabActiveText else MaterialTheme.colorScheme.onSurfaceVariant
    val countBackground = when {
        isComplete && selected -> Color(0x38BBF7D0)
        isComplete -> Color(0x294ADE80)
        selected -> EasterTabActiveCountBackground
        else -> Color.White.copy(alpha = 0.08f)
    }
    val countColor = when {
        isComplete && selected -> Color(0xFFBBF7D0)
        isComplete -> Color(0xFF86EFAC)
        selected -> EasterTabActiveCountText
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    }

    Row(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .background(containerColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
        )
        Text(
            text = "$found/$total",
            modifier = Modifier
                .background(countBackground, RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = countColor,
        )
    }
}

@Composable
private fun EasterCollectionSummary(
    found: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val isComplete = found >= total
    val text = if (isComplete) {
        stringResource(R.string.easter_collection_complete)
    } else {
        stringResource(R.string.easter_collection_progress, found, total)
    }

    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isComplete) EasterBadgeFoundBackground else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isComplete) FontWeight.Bold else FontWeight.SemiBold,
        color = if (isComplete) EasterBadgeFoundGreen else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EasterEggStatusBadge(
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (unlocked) EasterBadgeFoundBackground else EasterBadgeLockedBackground
    val textColor = if (unlocked) EasterBadgeFoundGreen else EasterBadgeLockedRed
    val label = if (unlocked) {
        stringResource(R.string.easter_badge_found)
    } else {
        stringResource(R.string.easter_badge_locked)
    }

    Text(
        text = label.uppercase(),
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun EasterEggCard(
    item: EasterEggItem,
) {
    val containerColor = if (item.unlocked) item.palette.unlockedContainer else item.palette.lockedContainer
    val contentColor = if (item.unlocked) item.palette.unlockedContent else item.palette.lockedContent
    val descriptionRes = if (item.unlocked) item.descUnlockedRes else item.descLockedRes
    val hintRes = if (item.unlocked) item.hintUnlockedRes else item.hintLockedRes

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 92.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = item.emoji, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(item.titleRes),
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
                Text(
                    text = stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f),
                )
                if (!item.unlocked && item.progressCurrent != null && item.progressTotal != null) {
                    val progressText = if (item.progressIsSeconds) {
                        stringResource(R.string.easter_progress_seconds, item.progressCurrent, item.progressTotal)
                    } else {
                        stringResource(R.string.easter_progress_of, item.progressCurrent, item.progressTotal)
                    }
                    Text(
                        text = progressText,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor.copy(alpha = 0.9f),
                    )
                }
                hintRes?.let { res ->
                    val isCongrats = item.unlocked && item.hintUnlockedRes != null
                    Text(
                        text = stringResource(res),
                        modifier = if (isCongrats) {
                            Modifier
                                .background(EasterBadgeFoundBackground, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        } else {
                            Modifier
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCongrats) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCongrats) EasterBadgeFoundGreen else contentColor.copy(alpha = 0.7f),
                    )
                }
            }
            EasterEggStatusBadge(
                unlocked = item.unlocked,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
