package com.example.checklist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeleteRed = Color(0xFFD32F2F)
val DoneGreen = Color(0xFF2E7D32)

@Composable
fun ChecklistTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = Color(0xFFCFBCFF), onPrimary = Color(0xFF381E72),
            background = Color(0xFF121212), onBackground = Color(0xFFEAEAEA),
            surface = Color(0xFF121212), onSurface = Color(0xFFEAEAEA),
            surfaceVariant = Color(0xFF2A2833), onSurfaceVariant = Color(0xFFCAC4D0)
        ) else lightColorScheme(
            background = Color.White, surface = Color.White,
            surfaceVariant = Color(0xFFEDE7F6)
        ),
        content = content
    )
}
