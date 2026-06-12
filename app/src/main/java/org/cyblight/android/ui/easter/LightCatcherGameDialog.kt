package org.cyblight.android.ui.easter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.cyblight.android.R
import kotlin.math.roundToInt
import kotlin.random.Random

private const val TARGET_SCORE = 7
private const val GAME_SECONDS = 18
private const val ORB_LIFETIME_MS = 1400L

private data class LightOrb(
    val id: Int,
    val xRatio: Float,
    val yRatio: Float,
)

@Composable
fun LightCatcherGameDialog(
    isUnlocking: Boolean,
    onDismiss: () -> Unit,
    onWin: () -> Unit,
) {
    var score by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(GAME_SECONDS) }
    var activeOrb by remember { mutableStateOf<LightOrb?>(null) }
    var orbAlpha by remember { mutableFloatStateOf(1f) }
    var phase by remember { mutableStateOf(GamePhase.Playing) }
    var orbId by remember { mutableIntStateOf(0) }

    val animatedAlpha by animateFloatAsState(
        targetValue = orbAlpha,
        animationSpec = tween(220),
        label = "orbAlpha",
    )

    LaunchedEffect(phase) {
        if (phase != GamePhase.Playing) return@LaunchedEffect

        while (secondsLeft > 0 && score < TARGET_SCORE) {
            delay(1000)
            secondsLeft--
        }

        phase = when {
            score >= TARGET_SCORE -> GamePhase.Won
            else -> GamePhase.Lost
        }
    }

    LaunchedEffect(phase) {
        if (phase != GamePhase.Playing) {
            activeOrb = null
            return@LaunchedEffect
        }

        while (score < TARGET_SCORE && secondsLeft > 0) {
            orbId++
            activeOrb = LightOrb(
                id = orbId,
                xRatio = Random.nextFloat().coerceIn(0.12f, 0.78f),
                yRatio = Random.nextFloat().coerceIn(0.18f, 0.72f),
            )
            orbAlpha = 1f
            delay(ORB_LIFETIME_MS)
            orbAlpha = 0f
            delay(180)
            if (activeOrb?.id == orbId) {
                activeOrb = null
            }
            delay(Random.nextLong(250, 500))
        }
    }

    LaunchedEffect(phase) {
        if (phase == GamePhase.Won) {
            onWin()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (isUnlocking || phase == GamePhase.Playing) return@AlertDialog
            onDismiss()
        },
        title = {
            Text(
                text = stringResource(R.string.light_catcher_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (phase) {
                    GamePhase.Playing -> {
                        Text(
                            text = stringResource(R.string.light_catcher_rules),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.light_catcher_progress,
                                score,
                                TARGET_SCORE,
                                secondsLeft,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        LinearProgressIndicator(
                            progress = { score / TARGET_SCORE.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(height = 220.dp, width = 280.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        ) {
                            val orb = activeOrb
                            if (orb != null) {
                                val orbSize = 56.dp
                                val maxX = (maxWidth - orbSize).value
                                val maxY = (maxHeight - orbSize).value
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (orb.xRatio * maxX).roundToInt(),
                                                (orb.yRatio * maxY).roundToInt(),
                                            )
                                        }
                                        .size(orbSize)
                                        .alpha(animatedAlpha)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary,
                                                ),
                                            ),
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            if (activeOrb?.id == orb.id) {
                                                score++
                                                activeOrb = null
                                                orbAlpha = 0f
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "💡",
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                }
                            }
                        }
                    }
                    GamePhase.Won -> {
                        if (isUnlocking) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(R.string.light_catcher_unlocking),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.light_catcher_won),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    GamePhase.Lost -> {
                        Text(
                            text = stringResource(R.string.light_catcher_lost),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (phase) {
                GamePhase.Playing -> {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isUnlocking,
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
                GamePhase.Lost -> {
                    TextButton(
                        onClick = {
                            score = 0
                            secondsLeft = GAME_SECONDS
                            activeOrb = null
                            orbAlpha = 1f
                            phase = GamePhase.Playing
                        },
                        enabled = !isUnlocking,
                    ) {
                        Text(stringResource(R.string.light_catcher_retry))
                    }
                }
                GamePhase.Won -> {
                    if (!isUnlocking) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (phase == GamePhase.Lost) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isUnlocking,
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        },
    )
}

private enum class GamePhase {
    Playing,
    Won,
    Lost,
}
