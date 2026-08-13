package io.github.chayanforyou.quickball.ui.screens.settings.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.chayanforyou.quickball.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.graphics.toColorInt

/**
 * A modern Jetpack Compose Color Picker Dialog matching the Skydoves design:
 * - Manual Hex text input (#AARRGGBB) with two-way synchronization
 * - Color preview card
 * - Circular HSV Color Wheel
 * - Alpha slider
 * - Brightness slider
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color = Color.White,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    val currentColor = remember(hue, saturation, value, alpha) {
        val argb = AndroidColor.HSVToColor((alpha * 255).toInt(), floatArrayOf(hue, saturation, value))
        Color(argb)
    }

    var hexText by remember(currentColor) {
        mutableStateOf(String.format("#%08X", currentColor.toArgb()))
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.choose_color))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Manual Hex Text Input
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { newValue ->
                        hexText = newValue
                        val cleanHex = if (newValue.startsWith("#")) newValue else "#$newValue"
                        if (cleanHex.length == 9) {
                            try {
                                val parsedArgb = cleanHex.toColorInt()
                                val newColor = Color(parsedArgb)
                                val newHsv = FloatArray(3)
                                AndroidColor.colorToHSV(parsedArgb, newHsv)
                                hue = newHsv[0]
                                saturation = newHsv[1]
                                value = newHsv[2]
                                alpha = newColor.alpha
                            } catch (_: Exception) {
                                // Ignore invalid hex while typing
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.hex_color_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Color Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                )

                // 3. Circular HSV Wheel
                HsvColorWheel(
                    hue = hue,
                    saturation = saturation,
                    onHsvChanged = { newHue, newSaturation ->
                        hue = newHue
                        saturation = newSaturation
                    },
                    modifier = Modifier.size(200.dp)
                )

                // 4. Alpha Slider
                AlphaSliderBar(
                    color = currentColor.copy(alpha = 1f),
                    alpha = alpha,
                    onAlphaChanged = { alpha = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. Brightness Slider
                BrightnessSliderBar(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onValueChanged = { value = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text(stringResource(R.string.select))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onDismissRequest: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    ColorPickerDialog(
        initialColor = Color(initialColor),
        onDismissRequest = onDismissRequest,
        onColorSelected = { selectedColor ->
            onColorSelected(selectedColor.toArgb())
        }
    )
}

@Composable
private fun HsvColorWheel(
    hue: Float,
    saturation: Float,
    onHsvChanged: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                fun updateFromOffset(offset: Offset) {
                    val radius = size.width / 2f
                    val center = Offset(radius, radius)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)

                    val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                    val newSat = (distance / radius).coerceIn(0f, 1f)
                    onHsvChanged(angle, newSat)
                }

                detectTapGestures { offset ->
                    updateFromOffset(offset)
                }
            }
            .pointerInput(Unit) {
                fun updateFromOffset(offset: Offset) {
                    val radius = size.width / 2f
                    val center = Offset(radius, radius)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)

                    val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                    val newSat = (distance / radius).coerceIn(0f, 1f)
                    onHsvChanged(angle, newSat)
                }

                detectDragGestures { change, _ ->
                    change.consume()
                    updateFromOffset(change.position)
                }
            }
    ) {
        val radius = size.width / 2f
        val center = Offset(radius, radius)

        // Draw Hue Sweep Gradient
        val sweepShader = SweepGradientShader(center, colors)
        drawCircle(
            brush = ShaderBrush(sweepShader),
            radius = radius,
            center = center
        )

        // Draw Saturation Radial Gradient (White at center to Transparent at edge)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Draw Selector Ring/Dot
        val selectorDistance = saturation * radius
        val angleRad = Math.toRadians(hue.toDouble())
        val selectorX = center.x + (selectorDistance * cos(angleRad)).toFloat()
        val selectorY = center.y + (selectorDistance * sin(angleRad)).toFloat()
        val selectorOffset = Offset(selectorX, selectorY)

        drawCircle(
            color = Color.White,
            radius = 10.dp.toPx(),
            center = selectorOffset
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = 10.dp.toPx(),
            center = selectorOffset,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun AlphaSliderBar(
    color: Color,
    alpha: Float,
    onAlphaChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(color.copy(alpha = 0f), color)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = alpha,
            onValueChange = onAlphaChanged,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BrightnessSliderBar(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val fullBrightColor = remember(hue, saturation) {
        val argb = AndroidColor.HSVToColor(255, floatArrayOf(hue, saturation, 1f))
        Color(argb)
    }

    Box(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, fullBrightColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = value,
            onValueChange = onValueChanged,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
