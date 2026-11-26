package com.example.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    settings: Settings, onBack: () -> Unit,
    account: String? = null, syncStatus: String = "", onSignOut: () -> Unit = {},
    /** Returns null on success, or an error message. */
    onChangePassword: (suspend (current: String, new: String) -> String?)? = null,
    passwordFormOpen: Boolean = false,
    version: String = "",
    /** Returns the newer release, or null if up to date. May throw IOException. */
    onCheckUpdate: (suspend () -> UpdateInfo?)? = null
) {
    ClearFocusWhenKeyboardHides()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
                Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 8.dp))
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Section("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { i, label ->
                    val modifier = Modifier.weight(1f).height(56.dp)
                    if (settings.mode == i) Button({ settings.mode = i }, modifier, shape = RoundedCornerShape(16.dp)) {
                        Text(label, fontSize = 18.sp)
                    } else OutlinedButton({ settings.mode = i }, modifier, shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Text(label, fontSize = 18.sp)
                    }
                }
            }

            Section("Theme color")
            Text("Tints buttons, backgrounds and cards across the whole app.", fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ColorPicker(settings.themeColor, { settings.themeColor = it }, columns = 4)

            Section("Dark mode")
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { settings.pureBlack = !settings.pureBlack },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Pure black background", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("Saves battery on OLED screens", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.pureBlack, onCheckedChange = { settings.pureBlack = it })
            }

            if (account != null) {
                Section("Account")
                Text(account, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                Text(syncStatus, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (onChangePassword != null) PasswordForm(onChangePassword, passwordFormOpen)
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Sign out", fontSize = 18.sp, color = DeleteRed) }
            }

            if (onCheckUpdate != null) UpdateSection(version, onCheckUpdate)

            OutlinedButton(
                onClick = { settings.reset() },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 0.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Reset to defaults", fontSize = 18.sp) }
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun UpdateSection(version: String, check: suspend () -> UpdateInfo?) {
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<UpdateInfo?>(null) }
    val scope = rememberCoroutineScope()
    val uri = androidx.compose.ui.platform.LocalUriHandler.current

    Section("About")
    Text("Checklist $version", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
    if (message != null) Text(message!!, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val update = found
    if (update != null) Button(
        onClick = { uri.openUri(update.url) },
        modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp)
    ) { Text("Download ${update.version}", fontSize = 19.sp) }
    OutlinedButton(
        onClick = {
            scope.launch {
                checking = true; message = null
                try {
                    val u = check()
                    found = u
                    message = if (u == null) "You're up to date." else "Version ${u.version} is available."
                } catch (e: java.io.IOException) {
                    message = "Couldn't check for updates. Check your connection."
                }
                checking = false
            }
        },
        enabled = !checking,
        modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)
    ) { Text(if (checking) "Checking…" else "Check for updates", fontSize = 18.sp) }
}

@Composable
private fun PasswordForm(onChange: suspend (String, String) -> String?, startOpen: Boolean) {
    var open by remember { mutableStateOf(startOpen) }
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = { open = !open; message = null },
        modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)
    ) { Text("Change password", fontSize = 18.sp) }

    if (open) {
        @Composable fun field(label: String, value: String, set: (String) -> Unit) = OutlinedTextField(
            value = value, onValueChange = set, singleLine = true, label = { Text(label) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        )
        field("Current password", current) { current = it }
        field("New password", new) { new = it }
        field("Repeat new password", confirm) { confirm = it }
        if (message != null) Text(message!!, fontSize = 17.sp, color = if (ok) DoneGreen else DeleteRed)
        Button(
            onClick = {
                when {
                    new.length < 6 -> { ok = false; message = "The new password must be at least 6 characters" }
                    new != confirm -> { ok = false; message = "The new passwords don't match" }
                    else -> scope.launch {
                        busy = true; message = null
                        val err = onChange(current, new)
                        busy = false
                        ok = err == null
                        message = err ?: "Password changed. Other devices were signed out."
                        if (ok) { current = ""; new = ""; confirm = "" }
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp)
        ) { Text(if (busy) "Saving…" else "Save new password", fontSize = 19.sp) }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp))
}
