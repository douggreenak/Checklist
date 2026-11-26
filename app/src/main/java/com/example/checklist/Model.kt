package com.example.checklist

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class Item(text: String = "", checked: Boolean = false, val id: Long = nextId++) {
    var text by mutableStateOf(text)
    var checked by mutableStateOf(checked)

    companion object { private var nextId = 0L }
}

class TaskList(
    name: String = "New list",
    items: List<Item> = emptyList(),
    showCompleted: Boolean = false,
    color: Int = Palette[0],
    val id: String = UUID.randomUUID().toString()
) {
    var name by mutableStateOf(name)
    var showCompleted by mutableStateOf(showCompleted)
    var color by mutableStateOf(color)
    val items = mutableStateListOf<Item>().apply { addAll(items) }

    val remaining get() = items.count { !it.checked }
    val done get() = items.count { it.checked }
}

class Settings(mode: Int = 0, themeColor: Int = Palette[0], pureBlack: Boolean = false) {
    /** 0 = follow system, 1 = light, 2 = dark */
    var mode by mutableStateOf(mode)
    var themeColor by mutableStateOf(themeColor)
    var pureBlack by mutableStateOf(pureBlack)

    fun reset() { mode = 0; themeColor = Palette[0]; pureBlack = false }
}

object Store {
    private const val KEY = "lists"

    fun toJson(lists: List<TaskList>): String = JSONArray().also { arr ->
        lists.forEach { l ->
            arr.put(JSONObject()
                .put("id", l.id).put("name", l.name).put("showCompleted", l.showCompleted).put("argb", l.color)
                .put("items", JSONArray().also { ia ->
                    l.items.forEach { ia.put(JSONObject().put("t", it.text).put("c", it.checked)) }
                }))
        }
    }.toString()

    fun parse(json: String): List<TaskList> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ia = o.getJSONArray("items")
            TaskList(
                name = o.getString("name"),
                showCompleted = o.optBoolean("showCompleted"),
                color = if (o.has("argb")) o.getInt("argb") else Palette[o.optInt("color", i).mod(Palette.size)],
                id = o.getString("id"),
                items = (0 until ia.length()).map { Item(ia.getJSONObject(it).getString("t"), ia.getJSONObject(it).getBoolean("c")) }
            )
        }
    }

    fun load(prefs: SharedPreferences): List<TaskList> {
        runCatching { prefs.getString(KEY, null)?.let { return parse(it) } }
        // Migrate the single list from version 1 of the app.
        val old = runCatching {
            val arr = JSONArray(prefs.getString("items", "[]"))
            (0 until arr.length()).map { Item(arr.getJSONObject(it).getString("t"), arr.getJSONObject(it).getBoolean("c")) }
        }.getOrDefault(emptyList())
        return listOf(TaskList("My list", old))
    }

    fun settingsToJson(s: Settings): String =
        JSONObject().put("mode", s.mode).put("color", s.themeColor).put("black", s.pureBlack).toString()

    fun parseSettings(json: String): Settings {
        val o = JSONObject(json)
        return Settings(o.optInt("mode", 0), o.optInt("color", Palette[0]), o.optBoolean("black"))
    }

    fun loadSettings(prefs: SharedPreferences): Settings =
        runCatching { parseSettings(prefs.getString("settings", null)!!) }.getOrDefault(Settings())

    fun saveSettings(prefs: SharedPreferences, json: String) = prefs.edit().putString("settings", json).apply()

    fun save(prefs: SharedPreferences, json: String) = prefs.edit().putString(KEY, json).apply()
}
