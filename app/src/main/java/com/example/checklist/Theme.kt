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
    0xFF1E7BE0, 0xFF00897B, 0xFF3FA34D, 0xFFE8710A,
    0xFFD81B60, 0xFFE53935, 0xFF00ACC1, 0xFF7CB342,
    0xFFFDD835, 0xFF546E7A, 0xFF6D4C41, 0xFF827717
).map { it.toInt() }

/* Purple is not used anywhere in the app: hues from 245 to 325 degrees are skipped. */
const val PURPLE_FROM = 245f
const val PURPLE_TO = 325f

fun hueOf(argb: Int): Float {
    val r = (argb shr 16 and 255) / 255f; val g = (argb shr 8 and 255) / 255f; val b = (argb and 255) / 255f
    val mx = maxOf(r, g, b); val mn = minOf(r, g, b); val d = mx - mn
    if (d < 0.001f) return 0f
    val h = when (mx) { r -> ((g - b) / d) % 6f; g -> (b - r) / d + 2f; else -> (r - g) / d + 4f } * 60f
    return (h + 360f) % 360f
}

fun isPurple(argb: Int): Boolean {
    val r = (argb shr 16 and 255); val g = (argb shr 8 and 255); val b = (argb and 255)
    val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
    if (mx == 0 || (mx - mn).toFloat() / mx < 0.15f) return false   // greys are fine
    return hueOf(argb) in PURPLE_FROM..PURPLE_TO
}

/** Colors saved by older versions that were purple become the default blue. */
fun dePurple(argb: Int): Int = if (isPurple(argb)) Palette[0] else argb

/** The custom-color slider runs over every hue except purple. */
const val SLIDER_MAX = 360f - (PURPLE_TO - PURPLE_FROM)
fun sliderToHue(s: Float): Float = if (s < PURPLE_FROM) s else s + (PURPLE_TO - PURPLE_FROM)
fun hueToSlider(h: Float): Float = if (h < PURPLE_FROM) h else if (h >= PURPLE_TO) h - (PURPLE_TO - PURPLE_FROM) else PURPLE_FROM

class Accent(val color: Color, val on: Color)

/** A calmer version of an accent for big buttons, so they don't shout. */
fun Accent.softer(): Accent { val c = lerp(color, Color.Black, 0.3f); return Accent(c, onColor(c)) }

private fun onColor(c: Color) = if (c.luminance() > 0.5f) Color(0xFF15161A) else Color.White

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
        val bg = if (pureBlack) Color.Black else lerp(Color(0xFF0F1012), p, 0.10f)
        val card = if (pureBlack) Color(0xFF121212) else lerp(Color(0xFF1B1C20), p, 0.10f)
        val soft = lerp(card, p, 0.35f)
        darkColorScheme(
            primary = accent, onPrimary = onColor(accent),
            primaryContainer = soft, onPrimaryContainer = Color(0xFFEFF0F3),
            secondary = lerp(accent, Color(0xFFB0B3B8), 0.5f), onSecondary = Color(0xFF15161A),
            secondaryContainer = lerp(card, p, 0.25f), onSecondaryContainer = Color(0xFFEFF0F3),
            tertiary = lerp(accent, Color(0xFFB0B3B8), 0.5f), onTertiary = Color(0xFF15161A),
            tertiaryContainer = lerp(card, p, 0.25f), onTertiaryContainer = Color(0xFFEFF0F3),
            background = bg, onBackground = Color(0xFFEFF0F3),
            surface = card, onSurface = Color(0xFFEFF0F3), surfaceTint = accent,
            surfaceVariant = lerp(card, Color.White, 0.08f), onSurfaceVariant = Color(0xFFB2B5BC),
            surfaceContainerLowest = bg, surfaceContainerLow = lerp(card, Color.White, 0.02f),
            surfaceContainer = lerp(card, Color.White, 0.05f), surfaceContainerHigh = lerp(card, Color.White, 0.08f),
            surfaceContainerHighest = lerp(card, Color.White, 0.11f),
            outline = Color(0xFF8A8D93), outlineVariant = lerp(card, Color.White, 0.14f),
            inversePrimary = p, inverseSurface = Color(0xFFE3E4E8), inverseOnSurface = Color(0xFF2F3033)
        )
    } else lightColorScheme(
        primary = p, onPrimary = onColor(p),
        primaryContainer = lerp(Color.White, p, 0.22f), onPrimaryContainer = lerp(p, Color.Black, 0.65f),
        secondary = lerp(p, Color(0xFF666A70), 0.6f), onSecondary = Color.White,
        secondaryContainer = lerp(Color.White, p, 0.14f), onSecondaryContainer = lerp(p, Color.Black, 0.7f),
        tertiary = lerp(p, Color(0xFF666A70), 0.6f), onTertiary = Color.White,
        tertiaryContainer = lerp(Color.White, p, 0.14f), onTertiaryContainer = lerp(p, Color.Black, 0.7f),
        background = lerp(Color.White, p, 0.07f), onBackground = Color(0xFF1B1C20),
        surface = Color.White, onSurface = Color(0xFF1B1C20), surfaceTint = p,
        surfaceVariant = lerp(Color.White, p, 0.14f), onSurfaceVariant = Color(0xFF686B72),
        surfaceContainerLowest = Color.White, surfaceContainerLow = lerp(Color.White, p, 0.03f),
        surfaceContainer = lerp(Color.White, p, 0.06f), surfaceContainerHigh = lerp(Color.White, p, 0.09f),
        surfaceContainerHighest = lerp(Color.White, p, 0.12f),
        outline = lerp(Color(0xFF777A80), p, 0.2f), outlineVariant = lerp(Color.White, p, 0.16f),
        inversePrimary = lerp(p, Color.White, 0.5f), inverseSurface = Color(0xFF2F3033), inverseOnSurface = Color(0xFFF0F1F3)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
