package com.example.checklist

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/*
 * The shared document that is synced. Every field that can change carries the time (ms) it last changed;
 * the server merges two copies field by field, newest change wins, so two people can edit one list at once.
 * Deleted tasks and lists leave a tombstone. (Same format and rules as the web app and server/merge.php.)
 */
class DItem(val i: String, val t: String, val tu: Long, val c: Boolean, val cu: Long, val o: Double, val ou: Long)

class DList(
    val id: String, val name: String, val showCompleted: Boolean, val argb: Int, val lu: Long,
    val dead: Long, val del: Map<String, Long>, val items: List<DItem>
)

object Doc {
    fun parse(arr: JSONArray): List<DList> = (0 until arr.length()).map { k ->
        val o = arr.getJSONObject(k)
        val del = o.optJSONObject("del")
        val ia = o.optJSONArray("items") ?: JSONArray()
        DList(
            id = o.getString("id"), name = o.optString("name"), showCompleted = o.optBoolean("showCompleted"),
            argb = o.optInt("argb"), lu = o.optLong("lu"), dead = o.optLong("dead"),
            del = del?.keys()?.asSequence()?.associateWith { del.optLong(it) } ?: emptyMap(),
            items = (0 until ia.length()).map { j ->
                val x = ia.getJSONObject(j)
                DItem(x.getString("i"), x.optString("t"), x.optLong("tu"), x.optBoolean("c"), x.optLong("cu"), x.optDouble("o"), x.optLong("ou"))
            }
        )
    }

    fun toJson(doc: List<DList>): JSONArray = JSONArray().also { arr ->
        doc.forEach { l ->
            arr.put(JSONObject()
                .put("id", l.id).put("name", l.name).put("showCompleted", l.showCompleted).put("argb", l.argb)
                .put("lu", l.lu).put("dead", l.dead).put("del", JSONObject().also { d -> l.del.forEach { (k, v) -> d.put(k, v) } })
                .put("items", JSONArray().also { ia ->
                    l.items.forEach {
                        ia.put(JSONObject().put("i", it.i).put("t", it.t).put("tu", it.tu).put("c", it.c).put("cu", it.cu).put("o", it.o).put("ou", it.ou))
                    }
                }))
        }
    }

    /** What the user sees, for noticing that something changed. */
    fun signature(lists: List<TaskList>): String = buildString {
        lists.forEach { l ->
            append(l.id).append('|').append(l.name).append('|').append(l.showCompleted).append('|').append(l.color).append('\n')
            l.items.forEach { append(it.sid).append('|').append(it.text).append('|').append(it.checked).append('\n') }
            append('#')
        }
    }

    /** Stamps only the fields that changed since [prev] with [now]; unchanged fields keep their old time. */
    fun build(prev: List<DList>, lists: List<TaskList>, now: Long): List<DList> {
        val prevById = prev.associateBy { it.id }
        val out = ArrayList<DList>()
        for (l in lists) {
            val p = prevById[l.id]
            val pItems = p?.items?.associateBy { it.i } ?: emptyMap()
            val del = HashMap(p?.del ?: emptyMap())
            val present = l.items.map { it.sid }.toSet()
            pItems.keys.filter { it !in present }.forEach { if (it !in del) del[it] = now }
            // Keep each task's old position key while it stays in order; give moved or new tasks one between their neighbours.
            val os = arrayOfNulls<Double>(l.items.size)
            val po = l.items.map { pItems[it.sid]?.o }
            // Longest run of tasks whose old keys are still in order keeps them (so one move changes one task).
            val best = IntArray(po.size); val from = IntArray(po.size) { -1 }
            var end = -1
            for (a in po.indices) {
                val x = po[a] ?: continue
                best[a] = 1
                for (b in 0 until a) { val y = po[b] ?: continue; if (y < x && best[b] + 1 > best[a]) { best[a] = best[b] + 1; from[a] = b } }
                if (end < 0 || best[a] > best[end]) end = a
            }
            var cur = end
            while (cur >= 0) { os[cur] = po[cur]; cur = from[cur] }
            var k = 0
            while (k < os.size) {
                if (os[k] != null) { k++; continue }
                val lo = (k - 1 downTo 0).firstNotNullOfOrNull { os[it] } ?: Double.NEGATIVE_INFINITY
                var to = k
                while (to < os.size && os[to] == null) to++
                val hi = if (to < os.size) os[to]!! else Double.POSITIVE_INFINITY
                val run = to - k
                for (j in 0 until run) os[k + j] = when {
                    lo == Double.NEGATIVE_INFINITY && hi == Double.POSITIVE_INFINITY -> (k + j).toDouble()
                    lo == Double.NEGATIVE_INFINITY -> hi - (run - j)
                    hi == Double.POSITIVE_INFINITY -> lo + j + 1
                    else -> lo + (hi - lo) * (j + 1) / (run + 1)
                }
                k = to
            }
            val same = p != null && p.name == l.name && p.showCompleted == l.showCompleted && p.argb == l.color
            out.add(DList(
                l.id, l.name, l.showCompleted, l.color, if (same) p!!.lu else now, 0L, del,
                l.items.mapIndexed { n, it ->
                    val q = pItems[it.sid]
                    DItem(
                        it.sid,
                        it.text, if (q != null && q.t == it.text) q.tu else now,
                        it.checked, if (q != null && q.c == it.checked) q.cu else now,
                        os[n]!!, if (q != null && q.o == os[n]) q.ou else now
                    )
                }
            ))
        }
        val have = lists.map { it.id }.toSet()
        prev.filter { it.id !in have }.forEach { p ->
            out.add(if (p.dead != 0L) p else DList(p.id, "", false, 0, 0, now, emptyMap(), emptyList()))
        }
        return out
    }

    /** Shows [doc] on screen, updating existing lists and tasks in place so typing and focus are not disturbed. */
    fun apply(doc: List<DList>, lists: MutableList<TaskList>) {
        val oldLists = lists.associateBy { it.id }
        val target = doc.filter { it.dead == 0L }.map { d ->
            val l = oldLists[d.id] ?: TaskList(id = d.id)
            if (l.name != d.name) l.name = d.name
            if (l.showCompleted != d.showCompleted) l.showCompleted = d.showCompleted
            val color = if (d.argb != 0) dePurple(d.argb) else Palette[0]
            if (l.color != color) l.color = color
            val oldItems = l.items.associateBy { it.sid }
            val items = d.items.map { x ->
                val it = oldItems[x.i] ?: Item(x.t, x.c, sid = x.i)
                if (it.text != x.t) it.text = x.t
                if (it.checked != x.c) it.checked = x.c
                it
            }
            if (l.items.size != items.size || l.items.indices.any { l.items[it] !== items[it] }) { l.items.clear(); l.items.addAll(items) }
            l
        }
        if (lists.size != target.size || lists.indices.any { lists[it] !== target[it] }) { lists.clear(); lists.addAll(target) }
    }

    fun loadPrev(prefs: SharedPreferences): List<DList>? =
        runCatching { parse(JSONArray(prefs.getString("doc", null)!!)) }.getOrNull()

    fun savePrev(prefs: SharedPreferences, doc: List<DList>) = prefs.edit().putString("doc", toJson(doc).toString()).apply()
}
