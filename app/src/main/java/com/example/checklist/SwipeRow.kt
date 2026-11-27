package com.example.checklist

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** The row must be dragged this fraction of its width before a swipe counts. Letting go earlier snaps back. */
const val SWIPE_FRACTION = 1f / 3f

/**
 * Swipe right runs [onSwipeRight] and the row snaps back (the item usually moves elsewhere). There is no swipe-to-delete.
 * It only fires when the finger is released at least a third of the way across, however fast the flick.
 */
@Composable
fun SwipeRow(
    rightLabel: String?,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    @androidx.annotation.DrawableRes rightIcon: Int = R.drawable.ic_check,
    content: @Composable () -> Unit
) {
    val right by rememberUpdatedState(onSwipeRight)
    val offset = remember { Animatable(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    val canRight = rightLabel != null
    val threshold = width * SWIPE_FRACTION
    val x = offset.value
    val armed = abs(x) >= threshold

    Box(
        modifier.clip(RoundedCornerShape(20.dp)).onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(canRight) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val end = offset.value
                        scope.launch {
                            if (end >= width * SWIPE_FRACTION && canRight) {
                                right()
                                offset.animateTo(0f)
                            } else offset.animateTo(0f)
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        val next = offset.value + delta
                        scope.launch { offset.snapTo(if (canRight) next.coerceAtLeast(0f) else 0f) }
                    }
                )
            }
    ) {
        if (x != 0f) {
            Row(
                Modifier.matchParentSize()
                    .background(DoneGreen.copy(alpha = if (armed) 1f else 0.55f))
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(painterResource(rightIcon), null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(rightLabel ?: "", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }) { content() }
    }
}
