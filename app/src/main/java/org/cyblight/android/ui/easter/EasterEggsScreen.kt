package org.cyblight.android.ui.easter

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import org.cyblight.android.R
import org.cyblight.android.data.api.EasterFlagsDto
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
)

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
    modifier: Modifier = Modifier,
) {
    val appEggs = remember(flags) {
        listOf(
            EasterEggItem("💡", R.string.easter_light_catcher_title, flags?.lightCatcher == true, EasterEggPalettes.lightCatcher),
            EasterEggItem("🌙", R.string.easter_night_guard_title, flags?.nightGuard == true, EasterEggPalettes.nightGuard),
            EasterEggItem("👆", R.string.easter_trusted_fingerprint_title, flags?.trustedFingerprint == true, EasterEggPalettes.trustedFingerprint),
            EasterEggItem("🔔", R.string.easter_echo_title, flags?.echo == true, EasterEggPalettes.echo),
            EasterEggItem("📚", R.string.easter_archivist_title, flags?.archivist == true, EasterEggPalettes.archivist),
        )
    }
    val bridgeEgg = remember(flags) {
        EasterEggItem("🌉", R.string.easter_bridge_title, flags?.bridge == true, EasterEggPalettes.bridge)
    }
    val websiteEggs = remember(flags) {
        listOf(
            EasterEggItem(
                emoji = "🍓",
                titleRes = R.string.easter_strawberry_title,
                unlocked = flags?.strawberry == true,
                palette = EasterEggPalettes.strawberry,
            ),
            EasterEggItem(
                emoji = "🪞",
                titleRes = R.string.easter_profile_mirror_title,
                unlocked = flags?.profileMirror == true,
                palette = EasterEggPalettes.profileMirror,
            ),
            EasterEggItem(
                emoji = "🌑",
                titleRes = R.string.easter_dark_trigger_title,
                unlocked = flags?.darkTrigger == true,
                palette = EasterEggPalettes.darkTrigger,
            ),
            EasterEggItem(
                emoji = "📬",
                titleRes = R.string.easter_postmaster_title,
                unlocked = flags?.postmaster == true,
                palette = EasterEggPalettes.postmaster,
            ),
        )
    }

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
private fun EasterEggCard(
    item: EasterEggItem,
) {
    val containerColor = if (item.unlocked) item.palette.unlockedContainer else item.palette.lockedContainer
    val contentColor = if (item.unlocked) item.palette.unlockedContent else item.palette.lockedContent

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = item.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(item.titleRes),
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Text(
                text = if (item.unlocked) {
                    stringResource(R.string.easter_unlocked)
                } else {
                    stringResource(R.string.easter_locked)
                },
                color = contentColor.copy(alpha = if (item.unlocked) 0.9f else 0.75f),
            )
        }
    }
}
