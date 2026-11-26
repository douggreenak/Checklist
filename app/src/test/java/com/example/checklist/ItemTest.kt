package com.example.checklist

import org.junit.Assert.*
import org.junit.Test

class ItemTest {
    @Test fun newItemIsEmptyAndUnchecked() {
        val i = Item()
        assertEquals("", i.text)
        assertFalse(i.checked)
    }

    @Test fun itemsGetUniqueIds() {
        assertNotEquals(Item().id, Item().id)
    }
}
