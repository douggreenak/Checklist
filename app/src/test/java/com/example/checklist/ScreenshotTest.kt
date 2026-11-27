package com.example.checklist

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import org.junit.Rule
import org.junit.Test

private fun sampleLists() = listOf(
    TaskList("Groceries", listOf(Item("Milk", true), Item("Eggs"), Item("Bread"), Item("Coffee beans")), color = Palette[0]),
    TaskList("Work", listOf(Item("Send invoice"), Item("Call plumber", true), Item("Book flights", true)), color = Palette[1]),
    TaskList("Weekend", listOf(Item("Hike"), Item("Laundry")), color = Palette[2]),
    TaskList("Ideas", color = Palette[4])
)

private fun sampleList() = TaskList(
    "Groceries",
    listOf(Item("Eggs"), Item("Bread"), Item("Coffee beans"), Item(""), Item("Milk", true), Item("Butter", true)),
    showCompleted = true
)

class LightScreenshots {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun home() = paparazzi.snapshot { ChecklistTheme(false) { HomeScreen(sampleLists(), "Thursday, November 26", {}, {}, {}) } }
    @Test fun list() = paparazzi.snapshot { ChecklistTheme(false) { ListScreen(sampleList(), {}, {}) } }
    @Test fun all() = paparazzi.snapshot { ChecklistTheme(false) { AllScreen(sampleLists()) {} } }
}

class DarkScreenshots {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT))
    @Test fun home() = paparazzi.snapshot { ChecklistTheme(true) { HomeScreen(sampleLists(), "Thursday, November 26", {}, {}, {}) } }
    @Test fun list() = paparazzi.snapshot { ChecklistTheme(true) { ListScreen(sampleList(), {}, {}) } }
}

private val teal = Palette[2]

class ThemeScreenshots {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT))
    @Test fun settingsDarkBlack() = paparazzi.snapshot {
        ChecklistTheme(true, teal, pureBlack = true) { SettingsScreen(Settings(2, teal, true), onBack = {}) }
    }
    @Test fun homeDarkBlackTeal() = paparazzi.snapshot {
        ChecklistTheme(true, teal, pureBlack = true) { HomeScreen(sampleLists(), "Thursday, November 26", {}, {}, {}) }
    }
}

class LightThemeScreenshots {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun settingsLight() = paparazzi.snapshot { ChecklistTheme(false) { SettingsScreen(Settings(), onBack = {}, account = "https://example.com/checklist/api.php", syncStatus = "Synced") } }
    @Test fun homeLightPink() = paparazzi.snapshot {
        ChecklistTheme(false, Palette[5]) { HomeScreen(sampleLists(), "Thursday, November 26", {}, {}, {}) }
    }
    @Test fun colorPicker() = paparazzi.snapshot {
        ChecklistTheme(false) {
            androidx.compose.material3.Surface { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(24.dp)) {
                ColorPicker(Palette[1], {}) } }
        }
    }
}

class LoginScreenshots {
    @get:Rule val light = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun loginLight() = light.snapshot { ChecklistTheme(false) { LoginScreen("example.com/checklist", false, "Wrong password") { _, _ -> } } }
}

class FullSettingsScreenshot {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 3000))
    @Test fun settingsWithPassword() = paparazzi.snapshot {
        ChecklistTheme(false) {
            SettingsScreen(Settings(), onBack = {}, account = "https://example.com/checklist/api.php", syncStatus = "Synced",
                onChangePassword = { _, _ -> null }, passwordFormOpen = true)
        }
    }
}

class UpdateBannerScreenshot {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun homeWithUpdate() = paparazzi.snapshot {
        ChecklistTheme(false) { HomeScreen(sampleLists(), "Thursday, November 26", {}, {}, {}, update = UpdateInfo("1.3.0", "", "")) }
    }
}

class EmptyScreenshots {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun emptyList() = paparazzi.snapshot { ChecklistTheme(false) { ListScreen(TaskList("Ideas", color = Palette[4]), {}, {}) } }
    @Test fun noLists() = paparazzi.snapshot { ChecklistTheme(false) { HomeScreen(emptyList<TaskList>(), "Thursday, November 26", onOpen = {}, onAdd = {}, onDelete = {}) } }
}
