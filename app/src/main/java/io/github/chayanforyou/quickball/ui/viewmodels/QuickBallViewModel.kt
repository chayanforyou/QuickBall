package io.github.chayanforyou.quickball.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.chayanforyou.quickball.domain.AppPreference
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickBallUiState(
    val isQuickBallEnabled: Boolean = true,
    val showOnLockScreen: Boolean = false,
    val hideOnLandscape: Boolean = false,
    val stickToEdge: Boolean = true,
    val ballSize: Float = 48f,
    val isAccessibilityGranted: Boolean = false,
    val selectedMenuItems: List<QuickBallMenuItem> = emptyList(),
    val autoHideApps: Set<String> = emptySet()
)

class QuickBallViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreference.getInstance(application)

    private val _uiState = MutableStateFlow(
        QuickBallUiState(
            isQuickBallEnabled = prefs.isQuickBallEnabled,
            showOnLockScreen = prefs.isShowOnLockScreenEnabled,
            hideOnLandscape = prefs.isHideOnLandscapeEnabled,
            stickToEdge = prefs.isStickToEdgeEnabled,
            ballSize = prefs.ballSize,
            selectedMenuItems = prefs.selectedMenuItems,
            autoHideApps = prefs.autoHideApps
        )
    )
    val uiState: StateFlow<QuickBallUiState> = _uiState.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.isOnboardingCompleted)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _targetIndex = MutableStateFlow(0)
    val targetIndex: StateFlow<Int> = _targetIndex.asStateFlow()

    fun setTargetIndex(index: Int) {
        _targetIndex.value = index
    }

    fun refreshPermissionsState(isAccessibilityGranted: Boolean) {
        _uiState.update {
            it.copy(
                isAccessibilityGranted = isAccessibilityGranted,
                isQuickBallEnabled = prefs.isQuickBallEnabled
            )
        }
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.isOnboardingCompleted = completed
        _isOnboardingCompleted.value = completed
    }

    fun setQuickBallEnabled(enabled: Boolean) {
        prefs.isQuickBallEnabled = enabled
        _uiState.update { it.copy(isQuickBallEnabled = enabled) }
    }

    fun setShowOnLockScreen(enabled: Boolean) {
        prefs.isShowOnLockScreenEnabled = enabled
        _uiState.update { it.copy(showOnLockScreen = enabled) }
    }

    fun setHideOnLandscape(enabled: Boolean) {
        prefs.isHideOnLandscapeEnabled = enabled
        _uiState.update { it.copy(hideOnLandscape = enabled) }
    }

    fun setStickToEdge(enabled: Boolean) {
        prefs.isStickToEdgeEnabled = enabled
        _uiState.update { it.copy(stickToEdge = enabled) }
    }

    fun setBallSize(size: Float) {
        prefs.ballSize = size
        _uiState.update { it.copy(ballSize = size) }
    }

    fun loadSelectedMenuItems() {
        viewModelScope.launch {
            val items = prefs.selectedMenuItems
            _uiState.update { it.copy(selectedMenuItems = items) }
        }
    }

    fun selectMenuItem(index: Int, menuItem: QuickBallMenuItem) {
        val currentItems = _uiState.value.selectedMenuItems.toMutableList()
        if (index in currentItems.indices) {
            currentItems[index] = menuItem
            prefs.selectedMenuItems = currentItems
            _uiState.update { it.copy(selectedMenuItems = currentItems) }
        }
    }

    fun updateMenuItems(newItems: List<QuickBallMenuItem>) {
        prefs.selectedMenuItems = newItems
        _uiState.update { it.copy(selectedMenuItems = newItems) }
    }

    fun setAutoHideApps(apps: Set<String>) {
        prefs.autoHideApps = apps
        _uiState.update { it.copy(autoHideApps = apps) }
    }

    fun toggleAutoHideApp(packageName: String) {
        val currentSet = _uiState.value.autoHideApps.toMutableSet()
        if (currentSet.contains(packageName)) {
            currentSet.remove(packageName)
        } else {
            currentSet.add(packageName)
        }
        setAutoHideApps(currentSet)
    }
}
