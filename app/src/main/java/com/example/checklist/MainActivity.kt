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
            SyncEffect(session, lists, prefs, this@MainActivity)

            var openId by rememberSaveable { mutableStateOf<String?>(null) }
            var inSettings by rememberSaveable { mutableStateOf(false) }
            var showAll by rememberSaveable { mutableStateOf(false) }
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
            BackHandler(enabled = open != null || inSettings || showAll) {
                if (inSettings) inSettings = false else if (showAll) showAll = false else openId = null
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
                        onSignOut = { session.signOut(); inSettings = false; showAll = false; openId = null },
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
                    showAll -> AllScreen(lists, onBack = { showAll = false })
                    open != null -> ListScreen(open, onBack = { openId = null }, onDeleteList = { lists.remove(open); openId = null })
                    else -> HomeScreen(
                        lists,
                        subtitle = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                        onOpen = { openId = it.id },
                        onAdd = { val l = TaskList(color = Palette[lists.size % Palette.size]); lists.add(l); openId = l.id },
                        onDelete = { lists.remove(it) },
                        onSettings = { inSettings = true },
                        onOpenAll = { showAll = true },
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
 * Live sync while signed in. Changes go to the server about half a second after you stop typing; the server merges them with
 * other people's changes (field by field, newest wins) and sends back the combined lists. Every couple of seconds we also ask
 * whether anything changed, so two people see each other's check-offs almost immediately. Only runs while the app is on screen.
 */
@Composable
private fun SyncEffect(session: Session, lists: MutableList<TaskList>, prefs: android.content.SharedPreferences, activity: ComponentActivity) {
    LaunchedEffect(session.token) {
        val token = session.token ?: return@LaunchedEffect
        val api = try { Api(session.server) } catch (e: IllegalArgumentException) { session.signOut(); return@LaunchedEffect }
        var prev: List<DList>? = Doc.loadPrev(prefs)   // last document the server confirmed; null on a brand-new sign-in
        var knownRev = -1L
        var lastSig: String? = null

        suspend fun syncOnce() {
            if (prev == null) {
                // First time on this device: take what the server has (upgrading old data), never overwrite it.
                val r = withContext(Dispatchers.IO) { api.fetch(token) }
                if (r.doc != null) {
                    val d = Doc.parse(r.doc)
                    Doc.apply(d, lists); prev = d; knownRev = r.rev; Doc.savePrev(prefs, d)
                } else prev = emptyList()
            }
            val sent = Doc.build(prev!!, lists, System.currentTimeMillis())
            val sig = Doc.signature(lists)
            val r = withContext(Dispatchers.IO) { api.sync(token, Doc.toJson(sent)) }
            val merged = Doc.parse(r.doc!!)
            if (Doc.signature(lists) == sig) {
                Doc.apply(merged, lists); prev = merged; lastSig = Doc.signature(lists)
            } else {
                prev = sent                          // edited while waiting: the next round sends those too
            }
            knownRev = r.rev
            Doc.savePrev(prefs, prev!!)
        }

        session.status = "Syncing…"
        var tickSig = ""
        var tick = 0
        while (true) {
            delay(400)
            tick++
            if (session.token == null) return@LaunchedEffect
            if (!activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) continue
            val sig = Doc.signature(lists)
            val stable = sig == tickSig
            tickSig = sig
            try {
                var go = prev == null || (sig != lastSig && stable)
                if (!go && tick % 5 == 0) go = withContext(Dispatchers.IO) { api.rev(token) } != knownRev
                if (go) { syncOnce(); session.status = "Synced" }
            } catch (e: ApiException) {
                if (e.code == 401) { session.signOut(); return@LaunchedEffect }
                session.status = e.message ?: "Sync problem"
                delay(5_000)
            } catch (e: IOException) {
                session.status = "Offline – will retry"
                delay(5_000)
            }
        }
    }
}
