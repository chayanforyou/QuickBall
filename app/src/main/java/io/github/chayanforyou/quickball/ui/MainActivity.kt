package io.github.chayanforyou.quickball.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.chayanforyou.quickball.ui.navigation.Screen
import io.github.chayanforyou.quickball.ui.screens.settings.AdvancedSettingsScreen
import io.github.chayanforyou.quickball.ui.screens.autohide.AutoHideSettingsScreen
import io.github.chayanforyou.quickball.ui.screens.home.HomeScreen
import io.github.chayanforyou.quickball.ui.screens.onboarding.OnboardingScreen
import io.github.chayanforyou.quickball.ui.screens.shortcut.SelectAppsScreen
import io.github.chayanforyou.quickball.ui.screens.shortcut.SelectShortcutScreen
import io.github.chayanforyou.quickball.ui.screens.shortcut.ShortcutMenuScreen
import io.github.chayanforyou.quickball.ui.theme.AppTheme
import io.github.chayanforyou.quickball.ui.viewmodels.QuickBallViewModel
import io.github.chayanforyou.quickball.utils.LanguageUtils

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtils.getLocalizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                val navController = rememberNavController()
                val viewModel: QuickBallViewModel = viewModel()
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
                val targetIndex by viewModel.targetIndex.collectAsState()

                val startDestination = remember {
                    if (isOnboardingCompleted) Screen.Home.name else Screen.Onboarding.name
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    composable(Screen.Onboarding.name) {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                viewModel.setOnboardingCompleted(true)
                                navController.navigate(Screen.Home.name) {
                                    popUpTo(Screen.Onboarding.name) { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    composable(Screen.Home.name) {
                        HomeScreen(
                            onNavigateToShortcuts = { navController.navigate(Screen.ShortcutMenu.name) },
                            onNavigateToAutoHide = { navController.navigate(Screen.AutoHideSettings.name) },
                            onNavigateToAdvanced = { navController.navigate(Screen.AdvancedSettings.name) },
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel
                        )
                    }

                    composable(Screen.ShortcutMenu.name) {
                        ShortcutMenuScreen(
                            onNavigateToSelection = { index ->
                                viewModel.setTargetIndex(index)
                                navController.navigate(Screen.SelectShortcut.name)
                            },
                            onNavigateBack = { navController.navigateUp() },
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel
                        )
                    }

                    composable(Screen.SelectShortcut.name) {
                        SelectShortcutScreen(
                            targetIndex = targetIndex,
                            onNavigateToApps = {
                                navController.navigate(Screen.SelectApps.name)
                            },
                            onNavigateBack = { navController.navigateUp() },
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel
                        )
                    }

                    composable(Screen.SelectApps.name) {
                        SelectAppsScreen(
                            targetIndex = targetIndex,
                            onNavigateBack = { navController.navigateUp() },
                            onAppSelected = {
                                navController.popBackStack(Screen.ShortcutMenu.name, inclusive = false)
                            },
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel
                        )
                    }

                    composable(Screen.AutoHideSettings.name) {
                        AutoHideSettingsScreen(
                            onNavigateBack = { navController.navigateUp() },
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel
                        )
                    }

                    composable(Screen.AdvancedSettings.name) {
                        AdvancedSettingsScreen(
                            onNavigateBack = { navController.navigateUp() },
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}