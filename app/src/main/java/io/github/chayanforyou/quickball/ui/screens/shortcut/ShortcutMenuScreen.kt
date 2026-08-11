package io.github.chayanforyou.quickball.ui.screens.shortcut

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.ui.screens.shortcut.components.DraggableItem
import io.github.chayanforyou.quickball.ui.screens.shortcut.components.draggableContainer
import io.github.chayanforyou.quickball.ui.screens.shortcut.components.rememberDragDropState
import io.github.chayanforyou.quickball.ui.theme.AppCardDefaults
import io.github.chayanforyou.quickball.ui.viewmodels.QuickBallViewModel
import io.github.chayanforyou.quickball.utils.getAppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutMenuScreen(
    onNavigateToSelection: (targetIndex: Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickBallViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val menuItems = uiState.selectedMenuItems

    var itemsList by remember(menuItems) { mutableStateOf(menuItems) }
    val listState = rememberLazyListState()

    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        val updated = itemsList.toMutableList()
        val movedItem = updated.removeAt(fromIndex)
        updated.add(toIndex, movedItem)
        itemsList = updated
        viewModel.updateMenuItems(updated)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shortcuts_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.menu_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val handleWidthPx = with(LocalDensity.current) { 72.dp.toPx() }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .draggableContainer(dragDropState, handleWidthPx),
                    state = listState,
                    userScrollEnabled = false
                ) {
                    itemsIndexed(
                        items = itemsList,
                        key = { index, item -> "${item.action.name}_${item.packageName ?: ""}_$index" }
                    ) { index, item ->
                        DraggableItem(
                            dragDropState = dragDropState,
                            index = index
                        ) { isDragging ->
                            ShortcutItemRow(
                                item = item,
                                isDragging = isDragging,
                                onClick = { onNavigateToSelection(index) }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(start = 4.dp, top = 24.dp, end = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "• ${stringResource(R.string.select_shortcut_description)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Text(
                    text = "• ${stringResource(R.string.reorder_shortcut_description)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun ShortcutItemRow(
    item: QuickBallMenuItem,
    isDragging: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = stringResource(R.string.drag_handle_description),
            tint = if (isDragging) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(24.dp))

        val appIcon = if (item.action == MenuAction.LAUNCH_APP && item.packageName != null) {
            context.getAppIcon(item.packageName)
        } else null

        if (appIcon != null) {
            Image(
                bitmap = appIcon.toBitmap().asImageBitmap(),
                contentDescription = item.getTitle(context),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.getTitle(context),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
