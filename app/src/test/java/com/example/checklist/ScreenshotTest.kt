package com.example.checklist

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test fun filledList() {
        val items = mutableStateListOf(
            Item("Milk", true), Item("Eggs"), Item("Bread"), Item("Coffee beans"), Item("")
        )
        paparazzi.snapshot { MaterialTheme { ChecklistScreen(items) } }
    }
}
