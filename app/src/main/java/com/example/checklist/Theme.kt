package com.example.checklist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

val DeleteRed = Color(0xFFE53935)
val DoneGreen = Color(0xFF2E9E5B)

/** Preset colors offered for lists and for the app theme (ARGB ints). */
val Palette: List<Int> = listOf(
    0xFF6C4DD6, 0xFF1E7BE0, 0xFF00897B, 0xFF3FA34D,
    0xFFE8710A, 0xFFD81B60, 0xFFE53935, 0xFF8E24AA,
    0xFF00ACC1, 0xFF7CB342, 0xFFFDD835, 0xFF546E7A
).map { it.toInt() }

class Accent(val color: Color, val on: Color)

/** A calmer version of an accent for big buttons, so they don't shout. */
fun Accent.softer(): Accent { val c = lerp(color, Color.Black, 0.3f); return Accent(c, onColor(c)) }

private fun onColor(c: Color) = if (c.luminance() > 0.5f) Color(0xFF16141C) else Color.White

/** The color to draw a list (or the app theme) with, adjusted so it reads well in light or dark mode. */
@Composable
fun accentFor(argb: Int): Accent {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = Color(argb)
    val c = if (dark) lerp(base, Color.White, 0.45f) else base
    return Accent(c, onColor(c))
}

@Composable
fun ChecklistTheme(
    dark: Boolean = isSystemInDarkTheme(),
    primary: Int = Palette[0],
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val p = Color(primary)
    val scheme = if (dark) {
        val accent = lerp(p, Color.White, 0.45f)
        val bg = if (pureBlack) Color.Black else lerp(Color(0xFF0F0F12), p, 0.10f)
        val card = if (pureBlack) Color(0xFF121212) else lerp(Color(0xFF1C1B22), p, 0.10f)
        darkColorScheme(
            primary = accent, onPrimary = onColor(accent),
            background = bg, onBackground = Color(0xFFF0EEF4),
            surface = card, onSurface = Color(0xFFF0EEF4),
            surfaceVariant = lerp(card, Color.White, 0.08f), onSurfaceVariant = Color(0xFFB4B0BE),
            outlineVariant = lerp(card, Color.White, 0.14f)
        )
    } else lightColorScheme(
        primary = p, onPrimary = onColor(p),
        background = lerp(Color.White, p, 0.07f), onBackground = Color(0xFF1B1A20),
        surface = Color.White, onSurface = Color(0xFF1B1A20),
        surfaceVariant = lerp(Color.White, p, 0.14f), onSurfaceVariant = Color(0xFF6A6773),
        outlineVariant = lerp(Color.White, p, 0.16f)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
