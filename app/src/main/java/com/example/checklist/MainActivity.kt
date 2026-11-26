package com.example.checklist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

class Item(text: String = "", checked: Boolean = false, val id: Long = nextId++) {
    var text by mutableStateOf(text)
    var checked by mutableStateOf(checked)
    companion object { var nextId = 0L }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("checklist", Context.MODE_PRIVATE)
        val items = mutableStateListOf<Item>()
        runCatching {
            val arr = JSONArray(prefs.getString("items", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(Item(o.getString("t"), o.getBoolean("c")))
            }
        }
        setContent {
            // Save whenever anything changes.
            LaunchedEffect(Unit) {
                snapshotFlow {
                    JSONArray().also { a ->
                        items.forEach { a.put(JSONObject().put("t", it.text).put("c", it.checked)) }
                    }.toString()
                }.collect { prefs.edit().putString("items", it).apply() }
            }
            MaterialTheme { ChecklistScreen(items) }
        }
    }
}

@Composable
fun ChecklistScreen(items: SnapshotStateList<Item>) {
    var focusId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    fun addAfter(index: Int) {
        val item = Item()
        items.add(index + 1, item)
        focusId = item.id
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Button(
                onClick = { addAfter(items.lastIndex) },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                    .padding(12.dp).height(72.dp)
            ) { Text("+ Add item", fontSize = 24.sp) }
        }
    ) { pad ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(pad).statusBarsPadding().fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                ItemRow(
                    item = item,
                    requestFocus = focusId == item.id,
                    onFocused = { focusId = null },
                    onEnter = { addAfter(items.indexOf(item)) },
                    onDelete = { items.remove(item) }
                )
            }
        }
    }
}

@Composable
fun ItemRow(item: Item, requestFocus: Boolean, onFocused: () -> Unit, onEnter: () -> Unit, onDelete: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) { focus.requestFocus(); onFocused() }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { item.checked = !item.checked },
            modifier = Modifier.size(64.dp),
            contentPadding = PaddingValues(0.dp),
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
            colors = if (item.checked) ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) else ButtonDefaults.outlinedButtonColors()
        ) { Text(if (item.checked) "✓" else "", fontSize = 36.sp) }

        OutlinedTextField(
            value = item.text,
            onValueChange = { item.text = it.replace("\n", "") },
            modifier = Modifier.weight(1f).heightIn(min = 64.dp).focusRequester(focus),
            textStyle = TextStyle(
                fontSize = 22.sp,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) Color.Gray else Color.Black
            ),
            placeholder = { Text("Type item…", fontSize = 22.sp) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onEnter() })
        )

        Button(
            onClick = onDelete,
            modifier = Modifier.size(64.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) { Text("✕", fontSize = 28.sp) }
    }
}
