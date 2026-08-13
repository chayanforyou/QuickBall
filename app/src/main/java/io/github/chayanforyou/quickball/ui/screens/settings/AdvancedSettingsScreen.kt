package io.github.chayanforyou.quickball.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.core.QuickBallService
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.PillGesture
import io.github.chayanforyou.quickball.ui.screens.settings.components.ColorPickerDialog
import io.github.chayanforyou.quickball.ui.screens.settings.components.GestureActionBottomSheet
import io.github.chayanforyou.quickball.ui.screens.settings.components.GestureSettingRow
import io.github.chayanforyou.quickball.ui.screens.settings.components.HapticIntensitySelector
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
    val menuColor = uiState.menuColor
    val menuIconColor = uiState.menuIconColor
    val pillColor = uiState.pillColor
    val pillHeight = uiState.pillHeight
    val pillThickness = uiState.pillThickness
    val pillTouchWidth = uiState.pillTouchWidth
    val pillArcAngle = uiState.pillArcAngle
    val isPillGestureEnabled = uiState.isPillGestureEnabled
    val pillDoubleTapAction = uiState.pillDoubleTapAction
    val pillTripleTapAction = uiState.pillTripleTapAction
    val pillLongPressAction = uiState.pillLongPressAction
    val pillSwipeUpAction = uiState.pillSwipeUpAction
    val pillSwipeDownAction = uiState.pillSwipeDownAction
    val isHapticFeedbackEnabled = uiState.isHapticFeedbackEnabled
    val hapticIntensity = uiState.hapticIntensity

    var showColorDialog by remember { mutableStateOf(false) }
    var showIconColorDialog by remember { mutableStateOf(false) }
    var showMenuColorDialog by remember { mutableStateOf(false) }
    var showMenuIconColorDialog by remember { mutableStateOf(false) }
    var showPillColorDialog by remember { mutableStateOf(false) }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ball_color_title),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showColorDialog = true }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.resetBallColor()
                                    context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(ballColor))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { showColorDialog = true }
                            )
                        }
                    }

                    // Ball Icon Color Setting Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ball_icon_color_title),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showIconColorDialog = true }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.resetBallIconColor()
                                    context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(ballIconColor))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { showIconColorDialog = true }
                            )
                        }
                    }

                    // Ball Size Setting Option
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.ball_size_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.resetBallSize()
                                        context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                        contentDescription = stringResource(R.string.reset_to_default),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${ballSize.toInt()} dp",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = ballSize,
                            onValueChange = { value ->
                                viewModel.setBallSize(value)
                                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                            },
                            steps = 19,
                            valueRange = 40f..60f
                        )
                    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.menu_color_title),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showMenuColorDialog = true }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.resetMenuColor()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(menuColor))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { showMenuColorDialog = true }
                            )
                        }
                    }

                    // Menu Icon Color Setting Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.menu_icon_color_title),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showMenuIconColorDialog = true }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.resetMenuIconColor()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(menuIconColor))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { showMenuIconColorDialog = true }
                            )
                        }
                    }

                    // Menu Size Setting Option
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.menu_size_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.resetMenuSize()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                        contentDescription = stringResource(R.string.reset_to_default),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${menuSize.toInt()} dp",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = menuSize,
                            onValueChange = { value ->
                                viewModel.setMenuSize(value)
                            },
                            steps = 19,
                            valueRange = 40f..60f
                        )
                    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.pill_color_title),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showPillColorDialog = true }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.resetPillColor()
                                    context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(pillColor))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { showPillColorDialog = true }
                            )
                        }
                    }

                    // Pill Height Setting Option
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.pill_height_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.resetPillHeight()
                                        context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                        contentDescription = stringResource(R.string.reset_to_default),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${pillHeight.toInt()} dp",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = pillHeight,
                            onValueChange = { value ->
                                viewModel.setPillHeight(value)
                                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                            },
                            steps = 49,
                            valueRange = 30f..80f
                        )
                    }

                    // Pill Thickness Setting Option
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.pill_thickness_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.resetPillThickness()
                                        context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                        contentDescription = stringResource(R.string.reset_to_default),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${pillThickness.toInt()} dp",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = pillThickness,
                            onValueChange = { value ->
                                viewModel.setPillThickness(value)
                                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                            },
                            steps = 8,
                            valueRange = 1f..10f
                        )
                    }

                    // Touch Area Width Setting Option
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.pill_touch_width_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.resetPillTouchWidth()
                                        context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                        contentDescription = stringResource(R.string.reset_to_default),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${pillTouchWidth.toInt()} dp",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = pillTouchWidth,
                            onValueChange = { value ->
                                viewModel.setPillTouchWidth(value)
                                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                            },
                            steps = 34,
                            valueRange = 15f..50f
                        )
                    }

                    /*
                    // Pill Arc Angle Setting Option
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.pill_arc_angle_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.resetPillArcAngle()
                                        controlQuickBallService(
                                            context,
                                            QuickBallService.ACTION_UPDATE_PILL
                                        )
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                        contentDescription = stringResource(R.string.reset_to_default),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${pillArcAngle.toInt()}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = pillArcAngle,
                            onValueChange = { value ->
                                viewModel.setPillArcAngle(value)
                                controlQuickBallService(
                                    context,
                                    QuickBallService.ACTION_UPDATE_PILL
                                )
                            },
                            steps = 69,
                            valueRange = 0f..70f
                        )
                    }
                    */
                }
            }

            // Gesture Settings Header & Card
            Text(
                text = stringResource(R.string.gesture_header_title),
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
                            checked = isPillGestureEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setPillGestureEnabled(enabled)
                                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Double tap
                    GestureSettingRow(
                        title = stringResource(R.string.double_tap_title),
                        actionName = pillDoubleTapAction,
                        enabled = isPillGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.DOUBLE_TAP }
                    )

                    // Triple tap
                    GestureSettingRow(
                        title = stringResource(R.string.triple_tap_title),
                        actionName = pillTripleTapAction,
                        enabled = isPillGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.TRIPLE_TAP }
                    )

                    // Long press
                    GestureSettingRow(
                        title = stringResource(R.string.long_press_title),
                        actionName = pillLongPressAction,
                        enabled = isPillGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.LONG_PRESS }
                    )

                    // Swipe up
                    GestureSettingRow(
                        title = stringResource(R.string.swipe_up_title),
                        actionName = pillSwipeUpAction,
                        enabled = isPillGestureEnabled,
                        onClick = { activeGestureBottomSheet = PillGesture.SWIPE_UP }
                    )

                    // Swipe down
                    GestureSettingRow(
                        title = stringResource(R.string.swipe_down_title),
                        actionName = pillSwipeDownAction,
                        enabled = isPillGestureEnabled,
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

    activeGestureBottomSheet?.let { gesture ->
        val (title, currentAction, onSelect) = when (gesture) {
            PillGesture.DOUBLE_TAP -> Triple(
                stringResource(gesture.titleRes),
                pillDoubleTapAction,
                { action: MenuAction ->
                    viewModel.setPillDoubleTapAction(action.name)
                    context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                }
            )
            PillGesture.TRIPLE_TAP -> Triple(
                stringResource(gesture.titleRes),
                pillTripleTapAction,
                { action: MenuAction ->
                    viewModel.setPillTripleTapAction(action.name)
                    context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                }
            )
            PillGesture.LONG_PRESS -> Triple(
                stringResource(gesture.titleRes),
                pillLongPressAction,
                { action: MenuAction ->
                    viewModel.setPillLongPressAction(action.name)
                    context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                }
            )
            PillGesture.SWIPE_UP -> Triple(
                stringResource(gesture.titleRes),
                pillSwipeUpAction,
                { action: MenuAction ->
                    viewModel.setPillSwipeUpAction(action.name)
                    context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                }
            )
            PillGesture.SWIPE_DOWN -> Triple(
                stringResource(gesture.titleRes),
                pillSwipeDownAction,
                { action: MenuAction ->
                    viewModel.setPillSwipeDownAction(action.name)
                    context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                }
            )
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

