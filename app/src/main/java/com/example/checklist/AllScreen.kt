package com.example.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One row of the All tasks screen. The Item is the very same object the list owns, so edits show up in both places. */
private class Entry(val list: TaskList, val item: Item)

/** Every task from every list in one place, grouped by list. */
@Composable
fun AllScreen(lists: List<TaskList>, onBack: () -> Unit) {
    ClearFocusWhenKeyboardHides()
    val accent = Accent(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    val total = lists.sumOf { it.items.size }
    val done = lists.sumOf { it.done }
    val active = lists.map { l -> l to l.items.filter { !it.checked } }.filter { it.second.isNotEmpty() }
    val completed = lists.map { l -> l to l.items.filter { it.checked } }.filter { it.second.isNotEmpty() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(64.dp)) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = accent.color, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.clip(CircleShape).background(accent.color.copy(alpha = 0.14f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("$total ${if (total == 1) "task" else "tasks"}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent.color)
                    }
                    Spacer(Modifier.width(4.dp))
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(64.dp)) {
                            Icon(painterResource(R.drawable.ic_more_vert), "Menu", tint = accent.color, modifier = Modifier.size(30.dp))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (showCompleted) "Hide completed" else "Show completed", fontSize = 19.sp) },
                                leadingIcon = { Icon(painterResource(if (showCompleted) R.drawable.ic_visibility_off else R.drawable.ic_visibility), null) },
                                onClick = { showCompleted = !showCompleted; menu = false },
                                modifier = Modifier.heightIn(min = 64.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("Uncheck all", fontSize = 19.sp) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_undo), null) },
                                enabled = done > 0,
                                onClick = { lists.forEach { l -> l.items.forEach { it.checked = false } }; menu = false },
                                modifier = Modifier.heightIn(min = 64.dp)
                            )
                        }
                    }
                }
                Text("All tasks", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = accent.color, modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    if (total == 0) "No tasks" else "${total - done} to do · $done done",
                    fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 10.dp)
                )
                if (total > 0) LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    color = accent.color, trackColor = accent.color.copy(alpha = 0.18f),
                    gapSize = 0.dp, drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(8.dp).clip(CircleShape)
                )
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (active.isEmpty()) item {
                if (total == 0) EmptyState(R.drawable.ic_edit_note, "No tasks yet", "Add tasks inside a list and they all show up here.")
                else EmptyState(R.drawable.ic_task_alt, "All done!", "Nothing left to do in any list.")
            }
            for ((list, tasks) in active) {
                item(key = "h-${list.id}") { ListHeader(list, Modifier.animateItem()) }
                items(tasks, key = { "${list.id}:${it.id}" }) { item ->
                    TaskRow(item, list, accentFor(list.color), false, {}, {}, Modifier.animateItem())
                }
            }
            if (completed.isNotEmpty()) {
                item(key = "completed-header") {
                    Row(
                        Modifier.fillMaxWidth().animateItem().clip(RoundedCornerShape(16.dp))
                            .clickable { showCompleted = !showCompleted }
                            .heightIn(min = 72.dp).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(if (showCompleted) R.drawable.ic_keyboard_arrow_down else R.drawable.ic_chevron_right),
                            null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Completed", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 2.dp)) {
                            Text("$done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (showCompleted) for ((list, tasks) in completed) {
                    item(key = "ch-${list.id}") { ListHeader(list, Modifier.animateItem()) }
                    items(tasks, key = { "${list.id}:${it.id}" }) { item ->
                        TaskRow(item, list, accentFor(list.color), false, {}, {}, Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(list: TaskList, modifier: Modifier) {
    val accent = accentFor(list.color)
    Row(modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(accent.color))
        Spacer(Modifier.width(10.dp))
        Text(list.name.ifBlank { "Untitled" }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent.color, maxLines = 1)
    }
}
