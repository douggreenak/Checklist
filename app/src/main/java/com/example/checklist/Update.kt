package com.example.checklist

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UpdateInfo(val version: String, val url: String, val notes: String, val apkUrl: String = "")

object Updates {
    private const val LATEST = "https://api.github.com/repos/douggreenak/Checklist/releases/latest"

    /** "v1.10.0" -> [1, 10, 0]. Anything that isn't a number counts as 0. */
    fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    fun isNewer(latest: String, current: String): Boolean {
        val a = parts(latest); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private const val LATEST_PAGE = "https://github.com/douggreenak/Checklist/releases/latest"

    /**
     * Reads the newest GitHub release. Returns null when [current] is already the newest. Blocking: call off the main thread.
     * GitHub's API allows only 60 anonymous requests per hour per IP address (shared Wi-Fi and mobile networks can hit
     * that), so if it refuses we fall back to the website's "latest release" redirect, which has no such limit.
     */
    @Throws(IOException::class)
    fun check(current: String): UpdateInfo? =
        try { checkApi(current) } catch (e: RateLimited) { checkRedirect(current) }

    private class RateLimited : IOException("GitHub rate limit")

    /** No API needed: github.com/.../releases/latest redirects to .../releases/tag/<tag>. */
    private fun checkRedirect(current: String): UpdateInfo? {
        val c = URL(LATEST_PAGE).openConnection() as HttpURLConnection
        try {
            c.instanceFollowRedirects = false
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.setRequestProperty("User-Agent", "Checklist-Android")
            val code = c.responseCode
            val location = c.getHeaderField("Location")
            if (code !in 300..399 || location == null || "/releases/tag/" !in location) return null // no releases yet
            val tag = location.substringAfter("/releases/tag/").substringBefore('?').substringBefore('/')
            if (!isNewer(tag, current)) return null
            val v = tag.removePrefix("v")
            return UpdateInfo(v, "https://github.com/douggreenak/Checklist/releases/tag/$tag", "",
                "https://github.com/douggreenak/Checklist/releases/download/$tag/Checklist-$v.apk")
        } finally {
            c.disconnect()
        }
    }

    private fun checkApi(current: String): UpdateInfo? {
        val c = URL(LATEST).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "Checklist-Android")
            val code = c.responseCode
            if (code == 404) return null // no releases yet
            if (code == 403 || code == 429) throw RateLimited()
            if (code !in 200..299) throw IOException("GitHub answered $code")
            val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val tag = o.getString("tag_name")
            if (!isNewer(tag, current)) return null
            val assets = o.optJSONArray("assets")
            val page = o.optString("html_url"); var apk = ""
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) { apk = a.getString("browser_download_url"); break }
            }
            return UpdateInfo(tag.removePrefix("v"), page, o.optString("body"), apk)
        } finally {
            c.disconnect()
        }
    }
}
