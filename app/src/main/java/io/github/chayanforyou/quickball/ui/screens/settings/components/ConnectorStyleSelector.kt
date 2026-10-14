package io.github.chayanforyou.quickball.ui.screens.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.models.MenuConnectorStyle
import io.github.chayanforyou.quickball.ui.components.drawConnectors
import io.github.chayanforyou.quickball.ui.floating.FanLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TILE_COLUMNS = 3

// Sample fan drawn in each tile, in dp before scaling: 3 items at the default sizes
private const val SAMPLE_ITEM_COUNT = 3
private const val SAMPLE_BALL_SIZE = 45f
private const val SAMPLE_BUTTON_SIZE = 50f
private const val SAMPLE_RADIUS = 90f

private val SampleLayout = FanLayout.compute(
    count = SAMPLE_ITEM_COUNT,
    buttonSize = SAMPLE_BUTTON_SIZE,
    radius = SAMPLE_RADIUS,
    halfFab = SAMPLE_BALL_SIZE / 2f,
    density = 1f
)

private val TileShape = RoundedCornerShape(12.dp)
private val RowPreviewShape = RoundedCornerShape(6.dp)
private val RowPreviewStrokeWidth = 1.dp
private val SampleHeight = 72.dp
private val SampleStrokeWidth = 1.5.dp

/**
 * Settings row: "Style" on the left, a picture of the selected style on the right.
 * Tapping it opens [ConnectorStyleBottomSheet] with every style.
 */
@Composable
fun ConnectorStyleRow(
    title: String,
    selectedStyleName: String,
    onReset: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val selectedStyle = MenuConnectorStyle.entries.find { it.name == selectedStyleName }
        ?: MenuConnectorStyle.DOTTED

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title with the selected style name under it
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(selectedStyle.titleRes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                    contentDescription = stringResource(R.string.reset_to_default),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            ConnectorStyleSample(
                style = selectedStyle,
                connectorColor = colors.onSurface,
                itemColor = colors.onSurfaceVariant.copy(alpha = 0.55f),
                ballColor = colors.onSurface,
                strokeWidth = RowPreviewStrokeWidth,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RowPreviewShape)
                    .background(colors.surfaceContainerHighest)
                    .border(1.dp, colors.outlineVariant, RowPreviewShape)
                    .clickable(onClick = onClick)
                    .padding(3.dp)
            )
        }
    }
}

/**
 * Bottom sheet with every connector style as a picture tile. Picking one closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectorStyleBottomSheet(
    title: String,
    selectedStyleName: String,
    onStyleSelected: (MenuConnectorStyle) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Opens half way; drag up or scroll to see every style
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Title stays put; the tiles scroll when they do not fit, such as on small
            // screens or in landscape
            ConnectorStyleSelector(
                selectedStyleName = selectedStyleName,
                onStyleSelected = { style ->
                    onStyleSelected(style)
                    onDismissRequest()
                },
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}

/**
 * Grid of tiles, each showing a small picture of a connector style with its name.
 */
@Composable
fun ConnectorStyleSelector(
    selectedStyleName: String,
    onStyleSelected: (MenuConnectorStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuConnectorStyle.entries.chunked(TILE_COLUMNS).forEach { rowStyles ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowStyles.forEach { style ->
                    ConnectorStyleTile(
                        style = style,
                        isSelected = style.name == selectedStyleName,
                        onClick = { onStyleSelected(style) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectorStyleTile(
    style: MenuConnectorStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (isSelected) colors.primary else colors.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(TileShape)
            .background(if (isSelected) colors.primary.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.outlineVariant,
                shape = TileShape
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConnectorStyleSample(
            style = style,
            connectorColor = accent,
            itemColor = colors.onSurfaceVariant.copy(alpha = 0.55f),
            ballColor = accent,
            modifier = Modifier
                .fillMaxWidth()
                .height(SampleHeight)
        )

        Text(
            text = stringResource(style.titleRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/**
 * The sample fan scaled to fit the tile, ball on the right as in the overlay.
 */
@Composable
fun ConnectorStyleSample(
    style: MenuConnectorStyle,
    connectorColor: Color,
    itemColor: Color,
    ballColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = SampleStrokeWidth
) {
    Canvas(modifier = modifier) {
        val halfBall = SAMPLE_BALL_SIZE / 2f
        val halfButton = SAMPLE_BUTTON_SIZE / 2f

        val reach = SampleLayout.offsets.maxOf { -it.first } + halfButton
        val halfHeight = max(SampleLayout.offsets.maxOf { abs(it.second) } + halfButton, halfBall)
        val fullWidth = reach + halfBall
        val unit = min(size.width / fullWidth, size.height / (halfHeight * 2f))

        val center = Offset(
            x = (size.width - fullWidth * unit) / 2f + reach * unit,
            y = size.height / 2f
        )

        drawConnectors(
            layout = SampleLayout,
            style = style,
            color = connectorColor,
            center = center,
            unit = unit,
            strokeWidth = strokeWidth.toPx(),
            halfBall = halfBall,
            halfButton = halfButton
        )

        SampleLayout.offsets.forEach { (offsetX, offsetY) ->
            drawCircle(
                color = itemColor,
                radius = halfButton * unit,
                center = center + Offset(offsetX * unit, offsetY * unit)
            )
        }

        drawCircle(color = ballColor, radius = halfBall * unit, center = center)
    }
}
