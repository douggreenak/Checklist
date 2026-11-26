package com.example.checklist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("checklist", Context.MODE_PRIVATE)
        val lists = mutableStateListOf<TaskList>().apply { addAll(Store.load(prefs)) }
        val settings = Store.loadSettings(prefs)
        setContent {
            LaunchedEffect(Unit) { snapshotFlow { Store.toJson(lists) }.collect { Store.save(prefs, it) } }
            LaunchedEffect(Unit) { snapshotFlow { Store.settingsToJson(settings) }.collect { Store.saveSettings(prefs, it) } }
            var openId by rememberSaveable { mutableStateOf<String?>(null) }
            var inSettings by rememberSaveable { mutableStateOf(false) }
            val open = lists.firstOrNull { it.id == openId }
            BackHandler(enabled = open != null || inSettings) {
                if (inSettings) inSettings = false else openId = null
            }
            val dark = when (settings.mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            ChecklistTheme(dark, settings.themeColor, settings.pureBlack) {
                when {
                    inSettings -> SettingsScreen(settings) { inSettings = false }
                    open != null -> ListScreen(open, onBack = { openId = null }, onDeleteList = { lists.remove(open); openId = null })
                    else -> HomeScreen(
                        lists,
                        subtitle = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                        onOpen = { openId = it.id },
                        onAdd = { val l = TaskList(color = Palette[lists.size % Palette.size]); lists.add(l); openId = l.id },
                        onDelete = { lists.remove(it) },
                        onSettings = { inSettings = true }
                    )
                }
            }
        }
    }
}
