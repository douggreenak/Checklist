package com.example.checklist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

val DeleteRed = Color(0xFFE53935)
val DoneGreen = Color(0xFF2E9E5B)

/** Accent colors a list can have, like the colored lists in Microsoft To Do. */
private val palette = listOf(
    Color(0xFF6C4DD6), Color(0xFF1E7BE0), Color(0xFF00897B),
    Color(0xFF3FA34D), Color(0xFFE8710A), Color(0xFFD81B60)
)
val AccentCount = palette.size

class Accent(val color: Color, val on: Color)

@Composable
fun accentFor(index: Int): Accent {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = palette[index.mod(palette.size)]
    return if (dark) Accent(lerp(base, Color.White, 0.45f), Color(0xFF16141C)) else Accent(base, Color.White)
}

@Composable
fun ChecklistTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = Color(0xFFCFBCFF), onPrimary = Color(0xFF381E72),
            background = Color(0xFF0F0F12), onBackground = Color(0xFFF0EEF4),
            surface = Color(0xFF1C1B22), onSurface = Color(0xFFF0EEF4),
            surfaceVariant = Color(0xFF2A2833), onSurfaceVariant = Color(0xFFB4B0BE),
            outlineVariant = Color(0xFF34333D)
        ) else lightColorScheme(
            primary = Color(0xFF6C4DD6),
            background = Color(0xFFF4F3F8), onBackground = Color(0xFF1B1A20),
            surface = Color.White, onSurface = Color(0xFF1B1A20),
            surfaceVariant = Color(0xFFEDE9F6), onSurfaceVariant = Color(0xFF6A6773),
            outlineVariant = Color(0xFFE3E1EA)
        ),
        content = content
    )
}
