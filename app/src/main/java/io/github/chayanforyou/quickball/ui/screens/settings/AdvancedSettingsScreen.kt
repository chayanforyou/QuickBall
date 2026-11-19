package io.github.chayanforyou.quickball.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.core.QuickBallService
import io.github.chayanforyou.quickball.domain.AppDefaults
import io.github.chayanforyou.quickball.domain.models.MenuConnectorStyle
import io.github.chayanforyou.quickball.domain.models.QuickBallTheme
import io.github.chayanforyou.quickball.ui.components.AppSwitch
import io.github.chayanforyou.quickball.ui.screens.settings.components.ColorPickerDialog
import io.github.chayanforyou.quickball.ui.screens.settings.components.ColorSettingRow
import io.github.chayanforyou.quickball.ui.screens.settings.components.ConnectorStyleBottomSheet
import io.github.chayanforyou.quickball.ui.screens.settings.components.ConnectorStyleRow
import io.github.chayanforyou.quickball.ui.screens.settings.components.SliderSettingItem
import io.github.chayanforyou.quickball.ui.screens.settings.components.ThemeBottomSheet
import io.github.chayanforyou.quickball.ui.screens.settings.components.ThemePreviewColors
import io.github.chayanforyou.quickball.ui.screens.settings.components.ThemeRow
import io.github.chayanforyou.quickball.ui.theme.AppCardDefaults
import io.github.chayanforyou.quickball.ui.viewmodels.QuickBallViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val isBallBorderEnabled = uiState.isBallBorderEnabled
    val ballBorderColor = uiState.ballBorderColor
    val ballBorderWidth = uiState.ballBorderWidth
    val isMenuBorderEnabled = uiState.isMenuBorderEnabled
    val menuBorderColor = uiState.menuBorderColor
    val menuBorderWidth = uiState.menuBorderWidth
    val menuConnectorColor = uiState.menuConnectorColor
    val menuConnectorStyle = uiState.menuConnectorStyle
    val menuConnectorWidth = uiState.menuConnectorWidth
    val toastBgColor = uiState.toastBgColor
    val toastFgColor = uiState.toastFgColor
    val pillColor = uiState.pillColor
    val pillHeight = uiState.pillHeight
    val pillThickness = uiState.pillThickness
    val pillTouchWidth = uiState.pillTouchWidth

    // Null once any color was changed by hand
    val selectedTheme = QuickBallTheme.matching(
        ballColor = ballColor,
        ballIconColor = ballIconColor,
        ballBorderColor = ballBorderColor,
        menuColor = menuColor,
        menuIconColor = menuIconColor,
        menuBorderColor = menuBorderColor,
        connectorColor = menuConnectorColor,
        pillColor = pillColor,
        toastBgColor = toastBgColor,
        toastFgColor = toastFgColor
    )
    val currentThemeColors = ThemePreviewColors(
        ballColor = ballColor,
        ballIconColor = ballIconColor,
        ballBorderColor = ballBorderColor.takeIf { uiState.isBallBorderEnabled },
        menuColor = menuColor,
        menuIconColor = menuIconColor,
        menuBorderColor = menuBorderColor.takeIf { uiState.isMenuBorderEnabled },
        connectorColor = menuConnectorColor
    )

    var showColorDialog by remember { mutableStateOf(false) }
    var showIconColorDialog by remember { mutableStateOf(false) }
    var showMenuColorDialog by remember { mutableStateOf(false) }
    var showMenuIconColorDialog by remember { mutableStateOf(false) }
    var showBallBorderColorDialog by remember { mutableStateOf(false) }
    var showMenuBorderColorDialog by remember { mutableStateOf(false) }
    var showMenuConnectorColorDialog by remember { mutableStateOf(false) }
    var showConnectorStyleSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showPillColorDialog by remember { mutableStateOf(false) }
    var showToastBgColorDialog by remember { mutableStateOf(false) }
    var showToastFgColorDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = viewModel.exportThemeConfig()
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(json.toByteArray())
                    } ?: error("No output stream")
                }.isSuccess
            }
            val message = if (saved) R.string.settings_exported else R.string.settings_export_failed
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching {
                    if (!context.fileName(uri).endsWith(".qbtheme", ignoreCase = true)) return@runCatching null
                    context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                }.getOrNull()
            }
            val imported = json != null && viewModel.importThemeConfig(json)
            if (imported) {
                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
            }
            val message = if (imported) R.string.settings_imported else R.string.settings_import_failed
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

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
                },
                actions = {
                    Box {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(stringResource(R.string.import_export_settings)) } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.import_export_settings)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_settings)) },
                                leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    importLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_settings)) },
                                leadingIcon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    exportLauncher.launch("quickball.qbtheme")
                                }
                            )
                        }
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
            // Theme Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                ThemeRow(
                    title = stringResource(R.string.theme_title),
                    selectedTheme = selectedTheme,
                    currentColors = currentThemeColors,
                    connectorStyleName = menuConnectorStyle,
                    onReset = {
                        viewModel.applyTheme(QuickBallTheme.DEFAULT)
                        context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                        context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
                    },
                    onClick = { showThemeSheet = true },
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Connector Settings Header & Card
            Text(
                text = stringResource(R.string.connector_section_title),
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
                    // Menu Connector Style Setting Option
                    ConnectorStyleRow(
                        title = stringResource(R.string.setting_style_title),
                        selectedStyleName = menuConnectorStyle,
                        onReset = { viewModel.resetMenuConnectorStyle() },
                        onClick = { showConnectorStyleSheet = true }
                    )

                    // Menu Connector Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.setting_color_title),
                        color = menuConnectorColor,
                        onReset = { viewModel.resetMenuConnectorColor() },
                        onClick = { showMenuConnectorColorDialog = true }
                    )

                    // Menu Connector Width Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.setting_width_title),
                        value = menuConnectorWidth,
                        valueRange = AppDefaults.MENU_CONNECTOR_WIDTH_MIN..AppDefaults.MENU_CONNECTOR_WIDTH_MAX,
                        steps = 19,
                        decimals = 1,
                        enabled = menuConnectorStyle != MenuConnectorStyle.NONE.name,
                        onValueChange = { viewModel.setMenuConnectorWidth(it) },
                        onReset = { viewModel.resetMenuConnectorWidth() }
                    )
                }
            }

            // Ball Settings Header & Card
            Text(
                text = stringResource(R.string.ball_header_title),
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
                    // Ball Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.setting_color_title),
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
                        valueRange = AppDefaults.BALL_SIZE_MIN..AppDefaults.BALL_SIZE_MAX,
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

                    SettingsSubHeader(title = stringResource(R.string.border_section_title)) {
                        AppSwitch(
                            checked = isBallBorderEnabled,
                            scale = 0.65f,
                            onCheckedChange = { enabled ->
                                viewModel.setBallBorderEnabled(enabled)
                                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                            }
                        )
                    }

                    // Ball Border Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.setting_color_title),
                        color = ballBorderColor,
                        enabled = isBallBorderEnabled,
                        onReset = {
                            viewModel.resetBallBorderColor()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                        },
                        onClick = { showBallBorderColorDialog = true }
                    )

                    // Ball Border Width Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.setting_width_title),
                        value = ballBorderWidth,
                        valueRange = AppDefaults.BORDER_WIDTH_MIN..AppDefaults.BORDER_WIDTH_MAX,
                        steps = 5,
                        decimals = 1,
                        enabled = isBallBorderEnabled,
                        onValueChange = { value ->
                            viewModel.setBallBorderWidth(value)
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                        },
                        onReset = {
                            viewModel.resetBallBorderWidth()
                            context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                        }
                    )
                }
            }

            // Menu Settings Header & Card
            Text(
                text = stringResource(R.string.menu_header_title),
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
                    // Menu Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.setting_color_title),
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
                        valueRange = AppDefaults.MENU_SIZE_MIN..AppDefaults.MENU_SIZE_MAX,
                        steps = 15,
                        onValueChange = { viewModel.setMenuSize(it) },
                        onReset = { viewModel.resetMenuSize() }
                    )

                    // Menu Icon Size Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.menu_icon_size_title),
                        value = menuIconSize,
                        valueRange = AppDefaults.MENU_ICON_SIZE_MIN..AppDefaults.MENU_ICON_SIZE_MAX,
                        steps = 19,
                        onValueChange = { viewModel.setMenuIconSize(it) },
                        onReset = { viewModel.resetMenuIconSize() }
                    )

                    // Menu Radius Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.menu_radius_title),
                        value = menuRadius,
                        valueRange = AppDefaults.MENU_RADIUS_MIN..AppDefaults.MENU_RADIUS_MAX,
                        steps = 59,
                        onValueChange = { viewModel.setMenuRadius(it) },
                        onReset = { viewModel.resetMenuRadius() }
                    )

                    SettingsSubHeader(title = stringResource(R.string.border_section_title)) {
                        AppSwitch(
                            checked = isMenuBorderEnabled,
                            scale = 0.65f,
                            onCheckedChange = { viewModel.setMenuBorderEnabled(it) }
                        )
                    }

                    // Menu Border Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.setting_color_title),
                        color = menuBorderColor,
                        enabled = isMenuBorderEnabled,
                        onReset = { viewModel.resetMenuBorderColor() },
                        onClick = { showMenuBorderColorDialog = true }
                    )

                    // Menu Border Width Setting Option
                    SliderSettingItem(
                        title = stringResource(R.string.setting_width_title),
                        value = menuBorderWidth,
                        valueRange = AppDefaults.BORDER_WIDTH_MIN..AppDefaults.BORDER_WIDTH_MAX,
                        steps = 5,
                        decimals = 1,
                        enabled = isMenuBorderEnabled,
                        onValueChange = { viewModel.setMenuBorderWidth(it) },
                        onReset = { viewModel.resetMenuBorderWidth() }
                    )
                }
            }

            // Pill Settings Header & Card
            Text(
                text = stringResource(R.string.pill_header_title),
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
                    // Pill Color Setting Option
                    ColorSettingRow(
                        title = stringResource(R.string.setting_color_title),
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
                        valueRange = AppDefaults.PILL_HEIGHT_MIN..AppDefaults.PILL_HEIGHT_MAX,
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
                        valueRange = AppDefaults.PILL_THICKNESS_MIN..AppDefaults.PILL_THICKNESS_MAX,
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
                        valueRange = AppDefaults.PILL_TOUCH_WIDTH_MIN..AppDefaults.PILL_TOUCH_WIDTH_MAX,
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

    if (showBallBorderColorDialog) {
        ColorPickerDialog(
            initialColor = ballBorderColor,
            onDismissRequest = { showBallBorderColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setBallBorderColor(selectedColorInt)
                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                showBallBorderColorDialog = false
            }
        )
    }

    if (showMenuBorderColorDialog) {
        ColorPickerDialog(
            initialColor = menuBorderColor,
            onDismissRequest = { showMenuBorderColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setMenuBorderColor(selectedColorInt)
                showMenuBorderColorDialog = false
            }
        )
    }

    if (showThemeSheet) {
        ThemeBottomSheet(
            title = stringResource(R.string.theme_title),
            selectedTheme = selectedTheme,
            connectorStyleName = menuConnectorStyle,
            onThemeSelected = { theme ->
                viewModel.applyTheme(theme)
                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_BALL)
                context.controlQuickBallService(QuickBallService.ACTION_UPDATE_PILL)
            },
            onDismissRequest = { showThemeSheet = false }
        )
    }

    if (showConnectorStyleSheet) {
        ConnectorStyleBottomSheet(
            title = stringResource(R.string.connector_section_title),
            selectedStyleName = menuConnectorStyle,
            onStyleSelected = { viewModel.setMenuConnectorStyle(it.name) },
            onDismissRequest = { showConnectorStyleSheet = false }
        )
    }

    if (showMenuConnectorColorDialog) {
        ColorPickerDialog(
            initialColor = menuConnectorColor,
            onDismissRequest = { showMenuConnectorColorDialog = false },
            onColorSelected = { selectedColorInt ->
                viewModel.setMenuConnectorColor(selectedColorInt)
                showMenuConnectorColorDialog = false
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
}

@Composable
private fun SettingsSubHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            trailing?.invoke()
        }
    }
}

private fun Context.fileName(uri: Uri): String {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }.orEmpty()
}

private fun Context.controlQuickBallService(action: String) {
    startService(
        Intent(this, QuickBallService::class.java).apply {
            this.action = action
        }
    )
}
