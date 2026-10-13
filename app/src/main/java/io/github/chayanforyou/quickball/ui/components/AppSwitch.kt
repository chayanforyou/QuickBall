package io.github.chayanforyou.quickball.ui.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

private const val SWITCH_SCALE = 0.8f

/**
 * Material switch drawn smaller than the default, taking only the space of its scaled size.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val width = (placeable.width * SWITCH_SCALE).roundToInt()
                val height = (placeable.height * SWITCH_SCALE).roundToInt()
                layout(width, height) {
                    placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2)
                }
            }
            .scale(SWITCH_SCALE)
    )
}
