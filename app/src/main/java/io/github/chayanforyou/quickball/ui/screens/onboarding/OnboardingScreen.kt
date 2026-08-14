package io.github.chayanforyou.quickball.ui.screens.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.ui.screens.onboarding.components.PermissionCard
import io.github.chayanforyou.quickball.utils.PermissionUtils

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }

    var isAccessibilityGranted by remember {
        mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context))
    }
    var canWriteSettings by remember {
        mutableStateOf(PermissionUtils.canModifySystemSettings(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = PermissionUtils.isAccessibilityServiceEnabled(context)
                canWriteSettings = PermissionUtils.canModifySystemSettings(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            ) {
                repeat(2) { pageIndex ->
                    val isActive = pageIndex == currentPage
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isActive) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Screen Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (currentPage == 0) {
                    WelcomePageContent(
                        onNextPage = { currentPage = 1 }
                    )
                } else {
                    PermissionsPageContent(
                        isAccessibilityGranted = isAccessibilityGranted,
                        canWriteSettings = canWriteSettings,
                        onGrantAccessibility = { PermissionUtils.openAccessibilitySettings(context) },
                        onGrantSystemSettings = { PermissionUtils.openSystemSettingsPermission(context) },
                        onBack = { currentPage = 0 },
                        onStartUsing = onOnboardingComplete
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePageContent(
    onNextPage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BulletItem(text = stringResource(R.string.onboarding_bullet_1))
            BulletItem(text = stringResource(R.string.onboarding_bullet_2))
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNextPage,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_get_started),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun PermissionsPageContent(
    isAccessibilityGranted: Boolean,
    canWriteSettings: Boolean,
    onGrantAccessibility: () -> Unit,
    onGrantSystemSettings: () -> Unit,
    onBack: () -> Unit,
    onStartUsing: () -> Unit
) {
    val areAllPermissionsGranted = isAccessibilityGranted && canWriteSettings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_permissions_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Accessibility Permission Card
            PermissionCard(
                title = stringResource(R.string.onboarding_accessibility_title),
                description = stringResource(R.string.onboarding_accessibility_description),
                isGranted = isAccessibilityGranted,
                onGrantClick = onGrantAccessibility
            )

            // System Settings Permission Card
            PermissionCard(
                title = stringResource(R.string.system_settings_title),
                description = stringResource(R.string.onboarding_system_settings_description),
                isGranted = canWriteSettings,
                onGrantClick = onGrantSystemSettings
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        if (!areAllPermissionsGranted) {
            Text(
                text = stringResource(R.string.onboarding_permissions_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Bottom Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(
                    text = stringResource(R.string.onboarding_back),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onStartUsing,
                enabled = areAllPermissionsGranted,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    disabledContentColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = stringResource(R.string.onboarding_start_using),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
