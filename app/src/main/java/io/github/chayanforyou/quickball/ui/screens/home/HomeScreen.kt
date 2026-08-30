package io.github.chayanforyou.quickball.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chayanforyou.quickball.BuildConfig
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.core.QuickBallService
import io.github.chayanforyou.quickball.ui.screens.home.components.AppRatingDialog
import io.github.chayanforyou.quickball.ui.screens.home.components.DokiBottomSheet
import io.github.chayanforyou.quickball.ui.screens.home.components.LanguageSelectionBottomSheet
import io.github.chayanforyou.quickball.ui.screens.home.components.SettingNavigationRow
import io.github.chayanforyou.quickball.ui.screens.home.components.SettingSwitchRow
import io.github.chayanforyou.quickball.ui.theme.AppCardDefaults
import io.github.chayanforyou.quickball.ui.theme.CookieFontFamily
import io.github.chayanforyou.quickball.ui.viewmodels.QuickBallViewModel
import io.github.chayanforyou.quickball.utils.AppRater
import io.github.chayanforyou.quickball.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToShortcuts: () -> Unit,
    onNavigateToAutoHide: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickBallViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showDokiSheet by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppRater.initAppRater(context) {
            showRateDialog = true
        }
    }

    if (showLanguageSheet) {
        LanguageSelectionBottomSheet(onDismissRequest = {
            showLanguageSheet = false
        })
    }

    if (showDokiSheet) {
        DokiBottomSheet(onDismissRequest = {
            showDokiSheet = false
        })
    }

    if (showRateDialog) {
        AppRatingDialog(
            onDismissRequest = {
                showRateDialog = false
                AppRater.remindLater(context)
            },
            onRate = {
                showRateDialog = false
                AppRater.openPlayStore(context)
            },
            onEmail = {
                showRateDialog = false
                AppRater.openEmail(context)
            }
        )
    }

    val isAccessibilityGranted = uiState.isAccessibilityGranted
    val isQuickBallEnabled = uiState.isQuickBallEnabled
    val lockBallPosition = uiState.lockBallPosition
    val showOnLockScreen = uiState.showOnLockScreen
    val hideOnLandscape = uiState.hideOnLandscape
    val stickToEdge = uiState.stickToEdge

    fun refreshPermissionsState() {
        val hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
        viewModel.refreshPermissionsState(hasAccessibility)

        val action = if (hasAccessibility && isQuickBallEnabled) {
            QuickBallService.ACTION_ENABLE
        } else {
            QuickBallService.ACTION_DISABLE
        }
        controlQuickBallService(context, action)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionsState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { showLanguageSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_language),
                            contentDescription = stringResource(R.string.language_title)
                        )
                    }
                    IconButton(onClick = { showDokiSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_battery_restriction),
                            contentDescription = stringResource(R.string.remove_battery_restriction)
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/chayanforyou/QuickBall".toUri()
                        )
                        context.startActivity(intent)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = stringResource(R.string.github_repository)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = AppCardDefaults.cardColors()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://chayanforyou.gumroad.com/coffee".toUri()
                                )
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_coffee),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    text = stringResource(R.string.support_me),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = CookieFontFamily,
                                        fontSize = 14.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Accessibility Setup Card (shown at top if accessibility is OFF)
            if (!isAccessibilityGranted) {
                AccessibilitySetupCard(
                    onOpenSettingsClick = { PermissionUtils.openAccessibilitySettings(context) }
                )
            }

            // Main Service Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.enable_quick_ball),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isQuickBallEnabled,
                        enabled = isAccessibilityGranted,
                        onCheckedChange = { checked ->
                            viewModel.setQuickBallEnabled(checked)
                            controlQuickBallService(
                                context,
                                if (checked) QuickBallService.ACTION_ENABLE else QuickBallService.ACTION_DISABLE
                            )
                        }
                    )
                }
            }

            // Standalone "Settings" Header
            Text(
                text = stringResource(R.string.settings_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingNavigationRow(
                        title = stringResource(R.string.shortcuts_title),
                        onClick = onNavigateToShortcuts
                    )

                    SettingNavigationRow(
                        title = stringResource(R.string.hide_automatically_title),
                        subtitle = stringResource(R.string.hide_automatically_description),
                        onClick = onNavigateToAutoHide
                    )

                    SettingSwitchRow(
                        title = stringResource(R.string.lock_ball_position_title),
                        checked = lockBallPosition,
                        onCheckedChange = { checked ->
                            viewModel.setLockBallPosition(checked)
                        }
                    )

                    SettingSwitchRow(
                        title = stringResource(R.string.stick_to_edge_title),
                        checked = stickToEdge,
                        onCheckedChange = { checked ->
                            viewModel.setStickToEdge(checked)
                            controlQuickBallService(
                                context,
                                if (checked) QuickBallService.ACTION_STASH else QuickBallService.ACTION_UNSTASH
                            )
                        }
                    )

                    SettingSwitchRow(
                        title = stringResource(R.string.show_on_lock_screen_title),
                        checked = showOnLockScreen,
                        onCheckedChange = { checked ->
                            viewModel.setShowOnLockScreen(checked)
                        }
                    )

                    SettingSwitchRow(
                        title = stringResource(R.string.hide_on_landscape_title),
                        checked = hideOnLandscape,
                        onCheckedChange = { checked ->
                            viewModel.setHideOnLandscape(checked)
                        }
                    )
                }
            }

            // Advanced Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = AppCardDefaults.cardColors()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAdvanced() }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.advanced_settings_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessibilitySetupCard(
    onOpenSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = AppCardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.turn_on_quick_ball),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.accessibility_setup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onOpenSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.open_accessibility_settings),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

private fun controlQuickBallService(context: Context, action: String) {
    context.startService(
        Intent(context, QuickBallService::class.java).apply {
            this.action = action
        }
    )
}
