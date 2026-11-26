package com.example.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Big preset swatches plus a rainbow slider for any color you like. Changes apply immediately. */
@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, columns: Int = 4) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Palette.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { argb ->
                    val c = Color(argb)
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(c)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable(role = Role.RadioButton, onClickLabel = "Choose color") { onSelect(argb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (argb == selected) Icon(
                            painterResource(R.drawable.ic_check), "Selected",
                            tint = if (c.luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Custom color", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(selected, it) }
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(9.dp)).background(
                    Brush.horizontalGradient((0..360 step 30).map {
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 0.75f, 0.85f)))
                    })
                )
            )
            Slider(
                value = hsv[0], valueRange = 0f..360f,
                onValueChange = { onSelect(android.graphics.Color.HSVToColor(floatArrayOf(it, 0.75f, 0.85f))) },
                colors = SliderDefaults.colors(
                    thumbColor = Color(selected),
                    activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun ColorPickerDialog(title: String, selected: Int, onSelect: (Int) -> Unit, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { ColorPicker(selected, onSelect) },
        confirmButton = {
            Button(onClick = onDone, modifier = Modifier.height(52.dp)) { Text("Done", fontSize = 18.sp) }
        }
    )
}
