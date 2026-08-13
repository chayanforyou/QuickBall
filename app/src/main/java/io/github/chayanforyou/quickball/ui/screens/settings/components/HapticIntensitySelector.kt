package io.github.chayanforyou.quickball.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.chayanforyou.quickball.domain.models.HapticIntensity

@Composable
fun HapticIntensitySelector(
    selectedIntensityName: String,
    onIntensitySelected: (HapticIntensity) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val items = HapticIntensity.entries
    val shape = RoundedCornerShape(24.dp)
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha),
                shape = shape
            )
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, intensity ->
                val isSelected = intensity.name == selectedIntensityName
                val segmentShape = when (index) {
                    0 -> RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 4.dp, bottomEnd = 4.dp)
                    items.lastIndex -> RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp, topStart = 4.dp, bottomStart = 4.dp)
                    else -> RoundedCornerShape(4.dp)
                }

                val backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                }

                val textColor = if (enabled) {
                    if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(segmentShape)
                        .background(backgroundColor)
                        .clickable(enabled = enabled) { onIntensitySelected(intensity) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = stringResource(id = intensity.titleRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }

                if (index < items.lastIndex) {
                    val nextIsSelected = items[index + 1].name == selectedIntensityName
                    if (!isSelected && !nextIsSelected) {
                        VerticalDivider(
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)
                        )
                    }
                }
            }
        }
    }
}
