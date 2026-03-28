package com.mappingsolution.ui.library.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ColorPickerDialog(
    initialHex: String,
    onConfirm: (hex: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(initialHex) { hexToHsv(initialHex) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var bri by remember { mutableFloatStateOf(initialHsv[2]) }

    val selectedColor = Color.hsv(hue, sat, bri)
    val initialColor = remember(initialHex) { parseHex(initialHex) }

    // Hue rainbow stops
    val hueGradient = remember {
        listOf(
            Color.Red,
            Color(1f, 0.5f, 0f),
            Color.Yellow,
            Color(0f, 1f, 0f),
            Color.Cyan,
            Color.Blue,
            Color(0.5f, 0f, 1f),
            Color.Magenta,
            Color.Red,
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Pick a color", style = MaterialTheme.typography.titleMedium)

                // ── SV gradient box ──────────────────────────────────────────
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                fun update(o: Offset) {
                                    sat = (o.x / size.width).coerceIn(0f, 1f)
                                    bri = 1f - (o.y / size.height).coerceIn(0f, 1f)
                                }
                                update(down.position)
                                drag(down.id) { change -> update(change.position) }
                            }
                        },
                ) {
                    // White → full hue (horizontal)
                    drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
                    // Transparent → Black (vertical overlay)
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    // Crosshair indicator
                    val cx = sat * size.width
                    val cy = (1f - bri) * size.height
                    drawCircle(Color.White, 12f, Offset(cx, cy), style = Stroke(width = 3f))
                    drawCircle(Color.Black.copy(alpha = 0.4f), 12f, Offset(cx, cy), style = Stroke(width = 1f))
                }

                // ── Hue slider ───────────────────────────────────────────────
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                fun update(o: Offset) {
                                    hue = (o.x / size.width * 360f).coerceIn(0f, 360f)
                                }
                                update(down.position)
                                drag(down.id) { change -> update(change.position) }
                            }
                        },
                ) {
                    drawRect(Brush.horizontalGradient(hueGradient))
                    val thumbX = hue / 360f * size.width
                    drawCircle(Color.White, 12f, Offset(thumbX, size.height / 2f), style = Stroke(width = 3f))
                    drawCircle(Color.Black.copy(alpha = 0.3f), 12f, Offset(thumbX, size.height / 2f), style = Stroke(width = 1f))
                }

                // ── Before / After preview ───────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Before",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .drawBehind { drawCircle(initialColor) }
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .drawBehind { drawCircle(selectedColor) }
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    Text(
                        "After",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        hsvToHex(hue, sat, bri),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }

                // ── Buttons ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(hsvToHex(hue, sat, bri)) }) { Text("Select") }
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

internal fun parseHex(hex: String): Color {
    return try {
        val argb = android.graphics.Color.parseColor(hex)
        Color(argb)
    } catch (_: Exception) {
        Color(0xFF2196F3)
    }
}

internal fun hexToHsv(hex: String): FloatArray {
    return try {
        val argb = android.graphics.Color.parseColor(hex)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv
    } catch (_: Exception) {
        floatArrayOf(210f, 0.86f, 0.95f)
    }
}

internal fun hsvToHex(hue: Float, sat: Float, bri: Float): String {
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri))
    return String.format(
        "#FF%02X%02X%02X",
        android.graphics.Color.red(argb),
        android.graphics.Color.green(argb),
        android.graphics.Color.blue(argb),
    )
}
