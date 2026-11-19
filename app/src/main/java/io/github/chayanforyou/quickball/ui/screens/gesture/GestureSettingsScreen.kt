package io.github.chayanforyou.quickball.ui.screens.gesture

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.PillGesture
import io.github.chayanforyou.quickball.ui.components.AppSwitch
import io.github.chayanforyou.quickball.ui.screens.settings.components.GestureActionBottomSheet
import io.github.chayanforyou.quickball.ui.screens.settings.components.GestureSettingRow
import io.github.chayanforyou.quickball.ui.theme.AppCardDefaults
import io.github.chayanforyou.quickball.ui.viewmodels.QuickBallViewModel
import androidx.compose.ui.platform.LocalContext
import io.github.chayanforyou.quickball.domain.models.HapticIntensity
import io.github.chayanforyou.quickball.ui.screens.settings.components.SegmentedSelector
import io.github.chayanforyou.quickball.utils.performHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickBallViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isGestureEnabled = uiState.isGestureEnabled
    val singleTapAction = uiState.singleTapAction
    val doubleTapAction = uiState.doubleTapAction
    val tripleTapAction = uiState.tripleTapAction
    val longPressAction = uiState.longPressAction
    val swipeUpAction = uiState.swipeUpAction
    val swipeDownAction = uiState.swipeDownAction
    val isHapticFeedbackEnabled = uiState.isHapticFeedbackEnabled
    val hapticIntensity = uiState.hapticIntensity

    var activeGestureBottomSheet by remember { mutableStateOf<PillGesture?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.gestures_haptics_title),
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
            // Gesture Settings Header
            Text(
                text = stringResource(R.string.gesture_header_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Enable Gestures Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.enable_gestures_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        AppSwitch(
                            checked = isGestureEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setGestureEnabled(enabled)
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Single tap
                    GestureSettingRow(
                        title = stringResource(R.string.single_tap_title),
                        actionName = singleTapAction,
                        enabled = isGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.SINGLE_TAP }
                    )

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
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.vibration_feedback_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        AppSwitch(
                            checked = isHapticFeedbackEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setHapticFeedbackEnabled(enabled)
                                if (enabled) context.performHapticFeedback()
                            }
                        )
                    }

                    SegmentedSelector(
                        items = HapticIntensity.entries,
                        selectedItem = HapticIntensity.entries.find { it.name == hapticIntensity },
                        itemTitle = { stringResource(it.titleRes) },
                        enabled = isHapticFeedbackEnabled,
                        onItemSelected = { intensity ->
                            viewModel.setHapticIntensity(intensity.name)
                            context.performHapticFeedback()
                        }
                    )
                }
            }
        }
    }

    activeGestureBottomSheet?.let { gesture ->
        val (title, currentAction, onSelect) = when (gesture) {
            PillGesture.SINGLE_TAP -> Triple(
                stringResource(gesture.titleRes),
                singleTapAction
            ) { action: MenuAction ->
                viewModel.setSingleTapAction(action.name)
            }

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
