package org.cyblight.android.ui.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.cyblight.android.data.preferences.SwipeBackEdgeWidth
import org.cyblight.android.data.preferences.SwipeBackSensitivity

@Composable
fun SwipeBackContainer(
    enabled: Boolean,
    edgeWidth: SwipeBackEdgeWidth,
    sensitivity: SwipeBackSensitivity,
    onSwipeBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { edgeWidth.widthDp.dp.toPx() }
    val thresholdFraction = sensitivity.fraction

    Box(
        modifier = modifier.pointerInput(enabled, edgeWidthPx, thresholdFraction) {
            if (!enabled) return@pointerInput
            var totalDrag = 0f
            var startedFromEdge = false
            var triggered = false

            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    startedFromEdge = offset.x <= edgeWidthPx
                    totalDrag = 0f
                    triggered = false
                },
                onDragEnd = {
                    startedFromEdge = false
                    totalDrag = 0f
                    triggered = false
                },
                onDragCancel = {
                    startedFromEdge = false
                    totalDrag = 0f
                    triggered = false
                },
                onHorizontalDrag = { _, dragAmount ->
                    if (!startedFromEdge || triggered) return@detectHorizontalDragGestures
                    if (dragAmount < 0f) {
                        startedFromEdge = false
                        totalDrag = 0f
                        return@detectHorizontalDragGestures
                    }
                    totalDrag += dragAmount
                    if (totalDrag >= size.width * thresholdFraction) {
                        triggered = true
                        onSwipeBack()
                    }
                },
            )
        },
    ) {
        content()
    }
}
