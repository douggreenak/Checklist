package com.example.checklist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("checklist", Context.MODE_PRIVATE)
        val lists = mutableStateListOf<TaskList>().apply { addAll(Store.load(prefs)) }
        val settings = Store.loadSettings(prefs)
        val session = Session(prefs)
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: ""
        setContent {
            LaunchedEffect(Unit) { snapshotFlow { Store.toJson(lists) }.collect { Store.save(prefs, it) } }
            LaunchedEffect(Unit) { snapshotFlow { Store.settingsToJson(settings) }.collect { Store.saveSettings(prefs, it) } }
            SyncEffect(session, lists)

            var openId by rememberSaveable { mutableStateOf<String?>(null) }
            var inSettings by rememberSaveable { mutableStateOf(false) }
            var update by remember { mutableStateOf<UpdateInfo?>(null) }
            var updateDismissed by remember { mutableStateOf(false) }
            var installProgress by remember { mutableStateOf<Float?>(null) }
            var installMessage by remember { mutableStateOf<String?>(null) }
            val installScope = rememberCoroutineScope()
            val startInstall: (UpdateInfo) -> Unit = { info ->
                if (installProgress == null) installScope.launch {
                    installMessage = null; installProgress = 0f
                    installMessage = Installer.downloadAndInstall(this@MainActivity, info) { installProgress = it }
                    installProgress = null
                }
            }
            // Quietly look for a newer GitHub release each time the app starts.
            LaunchedEffect(Unit) {
                update = try { withContext(Dispatchers.IO) { Updates.check(version) } } catch (e: IOException) { null }
            }
            val open = lists.firstOrNull { it.id == openId }
            BackHandler(enabled = open != null || inSettings) {
                if (inSettings) inSettings = false else openId = null
            }
            val dark = when (settings.mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            ChecklistTheme(dark, settings.themeColor, settings.pureBlack) {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            val scope = rememberCoroutineScope()
                var busy by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                when {
                    session.token == null -> LoginScreen(session.server, busy, error) { server, password ->
                        scope.launch {
                            busy = true; error = null
                            try {
                                val api = Api(server)
                                val token = withContext(Dispatchers.IO) { api.login(password) }
                                session.signIn(normalizeServerUrl(server), token)
                            } catch (e: ApiException) {
                                error = e.message
                            } catch (e: IOException) {
                                error = "Can't reach the server. Check the address and your connection."
                            } catch (e: IllegalArgumentException) {
                                error = e.message
                            }
                            busy = false
                        }
                    }
                    inSettings -> SettingsScreen(
                        settings, onBack = { inSettings = false },
                        account = session.server, syncStatus = session.status,
                        version = version,
                        onCheckUpdate = { withContext(Dispatchers.IO) { Updates.check(version) } },
                        onInstall = startInstall, installProgress = installProgress, installMessage = installMessage,
                        onSignOut = { session.signOut(); inSettings = false; openId = null },
                        onChangePassword = { current, new ->
                            try {
                                val newToken = withContext(Dispatchers.IO) { Api(session.server).changePassword(session.token ?: "", current, new) }
                                session.signIn(session.server, newToken)
                                null
                            } catch (e: ApiException) {
                                e.message ?: "Couldn't change the password"
                            } catch (e: IOException) {
                                "Can't reach the server. Try again when you're online."
                            }
                        }
                    )
                    open != null -> ListScreen(open, onBack = { openId = null }, onDeleteList = { lists.remove(open); openId = null })
                    else -> HomeScreen(
                        lists,
                        subtitle = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                        onOpen = { openId = it.id },
                        onAdd = { val l = TaskList(color = Palette[lists.size % Palette.size]); lists.add(l); openId = l.id },
                        onDelete = { lists.remove(it) },
                        onSettings = { inSettings = true },
                        update = update.takeUnless { updateDismissed },
                        onUpdate = { update?.let(startInstall) },
                        installProgress = installProgress, installMessage = installMessage,
                        onDismissUpdate = { updateDismissed = true }
                    )
                }
            }
        }
    }
}

/**
 * Keeps [lists] in sync with the server while signed in: pulls on sign-in and every 20 seconds,
 * and pushes shortly after each local change. If both sides changed, the most recent save wins.
 */
@Composable
private fun SyncEffect(session: Session, lists: MutableList<TaskList>) {
    LaunchedEffect(session.token) {
        val token = session.token ?: return@LaunchedEffect
        val api = try { Api(session.server) } catch (e: IllegalArgumentException) { session.signOut(); return@LaunchedEffect }
        var synced: String? = null // what the server is known to hold, in local format

        fun dirty() = synced == null || Store.toJson(lists) != synced

        suspend fun guard(block: suspend () -> Unit) {
            try {
                block()
                session.status = "Synced"
            } catch (e: ApiException) {
                if (e.code == 401) session.signOut() else session.status = e.message ?: "Sync problem"
            } catch (e: IOException) {
                session.status = "Offline – will retry"
            }
        }

        suspend fun pull() {
            val remote = withContext(Dispatchers.IO) { api.fetch(token) }
            if (remote.json == null) {
                val json = Store.toJson(lists)
                withContext(Dispatchers.IO) { api.push(token, json) }
                synced = json
            } else if (!dirty() || synced == null) {
                val parsed = Store.parse(remote.json)
                if (!dirty() || synced == null) { // nothing changed locally while we were waiting
                    if (Store.toJson(parsed) != Store.toJson(lists)) { lists.clear(); lists.addAll(parsed) }
                    synced = Store.toJson(lists)
                }
            }
        }

        session.status = "Syncing…"
        guard { pull() }

        launch {
            while (true) {
                delay(20_000)
                if (session.token == null) return@launch
                if (synced == null || !dirty()) guard { pull() } else guard { push(api, token, lists)?.let { synced = it } }
            }
        }

        snapshotFlow { Store.toJson(lists) }.collectLatest { json ->
            // Never push before a successful first pull, or we could overwrite newer data on the server.
            if (synced == null || json == synced) return@collectLatest
            delay(800)
            while (true) {
                try {
                    withContext(Dispatchers.IO) { api.push(token, json) }
                    synced = json; session.status = "Synced"; return@collectLatest
                } catch (e: ApiException) {
                    if (e.code == 401) session.signOut() else session.status = e.message ?: "Sync problem"
                    return@collectLatest
                } catch (e: IOException) {
                    session.status = "Offline – will retry"
                    delay(15_000)
                }
            }
        }
    }
}

private suspend fun push(api: Api, token: String, lists: List<TaskList>): String? {
    val json = Store.toJson(lists)
    withContext(Dispatchers.IO) { api.push(token, json) }
    return json
}
