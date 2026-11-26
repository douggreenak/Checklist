package com.example.checklist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class ChecklistScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun show(list: TaskList) = rule.setContent { ChecklistTheme(false) { ListScreen(list, {}, {}) } }

    @Test fun addButtonCreatesItem() {
        val list = TaskList("t")
        show(list)
        rule.onNodeWithText("Add task").performClick()
        rule.waitForIdle()
        assert(list.items.size == 1)
    }

    @Test fun enterAddsNextItem() {
        val list = TaskList("t", listOf(Item("milk")))
        show(list)
        rule.onNodeWithText("milk").performImeAction()
        rule.waitForIdle()
        assert(list.items.size == 2 && list.items[1].text.isEmpty())
    }

    @Test fun deleteRemovesImmediately() {
        val list = TaskList("t", listOf(Item("milk")))
        show(list)
        rule.onNodeWithContentDescription("Delete task").performClick()
        rule.waitForIdle()
        assert(list.items.isEmpty())
    }

    @Test fun swipeLeftDeletes() {
        val list = TaskList("t", listOf(Item("milk")))
        show(list)
        rule.onNodeWithText("milk").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assert(list.items.isEmpty())
    }

    @Test fun swipeRightCompletesAndCompletedSectionShows() {
        val list = TaskList("t", listOf(Item("milk")))
        show(list)
        rule.onNodeWithText("milk").performTouchInput { swipeRight() }
        rule.waitForIdle()
        assert(list.items[0].checked)
        rule.onNodeWithText("Completed", substring = true).performClick()
        rule.onNodeWithText("milk").assertExists()
    }
}
