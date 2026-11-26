package com.example.checklist

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Android hides the keyboard on "back" but leaves the text field focused, so its cursor keeps blinking.
 * Call this once in a screen with text fields: when the keyboard goes away, the focus goes with it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClearFocusWhenKeyboardHides() {
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    var wasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (wasVisible && !imeVisible) focusManager.clearFocus()
        wasVisible = imeVisible
    }
}
