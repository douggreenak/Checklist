package com.example.checklist

import org.junit.Assert.*
import org.junit.Test

class DocTest {
    private fun list(vararg texts: String) = TaskList("L", texts.map { Item(it) })

    @Test fun onlyChangedFieldsGetNewTimes() {
        val l = list("a", "b")
        val first = Doc.build(emptyList(), listOf(l), 100)
        l.items[0].checked = true
        val second = Doc.build(first, listOf(l), 200)
        val a = second[0].items[0]; val b = second[0].items[1]
        assertEquals(200, a.cu); assertEquals(100, a.tu)   // only the check changed
        assertEquals(100, b.cu); assertEquals(100, b.tu)
    }

    @Test fun deletingLeavesATombstone() {
        val l = list("a", "b")
        val first = Doc.build(emptyList(), listOf(l), 100)
        val gone = l.items[0].sid
        l.items.removeAt(0)
        val second = Doc.build(first, listOf(l), 300)
        assertEquals(1, second[0].items.size)
        assertEquals(300L, second[0].del[gone])
    }

    @Test fun deletedListBecomesDead() {
        val l = list("a")
        val first = Doc.build(emptyList(), listOf(l), 100)
        val second = Doc.build(first, emptyList(), 400)
        assertEquals(400L, second.single { it.id == l.id }.dead)
    }

    @Test fun movingOneTaskOnlyChangesItsPosition() {
        val l = list("a", "b", "c")
        val first = Doc.build(emptyList(), listOf(l), 100)
        val c = l.items.removeAt(2); l.items.add(0, c)
        val second = Doc.build(first, listOf(l), 200)
        val byText = second[0].items.associateBy { it.t }
        assertTrue(byText["c"]!!.o < byText["a"]!!.o)
        assertEquals(200, byText["c"]!!.ou)
        assertEquals(100, byText["a"]!!.ou); assertEquals(100, byText["b"]!!.ou)
    }

    @Test fun applyUpdatesInPlaceAndRoundTripsJson() {
        val l = list("a", "b")
        val doc = Doc.build(emptyList(), listOf(l), 100)
        val lists = mutableListOf(l)
        val keep = l.items[0]
        val remote = Doc.parse(Doc.toJson(doc)).map { d ->
            DList(d.id, "Renamed", d.showCompleted, d.argb, d.lu, 0, d.del, d.items.map { if (it.t == "a") DItem(it.i, it.t, it.tu, true, 999, it.o, it.ou) else it })
        }
        Doc.apply(remote, lists)
        assertSame(l, lists[0]); assertSame(keep, l.items[0])
        assertEquals("Renamed", l.name); assertTrue(keep.checked)
    }

    @Test fun applyDropsDeadListsAndAddsNewTasks() {
        val l = list("a")
        val lists = mutableListOf(l)
        val doc = Doc.build(emptyList(), listOf(l), 100)
        val withNew = doc.map { d -> DList(d.id, d.name, d.showCompleted, d.argb, d.lu, 0, d.del, d.items + DItem("zz", "new", 1, false, 1, 9.0, 1)) }
        Doc.apply(withNew, lists)
        assertEquals(listOf("a", "new"), l.items.map { it.text })
        Doc.apply(withNew.map { DList(it.id, "", false, 0, 0, 5, emptyMap(), emptyList()) }, lists)
        assertTrue(lists.isEmpty())
    }
}
