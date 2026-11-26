package com.example.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Swipe right runs [onSwipeRight] and the row snaps back (the item usually moves elsewhere).
 * Swipe left runs [onSwipeLeft] (delete) immediately, with no confirmation.
 */
@Composable
fun SwipeRow(
    rightLabel: String?,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val right by rememberUpdatedState(onSwipeRight)
    val left by rememberUpdatedState(onSwipeLeft)
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        when (it) {
            SwipeToDismissBoxValue.StartToEnd -> { right(); false }
            SwipeToDismissBoxValue.EndToStart -> { left(); true }
            else -> false
        }
    })
    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        enableDismissFromStartToEnd = rightLabel != null,
        backgroundContent = {
            val toRight = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val toLeft = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                Modifier.fillMaxSize().background(
                    when {
                        toRight -> DoneGreen
                        toLeft -> DeleteRed
                        else -> Color.Transparent
                    }
                ).padding(horizontal = 24.dp),
                contentAlignment = if (toRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (toRight) Text(rightLabel ?: "", color = Color.White, fontSize = 22.sp)
                if (toLeft) Text("Delete", color = Color.White, fontSize = 22.sp)
            }
        },
        content = { content() }
    )
}
