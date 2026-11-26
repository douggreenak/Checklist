package com.example.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ListScreen(list: TaskList, onBack: () -> Unit, onDeleteList: () -> Unit) {
    var focusId by remember { mutableStateOf<Long?>(null) }
    val active = list.items.filter { !it.checked }
    val completed = list.items.filter { it.checked }

    fun addAfter(item: Item?) {
        val new = Item()
        val at = if (item == null) list.items.size else list.items.indexOf(item) + 1
        list.items.add(at, new)
        focusId = new.id
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ListTopBar(list, onBack, onDeleteList) },
        bottomBar = {
            Button(
                onClick = { addAfter(active.lastOrNull()) },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp).height(72.dp)
            ) { Text("+ Add task", fontSize = 24.sp) }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (active.isEmpty()) item {
                Text(
                    if (completed.isEmpty()) "No tasks yet. Tap “+ Add task”." else "All done! 🎉",
                    fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(active, key = { it.id }) { item ->
                TaskRow(
                    item, requestFocus = focusId == item.id, onFocused = { focusId = null },
                    onEnter = { addAfter(item) }, modifier = Modifier.animateItem(),
                    list = list
                )
            }
            if (completed.isNotEmpty()) {
                item(key = "completed-header") {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).animateItem()
                            .clickable { list.showCompleted = !list.showCompleted }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (if (list.showCompleted) "▾" else "▸") + "  Completed (${completed.size})",
                            fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (list.showCompleted) items(completed, key = { it.id }) { item ->
                    TaskRow(item, false, {}, {}, Modifier.animateItem(), list)
                }
            }
        }
    }
}

@Composable
private fun ListTopBar(list: TaskList, onBack: () -> Unit, onDeleteList: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
            Text("←", fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        BasicTextField(
            value = list.name,
            onValueChange = { list.name = it.replace("\n", "") },
            singleLine = true,
            textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(56.dp)) {
                Text("⋮", fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (list.showCompleted) "Hide completed" else "Show completed", fontSize = 20.sp) },
                    onClick = { list.showCompleted = !list.showCompleted; menu = false },
                    modifier = Modifier.heightIn(min = 56.dp)
                )
                DropdownMenuItem(
                    text = { Text("Delete list", fontSize = 20.sp, color = DeleteRed) },
                    onClick = { menu = false; onDeleteList() },
                    modifier = Modifier.heightIn(min = 56.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    item: Item, requestFocus: Boolean, onFocused: () -> Unit, onEnter: () -> Unit,
    modifier: Modifier, list: TaskList
) {
    SwipeRow(
        rightLabel = if (item.checked) "↩ Not done" else "✓ Done",
        onSwipeRight = { item.checked = !item.checked },
        onSwipeLeft = { list.items.remove(item) },
        modifier = modifier
    ) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CheckButton(item.checked) { item.checked = !item.checked }
            TaskField(item, requestFocus, onFocused, onEnter, Modifier.weight(1f))
            Button(
                onClick = { list.items.remove(item) },
                modifier = Modifier.size(64.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeleteRed, contentColor = Color.White)
            ) { Text("✕", fontSize = 28.sp) }
        }
    }
}

@Composable
private fun CheckButton(checked: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
        colors = if (checked) ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) else ButtonDefaults.outlinedButtonColors()
    ) { Text(if (checked) "✓" else "", fontSize = 36.sp) }
}

@Composable
private fun TaskField(item: Item, requestFocus: Boolean, onFocused: () -> Unit, onEnter: () -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) { focus.requestFocus(); onFocused() }
    }
    val fg = MaterialTheme.colorScheme.onBackground
    OutlinedTextField(
        value = item.text,
        onValueChange = { item.text = it.replace("\n", "") },
        modifier = modifier.heightIn(min = 64.dp).focusRequester(focus),
        textStyle = TextStyle(
            fontSize = 22.sp,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            color = if (item.checked) fg.copy(alpha = 0.5f) else fg
        ),
        placeholder = { Text("Type task…", fontSize = 22.sp) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onEnter() })
    )
}
