package com.example.checklist

import androidx.compose.runtime.mutableStateListOf
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import org.junit.Rule
import org.junit.Test

private fun sample() = mutableStateListOf(
    Item("Milk", true), Item("Eggs"), Item("Bread"), Item("Coffee beans"), Item("")
)

class LightScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun light() {
        paparazzi.snapshot { ChecklistTheme(dark = false) { ChecklistScreen(sample()) } }
    }
}

class DarkScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT))
    @Test fun dark() {
        paparazzi.snapshot { ChecklistTheme(dark = true) { ChecklistScreen(sample()) } }
    }
}
