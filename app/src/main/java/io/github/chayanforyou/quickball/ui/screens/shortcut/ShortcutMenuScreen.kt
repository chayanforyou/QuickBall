package io.github.chayanforyou.quickball.ui.screens.shortcut

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.AppDefaults
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.ui.screens.shortcut.components.DraggableItem
import io.github.chayanforyou.quickball.ui.screens.shortcut.components.MenuPreview
import io.github.chayanforyou.quickball.ui.screens.shortcut.components.draggableContainer
import io.github.chayanforyou.quickball.ui.screens.shortcut.components.rememberDragDropState
import io.github.chayanforyou.quickball.ui.theme.AppCardDefaults
import io.github.chayanforyou.quickball.ui.viewmodels.QuickBallViewModel
import io.github.chayanforyou.quickball.utils.getAppIcon

// Fixed row height, so the list can size itself inside the scrolling page
private val ShortcutRowHeight = 52.dp
private val ListVerticalPadding = 8.dp

// Width of the strip at the start of a row that starts a reorder drag
private val DragHandleWidth = 56.dp

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
    val latestItems by rememberUpdatedState(menuItems)

    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    val canRemove = menuItems.size > AppDefaults.MENU_ITEMS_MIN
    val canAdd = menuItems.size < AppDefaults.MENU_ITEMS_MAX

    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        val updated = latestItems.toMutableList()
        if (fromIndex in updated.indices && toIndex in updated.indices) {
            updated.add(toIndex, updated.removeAt(fromIndex))
            viewModel.updateMenuItems(updated)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
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
                },
                actions = {
                    // Current count out of the allowed maximum
                    Text(
                        text = "${menuItems.size}/${AppDefaults.MENU_ITEMS_MAX}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 20.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview Header & Card
            // SectionHeader(title = stringResource(R.string.preview_title))

            // Card(
            //     modifier = Modifier.fillMaxWidth(),
            //     shape = RoundedCornerShape(16.dp),
            //     colors = AppCardDefaults.cardColors()
            // ) {
            //     MenuPreview(
            //         items = menuItems,
            //         uiState = uiState,
            //         modifier = Modifier.padding(16.dp)
            //     )
            // }

            // Shortcuts Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                val handleWidthPx = with(LocalDensity.current) { DragHandleWidth.toPx() }

                LazyColumn(
                    modifier = Modifier
                        .height(ShortcutRowHeight * menuItems.size + ListVerticalPadding * 2)
                        .padding(vertical = ListVerticalPadding)
                        .draggableContainer(
                            dragDropState = dragDropState,
                            handleWidthPx = handleWidthPx,
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        ),
                    state = listState,
                    userScrollEnabled = false
                ) {
                    itemsIndexed(
                        items = menuItems,
                        key = { _, item -> item.packageName ?: item.action.name }
                    ) { index, item ->
                        DraggableItem(
                            dragDropState = dragDropState,
                            index = index
                        ) { isDragging ->
                            ShortcutItemRow(
                                item = item,
                                isDragging = isDragging,
                                canRemove = canRemove,
                                onClick = { onNavigateToSelection(index) },
                                onRemove = { viewModel.removeMenuItem(index) }
                            )
                        }
                    }
                }
            }

            // Add Shortcut Button
            Button(
                onClick = { onNavigateToSelection(menuItems.size) },
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_shortcut),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Hints
            Column(
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    stringResource(R.string.select_shortcut_description),
                    stringResource(R.string.reorder_shortcut_description),
                    stringResource(
                        R.string.shortcut_limit_description,
                        AppDefaults.MENU_ITEMS_MIN,
                        AppDefaults.MENU_ITEMS_MAX
                    )
                ).forEach { hint ->
                    Text(
                        text = "• $hint",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShortcutItemRow(
    item: QuickBallMenuItem,
    isDragging: Boolean,
    canRemove: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ShortcutRowHeight)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
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

        Spacer(modifier = Modifier.width(16.dp))

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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(
                imageVector = Icons.Outlined.RemoveCircleOutline,
                contentDescription = stringResource(R.string.remove_shortcut),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (canRemove) 0.6f else 0.2f
                )
            )
        }
    }
}
