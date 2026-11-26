package com.example.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
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
private fun Section(title: String) {
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp))
}
