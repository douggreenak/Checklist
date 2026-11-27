package com.example.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var scrollToId by remember { mutableStateOf<Long?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var lastSwapOffset by remember { mutableIntStateOf(Int.MIN_VALUE) }

    fun addAfter(item: Item?) {
        val new = Item()
        val at = if (item == null) list.items.size else list.items.indexOf(item) + 1
        list.items.add(at, new)
        focusId = new.id
        scrollToId = new.id
    }

    // Bring a freshly added task into view.
    LaunchedEffect(scrollToId) {
        val id = scrollToId ?: return@LaunchedEffect
        withFrameNanos { }
        val index = active.indexOfFirst { it.id == id }
        val info = listState.layoutInfo
        val shown = info.visibleItemsInfo.firstOrNull { it.key == id }
        val fullyVisible = shown != null && shown.offset >= info.viewportStartOffset && shown.offset + shown.size <= info.viewportEndOffset
        if (index >= 0 && !fullyVisible) listState.animateScrollToItem(index)
        scrollToId = null
    }

    fun drag(id: Long, dy: Float) {
        dragDelta += dy
        val info = listState.layoutInfo
        val me = info.visibleItemsInfo.firstOrNull { it.key == id } ?: return
        if (me.offset != lastSwapOffset) {
            val center = me.offset + me.size / 2f + dragDelta
            val activeIds = active.map { it.id }.toSet()
            val target = info.visibleItemsInfo.firstOrNull { it.key != id && it.key in activeIds && center >= it.offset && center <= it.offset + it.size }
            if (target != null) {
                val i = list.items.indexOfFirst { it.id == id }
                val j = list.items.indexOfFirst { it.id == target.key }
                if (i >= 0 && j >= 0) {
                    val a = list.items[i]; list.items[i] = list.items[j]; list.items[j] = a
                    val newOffset = if (target.offset < me.offset) target.offset else target.offset + target.size - me.size
                    dragDelta += me.offset - newOffset
                    lastSwapOffset = me.offset
                }
            }
        }
        // Scroll when held near the top or bottom edge.
        val top = me.offset + dragDelta; val bottom = top + me.size
        val step = when {
            top < info.viewportStartOffset + 40 -> -28f
            bottom > info.viewportEndOffset - 40 -> 28f
            else -> 0f
        }
        if (step != 0f) scope.launch { dragDelta += listState.scrollBy(step) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ListTopBar(list, accent, onBack, onDeleteList) { picker = true } },
        bottomBar = {
            Button(
                onClick = { addAfter(active.lastOrNull()) },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.on),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).height(76.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add task", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) { pad ->
        if (picker) ColorPickerDialog("List color", list.color, { list.color = it }) { picker = false }
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (active.isEmpty()) item {
                if (completed.isEmpty()) EmptyState(R.drawable.ic_edit_note, "No tasks yet", "Tap “Add task”, type, and press Enter for the next one.")
                else EmptyState(R.drawable.ic_task_alt, "All done!", "Nothing left to do on this list.")
            }
            items(active, key = { it.id }) { item ->
                val dragging = draggingId == item.id
                TaskRow(
                    item, list, accent, focusId == item.id, { focusId = null }, { addAfter(item) },
                    if (dragging) Modifier.zIndex(1f).graphicsLayer { translationY = dragDelta } else Modifier.animateItem(),
                    reorder = Reorder(
                        dragging,
                        onStart = { draggingId = item.id; dragDelta = 0f; lastSwapOffset = Int.MIN_VALUE },
                        onMove = { drag(item.id, it) },
                        onEnd = { draggingId = null; dragDelta = 0f }
                    )
                )
            }
            if (completed.isNotEmpty()) {
                item(key = "completed-header") {
                    Row(
                        Modifier.fillMaxWidth().animateItem().clip(RoundedCornerShape(16.dp))
                            .clickable { list.showCompleted = !list.showCompleted }
                            .heightIn(min = 72.dp).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(if (list.showCompleted) R.drawable.ic_keyboard_arrow_down else R.drawable.ic_chevron_right),
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
            IconButton(onClick = onBack, modifier = Modifier.size(64.dp)) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = accent.color, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(CircleShape).background(accent.color.copy(alpha = 0.14f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "${list.items.size} ${if (list.items.size == 1) "task" else "tasks"}",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent.color
                )
            }
            Spacer(Modifier.width(4.dp))
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(64.dp)) {
                    Icon(painterResource(R.drawable.ic_more_vert), "Menu", tint = accent.color, modifier = Modifier.size(30.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (list.showCompleted) "Hide completed" else "Show completed", fontSize = 19.sp) },
                        leadingIcon = { Icon(painterResource(if (list.showCompleted) R.drawable.ic_visibility_off else R.drawable.ic_visibility), null) },
                        onClick = { list.showCompleted = !list.showCompleted; menu = false },
                        modifier = Modifier.heightIn(min = 64.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Uncheck all", fontSize = 19.sp) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_undo), null) },
                        enabled = list.done > 0,
                        onClick = { list.items.forEach { it.checked = false }; menu = false },
                        modifier = Modifier.heightIn(min = 64.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Change color", fontSize = 19.sp) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_palette), null, tint = accent.color) },
                        onClick = { menu = false; onChangeColor() },
                        modifier = Modifier.heightIn(min = 64.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Delete list", fontSize = 19.sp, color = DeleteRed) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_delete), null, tint = DeleteRed) },
                        onClick = { menu = false; onDeleteList() },
                        modifier = Modifier.heightIn(min = 64.dp)
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
internal fun TaskRow(
    item: Item, list: TaskList, accent: Accent,
    requestFocus: Boolean, onFocused: () -> Unit, onEnter: () -> Unit, modifier: Modifier,
    reorder: Reorder? = null
) {
    SwipeRow(
        rightLabel = if (item.checked) "Not done" else "Done",
        rightIcon = if (item.checked) R.drawable.ic_undo else R.drawable.ic_check,
        onSwipeRight = { item.checked = !item.checked },
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 100.dp).height(IntrinsicSize.Min).padding(start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckCircle(item.checked, accent) { item.checked = !item.checked }
                TaskField(item, requestFocus, onFocused, onEnter, Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 10.dp))
                if (reorder != null) DragHandle(reorder)
                IconButton(
                    onClick = { list.items.remove(item) },
                    modifier = Modifier.size(68.dp).clip(CircleShape).background(DeleteRed.copy(alpha = 0.12f))
                ) { Icon(painterResource(R.drawable.ic_close), "Delete task", tint = DeleteRed, modifier = Modifier.size(32.dp)) }
            }
        }
    }
}

class Reorder(val dragging: Boolean, val onStart: () -> Unit, val onMove: (Float) -> Unit, val onEnd: () -> Unit)

/** Grab and drag to move a task up or down. */
@Composable
private fun DragHandle(reorder: Reorder) {
    val r by rememberUpdatedState(reorder)
    Box(
        Modifier.width(52.dp).fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { r.onStart() },
                    onDragEnd = { r.onEnd() },
                    onDragCancel = { r.onEnd() },
                    onDrag = { change, amount -> change.consume(); r.onMove(amount.y) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_drag_handle), "Drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun CheckCircle(checked: Boolean, accent: Accent, onClick: () -> Unit) {
    Box(
        Modifier.width(104.dp).fillMaxHeight().clip(RoundedCornerShape(28.dp)).clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(54.dp).clip(CircleShape)
                .background(if (checked) accent.color else Color.Transparent)
                .border(3.5.dp, accent.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(painterResource(R.drawable.ic_check), "Done", tint = accent.on, modifier = Modifier.size(38.dp))
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
