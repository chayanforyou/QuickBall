package io.github.chayanforyou.quickball.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.core.QuickBallService
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.PillGesture
import io.github.chayanforyou.quickball.ui.screens.settings.components.ColorPickerDialog
import io.github.chayanforyou.quickball.ui.screens.settings.components.ColorSettingRow
import io.github.chayanforyou.quickball.ui.screens.settings.components.GestureActionBottomSheet
import io.github.chayanforyou.quickball.ui.screens.settings.components.GestureSettingRow
import io.github.chayanforyou.quickball.ui.screens.settings.components.HapticIntensitySelector
import io.github.chayanforyou.quickball.ui.screens.settings.components.SliderSettingItem
import io.github.chayanforyou.quickball.ui.theme.AppCardDefaults
import io.github.chayanforyou.quickball.ui.viewmodels.QuickBallViewModel
import io.github.chayanforyou.quickball.utils.performHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickBallViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val ballSize = uiState.ballSize
    val ballColor = uiState.ballColor
    val ballIconColor = uiState.ballIconColor
    val menuSize = uiState.menuSize
    val menuIconSize = uiState.menuIconSize
    val menuRadius = uiState.menuRadius
    val menuColor = uiState.menuColor
    val menuIconColor = uiState.menuIconColor
    val toastBgColor = uiState.toastBgColor
    val toastFgColor = uiState.toastFgColor
    val pillColor = uiState.pillColor
    val pillHeight = uiState.pillHeight
    val pillThickness = uiState.pillThickness
    val pillTouchWidth = uiState.pillTouchWidth
    val isGestureEnabled = uiState.isGestureEnabled
    val doubleTapAction = uiState.doubleTapAction
    val tripleTapAction = uiState.tripleTapAction
    val longPressAction = uiState.longPressAction
    val swipeUpAction = uiState.swipeUpAction
    val swipeDownAction = uiState.swipeDownAction
    val isHapticFeedbackEnabled = uiState.isHapticFeedbackEnabled
    val hapticIntensity = uiState.hapticIntensity

    var showColorDialog by remember { mutableStateOf(false) }
    var showIconColorDialog by remember { mutableStateOf(false) }
    var showMenuColorDialog by remember { mutableStateOf(false) }
    var showMenuIconColorDialog by remember { mutableStateOf(false) }
    var showPillColorDialog by remember { mutableStateOf(false) }
    var showToastBgColorDialog by remember { mutableStateOf(false) }
    var showToastFgColorDialog by remember { mutableStateOf(false) }
    var activeGestureBottomSheet by remember { mutableStateOf<PillGesture?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.advanced_settings_title),
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ball Settings Header & Card
            Text(
                text = stringResource(R.string.ball_header_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Ball Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.ball_color_title),
                        color = ballColor,
                        onReset = {
                            viewModel.resetBallColor()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                        },
                        onClick = { showColorDialog = true }
                    )

                    // Ball Icon Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.ball_icon_color_title),
                        color = ballIconColor,
                        onReset = {
                            viewModel.resetBallIconColor()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                        },
                        onClick = { showIconColorDialog = true }
                    )

                    // Ball Size Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.ball_size_title),
                        value = ballSize,
                        valueRange = 40f..60f,
                        steps = 19,
                        onValueChange = { value ->
                            viewModel.setBallSize(value)
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                        },
                        onReset = {
                            viewModel.resetBallSize()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                        }
                    )
                }
            }

            // Menu Settings Header & Card
            Text(
                text = stringResource(R.string.menu_header_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Menu Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.menu_color_title),
                        color = menuColor,
                        onReset = { viewModel.resetMenuColor() },
                        onClick = { showMenuColorDialog = true }
                    )

                    // Menu Icon Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.menu_icon_color_title),
                        color = menuIconColor,
                        onReset = { viewModel.resetMenuIconColor() },
                        onClick = { showMenuIconColorDialog = true }
                    )

                    // Menu Size Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.menu_size_title),
                        value = menuSize,
                        valueRange = 40f..60f,
                        steps = 19,
                        onValueChange = { viewModel.setMenuSize(it) },
                        onReset = { viewModel.resetMenuSize() }
                    )

                    // Menu Icon Size Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.menu_icon_size_title),
                        value = menuIconSize,
                        valueRange = 15f..35f,
                        steps = 19,
                        onValueChange = { viewModel.setMenuIconSize(it) },
                        onReset = { viewModel.resetMenuIconSize() }
                    )

                    // Menu Radius Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.menu_radius_title),
                        value = menuRadius,
                        valueRange = 60f..100f,
                        steps = 39,
                        onValueChange = { viewModel.setMenuRadius(it) },
                        onReset = { viewModel.resetMenuRadius() }
                    )
                }
            }

            // Pill Settings Header & Card
            Text(
                text = stringResource(R.string.pill_header_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Pill Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.pill_color_title),
                        color = pillColor,
                        onReset = {
                            viewModel.resetPillColor()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                        },
                        onClick = { showPillColorDialog = true }
                    )

                    // Pill Height Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.pill_height_title),
                        value = pillHeight,
                        valueRange = 30f..80f,
                        steps = 49,
                        onValueChange = { value ->
                            viewModel.setPillHeight(value)
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                        },
                        onReset = {
                            viewModel.resetPillHeight()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                        }
                    )

                    // Pill Thickness Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.pill_thickness_title),
                        value = pillThickness,
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChange = { value ->
                            viewModel.setPillThickness(value)
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                        },
                        onReset = {
                            viewModel.resetPillThickness()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                        }
                    )

                    // Touch Area Width Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.pill_touch_width_title),
                        value = pillTouchWidth,
                        valueRange = 15f..50f,
                        steps = 34,
                        onValueChange = { value ->
                            viewModel.setPillTouchWidth(value)
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                        },
                        onReset = {
                            viewModel.resetPillTouchWidth()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                        }
                    )
                }
            }

            // Toast Settings Header & Card
            Text(
                text = stringResource(R.string.toast_header_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Toast Background Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.toast_bg_color_title),
                        color = toastBgColor,
                        onReset = { viewModel.resetToastBgColor() },
                        onClick = { showToastBgColorDialog = true }
                    )

                    // Toast Foreground Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.toast_fg_color_title),
                        color = toastFgColor,
                        onReset = { viewModel.resetToastFgColor() },
                        onClick = { showToastFgColorDialog = true }
                    )
                }
            }

            // Gesture Settings Header
            Text(
                text = stringResource(R.string.gesture_header_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
            )

            // Gesture Requirement Notice Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.gesture_requirement_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Enable Gestures Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.enable_gestures_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = stringResource(R.string.enable_gestures_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isGestureEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setGestureEnabled(enabled)
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Double tap
                    GestureSettingRow(
                        title = stringResource(R.string.double_tap_title),
                        actionName = doubleTapAction,
                        enabled = isGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.DOUBLE_TAP }
                    )

                    // Triple tap
                    GestureSettingRow(
                        title = stringResource(R.string.triple_tap_title),
                        actionName = tripleTapAction,
                        enabled = isGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.TRIPLE_TAP }
                    )

                    // Long press
                    GestureSettingRow(
                        title = stringResource(R.string.long_press_title),
                        actionName = longPressAction,
                        enabled = isGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.LONG_PRESS }
                    )

                    // Swipe up
                    GestureSettingRow(
                        title = stringResource(R.string.swipe_up_title),
                        actionName = swipeUpAction,
                        enabled = isGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.SWIPE_UP }
                    )

                    // Swipe down
                    GestureSettingRow(
                        title = stringResource(R.string.swipe_down_title),
                        actionName = swipeDownAction,
                        enabled = isGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.SWIPE_DOWN }
                    )
                }
            }

            // Haptics Section
            Text(
                text = stringResource(R.string.haptics_header_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vibration_feedback_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = stringResource(R.string.vibration_feedback_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isHapticFeedbackEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setHapticFeedbackEnabled(enabled)
                                if (enabled) context.performHapticFeedback()
                            }
                        )
                    }

                    HapticIntensitySelector(
                        selectedIntensityName = hapticIntensity,
                        enabled = isHapticFeedbackEnabled,
                        onIntensitySelected = { intensity ->
                            viewModel.setHapticIntensity(intensity.name)
                            context.performHapticFeedback()
                        }
                    )
                }
            }
        }
    }

    if (showColorDialog) {
        ColorPickerDialog(
            initialColor = ballColor,
            onDismissRequest = { showColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setBallColor(selectedColorInt)
                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                showColorDialog = false
            }
        )
    }

    if (showIconColorDialog) {
        ColorPickerDialog(
            initialColor = ballIconColor,
            onDismissRequest = { showIconColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setBallIconColor(selectedColorInt)
                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                showIconColorDialog = false
            }
        )
    }

    if (showMenuColorDialog) {
        ColorPickerDialog(
            initialColor = menuColor,
            onDismissRequest = { showMenuColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setMenuColor(selectedColorInt)
                showMenuColorDialog = false
            }
        )
    }

    if (showMenuIconColorDialog) {
        ColorPickerDialog(
            initialColor = menuIconColor,
            onDismissRequest = { showMenuIconColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setMenuIconColor(selectedColorInt)
                showMenuIconColorDialog = false
            }
        )
    }

    if (showPillColorDialog) {
        ColorPickerDialog(
            initialColor = pillColor,
            onDismissRequest = { showPillColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setPillColor(selectedColorInt)
                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                showPillColorDialog = false
            }
        )
    }

    if (showToastBgColorDialog) {
        ColorPickerDialog(
            initialColor = toastBgColor,
            onDismissRequest = { showToastBgColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setToastBgColor(selectedColorInt)
                showToastBgColorDialog = false
            }
        )
    }

    if (showToastFgColorDialog) {
        ColorPickerDialog(
            initialColor = toastFgColor,
            onDismissRequest = { showToastFgColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setToastFgColor(selectedColorInt)
                showToastFgColorDialog = false
            }
        )
    }

    activeGestureBottomSheet?.let { gesture ->
        val (title, currentAction, onSelect) = when (gesture) {
            PillGesture.DOUBLE_TAP -> Triple(
                stringResource(gesture.titleRes),
                doubleTapAction
            ) { action: MenuAction ->
                viewModel.setDoubleTapAction(action.name)
            }

            PillGesture.TRIPLE_TAP -> Triple(
                stringResource(gesture.titleRes),
                tripleTapAction
            ) { action: MenuAction ->
                viewModel.setTripleTapAction(action.name)
            }

            PillGesture.LONG_PRESS -> Triple(
                stringResource(gesture.titleRes),
                longPressAction
            ) { action: MenuAction ->
                viewModel.setLongPressAction(action.name)
            }

            PillGesture.SWIPE_UP -> Triple(
                stringResource(gesture.titleRes),
                swipeUpAction
            ) { action: MenuAction ->
                viewModel.setSwipeUpAction(action.name)
            }

            PillGesture.SWIPE_DOWN -> Triple(
                stringResource(gesture.titleRes),
                swipeDownAction
            ) { action: MenuAction ->
                viewModel.setSwipeDownAction(action.name)
            }
        }

        GestureActionBottomSheet(
            title = title,
            currentActionName = currentAction,
            onActionSelected = onSelect,
            onDismissRequest = { activeGestureBottomSheet = null }
        )
    }
}

private fun Context.controlQuickBallService(action: String) {
    startService(
        Intent(this, QuickBallService::class.java).apply {
            this.action = action
        }
    )
}
