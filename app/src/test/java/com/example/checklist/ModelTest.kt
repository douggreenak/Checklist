package com.example.checklist

import org.junit.Assert.*
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelTest {
    @Test fun countsRemainingAndDone() {
        val l = TaskList("x", listOf(Item("a"), Item("b", true), Item("c", true)))
        assertEquals(1, l.remaining)
        assertEquals(2, l.done)
    }

    @Test fun jsonRoundTrip() {
        val l = TaskList("Groceries", listOf(Item("milk", true), Item("eggs")), showCompleted = true, color = 0xFF123456.toInt())
        val back = Store.parse(Store.toJson(listOf(l))).single()
        assertEquals("Groceries", back.name)
        assertEquals(l.id, back.id)
        assertTrue(back.showCompleted)
        assertEquals(0xFF123456.toInt(), back.color)
        assertEquals(listOf("milk", "eggs"), back.items.map { it.text })
        assertEquals(listOf(true, false), back.items.map { it.checked })
    }

    @Test fun settingsRoundTrip() {
        val back = Store.parseSettings(Store.settingsToJson(Settings(2, 0xFF00897B.toInt(), true)))
        assertEquals(2, back.mode)
        assertEquals(0xFF00897B.toInt(), back.themeColor)
        assertTrue(back.pureBlack)
    }
}

class ServerUrlTest {
    @Test fun addsSchemeAndApiPath() =
        assertEquals("https://example.com/checklist/api.php", normalizeServerUrl(" example.com/checklist/ "))

    @Test fun keepsExplicitPhpFile() =
        assertEquals("https://example.com/x/api.php", normalizeServerUrl("https://example.com/x/api.php"))

    @Test fun rejectsPlainHttp() {
        assertThrows(IllegalArgumentException::class.java) { normalizeServerUrl("http://example.com") }
    }

    @Test fun rejectsBlank() {
        assertThrows(IllegalArgumentException::class.java) { normalizeServerUrl("  ") }
    }
}
