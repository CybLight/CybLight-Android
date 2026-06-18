package org.cyblight.android.ui.messages

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

private val SpoilerBackground = Color(0xF02A2206)
private val SpoilerDotBright = Color(0xFAFFDC46)
private val SpoilerDotMid = Color(0xD9FFEC82)
private val SpoilerDotWarm = Color(0xBFFFB624)

@Composable
fun SpoilerText(
    text: String,
    revealed: Boolean,
    onReveal: () -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    if (revealed) {
        Text(
            text = text,
            style = textStyle,
            modifier = modifier,
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "spoiler-drift")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(animation = tween(750, easing = LinearEasing)),
        label = "spoiler-shift",
    )

    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .clickable(onClick = onReveal)
            .drawBehind {
                drawSpoilerPattern(shift)
            },
    ) {
        Text(
            text = text,
            style = textStyle.copy(color = Color.Transparent),
        )
    }
}

private fun DrawScope.drawSpoilerPattern(shift: Float) {
    drawRect(SpoilerBackground)
    val step = 3.dp.toPx()
    var row = 0
    var y = shift % step
    while (y < size.height + step) {
        var col = 0
        var x = ((shift + row * 2f) % step)
        while (x < size.width + step) {
            val color = when ((row + col) % 3) {
                0 -> SpoilerDotBright
                1 -> SpoilerDotMid
                else -> SpoilerDotWarm
            }
            drawCircle(
                color = color,
                radius = 0.85.dp.toPx(),
                center = Offset(x, y),
            )
            x += step
            col++
        }
        y += step
        row++
    }
}
