package com.example.checklist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("checklist", Context.MODE_PRIVATE)
        val lists = mutableStateListOf<TaskList>().apply { addAll(Store.load(prefs)) }
        setContent {
            LaunchedEffect(Unit) {
                snapshotFlow { Store.toJson(lists) }.collect { Store.save(prefs, it) }
            }
            var openId by rememberSaveable { mutableStateOf<String?>(null) }
            val open = lists.firstOrNull { it.id == openId }
            BackHandler(enabled = open != null) { openId = null }
            ChecklistTheme {
                if (open == null) {
                    HomeScreen(
                        lists,
                        onOpen = { openId = it.id },
                        onAdd = { val l = TaskList(); lists.add(l); openId = l.id },
                        onDelete = { lists.remove(it) }
                    )
                } else {
                    ListScreen(open, onBack = { openId = null }, onDeleteList = { lists.remove(open); openId = null })
                }
            }
        }
    }
}
