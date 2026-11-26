package com.example.checklist

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import org.junit.Rule
import org.junit.Test

private fun sampleLists() = listOf(
    TaskList("Groceries", listOf(Item("Milk", true), Item("Eggs"), Item("Bread"), Item("Coffee beans"))),
    TaskList("Work", listOf(Item("Send invoice"), Item("Call plumber", true), Item("Book flights", true))),
    TaskList("Weekend", listOf(Item("Hike"), Item("Laundry"))),
    TaskList("Ideas")
)

private fun sampleList() = TaskList(
    "Groceries",
    listOf(Item("Eggs"), Item("Bread"), Item("Coffee beans"), Item(""), Item("Milk", true), Item("Butter", true)),
    showCompleted = true
)

class LightScreenshots {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun home() = paparazzi.snapshot { ChecklistTheme(false) { HomeScreen(sampleLists(), {}, {}, {}) } }
    @Test fun list() = paparazzi.snapshot { ChecklistTheme(false) { ListScreen(sampleList(), {}, {}) } }
}

class DarkScreenshots {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT))
    @Test fun home() = paparazzi.snapshot { ChecklistTheme(true) { HomeScreen(sampleLists(), {}, {}, {}) } }
    @Test fun list() = paparazzi.snapshot { ChecklistTheme(true) { ListScreen(sampleList(), {}, {}) } }
}
