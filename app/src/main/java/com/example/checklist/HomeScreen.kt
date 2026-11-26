package com.example.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    lists: List<TaskList>, subtitle: String,
    onOpen: (TaskList) -> Unit, onAdd: () -> Unit, onDelete: (TaskList) -> Unit,
    onSettings: () -> Unit = {},
    update: UpdateInfo? = null,
    onUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {}
) {
    val todo = lists.sumOf { it.remaining }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.statusBarsPadding().padding(start = 24.dp, end = 8.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("My Lists", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("$subtitle · $todo to do", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSettings, modifier = Modifier.size(68.dp)) {
                    Icon(painterResource(R.drawable.ic_settings), "Settings", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
        },
        bottomBar = {
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(76.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(8.dp))
                Text("New list", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (update != null) item(key = "update") {
                Surface(
                    onClick = onUpdate, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(start = 20.dp, top = 6.dp, bottom = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_download), null, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text("Update available", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Version ${update.version} · tap to download", fontSize = 16.sp)
                        }
                        IconButton(onClick = onDismissUpdate, modifier = Modifier.size(64.dp)) {
                            Icon(painterResource(R.drawable.ic_close), "Dismiss", modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
            if (lists.isEmpty()) item { EmptyState(R.drawable.ic_checklist, "No lists yet", "Tap “New list” to make your first one.") }
            items(lists, key = { it.id }) { list ->
                SwipeRow(rightLabel = null, onSwipeRight = {}, onSwipeLeft = { onDelete(list) }, modifier = Modifier.animateItem()) {
                    ListCard(list) { onOpen(list) }
                }
            }
        }
    }
}

@Composable
private fun ListCard(list: TaskList, onClick: () -> Unit) {
    val accent = accentFor(list.color)
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(accent.color), contentAlignment = Alignment.Center) {
                    Text(list.name.trim().firstOrNull()?.uppercase() ?: "•", color = accent.on, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(list.name.ifBlank { "Untitled" }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    Text(
                        when {
                            list.items.isEmpty() -> "Empty"
                            list.remaining == 0 -> "All done"
                            else -> "${list.remaining} to do" + if (list.done > 0) " · ${list.done} done" else ""
                        },
                        fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (list.remaining > 0) Text("${list.remaining}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = accent.color)
                else if (list.items.isNotEmpty()) Icon(painterResource(R.drawable.ic_check), "All done", tint = accent.color, modifier = Modifier.size(32.dp))
            }
            if (list.items.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { list.done.toFloat() / list.items.size },
                    color = accent.color, trackColor = accent.color.copy(alpha = 0.18f),
                    gapSize = 0.dp, drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun EmptyState(@androidx.annotation.DrawableRes icon: Int, title: String, hint: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Text(hint, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
