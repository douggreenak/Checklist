package com.example.checklist

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class ChecklistScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun addButtonCreatesItem() {
        val items = mutableStateListOf<Item>()
        rule.setContent { ChecklistScreen(items) }
        rule.onNodeWithText("+ Add item").performClick()
        rule.waitForIdle()
        assert(items.size == 1)
    }

    @Test fun enterAddsNextItem() {
        val items = mutableStateListOf(Item("milk"))
        rule.setContent { ChecklistScreen(items) }
        rule.onNodeWithText("milk").performImeAction()
        rule.waitForIdle()
        assert(items.size == 2)
        assert(items[1].text.isEmpty())
    }

    @Test fun deleteRemovesImmediately() {
        val items = mutableStateListOf(Item("milk"))
        rule.setContent { ChecklistScreen(items) }
        rule.onNodeWithText("✕").performClick()
        rule.waitForIdle()
        assert(items.isEmpty())
    }

    @Test fun checkTogglesItem() {
        val items = mutableStateListOf(Item("milk"))
        rule.setContent { ChecklistScreen(items) }
        items[0].checked = true
        rule.waitForIdle()
        rule.onNodeWithText("✓").assertExists()
    }
}
