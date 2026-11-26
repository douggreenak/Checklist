package com.example.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    rightIcon: ImageVector = Icons.Default.Check,
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
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        enableDismissFromStartToEnd = rightLabel != null,
        backgroundContent = {
            val toRight = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val toLeft = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Row(
                Modifier.fillMaxSize()
                    .background(if (toRight) DoneGreen else if (toLeft) DeleteRed else Color.Transparent)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (toRight) Arrangement.Start else Arrangement.End
            ) {
                if (toRight) {
                    Icon(rightIcon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(rightLabel ?: "", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                if (toLeft) {
                    Text("Delete", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        },
        content = { content() }
    )
}

val UndoIcon = Icons.Default.Refresh
