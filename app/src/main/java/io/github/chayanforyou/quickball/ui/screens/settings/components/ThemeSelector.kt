package io.github.chayanforyou.quickball.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.models.MenuConnectorStyle
import io.github.chayanforyou.quickball.domain.models.QuickBallTheme

private const val TILE_COLUMNS = 3

private val TileShape = RoundedCornerShape(12.dp)
private val RowPreviewShape = RoundedCornerShape(6.dp)
private val RowPreviewStrokeWidth = 1.dp
private val SampleHeight = 72.dp

/**
 * Colors a theme picture is drawn with: a theme's own, or the user's current ones.
 */
data class ThemePreviewColors(
    val ballColor: Int,
    val ballIconColor: Int,
    val ballBorderColor: Int?,
    val menuColor: Int,
    val menuIconColor: Int,
    val menuBorderColor: Int?,
    val connectorColor: Int
)

private fun QuickBallTheme.previewColors() = ThemePreviewColors(
    ballColor = ballColor,
    ballIconColor = ballIconColor,
    ballBorderColor = ballBorderColor.takeIf { isBorderEnabled },
    menuColor = menuColor,
    menuIconColor = menuIconColor,
    menuBorderColor = menuBorderColor.takeIf { isBorderEnabled },
    connectorColor = connectorColor
)

/**
 * Settings row: "Theme" on the left with the current theme name, a picture of the current
 * colors on the right.
 * Tapping it opens [ThemeBottomSheet] with every theme.
 */
@Composable
fun ThemeRow(
    title: String,
    selectedTheme: QuickBallTheme?,
    currentColors: ThemePreviewColors,
    connectorStyleName: String,
    onReset: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title with the current theme name under it, or "Custom" once colors were changed by hand
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
                text = selectedTheme?.title ?: stringResource(R.string.theme_custom),
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
            ThemeSample(
                colors = currentColors,
                connectorStyleName = connectorStyleName,
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
 * Bottom sheet with every theme as a picture tile. Picking one applies it and closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeBottomSheet(
    title: String,
    selectedTheme: QuickBallTheme?,
    connectorStyleName: String,
    onThemeSelected: (QuickBallTheme) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Opens fully expanded; scroll when the tiles do not fit
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickBallTheme.entries.chunked(TILE_COLUMNS).forEach { rowThemes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowThemes.forEach { theme ->
                            ThemeTile(
                                theme = theme,
                                connectorStyleName = connectorStyleName,
                                isSelected = theme == selectedTheme,
                                onClick = {
                                    onThemeSelected(theme)
                                    onDismissRequest()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Keep tiles in a short last row the same width as the rest
                        repeat(TILE_COLUMNS - rowThemes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(
    theme: QuickBallTheme,
    connectorStyleName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

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
        ThemeSample(
            colors = theme.previewColors(),
            connectorStyleName = connectorStyleName,
            modifier = Modifier
                .fillMaxWidth()
                .height(SampleHeight)
        )

        Text(
            text = theme.title,
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
 * The sample fan in the given colors, drawn with the user's connector style.
 */
@Composable
private fun ThemeSample(
    colors: ThemePreviewColors,
    connectorStyleName: String,
    modifier: Modifier = Modifier
) {
    val style = MenuConnectorStyle.entries.find { it.name == connectorStyleName }
        ?: MenuConnectorStyle.DOTTED

    ConnectorStyleSample(
        style = style,
        connectorColor = Color(colors.connectorColor),
        itemColor = Color(colors.menuColor),
        ballColor = Color(colors.ballColor),
        strokeWidth = RowPreviewStrokeWidth,
        itemBorderColor = colors.menuBorderColor?.let { Color(it) },
        ballBorderColor = colors.ballBorderColor?.let { Color(it) },
        itemIconColor = Color(colors.menuIconColor),
        ballIconColor = Color(colors.ballIconColor),
        modifier = modifier
    )
}
