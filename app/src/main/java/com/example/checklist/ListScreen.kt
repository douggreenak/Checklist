package com.example.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ListScreen(list: TaskList, onBack: () -> Unit, onDeleteList: () -> Unit) {
    ClearFocusWhenKeyboardHides()
    var focusId by remember { mutableStateOf<Long?>(null) }
    val active = list.items.filter { !it.checked }
    val completed = list.items.filter { it.checked }
    val accent = accentFor(list.color)
    var picker by remember { mutableStateOf(false) }

    fun addAfter(item: Item?) {
        val new = Item()
        val at = if (item == null) list.items.size else list.items.indexOf(item) + 1
        list.items.add(at, new)
        focusId = new.id
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ListTopBar(list, accent, onBack, onDeleteList) { picker = true } },
        bottomBar = {
            Button(
                onClick = { addAfter(active.lastOrNull()) },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.on),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).height(68.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add task", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) { pad ->
        if (picker) ColorPickerDialog("List color", list.color, { list.color = it }) { picker = false }
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (active.isEmpty()) item {
                if (completed.isEmpty()) EmptyState("📝", "No tasks yet", "Tap “Add task”, type, and press Enter for the next one.")
                else EmptyState("🎉", "All done!", "Nothing left to do on this list.")
            }
            items(active, key = { it.id }) { item ->
                TaskRow(item, list, accent, focusId == item.id, { focusId = null }, { addAfter(item) }, Modifier.animateItem())
            }
            if (completed.isNotEmpty()) {
                item(key = "completed-header") {
                    Row(
                        Modifier.fillMaxWidth().animateItem().clip(RoundedCornerShape(16.dp))
                            .clickable { list.showCompleted = !list.showCompleted }
                            .heightIn(min = 60.dp).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (list.showCompleted) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Completed", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 2.dp)) {
                            Text("${completed.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (list.showCompleted) items(completed, key = { it.id }) { item ->
                    TaskRow(item, list, accent, false, {}, {}, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun ListTopBar(list: TaskList, accent: Accent, onBack: () -> Unit, onDeleteList: () -> Unit, onChangeColor: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = accent.color, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.MoreVert, "Menu", tint = accent.color, modifier = Modifier.size(30.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (list.showCompleted) "Hide completed" else "Show completed", fontSize = 19.sp) },
                        leadingIcon = { Icon(Icons.Default.Check, null) },
                        onClick = { list.showCompleted = !list.showCompleted; menu = false },
                        modifier = Modifier.heightIn(min = 56.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Change color", fontSize = 19.sp) },
                        leadingIcon = { Box(Modifier.size(22.dp).clip(CircleShape).background(accent.color)) },
                        onClick = { menu = false; onChangeColor() },
                        modifier = Modifier.heightIn(min = 56.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Delete list", fontSize = 19.sp, color = DeleteRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = DeleteRed) },
                        onClick = { menu = false; onDeleteList() },
                        modifier = Modifier.heightIn(min = 56.dp)
                    )
                }
            }
        }
        BasicTextField(
            value = list.name,
            onValueChange = { list.name = it.replace("\n", "") },
            singleLine = true,
            textStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, color = accent.color),
            cursorBrush = SolidColor(accent.color),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Text(
            if (list.items.isEmpty()) "No tasks" else "${list.remaining} to do · ${list.done} done",
            fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 10.dp)
        )
        if (list.items.isNotEmpty()) LinearProgressIndicator(
            progress = { list.done.toFloat() / list.items.size },
            color = accent.color, trackColor = accent.color.copy(alpha = 0.18f),
            gapSize = 0.dp, drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(8.dp).clip(CircleShape)
        )
    }
}

@Composable
private fun TaskRow(
    item: Item, list: TaskList, accent: Accent,
    requestFocus: Boolean, onFocused: () -> Unit, onEnter: () -> Unit, modifier: Modifier
) {
    SwipeRow(
        rightLabel = if (item.checked) "Not done" else "Done",
        rightIcon = if (item.checked) UndoIcon else Icons.Default.Check,
        onSwipeRight = { item.checked = !item.checked },
        onSwipeLeft = { list.items.remove(item) },
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckCircle(item.checked, accent) { item.checked = !item.checked }
                TaskField(item, requestFocus, onFocused, onEnter, Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 10.dp))
                IconButton(
                    onClick = { list.items.remove(item) },
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(DeleteRed.copy(alpha = 0.12f))
                ) { Icon(Icons.Default.Close, "Delete task", tint = DeleteRed, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}

@Composable
private fun CheckCircle(checked: Boolean, accent: Accent, onClick: () -> Unit) {
    Box(
        Modifier.size(60.dp).clip(CircleShape).clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(if (checked) accent.color else Color.Transparent)
                .border(2.5.dp, accent.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Default.Check, "Done", tint = accent.on, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun TaskField(item: Item, requestFocus: Boolean, onFocused: () -> Unit, onEnter: () -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) { focus.requestFocus(); onFocused() }
    }
    val fg = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = item.text,
        onValueChange = { v ->
            if ('\n' in v) { item.text = v.replace("\n", ""); onEnter() } else item.text = v
        },
        modifier = modifier.focusRequester(focus),
        textStyle = TextStyle(
            fontSize = 21.sp,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            color = if (item.checked) fg.copy(alpha = 0.5f) else fg
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        maxLines = 4,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Sentences),
        keyboardActions = KeyboardActions(onNext = { onEnter() }),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (item.text.isEmpty()) Text("Type task…", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        }
    )
}
