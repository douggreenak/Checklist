package com.example.checklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(initialServer: String, busy: Boolean, error: String?, onSignIn: (server: String, password: String) -> Unit) {
    ClearFocusWhenKeyboardHides()
    var server by remember { mutableStateOf(initialServer) }
    var password by remember { mutableStateOf("") }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().imePadding().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", fontSize = 56.sp)
            Text("Checklist", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Sign in to see your lists", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = server, onValueChange = { server = it }, singleLine = true,
                label = { Text("Server address") }, placeholder = { Text("example.com/checklist") },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it }, singleLine = true,
                label = { Text("Password") },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (!busy) onSignIn(server, password) }),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            if (error != null) Text(error, color = DeleteRed, fontSize = 17.sp)
            Button(
                onClick = { onSignIn(server, password) }, enabled = !busy,
                shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(68.dp)
            ) { Text(if (busy) "Signing in…" else "Sign in", fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}
