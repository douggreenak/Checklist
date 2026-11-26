package com.example.checklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(lists: List<TaskList>, onOpen: (TaskList) -> Unit, onAdd: () -> Unit, onDelete: (TaskList) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Text(
                "My Lists", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 20.dp, bottom = 8.dp, end = 20.dp)
            )
        },
        bottomBar = {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).height(72.dp)
            ) { Text("+ New list", fontSize = 24.sp) }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (lists.isEmpty()) item {
                Text("No lists yet. Tap “+ New list”.", fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
            items(lists, key = { it.id }) { list ->
                SwipeRow(rightLabel = null, onSwipeRight = {}, onSwipeLeft = { onDelete(list) }) {
                    Surface(
                        onClick = { onOpen(list) },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.Center) {
                            Text(list.name.ifBlank { "Untitled" }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${list.remaining} to do" + if (list.done > 0) " · ${list.done} done" else "",
                                fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
