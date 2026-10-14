package io.github.chayanforyou.quickball.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import io.github.chayanforyou.quickball.domain.models.MenuConnectorStyle
import io.github.chayanforyou.quickball.ui.floating.ConnectorRenderer
import io.github.chayanforyou.quickball.ui.floating.FanLayout
import kotlin.math.hypot

/**
 * Draws the connectors of a resting fan, for a FAB on the right edge, with the same
 * [ConnectorRenderer] the overlay uses.
 *
 * Shared by the shortcut screen preview and the connector style picker.
 *
 * @param center FAB center, in pixels
 * @param unit length of one layout unit in pixels
 * @param strokeWidth line width in pixels; dot size and spacing follow from it
 * @param halfBall ball radius, in layout units
 * @param halfButton menu button radius, in layout units
 */
fun DrawScope.drawConnectors(
    layout: FanLayout,
    style: MenuConnectorStyle,
    color: Color,
    center: Offset,
    unit: Float,
    strokeWidth: Float,
    halfBall: Float,
    halfButton: Float
) {
    if (style == MenuConnectorStyle.NONE) return

    val renderer = ConnectorRenderer(style, color.toArgb(), strokeWidth)
    val isArc = style == MenuConnectorStyle.ARC
    val start = if (isArc) layout.arcRadius * unit else (halfBall + ConnectorRenderer.INSET_DP) * unit

    drawIntoCanvas { composeCanvas ->
        val canvas = composeCanvas.nativeCanvas

        if (isArc) {
            // From straight above the FAB down to the last item
            renderer.drawRing(
                canvas, center.x, center.y, start,
                Math.toRadians(180.0 - layout.arcEdgeAngle),
                Math.toRadians(90.0 + layout.arcEdgeAngle),
                opacity = 1f
            )
        } else if (renderer.hasRing) {
            val (ringStart, ringSweep) = FanLayout.angleRange(layout.offsets)
            renderer.drawRing(canvas, center.x, center.y, start, ringStart, ringSweep, opacity = 1f)
        }

        val bends = FanLayout.curveBends(layout.offsets)

        layout.offsets.forEachIndexed { index, (offsetX, offsetY) ->
            val distance = hypot(offsetX, offsetY)
            if (distance <= 0f) return@forEachIndexed

            renderer.drawConnector(
                canvas = canvas,
                centerX = center.x,
                centerY = center.y,
                directionX = offsetX / distance,
                directionY = offsetY / distance,
                start = start,
                end = (distance - halfButton) * unit,
                opacity = 1f,
                bend = bends[index]
            )
        }
    }
}
