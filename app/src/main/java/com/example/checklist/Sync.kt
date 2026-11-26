package com.example.checklist

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Turns "example.com/checklist" into "https://example.com/checklist/api.php". Only https is allowed. */
fun normalizeServerUrl(raw: String): String {
    var s = raw.trim().trimEnd('/')
    if (s.isEmpty()) throw IllegalArgumentException("Enter the server address")
    if (s.startsWith("http://", ignoreCase = true)) throw IllegalArgumentException("The server address must start with https://")
    if (!s.startsWith("https://", ignoreCase = true)) s = "https://$s"
    if (!s.endsWith(".php", ignoreCase = true)) s += "/api.php"
    return s
}

class ApiException(val code: Int, message: String) : IOException(message)

class Remote(val json: String?, val updatedAt: Long)

/** Talks to server/api.php. The app never sees the database or its password. */
class Api(server: String) {
    private val base = normalizeServerUrl(server)

    fun login(password: String): String =
        call("login", null, JSONObject().put("password", password)).getString("token")

    fun fetch(token: String): Remote {
        val o = call("get", token, null)
        return Remote(if (o.isNull("json")) null else o.getString("json"), o.optLong("updatedAt"))
    }

    fun push(token: String, json: String) {
        call("save", token, JSONObject().put("json", json))
    }

    private fun call(action: String, token: String?, body: JSONObject?): JSONObject {
        val c = URL("$base?action=$action").openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            val obj = runCatching { JSONObject(text) }.getOrNull()
            if (code !in 200..299) throw ApiException(code, obj?.optString("error")?.ifBlank { null } ?: "Server error ($code)")
            return obj ?: throw ApiException(code, "Unexpected reply from server")
        } finally {
            c.disconnect()
        }
    }
}

/** Where the app is signed in (server address and token) plus a short sync status for the UI. */
class Session(private val prefs: SharedPreferences) {
    var server by mutableStateOf(prefs.getString("server", "") ?: "")
    var token by mutableStateOf(prefs.getString("token", null))
    var status by mutableStateOf("")

    fun signIn(server: String, token: String) {
        this.server = server; this.token = token
        prefs.edit().putString("server", server).putString("token", token).apply()
    }

    fun signOut() {
        token = null; status = ""
        prefs.edit().remove("token").apply()
    }
}
