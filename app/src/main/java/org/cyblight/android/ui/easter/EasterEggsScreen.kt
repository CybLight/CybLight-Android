package org.cyblight.android.ui.easter

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

private data class EasterEggPalette(
    val unlockedContainer: Color,
    val lockedContainer: Color,
    val unlockedContent: Color,
    val lockedContent: Color,
)

private data class EasterEggItem(
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

private const val EASTER_EGGS_TOTAL = 12

private fun countUnlockedEasterEggs(flags: EasterFlagsDto?): Int {
    if (flags == null) return 0
    return listOf(
        flags.strawberry,
        flags.darkTrigger,
        flags.profileMirror,
        flags.lightCatcher,
        flags.postmaster,
        flags.developerMode,
        flags.themeFlux,
        flags.nightGuard,
        flags.trustedFingerprint,
        flags.bridge,
        flags.echo,
        flags.archivist,
    ).count { it }
}

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
            ),
            EasterEggItem(
                emoji = "📚",
                titleRes = R.string.easter_archivist_title,
                unlocked = flags?.archivist == true,
                palette = EasterEggPalettes.archivist,
                descUnlockedRes = R.string.easter_archivist_desc_unlocked,
                descLockedRes = R.string.easter_archivist_desc_locked,
                hintLockedRes = R.string.easter_archivist_hint_locked,
                progressCurrent = if (flags?.archivist != true) progress.archivistStepsCompleted else null,
                progressTotal = if (flags?.archivist != true) 4 else null,
            ),
        )
    }
    val bridgeEgg = remember(flags, progress) {
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
        )
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

        EasterEggsSectionTitle(
            emoji = "📱",
            title = stringResource(R.string.easter_section_app),
        )
        appEggs.forEach { item ->
            EasterEggCard(item = item)
        }

        EasterEggsSectionDivider(label = stringResource(R.string.easter_section_bridge))

        EasterEggsSectionTitle(
            emoji = "🌉",
            title = stringResource(R.string.easter_section_bridge),
        )
        EasterEggCard(item = bridgeEgg)

        EasterEggsSectionDivider(label = stringResource(R.string.easter_section_website))

        EasterEggsSectionTitle(
            emoji = "🌐",
            title = stringResource(R.string.easter_section_website),
        )
        websiteEggs.forEach { item ->
            EasterEggCard(item = item)
        }
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
private fun EasterEggsSectionTitle(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EasterEggsSectionDivider(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
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
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
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
