package io.github.chayanforyou.quickball.ui.screens.shortcut.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.MenuConnectorStyle
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.ui.floating.FanLayout
import io.github.chayanforyou.quickball.ui.floating.FloatTouchView
import io.github.chayanforyou.quickball.ui.viewmodels.QuickBallUiState
import io.github.chayanforyou.quickball.utils.getAppIcon
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val MAX_PREVIEW_HEIGHT_DP = 220f

/**
 * Static picture of the expanded radial menu with the current items and appearance settings.
 * Uses the same [FanLayout] as the overlay and is scaled down when it would be too large.
 */
@Composable
fun MenuPreview(
    items: List<QuickBallMenuItem>,
    uiState: QuickBallUiState,
    modifier: Modifier = Modifier
) {
    if (items.size < 2) return

    val context = LocalContext.current
    val buttonSize = uiState.menuSize
    val halfFab = uiState.ballSize / 2f

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Layout in dp, before scaling
        val layout = remember(items.size, buttonSize, uiState.menuRadius, halfFab) {
            FanLayout.compute(
                count = items.size,
                buttonSize = buttonSize,
                radius = uiState.menuRadius,
                halfFab = halfFab,
                density = 1f
            )
        }

        val reach = layout.offsets.maxOf { -it.first } + buttonSize / 2f
        val halfHeight = max(
            layout.offsets.maxOf { abs(it.second) } + buttonSize / 2f,
            max(halfFab, layout.arcRadius)
        )
        val fullWidth = reach + halfFab
        val fullHeight = halfHeight * 2f
        val scale = min(1f, min(maxWidth.value / fullWidth, MAX_PREVIEW_HEIGHT_DP / fullHeight))

        Box(modifier = Modifier.size((fullWidth * scale).dp, (fullHeight * scale).dp)) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawConnectors(
                    layout = layout,
                    uiState = uiState,
                    center = Offset((reach * scale).dp.toPx(), (halfHeight * scale).dp.toPx()),
                    unit = scale.dp.toPx()
                )
            }

            // Menu items
            items.forEachIndexed { index, item ->
                val (offsetX, offsetY) = layout.offsets[index]

                Box(
                    modifier = Modifier
                        .offset(
                            x = ((reach + offsetX - buttonSize / 2f) * scale).dp,
                            y = ((halfHeight + offsetY - buttonSize / 2f) * scale).dp
                        )
                        .size((buttonSize * scale).dp)
                        .background(Color(uiState.menuColor), CircleShape)
                        .circleBorder(uiState.menuBorderWidth * scale, uiState.menuBorderColor),
                    contentAlignment = Alignment.Center
                ) {
                    val iconModifier = Modifier.size((uiState.menuIconSize * scale).dp)
                    val appIcon = if (item.action == MenuAction.LAUNCH_APP && item.packageName != null) {
                        remember(item.packageName) {
                            context.getAppIcon(item.packageName)?.toBitmap()?.asImageBitmap()
                        }
                    } else null

                    if (appIcon != null) {
                        Image(bitmap = appIcon, contentDescription = null, modifier = iconModifier)
                    } else {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            modifier = iconModifier,
                            tint = Color(uiState.menuIconColor)
                        )
                    }
                }
            }

            // Floating ball, shown expanded
            Box(
                modifier = Modifier
                    .offset(
                        x = ((reach - halfFab) * scale).dp,
                        y = ((halfHeight - halfFab) * scale).dp
                    )
                    .size((uiState.ballSize * scale).dp)
                    .background(Color(uiState.ballColor), CircleShape)
                    .circleBorder(uiState.ballBorderWidth * scale, uiState.ballBorderColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu_close),
                    contentDescription = null,
                    modifier = Modifier.size(
                        ((uiState.ballSize - FloatTouchView.ICON_MARGIN_DP) * scale).dp
                    ),
                    tint = Color(uiState.ballIconColor)
                )
            }
        }
    }
}

private fun Modifier.circleBorder(widthDp: Float, color: Int): Modifier {
    return if (widthDp > 0f) border(widthDp.dp, Color(color), CircleShape) else this
}

/**
 * Draws the connectors the same way the overlay does, for a FAB on the right edge.
 *
 * @param unit length of one layout dp in pixels, after scaling
 */
private fun DrawScope.drawConnectors(
    layout: FanLayout,
    uiState: QuickBallUiState,
    center: Offset,
    unit: Float
) {
    val style = MenuConnectorStyle.entries.find { it.name == uiState.menuConnectorStyle }
        ?: MenuConnectorStyle.DOTTED
    if (style == MenuConnectorStyle.NONE) return

    val color = Color(uiState.menuConnectorColor)
    val width = uiState.menuConnectorWidth * unit
    val halfButton = uiState.menuSize / 2f * unit

    val start = if (style == MenuConnectorStyle.ARC) {
        layout.arcRadius * unit
    } else {
        (uiState.ballSize / 2f + FanLayout.CONNECTOR_INSET_DP) * unit
    }

    if (style == MenuConnectorStyle.ARC) {
        drawArc(
            color = color,
            startAngle = 180f - layout.arcEdgeAngle,
            sweepAngle = 90f + layout.arcEdgeAngle,
            useCenter = false,
            topLeft = Offset(center.x - start, center.y - start),
            size = Size(start * 2f, start * 2f),
            style = Stroke(width = width, cap = StrokeCap.Round)
        )
    }

    layout.offsets.forEach { (offsetX, offsetY) ->
        val distance = hypot(offsetX, offsetY)
        if (distance <= 0f) return@forEach

        val direction = Offset(offsetX / distance, offsetY / distance)
        val end = distance * unit - halfButton
        if (end <= start) return@forEach

        if (style == MenuConnectorStyle.DOTTED) {
            val dotRadius = width * FanLayout.DOT_SIZE_RATIO / 2f
            val dotSpacing = width * FanLayout.DOT_SPACING_RATIO

            var position = start
            while (position < end) {
                drawCircle(color, dotRadius, center + direction * position)
                position += dotSpacing
            }
        } else {
            drawLine(
                color = color,
                start = center + direction * start,
                end = center + direction * end,
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }
}
